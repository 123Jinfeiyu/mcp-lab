package com.sixzs.lovekb;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.nio.ByteBuffer;

/**
 * 录屏/截图用的前台服务 (foregroundServiceType="mediaProjection")
 *
 * 为什么必须有这个服务(Android 14+ 的硬性要求):
 *   1) MediaProjection 的授权结果(resultCode + resultData) 是一次性"票",
 *      必须在拿到票后立刻换出 MediaProjection 对象;
 *   2) createVirtualDisplay() 必须在「已声明 mediaProjection 类型的前台服务」里调用,
 *      否则系统直接抛:
 *        SecurityException: Must start a foreground service of type mediaProjection
 *                          before creating a VirtualDisplay
 *   3) 录屏要能在后台持续跑, 就必须靠一个常驻的前台服务维持投影会话。
 *
 * 正确顺序(本类严格遵守):
 *   前台服务启动 → startForeground(type=mediaProjection)
 *                → getMediaProjection(resultCode, resultData)   ← 用掉那张一次性票
 *                → registerCallback(onStop 时释放)
 *                → createVirtualDisplay(...)                     ← 必须在服务里
 *
 * 生命周期: 服务常驻(START_STICKY), 投影会话一直活着,
 *            截图时直接从 ImageReader 取一帧, 不需要反复授权。
 */
public class CaptureService extends Service {

    public static final String CH = "lovekb_capture";
    public static final String EXTRA_RESULT_CODE = "rc";
    public static final String EXTRA_RESULT_DATA = "data";

    /** 正在运行的服务实例 */
    public static CaptureService I;

    private MediaProjection projection;
    private MediaProjection.Callback projCallback;
    private ImageReader reader;
    private VirtualDisplay display;
    private int w, h, dpi;

    @Override
    public void onCreate() {
        super.onCreate();
        I = this;
        // 常驻前台通知: 用户可随时看到"正在录屏"
        startForegroundNotification();
        Logx.i("截图服务已启动(mediaProjection 类型)");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Android 12+ 后台启动前台服务会被拒, 这里尽力而为
        try {
            startForegroundNotification();
        } catch (Throwable t) {
            Logx.e("前台通知启动失败", t);
        }
        handleIntent(intent);
        return START_STICKY;
    }

    /** 取授权票并换投影对象 */
    private void handleIntent(Intent intent) {
        // 授权票优先从 Intent 里取(跨进程场景);
        // 取不到就用同进程静态槽位(Activity 与 Service 在同一进程时最可靠)
        int rc = Integer.MIN_VALUE;
        Intent data = null;
        if (intent != null) {
            rc = intent.getIntExtra(EXTRA_RESULT_CODE, Integer.MIN_VALUE);
            try {
                Object o = intent.getExtras() == null ? null
                        : intent.getExtras().get(EXTRA_RESULT_DATA);
                if (o instanceof Intent) data = (Intent) o;
            } catch (Throwable t) {
            }
        }
        if (rc == Integer.MIN_VALUE || data == null) {
            rc = pendingResultCode;
            data = pendingResultData;
        }
        // 注意: Activity.RESULT_OK == -1, 所以不能用 rc < 0 判断失败
        boolean ok = (rc == android.app.Activity.RESULT_OK) && data != null;
        Logx.i("收到授权票 rc=" + rc + ", data=" + (data == null ? "null" : "有")
                + (ok ? " (有效)" : " (无效)"));
        pendingResultCode = Integer.MIN_VALUE;
        pendingResultData = null;
        if (ok) {
            // 一次性票: 在服务里立刻换出投影对象
            createProjection(rc, data);
        }
    }

    /**
     * 同进程静态授权票槽位
     * Activity 与 Service 在同一进程, 用静态字段传最可靠
     * (跨进程传嵌套 Intent 会被系统剥离)
     */
    static volatile int pendingResultCode = Integer.MIN_VALUE;
    static volatile Intent pendingResultData = null;

    /** 放置授权票(供 Service 稍后取用) */
    public static void postTicket(int resultCode, Intent data) {
        pendingResultCode = resultCode;
        pendingResultData = data;
    }

