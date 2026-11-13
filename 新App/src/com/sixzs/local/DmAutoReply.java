package com.sixzs.local;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 主动私信自动回复(用户手动设置回复内容)
 *
 * 规则:
 *   只在「对方先发来私信」时回复, 且回复内容完全由用户在设置页写死, App 不生成内容。
 *   命中条件: 前台是目标App + 处于会话页(有输入框且输入框语义是聊天) + 有可点的「发送」
 *   防打扰: 同一句话只回一次; 一小时最多 N 条; 每条都记日志。
 */
public class DmAutoReply {

    /** 输入框像"聊天输入框"的关键词 */
    private static final String[] CHAT_HINTS = {
            "发消息", "说点什么", "发送消息", "和TA", "输入消息", "发私信", "聊一聊", "message", "chat"};
    private static final String[] TARGET_HINT = {"message", "chat", "msg", "private", "im_", "edittext"};

    private static String lastHandled = "";
    private static int sentInWindow = 0;
    private static long windowStart = 0L;

    public static void reset() {
        lastHandled = "";
        sentInWindow = 0;
        windowStart = 0L;
    }

    /**
     * 轮询入口: 命中就回一条, 返回处理说明; 未命中返回 null
     */
    public static String tick(Config cfg) {
        if (cfg == null || !cfg.autoDmReply) return null;
        String text = cfg.autoDmText == null ? "" : cfg.autoDmText.trim();
        if (text.isEmpty()) return "自动回复已开启但未设置回复内容";

        AccessibilityNodeInfo root = AccessService.root();
        if (root == null) return null;
        CharSequence pkgCs = root.getPackageName();
        String pkg = pkgCs == null ? "" : pkgCs.toString();
        if (!inTargets(cfg.autoDmApps, pkg)) return null;

        AccessibilityNodeInfo input = findChatInput(root);
        if (input == null) return null;
        AccessibilityNodeInfo send = findSendButton(root);
        if (send == null) return null;

        // 自动滑到最新, 免得用户手动翻
        scrollToNewest(root, input);

        String msg = latestPeerMessage(root, input);
        if (msg.isEmpty()) return null;
        if (msg.equals(lastHandled)) return null;   // 同一条不重复回
        lastHandled = msg;

        // 频率限制
        long now = System.currentTimeMillis();
        if (windowStart == 0L || now - windowStart > 3600000L) {
            windowStart = now;
            sentInWindow = 0;
        }
        if (sentInWindow >= cfg.autoDmMaxPerHour) {
            return "已达每小时上限(" + cfg.autoDmMaxPerHour + "条), 本条未回复: " + brief(msg);
        }

        if (!fillInput(input, text)) {
            return "填入回复内容失败: " + brief(msg);
        }
        sleep(400);
        if (!NodeUtil.click(NodeUtil.put(send))) {
            return "点击发送失败: " + brief(msg);
        }
        sentInWindow++;
        return "已自动回复: 「" + brief(msg) + "」→「" + brief(text) + "」";
    }

    /** 只填入不发送, 供设置页测试 */
    public static String fillOnly(Config cfg) {
        AccessibilityNodeInfo root = AccessService.root();
        if (root == null) return "读不到界面";
        AccessibilityNodeInfo input = findChatInput(root);
        if (input == null) return "当前不在私信会话页(找不到聊天输入框)";
        String text = cfg.autoDmText == null ? "" : cfg.autoDmText.trim();
        if (text.isEmpty()) return "未设置回复内容";
        return fillInput(input, text) ? "已填入: " + text : "填入失败";
    }

    // ---------------------------------------------------------------- 识别

    private static boolean inTargets(String targets, String pkg) {
        if (pkg == null) return false;
        if (targets == null || targets.trim().isEmpty()) return false;
        String[] arr = targets.split("[,，\\s]+");
        for (String t : arr) {
            if (!t.isEmpty() && pkg.equals(t.trim())) return true;
        }
        return false;
    }

    /** 聊天列表里若有更新的消息(未滑到底), 自动滑到底 */
    private static void scrollToNewest(AccessibilityNodeInfo root, AccessibilityNodeInfo input) {
        try {
            Rect ir = new Rect();
            input.getBoundsInScreen(ir);
            AccessibilityNodeInfo list = findChatList(root);
            if (list == null) return;
            List<AccessibilityNodeInfo> texts = findAllByClass(list, "android.widget.TextView");
            if (texts == null || texts.isEmpty()) return;
            int maxBottom = -1;
            for (AccessibilityNodeInfo n : texts) {
                if (!n.isVisibleToUser()) continue;
                Rect b = new Rect();
                n.getBoundsInScreen(b);
                if (b.bottom > maxBottom) maxBottom = b.bottom;
            }
            // 最后一条消息已经贴近输入框 -> 已在底部
            if (maxBottom > ir.top - 40) return;
            int x = ir.centerX();
            AccessService.swipe(x, ir.top - 60, x, ir.top - Math.max(200, ir.height()), 350);
            sleep(700);
        } catch (Throwable t) {
        }
    }

