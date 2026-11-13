package com.sixzs.lovekb;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 无障碍服务:
 *  - 找到当前聊天软件的输入框, 把回复直接填进去
 *  - 提供点击/滑动能力
 */
public class AccessService extends AccessibilityService {

    public static volatile AccessService I;
    /** 最近一次聚焦的输入框(App 包名 + 控件信息) */
    public static volatile String lastFocusPkg = "";

    @Override
    protected void onServiceConnected() {
        I = this;
        Logx.i("无障碍服务已连接");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            if (event == null) return;
            CharSequence pkg = event.getPackageName();
            if (pkg != null) lastFocusPkg = pkg.toString();
        } catch (Throwable t) {
        }
    }

    @Override
    public void onInterrupt() {
    }

    public static AccessibilityNodeInfo root() {
        AccessService s = I;
        if (s == null) return null;
        try {
            return s.getRootInActiveWindow();
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 找到「聊天输入框」:
     *  优先取当前有焦点的 EditText; 其次取屏幕下方、最可能可编辑的那个
     */
    public static AccessibilityNodeInfo findChatInput() {
        AccessibilityNodeInfo root = root();
        if (root == null) return null;
        List<AccessibilityNodeInfo> edits = findAllByClass(root, "android.widget.EditText");
        if (edits.isEmpty()) return null;

        AccessibilityNodeInfo focused = null;
        for (AccessibilityNodeInfo n : edits) {
            try {
                if (n.isFocused()) {
                    focused = n;
                    break;
                }
            } catch (Throwable e) {
            }
        }
        if (focused != null) return focused;

        // 没有焦点: 取最靠下(通常就是聊天输入框)且可编辑的那个
        AccessibilityNodeInfo best = null;
        int bestBottom = -1;
        for (AccessibilityNodeInfo n : edits) {
            if (!n.isVisibleToUser()) continue;
            try {
                if (!n.isEnabled() || !n.isEditable()) continue;
            } catch (Throwable e) {
                continue;
            }
            Rect r = new Rect();
            n.getBoundsInScreen(r);
            if (r.bottom > bestBottom) {
                bestBottom = r.bottom;
                best = n;
            }
        }
        return best != null ? best : edits.get(edits.size() - 1);
    }

    /** 把文本填进输入框 */
    public static boolean fillChatInput(String text) {
        AccessibilityNodeInfo input = findChatInput();
        if (input == null) return false;
        try {
            android.os.Bundle args = new android.os.Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            return input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        } catch (Throwable t) {
            Logx.e("填入输入框失败", t);
            return false;
        }
    }

    /**
     * 清空当前聊天输入框里的内容
     * 用于「撤回」: 把刚才填进去的话术撤回来
     */
    public static boolean clearChatInput() {
        AccessibilityNodeInfo input = findChatInput();
        if (input == null) return false;
        try {
            android.os.Bundle args = new android.os.Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "");
            return input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        } catch (Throwable t) {
            Logx.e("清空输入框失败", t);
            return false;
        }
    }

    /** 最近一次无障碍截图的失败原因 */
    public static volatile String lastShotError = "";

    /**
     * API 30+ 无障碍直接截图(免系统弹窗授权)
     * 走独立类 + 反射, 避免 Android 10 以下加载不了 API30 的类
     */
    public static android.graphics.Bitmap takeScreenshot() {
        AccessService s = I;
        if (s == null) {
            lastShotError = "无障碍服务未连接(请在设置里开启无障碍)";
            return null;
        }
        try {
            Class<?> c = Class.forName("com.sixzs.lovekb.ScreenShot30");
            java.lang.reflect.Method m = c.getDeclaredMethod("capture", AccessibilityService.class);
            m.setAccessible(true);
            Object o = m.invoke(null, s);
            if (o == null) {
                lastShotError = "无障碍截图调用无返回";
                return null;
            }
            // 取 Result 里的 bitmap / reason
            java.lang.reflect.Field fb = o.getClass().getDeclaredField("bitmap");
            fb.setAccessible(true);
            android.graphics.Bitmap bmp = (android.graphics.Bitmap) fb.get(o);
            java.lang.reflect.Field fr = o.getClass().getDeclaredField("reason");
            fr.setAccessible(true);
            String reason = String.valueOf(fr.get(o));
            lastShotError = reason == null || reason.isEmpty() ? "" : reason;
            return bmp;
        } catch (Throwable t) {
            lastShotError = "调用异常: " + t.getMessage();
            return null;
        }
    }

    /** 最近一次截图失败原因 */
    public static String lastShotError() {
        return lastShotError == null || lastShotError.isEmpty() ? "未知原因" : lastShotError;
    }

    /**
     * 抓当前聊天的上下文: 从节点树里读气泡文本, 按屏幕从上到下排序
     * 策略: 优先取"看起来像聊天列表"的容器(可滚动 + 文本多 + 不在顶/底导航条里),
     *      再退化为全屏文本(去掉顶/底导航带)
     */
    public static java.util.List<String> chatContext(int max) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        AccessibilityNodeInfo root = root();
        if (root == null) return out;

        int sw = 1080, sh = 1920;
        try {
            Rect r0 = new Rect();
            root.getBoundsInScreen(r0);
            if (r0.width() > 0) {
                sw = r0.width();
                sh = r0.height();
            }
        } catch (Throwable e) {
        }
        int topBand = (int) (sh * 0.10);      // 顶部导航条
        int bottomBand = (int) (sh * 0.90);   // 底部导航条

        AccessibilityNodeInfo list = findChatList(root);
        AccessibilityNodeInfo scope = list != null ? list : root;

        List<AccessibilityNodeInfo> texts = findAllByClass(scope, "android.widget.TextView");
        if (texts == null) return out;

        java.util.List<int[]> pos = new java.util.ArrayList<int[]>();
        java.util.List<String> content = new java.util.ArrayList<String>();
        for (AccessibilityNodeInfo n : texts) {
            if (!n.isVisibleToUser()) continue;
            CharSequence t = n.getText();
            if (t == null) continue;
            String s = t.toString().trim();
            if (s.isEmpty() || s.length() > 120) continue;
            if (s.equals("发送") || s.equals("语音") || s.equals("表情") || s.equals("+")) continue;
            Rect r = new Rect();
            n.getBoundsInScreen(r);
            if (r.height() <= 0) continue;
            // 全屏退化时剔掉顶/底导航带
            if (list == null) {
                if (r.top < topBand || r.bottom > bottomBand) continue;
            }
            pos.add(new int[]{r.top});
            content.add(s);
        }
        if (content.isEmpty()) return out;

        Integer[] idx = new Integer[content.size()];
        for (int i = 0; i < idx.length; i++) idx[i] = i;
        final java.util.List<int[]> fpos = pos;
        java.util.Arrays.sort(idx, new java.util.Comparator<Integer>() {
            public int compare(Integer a, Integer b) {
                return fpos.get(a)[0] - fpos.get(b)[0];
            }
        });
        int from = Math.max(0, idx.length - max);
        for (int i = from; i < idx.length; i++) {
            out.add(content.get(idx[i]));
        }
        return out;
    }

    /**
     * 当前聊天列表区域在屏幕上的坐标(供截图裁剪用)
     * 找不到返回 null(此时不裁剪, 用整屏)
     */
    public static Rect chatListBounds() {
        try {
            AccessibilityNodeInfo root = root();
            if (root == null) return null;
            AccessibilityNodeInfo list = findChatList(root);
            if (list == null) return null;
            Rect r = new Rect();
            list.getBoundsInScreen(r);
            if (r.width() < 60 || r.height() < 60) return null;
            // 略微外扩, 避免裁掉气泡边缘
            r.left = Math.max(0, r.left - 8);
            r.top = Math.max(0, r.top - 8);
            r.right = Math.min(r.right + 8, 100000);
            r.bottom = Math.min(r.bottom + 8, 100000);
            return r;
        } catch (Throwable e) {
            return null;
        }
    }

    /** 找聊天列表容器: 可滚动 + 文本多 + 位置在中间区域(排除顶/底导航条) */
    private static AccessibilityNodeInfo findChatList(AccessibilityNodeInfo root) {
        int sw = 1080, sh = 1920;
        try {
            Rect r0 = new Rect();
            root.getBoundsInScreen(r0);
            if (r0.width() > 0) {
                sw = r0.width();
                sh = r0.height();
            }
        } catch (Throwable e) {
        }
        AccessibilityNodeInfo best = null;
        int bestScore = -1;
        for (String cls : new String[]{"androidx.recyclerview.widget.RecyclerView", "android.widget.ListView",
                "android.widget.ScrollView"}) {
            for (AccessibilityNodeInfo n : findAllByClass(root, cls)) {
                if (!n.isVisibleToUser()) continue;
                Rect r = new Rect();
                n.getBoundsInScreen(r);
                int texts = findAllByClass(n, "android.widget.TextView").size();
                if (texts < 2) continue;
                int score = texts;
                // 可滚动加分
                try {
                    if (n.isScrollable()) score += 5;
                } catch (Throwable e) {
                }
                // 处在顶/底导航带的容器扣分(那是导航,不是聊天)
                if (r.height() < sh * 0.25) score -= 4;
                if (r.top < sh * 0.08 && r.bottom < sh * 0.20) score -= 6;
                if (r.bottom > sh * 0.93) score -= 6;
                if (score > bestScore) {
                    bestScore = score;
                    best = n;
                }
            }
        }
        return bestScore >= 3 ? best : null;
    }

    /**
     * 找到并点击「发送」按钮(填入文本后使用)
     * 顺序: 按文本(发送/发表/Send)找可点节点 → 找不到就找输入框右侧最近的可点节点
     */
    public static boolean clickSend() {
        AccessibilityNodeInfo root = root();
        if (root == null) return false;
        String[] keys = {"发送", "发表", "Send", "SEND"};
        for (String k : keys) {
            List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByText(k);
            if (list == null) continue;
            AccessibilityNodeInfo best = null;
            int bestRight = -1;
            for (AccessibilityNodeInfo n : list) {
                if (!n.isVisibleToUser()) continue;
                CharSequence t = n.getText();
                String s = t == null ? "" : t.toString();
                if (s.contains("发送候选") || s.contains("已发送")) continue;
                AccessibilityNodeInfo target = n.isClickable() ? n : clickableParent(n);
                if (target == null) continue;
                Rect r = new Rect();
                target.getBoundsInScreen(r);
                // 偏好右下角(聊天发送按钮通常在输入框右侧)
                if (r.right > bestRight) {
                    bestRight = r.right;
                    best = target;
                }
            }
            if (best != null) {
                try {
                    return best.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                } catch (Throwable e) {
                }
            }
        }
        // 兜底: 按 contentDescription 找(图标按钮)
        for (AccessibilityNodeInfo n : findAllByClass(root, "android.widget.ImageView")) {
            try {
                CharSequence d = n.getContentDescription();
                if (d == null) continue;
                String s = d.toString();
                if (!(s.contains("发送") || s.contains("send") || s.contains("Send"))) continue;
                if (!n.isVisibleToUser()) continue;
                AccessibilityNodeInfo target = n.isClickable() ? n : clickableParent(n);
                if (target != null && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
            } catch (Throwable e) {
            }
        }
        return false;
    }

    private static AccessibilityNodeInfo clickableParent(AccessibilityNodeInfo n) {
        try {
            AccessibilityNodeInfo p = n.getParent();
            for (int i = 0; i < 4 && p != null; i++) {
                if (p.isClickable()) return p;
                p = p.getParent();
            }
        } catch (Throwable e) {
        }
        return null;
    }

    /** 当前前台 App 包名 */
    public static String foregroundPkg() {
        AccessibilityNodeInfo root = root();
        if (root != null) {
            try {
                CharSequence cs = root.getPackageName();
                if (cs != null) return cs.toString();
            } catch (Throwable e) {
            }
        }
        return lastFocusPkg;
    }

    // ---------------------------------------------------------------- 手势

    public static boolean tap(float x, float y) {
        AccessService s = I;
        if (s == null) return false;
        try {
            Path p = new Path();
            p.moveTo(x, y);
            p.lineTo(x + 1, y + 1);
            GestureDescription.Builder b = new GestureDescription.Builder();
            b.addStroke(new GestureDescription.StrokeDescription(p, 0, 60));
            return s.dispatchGesture(b.build(), null, null);
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean swipe(float x1, float y1, float x2, float y2, long ms) {
        AccessService s = I;
        if (s == null) return false;
        try {
            Path p = new Path();
            p.moveTo(x1, y1);
            p.lineTo(x2, y2);
            GestureDescription.Builder b = new GestureDescription.Builder();
            b.addStroke(new GestureDescription.StrokeDescription(p, 0, Math.max(20, ms)));
            return s.dispatchGesture(b.build(), null, null);
        } catch (Throwable e) {
            return false;
        }
    }

    // ---------------------------------------------------------------- 节点遍历

    static List<AccessibilityNodeInfo> findAllByClass(AccessibilityNodeInfo n, String cls) {
        List<AccessibilityNodeInfo> out = new ArrayList<AccessibilityNodeInfo>();
        collect(n, cls, out, 0);
        return out;
    }

    private static void collect(AccessibilityNodeInfo n, String cls, List<AccessibilityNodeInfo> out, int depth) {
        if (n == null || out.size() > 60 || depth > 25) return;
        try {
            String cn = n.getClassName() == null ? "" : n.getClassName().toString();
            if (cn.equals(cls)) out.add(n);
            for (int i = 0; i < n.getChildCount(); i++) {
                AccessibilityNodeInfo c = n.getChild(i);
                if (c != null) collect(c, cls, out, depth + 1);
            }
        } catch (Throwable e) {
        }
    }
}
