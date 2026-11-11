package com.sixzs.local;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class AccessService extends AccessibilityService {

    public static AccessService I;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        I = this;
        Logx.i("AccessService connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        I = null;
        return super.onUnbind(intent);
    }

    public static AccessibilityNodeInfo root() {
        try {
            return I == null ? null : I.getRootInActiveWindow();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean globalAction(int action) {
        return I != null && I.performGlobalAction(action);
    }

    public static boolean gesture(Path path, long duration) {
        if (I == null) return false;
        try {
            GestureDescription.Builder b = new GestureDescription.Builder();
            b.addStroke(new GestureDescription.StrokeDescription(path, 0, Math.max(20, duration)));
            return I.dispatchGesture(b.build(), null, null);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean tap(float x, float y) {
        Path p = new Path();
        p.moveTo(x, y);
        p.lineTo(x + 1, y + 1);
        return gesture(p, 60);
    }

    public static boolean swipe(float x1, float y1, float x2, float y2, long ms) {
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        return gesture(p, ms <= 0 ? 300 : ms);
    }

    public static android.graphics.Bitmap screenshot(int l, int t, int r, int b) {
        android.graphics.Bitmap full = null;
        if (I != null && android.os.Build.VERSION.SDK_INT >= 30) {
            full = shotAccessibility();
        }
        if (full == null) {
            full = ScreenCap.capture();
        }
        if (full == null) return null;
        if (r > l && b > t) {
            int x = Math.max(0, l);
            int y = Math.max(0, t);
            int w = Math.min(full.getWidth() - x, r - l);
            int h = Math.min(full.getHeight() - y, b - t);
            if (w > 0 && h > 0) {
                android.graphics.Bitmap c = android.graphics.Bitmap.createBitmap(full, x, y, w, h);
                full.recycle();
                return c;
            }
        }
        return full;
    }

    private static android.graphics.Bitmap shotAccessibility() {
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        final android.graphics.Bitmap[] out = new android.graphics.Bitmap[1];
        try {
            I.takeScreenshot(android.view.Display.DEFAULT_DISPLAY, I.getMainExecutor(),
                    new TakeScreenshotCallback() {
                        public void onSuccess(ScreenshotResult sr) {
                            try {
                                android.graphics.Bitmap full =
                                        android.graphics.Bitmap.wrapHardwareBuffer(sr.getHardwareBuffer(), sr.getColorSpace());
                                if (full != null) {
                                    out[0] = full.copy(android.graphics.Bitmap.Config.ARGB_8888, false);
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
            latch.await(3, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Throwable e) {
        }
        return out[0];
    }
}