    /** 找聊天输入框: EditText + (提示语或控件id 表明是聊天) */
    private static AccessibilityNodeInfo findChatInput(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> edits = findAllByClass(root, "android.widget.EditText");
        if (edits == null) return null;
        for (AccessibilityNodeInfo n : edits) {
            if (!n.isVisibleToUser()) continue;
            CharSequence hint = n.getHintText();
            CharSequence desc = n.getContentDescription();
            String h = (hint == null ? "" : hint.toString()).toLowerCase();
            String d = (desc == null ? "" : desc.toString()).toLowerCase();
            String id = "";
            try {
                if (n.getViewIdResourceName() != null) id = n.getViewIdResourceName().toLowerCase();
            } catch (Throwable e) {
            }
            for (String k : CHAT_HINTS) {
                if (h.contains(k) || d.contains(k) || id.contains(k)) return n;
            }
        }
        return null;
    }

    /** 找「发送」按钮 */
    private static AccessibilityNodeInfo findSendButton(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByText("发送");
        if (list == null) return null;
        for (AccessibilityNodeInfo n : list) {
            if (!n.isVisibleToUser()) continue;
            if (n.isClickable()) return n;
        }
        // 不 clickable 就找它的可点击父节点
        for (AccessibilityNodeInfo n : list) {
            if (!n.isVisibleToUser()) continue;
            AccessibilityNodeInfo p = n.getParent();
            for (int i = 0; i < 3 && p != null; i++) {
                if (p.isClickable()) return p;
                p = p.getParent();
            }
        }
        return null;
    }

    /** 取会话里对方最新的一条消息(靠左的那条) */
    private static String latestPeerMessage(AccessibilityNodeInfo root, AccessibilityNodeInfo input) {
        AccessibilityNodeInfo list = findChatList(root);
        AccessibilityNodeInfo scope = list != null ? list : root;
        Rect ir = new Rect();
        input.getBoundsInScreen(ir);
        int screenW = ir.right > ir.left ? ir.right : 1080;

        List<AccessibilityNodeInfo> texts = findAllByClass(scope, "android.widget.TextView");
        if (texts == null) return "";
        String best = "";
        int bestBottom = -1;
        for (AccessibilityNodeInfo n : texts) {
            if (!n.isVisibleToUser()) continue;
            CharSequence t = n.getText();
            if (t == null) continue;
            String s = t.toString().trim();
            if (s.isEmpty() || s.length() > 120) continue;
            Rect b = new Rect();
            n.getBoundsInScreen(b);
            if (b.top >= ir.top) continue;            // 输入框上方的才是聊天内容
            if (b.left > screenW * 0.62) continue;    // 靠右的是自己发的, 跳过
            if (b.bottom > bestBottom) {
                bestBottom = b.bottom;
                best = s;
            }
        }
        return best;
    }

    /** 找聊天列表容器 */
    private static AccessibilityNodeInfo findChatList(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> rvs = new ArrayList<AccessibilityNodeInfo>();
        collectByClass(root, "androidx.recyclerview.widget.RecyclerView", rvs);
        collectByClass(root, "android.widget.ListView", rvs);
        AccessibilityNodeInfo best = null;
        int bestCount = 0;
        for (AccessibilityNodeInfo n : rvs) {
            int c = countTexts(n);
            if (c > bestCount) {
                bestCount = c;
                best = n;
            }
        }
        return bestCount >= 2 ? best : null;
    }

    private static void collectByClass(AccessibilityNodeInfo n, String cls, List<AccessibilityNodeInfo> out) {
        if (n == null || out.size() > 30) return;
        try {
            String cn = n.getClassName() == null ? "" : n.getClassName().toString();
            if (cn.equals(cls) && n.isVisibleToUser()) out.add(n);
            for (int i = 0; i < n.getChildCount(); i++) {
                AccessibilityNodeInfo c = n.getChild(i);
                if (c != null) collectByClass(c, cls, out);
            }
        } catch (Throwable e) {
        }
    }

    private static int countTexts(AccessibilityNodeInfo n) {
        List<AccessibilityNodeInfo> ts = findAllByClass(n, "android.widget.TextView");
        return ts == null ? 0 : ts.size();
    }

    /** 按类名收集节点(部分 android.jar 没有 findAccessibilityNodeInfosByClassName) */
    static List<AccessibilityNodeInfo> findAllByClass(AccessibilityNodeInfo n, String cls) {
        List<AccessibilityNodeInfo> out = new ArrayList<AccessibilityNodeInfo>();
        collectByClass(n, cls, out);
        return out;
    }

    // ---------------------------------------------------------------- 执行

    private static boolean fillInput(AccessibilityNodeInfo input, String text) {
        // 优先用系统 setText
        if (NodeUtil.setText(NodeUtil.put(input), text)) {
            sleep(200);
            return true;
        }
        // 退路: 用本机输入法提交
        try {
            LocalImeService.commit(text);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String brief(String s) {
        if (s == null) return "";
        return s.length() <= 18 ? s : s.substring(0, 18) + "…";
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }
}