    /** 建立前台通知; Android 10+ 必须指定服务类型 */
    private void startForegroundNotification() {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel c = new NotificationChannel(CH, "屏幕录制",
                        NotificationManager.IMPORTANCE_LOW);
                nm.createNotificationChannel(c);
            }
            Notification.Builder b = (Build.VERSION.SDK_INT >= 26)
                    ? new Notification.Builder(this, CH)
                    : new Notification.Builder(this);
            Notification n = b.setContentTitle("恋爱键盘 · 屏幕录制中")
                    .setContentText("后台维持投影会话, 供截图分析使用")
                    .setSmallIcon(android.R.drawable.ic_menu_edit)
                    .setOngoing(true)
                    .build();
            if (Build.VERSION.SDK_INT >= 29) {
                // 必须带上 mediaProjection 类型, 否则 Android 14+ 不给建虚拟屏
                startForeground(1002, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            } else {
                startForeground(1002, n);
            }
        } catch (Throwable t) {
            Logx.e("startForeground 失败", t);
        }
    }

    /**
     * 用一次性授权票换出 MediaProjection 对象, 再建虚拟屏
     * 必须在服务里做(createVirtualDisplay 的硬性要求)
     */
    private synchronized void createProjection(int resultCode, Intent data) {
        if (projection != null) {
            Logx.i("投影会话已存在, 忽略新的授权票");
            return;
        }
        try {
            MediaProjectionManager mpm = (MediaProjectionManager)
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (mpm == null) {
                Logx.i("拿不到 MediaProjectionManager");
                return;
            }

            // ① 用掉那张一次性票
            MediaProjection p = mpm.getMediaProjection(resultCode, data);
            if (p == null) {
                Logx.i("换投影对象失败(授权票无效或已被消费)");
                return;
            }
            projection = p;

            // ② 注册回调: 用户撤销授权 / 屏幕锁定时系统会回调 onStop
            try {
                projCallback = new MediaProjection.Callback() {
                    @Override
                    public void onStop() {
                        Logx.i("投影被系统停止(用户撤销授权或屏幕变化)");
                        releaseProjection();
                    }
                };
                projection.registerCallback(projCallback, new Handler(Looper.getMainLooper()));
            } catch (Throwable t) {
                Logx.e("registerCallback 失败", t);
            }

            // ③ 虚拟屏尺寸跟真实屏一致
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics dm = new DisplayMetrics();
            if (wm != null) wm.getDefaultDisplay().getRealMetrics(dm);
            w = dm.widthPixels;
            h = dm.heightPixels;
            dpi = dm.densityDpi;
            if (w <= 0 || h <= 0) {
                w = 1080;
                h = 2400;
            }

            // ④ 建虚拟屏(必须在 mediaProjection 类型的前台服务里)
            reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2);
            display = projection.createVirtualDisplay("lovekb-cap", w, h, dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    reader.getSurface(), null, null);

            Logx.i("投影会话已建立 " + w + "x" + h + " (mediaProjection 前台服务内)");
        } catch (Throwable t) {
            Logx.e("建立投影会话失败", t);
            releaseProjection();
        }
    }

    /** 投影会话是否可用 */
    public static boolean ready() {
        CaptureService s = I;
        return s != null && s.reader != null && s.projection != null;
    }

    /** 当前投影方式说明 */
    public static String mode() {
        if (Build.VERSION.SDK_INT >= 30 && AccessService.I != null) return "无障碍截图(免授权)";
        return ready() ? "MediaProjection(后台录屏中)" : "未授权(需点「授权截图」)";
    }

    /** 取一帧(拿到的是虚拟屏镜像) */
    public synchronized Bitmap grab() {
        if (reader == null) return null;
        Image img = null;
        for (int i = 0; i < 5 && img == null; i++) {
            try {
                img = reader.acquireLatestImage();
            } catch (Throwable t) {
            }
            if (img == null) {
                try {
                    Thread.sleep(60);
                } catch (InterruptedException e) {
                    return null;
                }
            }
        }
        if (img == null) return null;
        try {
            Image.Plane[] planes = img.getPlanes();
            ByteBuffer buf = planes[0].getBuffer();
            int rowStride = planes[0].getRowStride();
            int pixelStride = planes[0].getPixelStride();
            int rowPadding = rowStride - pixelStride * w;
            Bitmap bmp = Bitmap.createBitmap(
                    w + rowPadding / pixelStride, h, Bitmap.Config.ARGB_8888);
            buf.rewind();
            bmp.copyPixelsFromBuffer(buf);
            Bitmap cropped = Bitmap.createBitmap(bmp, 0, 0, w, h);
            bmp.recycle();
            return cropped;
        } catch (Throwable t) {
            Logx.e("取帧失败", t);
            return null;
        } finally {
            try {
                img.close();
            } catch (Throwable t) {
            }
        }
    }

    /** 释放投影会话(用户撤销授权 / 服务销毁) */
    private synchronized void releaseProjection() {
        try {
            if (display != null) display.release();
        } catch (Throwable t) {
        }
        try {
            if (reader != null) reader.close();
        } catch (Throwable t) {
        }
        try {
            if (projection != null) {
                if (projCallback != null) {
                    try {
                        projection.unregisterCallback(projCallback);
                    } catch (Throwable t) {
                    }
                    projCallback = null;
                }
                projection.stop();
            }
        } catch (Throwable t) {
        }
        display = null;
        reader = null;
        projection = null;
        Logx.i("投影会话已释放");
    }

    @Override
    public void onDestroy() {
        releaseProjection();
        if (I == this) I = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
