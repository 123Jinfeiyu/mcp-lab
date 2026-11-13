package com.sixzs.lovekb;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;

import java.io.ByteArrayOutputStream;

/**
 * 截图入口 (对外统一门面)
 *
 * 两条路:
 *   API 30+  无障碍 takeScreenshot —— 免授权弹窗, 应用在后台也能截
 *   更低版本 MediaProjection —— 由 CaptureService(mediaProjection 类型的前台服务)
 *            在后台维持投影会话, 这里直接向服务要一帧
 *
 * 注意: MediaProjection 的建屏已经全部搬到 CaptureService 里,
 *       这里只负责"取帧", 绝不在 Activity 里 createVirtualDisplay
 *       (Android 14+ 会抛 SecurityException)。
 */
public class ScreenCap {

    /** 最近一次截图的说明(成功/失败原因), 供 UI 展示 */
    public static volatile String lastInfo = "";

    /** 当前可用的截图方式 */
    public static String mode() {
        if (Build.VERSION.SDK_INT >= 30 && AccessService.I != null) return "无障碍截图(免授权)";
        if (CaptureService.ready()) return "MediaProjection(后台录屏中)";
        return "未授权(需点「授权截图」)";
    }

    /** 投影会话是否已建立 */
    public static boolean ready() {
        return CaptureService.ready();
    }

    /**
     * 用授权票启动/初始化 MediaProjection 投影会话
     *
     * 授权票(resultCode + resultData) 是一次性的, 必须当场转交给服务,
     * 由服务在前台服务上下文里换出 MediaProjection 对象并建虚拟屏。
     *
     * @return 是否成功把票送到服务
     */
    public static boolean init(Context ctx, int resultCode, Intent data) {
        if (resultCode != android.app.Activity.RESULT_OK || data == null) {
            lastInfo = "用户取消了授权";
            Logx.i("截图授权: 用户取消");
            return false;
        }
        try {
            // 授权票是一次性的: 先放进同进程静态槽位(最可靠),
            // 再启动服务, 服务在 onStartCommand 里取走并换出投影对象
            CaptureService.postTicket(resultCode, data);

            Intent svc = new Intent(ctx, CaptureService.class);
            // 跨进程备份通道(静态槽位失效时兜底)
            svc.putExtra(CaptureService.EXTRA_RESULT_CODE, resultCode);
            try {
                svc.putExtra(CaptureService.EXTRA_RESULT_DATA, data);
            } catch (Throwable t) {
            }
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(svc);
            else ctx.startService(svc);
            lastInfo = "授权票已交给后台录屏服务";
            Logx.i("授权票已交给 CaptureService(mediaProjection 前台服务)");
            return true;
        } catch (Throwable t) {
            Logx.e("启动 CaptureService 失败", t);
            lastInfo = "启动录屏服务失败: " + t.getMessage();
            return false;
        }
    }

    /** 确保录屏服务在跑(无票时只维持服务, 不重建投影) */
    public static boolean ensureService(Context ctx) {
        if (CaptureService.ready()) return true;
        try {
            Intent svc = new Intent(ctx, CaptureService.class);
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(svc);
            else ctx.startService(svc);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 最近一次截图的缓存
     * 截图一张 1080x2400 的 ARGB 图约 10MB, 用完不释放会持续涨内存。
     * 这里显式缓存一份, 供「清理」按钮随时释放。
     */
    private static volatile android.graphics.Bitmap lastShot = null;

    /** 缓存最近一张截图(会先释放上一张) */
    public static void keepShot(android.graphics.Bitmap b) {
        releaseShot();
        lastShot = b;
    }

    /** 释放缓存的截图, 返回释放的字节数 */
    public static int releaseShot() {
        android.graphics.Bitmap b = lastShot;
        lastShot = null;
        if (b == null) return 0;
        int bytes = 0;
        try {
            bytes = b.getByteCount();
        } catch (Throwable t) {
        }
        try {
            if (!b.isRecycled()) b.recycle();
        } catch (Throwable t) {
        }
        return bytes;
    }

    /** 当前缓存截图占用的字节数 */
    public static int shotBytes() {
        android.graphics.Bitmap b = lastShot;
        if (b == null) return 0;
        try {
            return b.getByteCount();
        } catch (Throwable t) {
            return 0;
        }
    }

    /**
     * 截一张图
     * Android 11+(API30+) 优先无障碍截图: 不需要系统授权弹窗, 应用在后台也能截
     * 否则向 CaptureService 要一帧(MediaProjection)
     */
    public static Bitmap capture() {
        if (Build.VERSION.SDK_INT >= 30) {
            Bitmap b = AccessService.takeScreenshot();
            if (b != null) {
                lastInfo = "无障碍截图成功";
                Logx.i("无障碍截图成功");
                return b;
            }
            // 无障碍截图失败: 回落 MediaProjection
            String why = AccessService.lastShotError();
            Logx.i("无障碍截图不可用: " + why + " → 尝试后台录屏");
            Bitmap p2 = captureProjection();
            if (p2 != null) {
                lastInfo = "已回落后台录屏(" + why + ")";
                return p2;
            }
            lastInfo = "截图失败: " + why;
            return null;
        }
        Bitmap p3 = captureProjection();
        if (p3 != null) {
            lastInfo = "后台录屏截图成功";
            Logx.i("后台录屏截图成功");
        } else {
            lastInfo = CaptureService.ready()
                    ? "取帧失败(投影会话异常)"
                    : "还没授权(设置页点「授权截图」)";
        }
        return p3;
    }

    /** 截图为 JPEG 字节(给视觉模型用) */
    public static byte[] captureJpeg(int quality) {
        Bitmap b = capture();
        if (b == null) return null;
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            b.compress(Bitmap.CompressFormat.JPEG, quality <= 0 ? 70 : quality, bo);
            b.recycle();
            return bo.toByteArray();
        } catch (Throwable t) {
            return null;
        }
    }

    /** 从后台录屏服务取一帧 */
    public static synchronized Bitmap captureProjection() {
        CaptureService s = CaptureService.I;
        if (s == null) return null;
        return s.grab();
    }

    /** 停止后台录屏(释放投影会话) */
    public static void stop(Context ctx) {
        try {
            if (ctx != null) ctx.stopService(new Intent(ctx, CaptureService.class));
        } catch (Throwable t) {
        }
    }
}
