package com.sixzs.local;

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
 * 因此必须隔离到独立类, 并由 AccessService 通过反射调用(低版本根本不会加载本类)。
 */
class ScreenShot30 {

    static Bitmap capture(final AccessibilityService svc) {
        final CountDownLatch latch = new CountDownLatch(1);
        final Bitmap[] out = new Bitmap[1];
        try {
            svc.takeScreenshot(Display.DEFAULT_DISPLAY, svc.getMainExecutor(),
                    new AccessibilityService.TakeScreenshotCallback() {
                        public void onSuccess(AccessibilityService.ScreenshotResult sr) {
                            try {
                                Bitmap full = Bitmap.wrapHardwareBuffer(
                                        sr.getHardwareBuffer(), sr.getColorSpace());
                                if (full != null) {
                                    out[0] = full.copy(Bitmap.Config.ARGB_8888, false);
                                    full.recycle();
                                }
                            } catch (Throwable e) {
                            } finally {
                                latch.countDown();
                            }
                        }

                        public void onFailure(int reason) {
                            latch.countDown();
                        }
                    });
            latch.await(3, TimeUnit.SECONDS);
        } catch (Throwable e) {
        }
        return out[0];
    }
}
