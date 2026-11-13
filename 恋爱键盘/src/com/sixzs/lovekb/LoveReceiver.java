package com.sixzs.lovekb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

/**
 * 广播入口: 让网页/脚本也能驱动
 *   adb shell am broadcast -n com.sixzs.lovekb/.LoveReceiver -a com.sixzs.lovekb.READ_CLIP
 *   adb shell am broadcast -n com.sixzs.lovekb/.LoveReceiver -a com.sixzs.lovekb.SEND_CLIP --es text "回复内容"
 *   adb shell am broadcast -n com.sixzs.lovekb/.LoveReceiver -a com.sixzs.lovekb.SET_CONFIG --es glmKey "..."
 *   adb shell am broadcast -n com.sixzs.lovekb/.LoveReceiver -a com.sixzs.lovekb.START_PANEL
 */
public class LoveReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String act = intent == null ? "" : String.valueOf(intent.getAction());
        try {
            if (act.contains("START_PANEL")) {
                // Android 12+ 后台启动前台服务会被拒(ForegroundServiceStartNotAllowedException)
                // 这里先试前台服务, 被拒就退回普通服务, 再不行给用户发一条可点击的通知
                Intent svc = new Intent(context, FloatService.class);
                boolean ok = false;
                try {
                    if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(svc);
                    else context.startService(svc);
                    ok = true;
                } catch (Throwable t) {
                    Logx.e("前台服务启动被拒(Android 12+ 后台限制), 改用普通服务", t);
                    try {
                        context.startService(svc);
                        ok = true;
                    } catch (Throwable t2) {
                        Logx.e("普通服务也启动不了, 已发通知请手动打开 App", t2);
                    }
                }
                if (ok) {
                    Logx.i("悬浮窗已启动");
                } else {
                    notifyOpenApp(context);
                }
                return;
            }
            if (act.contains("SET_CONFIG")) {
                SharedPreferences.Editor ed = context.getSharedPreferences("cfg", Context.MODE_PRIVATE).edit();
                boolean ch = false;
                if (intent.hasExtra("glmKey")) {
                    String k = String.valueOf(intent.getStringExtra("glmKey")).trim();
                    if (!k.isEmpty()) {
                        ed.putString("glmKey", k);
                        ch = true;
                    }
                }
                if (intent.hasExtra("model")) {
                    ed.putString("model", String.valueOf(intent.getStringExtra("model")));
                    ch = true;
                }
                if (intent.hasExtra("style")) {
                    ed.putString("style", String.valueOf(intent.getStringExtra("style")));
                    ch = true;
                }
                if (intent.hasExtra("types")) {
                    ed.putString("types", String.valueOf(intent.getStringExtra("types")));
                    ch = true;
                }
                if (intent.hasExtra("replyCount")) {
                    ed.putInt("replyCount", intent.getIntExtra("replyCount", 3));
                    ch = true;
                }
                if (intent.hasExtra("targetApps")) {
                    ed.putString("targetApps", String.valueOf(intent.getStringExtra("targetApps")));
                    ch = true;
                }
                if (intent.hasExtra("currentType")) {
                    ed.putString("currentType", String.valueOf(intent.getStringExtra("currentType")));
                    ch = true;
                }
                if (ch) {
                    ed.apply();
                    Logx.i("配置已更新");
                    // 类型变了就刷新悬浮窗下拉框(不用重启面板)
                    if (intent.hasExtra("currentType")) {
                        FloatService fs = FloatService.I;
                        if (fs != null) {
                            try {
                                fs.refreshTypeSpinner();
                            } catch (Throwable t) {
                            }
                        }
                    }
                }
                return;
            }
            if (act.contains("SEND_CLIP")) {
                String t = intent == null ? "" : String.valueOf(intent.getStringExtra("text"));
                if (!t.isEmpty()) {
                    Clip.write(context, t);
                    Logx.i("已写入剪贴板: " + t);
                }
                return;
            }
            if (act.contains("READ_CLIP")) {
                FloatService fs = FloatService.I;
                if (fs != null) fs.readClip();
                else Logx.i("悬浮窗未启动");
                return;
            }
            if (act.contains("GENERATE")) {
                FloatService fs = FloatService.I;
                if (fs != null) fs.generate();
                else Logx.i("悬浮窗未启动");
                return;
            }
            if (act.contains("PICK")) {
                FloatService fs = FloatService.I;
                int idx = intent.getIntExtra("index", 0);
                if (fs != null) fs.pick(idx);
                else Logx.i("悬浮窗未启动");
                return;
            }
            if (act.contains("FILL")) {
                FloatService fs = FloatService.I;
                if (fs != null) fs.fillIntoChat();
                else Logx.i("悬浮窗未启动");
                return;
            }
            if (act.contains("FOLD")) {
                FloatService fs = FloatService.I;
                if (fs != null) fs.setFolded(intent.getBooleanExtra("folded", true));
                else Logx.i("悬浮窗未启动");
                return;
            }
            if (act.contains("SET_SIZE")) {
                FloatService fs = FloatService.I;
                if (fs != null) fs.setSize(intent.getIntExtra("w", 400), intent.getIntExtra("h", 500));
                else Logx.i("悬浮窗未启动");
                return;
            }
            if (act.contains("SET_DRAFT")) {
                FloatService fs = FloatService.I;
                if (fs != null) {
                    String t = intent == null ? "" : String.valueOf(intent.getStringExtra("text"));
                    fs.setDraft(t);
                } else {
                    Logx.i("悬浮窗未启动");
                }
                return;
            }
            if (act.contains("SEND_DRAFT")) {
                FloatService fs = FloatService.I;
                if (fs != null) fs.sendDraft();
                else Logx.i("悬浮窗未启动");
                return;
            }
            if (act.contains("UNDO")) {
                FloatService fs = FloatService.I;
                if (fs != null) fs.undoReply();
                else Logx.i("悬浮窗未启动");
                return;
            }
            if (act.contains("CLEANUP")) {
                FloatService fs = FloatService.I;
                if (fs != null) {
                    Cleaner.Report rep = Cleaner.cleanup(fs, false);
                    Logx.quiet("网页触发清理: " + rep.text());
                } else {
                    Logx.i("悬浮窗未启动");
                }
                return;
            }
            if (act.contains("CAP_START")) {
                boolean ok = ScreenCap.ensureService(context);
                Logx.i(ok ? "后台录屏服务已启动" : "后台录屏服务启动失败");
                return;
            }
            if (act.contains("CAP_STOP")) {
                ScreenCap.stop(context);
                Logx.i("后台录屏服务已停止");
                return;
            }
            if (act.contains("TEST_SHOT")) {
                final android.graphics.Bitmap bmp = ScreenCap.capture();
                Logx.i("截图自检: 方式=" + ScreenCap.mode() + " 结果=" + ScreenCap.lastInfo
                        + (bmp != null ? " 尺寸=" + bmp.getWidth() + "x" + bmp.getHeight() : ""));
                return;
            }
            if (act.contains("READ_CONTEXT")) {
                FloatService fs = FloatService.I;
                if (fs != null) fs.readContext();
                else Logx.i("悬浮窗未启动");
                return;
            }
            if (act.contains("SHOT_ANALYZE")) {
                FloatService fs = FloatService.I;
                if (fs != null) fs.analyzeByShot();
                else Logx.i("悬浮窗未启动");
                return;
            }
            if (act.contains("UNDO")) {
                FloatService fs = FloatService.I;
                if (fs != null) fs.undoReply();
                else Logx.i("悬浮窗未启动");
                return;
            }
            if (act.contains("CLEANUP")) {
                FloatService fs = FloatService.I;
                if (fs != null) {
                    Cleaner.Report rep = Cleaner.cleanup(fs, false);
                    Logx.quiet("网页触发清理: " + rep.text());
                } else {
                    Logx.i("悬浮窗未启动");
                }
                return;
            }
            if (act.contains("CAP_START")) {
                boolean ok = ScreenCap.ensureService(context);
                Logx.i(ok ? "后台录屏服务已启动" : "后台录屏服务启动失败");
                return;
            }
            if (act.contains("CAP_STOP")) {
                ScreenCap.stop(context);
                Logx.i("后台录屏服务已停止");
                return;
            }
            if (act.contains("TEST_SHOT")) {
                final android.graphics.Bitmap bmp = ScreenCap.capture();
                Logx.i("截图自检: 方式=" + ScreenCap.mode() + " 结果=" + ScreenCap.lastInfo
                        + (bmp != null ? " 尺寸=" + bmp.getWidth() + "x" + bmp.getHeight() : ""));
                return;
            }
            if (act.contains("READ_CONTEXT")) {
                FloatService fs = FloatService.I;
                if (fs != null) fs.readContext();
                else Logx.i("悬浮窗未启动");
                return;
            }
            if (act.contains("SHOT_ANALYZE")) {
                FloatService fs = FloatService.I;
                if (fs != null) fs.analyzeByShot();
                else Logx.i("悬浮窗未启动");
                return;
            }
            if (act.contains("IME_READ")) {
                LoveImeService ime = LoveImeService.I;
                if (ime != null) ime.readAndGeneratePublic();
                else Logx.i("输入法未激活(先切到恋爱键盘输入法)");
                return;
            }
            if (act.contains("IME_COMMIT")) {
                LoveImeService ime = LoveImeService.I;
                if (ime != null) ime.commitIndex(intent.getIntExtra("index", 0));
                else Logx.i("输入法未激活(先切到恋爱键盘输入法)");
                return;
            }
        } catch (Throwable t) {
            Logx.e("广播处理失败", t);
        }
    }

    /** 后台启动被拒时, 通知用户点开 App 启动悬浮窗 */
    private void notifyOpenApp(Context context) {
        try {
            android.app.NotificationManager nm = (android.app.NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            String ch = "lovekb";
            if (Build.VERSION.SDK_INT >= 26) {
                android.app.NotificationChannel c = new android.app.NotificationChannel(ch, "恋爱键盘",
                        android.app.NotificationManager.IMPORTANCE_DEFAULT);
                nm.createNotificationChannel(c);
            }
            Intent it = new Intent(context, MainActivity.class);
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            int flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= android.app.PendingIntent.FLAG_IMMUTABLE;
            android.app.PendingIntent pi = android.app.PendingIntent.getActivity(context, 0, it, flags);
            android.app.Notification.Builder b = (Build.VERSION.SDK_INT >= 26)
                    ? new android.app.Notification.Builder(context, ch)
                    : new android.app.Notification.Builder(context);
            android.app.Notification n = b
                    .setContentTitle("请点此启动悬浮窗")
                    .setContentText("Android 12+ 不允许后台启动悬浮窗, 点这里打开 App")
                    .setSmallIcon(android.R.drawable.ic_menu_edit)
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();
            nm.notify(2001, n);
            Logx.i("已发通知: 点开 App 启动悬浮窗");
        } catch (Throwable t) {
            Logx.e("发通知失败", t);
        }
    }
}
