package com.sixzs.local;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 评论区舆情与风控:
 *   1) 采集当前屏幕上可见的评论(无障碍节点树)
 *   2) 交给 GLM 批量分类, 识别别人在评论区发的涉赌/涉黄/黑产引流广告
 *   3) 对可疑评论执行举报(长按 -> 举报 -> 选原因 -> 提交)
 *
 * 只处理【别人发布的内容】, 不主动向任何人发消息。
 */
public class CommentGuard {

    /** 评论候选类 */
    public static class Comment {
        public String text = "";
        public String label = "";
        public String level = "";
        public String reason = "";
        public String reply = "";    // AI 给的公开回复建议(人工确认后再发)
        public String intent = "";   // 意向: 赞美认可/主动求联系方式/想了解详情/纯闲聊/无意向
        public int top, bottom, left, right;
        public int nid = 0;      // NodeUtil 缓存 id, 用于定位/长按
    }

    /** 界面上常见的操作按钮/元信息, 不是评论正文 */
    private static final Set<String> NOISE = new HashSet<String>(Arrays.asList(
            "关注", "已关注", "回复", "展开", "展开更多", "收起", "翻译", "举报", "点赞", "赞",
            "分享", "收藏", "更多", "编辑", "置顶", "作者", "IP属地", "广告", "视频", "图文",
            "评论", "返回", "首页", "发现", "消息", "我", "作品", "喜欢", "收藏夹", "视频号",
            "登录", "注册", "搜索", "发送", "确定", "取消", "删除", "复制", "设为壁纸",
            "拍摄", "选择", "下一步", "完成", "同意", "拒绝", "暂不", "知道了"));

    /**
     * 深度采集: 自动向下滚动评论列表, 累积采集更多评论(含楼中楼回复),
     * 直到连续两轮没有新内容或达到上限。全程不需要人工滑动。
     */
    public static List<Comment> scanDeep(int maxRounds, int maxComments) {
        List<Comment> all = new ArrayList<Comment>();
        Set<String> seen = new HashSet<String>();
        int emptyRounds = 0;
        for (int r = 0; r <= maxRounds; r++) {
            List<Comment> batch = scan();
            int added = 0;
            for (Comment c : batch) {
                String k = c.text.trim();
                if (seen.add(k)) {
                    all.add(c);
                    added++;
                }
            }
            Logx.i("风控采集 第" + (r + 1) + "轮: 本轮 " + batch.size() + " 条, 新增 " + added
                    + " 条, 累计 " + all.size() + " 条");
            if (r >= maxRounds || all.size() >= maxComments) break;
            if (added == 0) {
                emptyRounds++;
                if (emptyRounds >= 2) {
                    Logx.i("风控采集: 连续两轮没有新评论, 判定已到底");
                    break;
                }
            } else {
                emptyRounds = 0;
            }
            if (!scrollListDown()) {
                Logx.i("风控采集: 列表无法继续滚动, 结束");
                break;
            }
            sleep(900);
        }
        return all;
    }

    /** 在评论列表容器内向下翻一页(手指上滑); 滑不动或面板消失返回 false */
    static boolean scrollListDown() {
        AccessibilityNodeInfo root = AccessService.root();
        if (root == null) return false;
        AccessibilityNodeInfo list = findListContainer(root);
        int x, y1, y2;
        if (list != null) {
            Rect r = new Rect();
            list.getBoundsInScreen(r);
            if (r.height() < 120) {
                Logx.i("风控采集: 评论容器太矮, 放弃滚动");
                return false;
            }
            x = r.centerX();
            y1 = r.top + (int) (r.height() * 0.72);
            y2 = r.top + (int) (r.height() * 0.30);
        } else {
            // 兜底: 找不到容器就在屏幕下半区滑动(评论区通常占下半屏)
            int h = 1280, w = 720;
            try {
                if (AccessService.I != null) {
                    android.util.DisplayMetrics dm = AccessService.I.getResources().getDisplayMetrics();
                    w = dm.widthPixels;
                    h = dm.heightPixels;
                }
            } catch (Throwable e) {
            }
            x = w / 2;
            y1 = (int) (h * 0.75);
            y2 = (int) (h * 0.32);
            Logx.i("风控采集: 未锁定容器, 改用屏幕下半区滑动");
        }
        boolean ok = AccessService.swipe(x, y1, x, y2, 450);
        sleep(700);
        // 滑完确认评论面板还在(防止把评论面板划没了)
        if (findListContainer(AccessService.root()) == null) {
            Logx.i("风控采集: 滑动后评论面板消失, 停止");
            return false;
        }
        return ok;
    }

