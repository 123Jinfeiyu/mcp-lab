package com.sixzs.lovekb;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Bitmap;
import android.view.Display;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * API 30+ 无障碍截图 (AccessibilityService.takeScreenshot)
 *
 * 注意: AccessibilityService.TakeScreenshotCallback 是 API 30 才有的类,
 * 若直接写在 AccessService 里, 在 Android 10 以下设备上会导致
 * NoClassDefFoundError -> 整个无障碍服务类加载失败 -> 服务绑不上。
 * 因此隔离到独立类, 由 AccessService 反射调用(低版本根本不会加载本类)。
 *
 * Android 15(API 35) 实测可用: 只要无障碍服务已连接且带
 * flagRetrieveInteractiveWindows, 就不需要 MediaProjection 的系统授权弹窗。
 */
class ScreenShot30 {

    /** 两次截图的最小间隔(系统要求 >=1s, 否则返回 ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT) */
    private static long lastShotAt = 0L;

    static class Result {
        Bitmap bitmap;
        int error = 0;      // 0 = 成功; 非 0 = AccessibilityService 的错误码
        String reason = "";
    }

    static Result capture(final AccessibilityService svc) {
        Result r = new Result();
        long gap = System.currentTimeMillis() - lastShotAt;
        if (gap < 1100) {
            try {
                Thread.sleep(1100 - gap);
            } catch (InterruptedException e) {
            }
        }
        lastShotAt = System.currentTimeMillis();

        final CountDownLatch latch = new CountDownLatch(1);
        final Result[] out = new Result[1];
        try {
            svc.takeScreenshot(Display.DEFAULT_DISPLAY, svc.getMainExecutor(),
                    new AccessibilityService.TakeScreenshotCallback() {
                        public void onSuccess(AccessibilityService.ScreenshotResult sr) {
                            try {
                                Bitmap full = Bitmap.wrapHardwareBuffer(
                                        sr.getHardwareBuffer(), sr.getColorSpace());
                                if (full != null) {
                                    out[0] = new Result();
                                    out[0].bitmap = full.copy(Bitmap.Config.ARGB_8888, false);
                                    full.recycle();
                                } else {
                                    out[0] = new Result();
                                    out[0].error = -2;
                                    out[0].reason = "截图缓冲区为空";
                                }
                            } catch (Throwable e) {
                                out[0] = new Result();
                                out[0].error = -2;
                                out[0].reason = "像素拷贝失败: " + e.getMessage();
                            } finally {
                                latch.countDown();
                            }
                        }

                        public void onFailure(int reason) {
                            out[0] = new Result();
                            out[0].error = reason;
                            out[0].reason = explain(reason);
                            latch.countDown();
                        }
                    });
            latch.await(4, TimeUnit.SECONDS);
        } catch (Throwable e) {
            r.error = -3;
            r.reason = "调用异常: " + e.getMessage();
        }
        if (out[0] != null) return out[0];
        if (r.error == 0) {
            r.error = -4;
            r.reason = "无障碍截图超时(4秒未返回)";
        }
        return r;
    }

    /* 系统错误码(与 AOSP AccessibilityService 一致, 但不直接引用以免 SDK 隐藏) */
    static final int ERR_INTERNAL = 1;
    static final int ERR_INTERVAL = 2;
    static final int ERR_NOT_SUPPORTED = 3;
    static final int ERR_SECURE = 4;
    static final int ERR_INVALID_DISPLAY = 5;

    /** 把系统错误码翻成人话 */
    static String explain(int code) {
        switch (code) {
            case ERR_SECURE:
                return "该窗口受保护(加密/支付类窗口不允许截图)";
            case ERR_INTERVAL:
                return "离上次截图太近, 稍等 1 秒再试";
            case ERR_NOT_SUPPORTED:
                return "当前系统不支持无障碍截图, 改用 MediaProjection 授权";
            case ERR_INVALID_DISPLAY:
                return "显示屏参数无效";
            case ERR_INTERNAL:
                return "系统内部错误";
            default:
                return "无障碍截图失败(错误码 " + code + ")";
        }
    }
}
