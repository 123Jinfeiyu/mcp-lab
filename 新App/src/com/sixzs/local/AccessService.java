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

    /** 长按(用于唤起评论的举报菜单) */
    public static boolean longTap(float x, float y) {
        Path p = new Path();
        p.moveTo(x, y);
        p.lineTo(x + 1, y + 1);
        return gesture(p, 900);
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
        // API 30+ 走独立类(反射调用, 避免低版本加载失败导致无障碍服务挂掉)
        try {
            if (android.os.Build.VERSION.SDK_INT >= 30 && I != null) {
                Class<?> c = Class.forName("com.sixzs.local.ScreenShot30");
                java.lang.reflect.Method m = c.getDeclaredMethod("capture", AccessibilityService.class);
                m.setAccessible(true);
                Object r = m.invoke(null, I);
                if (r instanceof android.graphics.Bitmap) return (android.graphics.Bitmap) r;
            }
        } catch (Throwable t) {
            Logx.e("无障碍截图失败", t);
        }
        return null;
    }
}