    /** 采集当前屏幕上可见的评论候选 */
    public static List<Comment> scan() {
        List<Comment> out = new ArrayList<Comment>();
        AccessibilityNodeInfo root = AccessService.root();
        if (root == null) return out;

        // 优先锁定评论列表容器(RecyclerView/ListView), 避免把界面装饰当评论
        AccessibilityNodeInfo list = findListContainer(root);
        if (list != null) {
            collect(list, out);
        } else {
            collect(root, out);
        }

        // 按 y 排序, 同 y 的按 x
        java.util.Collections.sort(out, new java.util.Comparator<Comment>() {
            public int compare(Comment a, Comment b) {
                if (a.top != b.top) return a.top - b.top;
                return a.left - b.left;
            }
        });
        // 去重(父节点与子节点同文本)
        List<Comment> uniq = new ArrayList<Comment>();
        Set<String> seen = new HashSet<String>();
        for (Comment c : out) {
            String k = c.text.trim();
            if (seen.contains(k)) continue;
            seen.add(k);
            uniq.add(c);
        }
        return uniq;
    }

    /** 找文本最多的列表容器(评论区) */
    private static AccessibilityNodeInfo findListContainer(AccessibilityNodeInfo root) {
        final List<AccessibilityNodeInfo> lists = new ArrayList<AccessibilityNodeInfo>();
        findByClass(root, "androidx.recyclerview.widget.RecyclerView", lists);
        findByClass(root, "android.widget.ListView", lists);
        if (lists.isEmpty()) return null;
        AccessibilityNodeInfo best = null;
        int bestCount = 0;
        for (AccessibilityNodeInfo n : lists) {
            int c = countTexts(n);
            if (c > bestCount) {
                bestCount = c;
                best = n;
            }
        }
        // 少于 3 条文本的容器多半不是评论列表
        if (bestCount < 3) return null;
        Logx.i("风控: 锁定评论列表容器, 文本 " + bestCount + " 条");
        return best;
    }

    private static void findByClass(AccessibilityNodeInfo n, String cls, List<AccessibilityNodeInfo> out) {
        if (n == null || out.size() > 40) return;
        try {
            String cn = n.getClassName() == null ? "" : n.getClassName().toString();
            if (cn.equals(cls) && n.isVisibleToUser()) out.add(n);
            for (int i = 0; i < n.getChildCount(); i++) {
                AccessibilityNodeInfo c = n.getChild(i);
                if (c != null) findByClass(c, cls, out);
            }
        } catch (Throwable e) {
        }
    }

    private static int countTexts(AccessibilityNodeInfo n) {
        final int[] cnt = new int[1];
        countTexts(n, cnt);
        return cnt[0];
    }

    private static void countTexts(AccessibilityNodeInfo n, int[] cnt) {
        if (n == null || cnt[0] > 500) return;
        try {
            CharSequence t = n.getText();
            String cls = n.getClassName() == null ? "" : n.getClassName().toString();
            if (t != null && cls.contains("TextView") && t.toString().trim().length() >= 2) cnt[0]++;
            for (int i = 0; i < n.getChildCount(); i++) {
                AccessibilityNodeInfo c = n.getChild(i);
                if (c != null) countTexts(c, cnt);
            }
        } catch (Throwable e) {
        }
    }

    private static void collect(AccessibilityNodeInfo n, List<Comment> out) {
        if (n == null || out.size() > 200) return;
        try {
            if (n.isVisibleToUser()) {
                CharSequence t = n.getText();
                String cls = n.getClassName() == null ? "" : n.getClassName().toString();
                if (t != null && cls.contains("TextView")) {
                    String s = t.toString().trim();
                    if (s.length() >= 2 && s.length() <= 120 && !NOISE.contains(s)
                            && !isAllDigits(s) && s.matches(".*[\\u4e00-\\u9fa5a-zA-Z].*")) {
                        Rect b = new Rect();
                        n.getBoundsInScreen(b);
                        if (b.width() > 0 && b.height() > 0) {
                            Comment c = new Comment();
                            c.text = s;
                            c.top = b.top; c.bottom = b.bottom; c.left = b.left; c.right = b.right;
                            c.nid = NodeUtil.put(n);
                            out.add(c);
                        }
                    }
                }
            }
            for (int i = 0; i < n.getChildCount(); i++) {
                AccessibilityNodeInfo ch = n.getChild(i);
                if (ch != null) collect(ch, out);
            }
        } catch (Throwable e) {
        }
    }

    private static boolean isAllDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }

    /** 批量交给 GLM 分类, 回填 label/level/reason */
    public static void classify(List<Comment> items, String key, String model) throws Exception {
        if (items.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            sb.append(i).append(". ").append(items.get(i).text).append("\n");
        }
        String prompt = "你是短视频评论区分析助手。下面是若干条用户评论, 请同时完成两件事:\n"
                + "① 内容风控: 判断是否为违规引流广告\n"
                + "② 意向识别: 判断这位用户是否表现出社交/合作兴趣\n\n"
                + "风控标签(选一个): 赌博引流 / 色情引流 / 诈骗黑产 / 垃圾广告 / 正常互动\n"
                + "  (赌博引流: 彩票/赌博/下注/回血/上分/带单等; 色情引流: 约炮/裸聊/福利姬/同城约等;"
                + " 诈骗黑产: 刷单/返利/杀猪盘等; 垃圾广告: 无关推广; 其余为正常互动)\n\n"
                + "意向标签(选一个): 赞美认可 / 主动求联系方式 / 想了解详情 / 纯闲聊 / 无意向\n"
                + "  (主动求联系方式: 出现 加QQ/加V/私信我/怎么联系/留个联系方式 等)\n\n"
                + "评论列表:\n" + sb + "\n"
                + "严格只输出 JSON 数组, 每项:\n"
                + "{\"i\":0,\"label\":\"正常互动\",\"level\":\"low\",\"intent\":\"赞美认可\","
                + "\"reason\":\"不超过20字\",\"reply\":\"给这条评论的一条自然搭话, 30字内; "
                + "纯闲聊/无意向可留空\"}\n"
                + "level 取 high/mid/low, 正常互动一律 low。";

        String reply = GlmAgent.callChatText(key, model, prompt);
        applyClassification(items, reply);
    }

    /** 容错解析模型输出: 先修全角标点, 再按 JSON 解析; 失败则逐条正则提取 */
    static void applyClassification(List<Comment> items, String raw) {
        String json = raw.trim();
        int a = json.indexOf('[');
        int b = json.lastIndexOf(']');
        if (a >= 0 && b > a) json = json.substring(a, b + 1);
        // 全角标点 -> 半角
        json = json.replace('：', ':').replace('，', ',').replace('“', '"')
                .replace('”', '"').replace('｛', '{').replace('｝', '}')
                .replace('［', '[').replace('］', ']');
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Comment c = pick(items, o.optInt("i", -1));
                if (c == null) continue;
                c.label = o.optString("label", "");
                c.level = o.optString("level", "low");
                c.reason = o.optString("reason", "");
                c.reply = o.optString("reply", "");
                c.intent = o.optString("intent", "");
            }
            return;
        } catch (Throwable e) {
            Logx.i("风控: JSON 解析失败, 改用容错提取");
        }
        // 容错: 逐条 { ... } 里用正则抓字段
        java.util.regex.Matcher mm = java.util.regex.Pattern
                .compile("\\{[^}]*\\}").matcher(json);
        while (mm.find()) {
            String one = mm.group();
            int idx = intOf(one, "i");
            Comment c = pick(items, idx);
            if (c == null) continue;
            c.label = strOf(one, "label");
            c.level = strOf(one, "level");
            c.reason = strOf(one, "reason");
            c.reply = strOf(one, "reply");
            c.intent = strOf(one, "intent");
        }
    }

    private static Comment pick(List<Comment> items, int idx) {
        if (idx < 0 || idx >= items.size()) return null;
        return items.get(idx);
    }

    private static int intOf(String s, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"" + key + "\"\\s*:\\s*(-?\\d+)").matcher(s);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    private static String strOf(String s, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(s);
        return m.find() ? m.group(1) : "";
    }

    /** 是否有社交/合作意向(用于线索筛选) */
    public static boolean hasIntent(Comment c) {
        String i = c.intent;
        return i != null && (i.contains("求联系方式") || i.contains("想了解") || i.contains("赞美"));
    }

    /**
     * 举报一条评论: 长按 -> 弹层里找"举报" -> 举报页选原因 -> 提交
     * 每一步都重新取节点树, 适配不同 App 的弹层结构。
     */
    public static String report(Comment c) {
        AccessibilityNodeInfo n = NodeUtil.get(c.nid);
        if (n == null) return "评论节点已失效, 请重新扫描";
        // 1) 长按
        boolean longPressed = false;
        try {
            longPressed = n.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
        } catch (Throwable e) {
        }
        Rect r = new Rect();
        n.getBoundsInScreen(r);
        if (!longPressed) {
            longPressed = AccessService.longTap(r.centerX(), r.centerY());
        }
        Logx.i("举报步骤1 长按: " + (longPressed ? "已执行" : "失败"));
        sleep(1200);

        // 2) 长按没弹菜单 -> 退回找"更多/···"入口
        if (!menuHasReport()) {
            Logx.i("举报步骤2 长按未弹出菜单, 尝试「更多」入口");
            boolean more = clickByText(new String[]{"更多", "···", "…"}, 0);
            Logx.i("举报步骤2: " + (more ? "已点击更多" : "未找到更多入口"));
            sleep(1200);
        }

        // 3) 弹层里找"举报"
        if (!clickByText(new String[]{"举报", "举报该评论", "举报此评论", "投诉"}, 0)) {
            return "未找到举报入口(长按/更多都没弹出菜单), 该 App 可能需点评论右侧「···」举报";
        }
        sleep(1500);

        // 4) 举报页选原因(优先针对垃圾广告/色情低俗/赌博)
        String[] reasons = {"垃圾广告", "色情低俗", "涉赌", "赌博", "欺诈", "恶意营销", "其他违规"};
        if (!clickByText(reasons, 0)) {
            return "已进入举报页, 但未找到原因选项, 请手动完成";
        }
        sleep(1000);
        // 5) 提交
        boolean submitted = clickByText(new String[]{"提交", "确定", "举报"}, 0);
        sleep(800);
        return submitted ? "已提交举报" : "已选择举报原因, 请确认提交";
    }

    /** 当前界面是否出现了含"举报"的菜单项 */
    private static boolean menuHasReport() {
        AccessibilityNodeInfo root = AccessService.root();
        if (root == null) return false;
        List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByText("举报");
        if (list == null) return false;
        for (AccessibilityNodeInfo n : list) {
            if (n.isVisibleToUser()) return true;
        }
        return false;
    }

    /** 在当前界面按文本依次查找并点击; skip>0 表示跳过前 N 个同名匹配 */
    private static boolean clickByText(String[] texts, int skip) {
        AccessibilityNodeInfo root = AccessService.root();
        if (root == null) return false;
        for (String t : texts) {
            List<AccessibilityNodeInfo> list =
                    root.findAccessibilityNodeInfosByText(t);
            if (list == null) continue;
            int idx = 0;
            for (AccessibilityNodeInfo n : list) {
                if (!n.isVisibleToUser()) continue;
                if (idx++ < skip) continue;
                CharSequence cs = n.getText();
                String s = cs == null ? "" : cs.toString();
                if (!s.contains(t) && (n.getContentDescription() == null
                        || !n.getContentDescription().toString().contains(t))) continue;
                if (NodeUtil.click(NodeUtil.put(n))) return true;
            }
        }
        return false;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }
}
