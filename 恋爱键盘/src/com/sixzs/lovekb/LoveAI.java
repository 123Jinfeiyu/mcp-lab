package com.sixzs.lovekb;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 恋爱消息分类 + 回复生成
 *
 * 输入: 对方发来的一条消息
 * 输出: 类型(幽默型/搭讪型/...) + 意图 + N 条可直接发送的候选回复
 */
public class LoveAI {

    public static class Result {
        public String type = "";
        public String intent = "";
        public List<String> replies = new ArrayList<String>();
        public String raw = "";
        /** 阶段1 从截图里读到的对方原话 */
        public String readMsg = "";
        /** 阶段1 没能从截图里读到聊天内容 */
        public boolean noContent = false;
        /** 各阶段耗时(毫秒), 供 UI 展示 */
        public long msVision = 0;
        public long msText = 0;
        public String imageInfo = "";

        public String timing() {
            StringBuilder sb = new StringBuilder();
            if (msVision > 0) sb.append("读图 ").append(msVision / 1000.0f).append("s");
            if (msText > 0) {
                if (sb.length() > 0) sb.append(" + ");
                sb.append("生成 ").append(msText / 1000.0f).append("s");
            }
            return sb.toString();
        }
    }

    /** 阶段1: 视觉模型只负责"读出对方说了什么", 不写回复 */
    private static final String READ_SYSTEM =
            "你是一个截图阅读助手。给你一张手机聊天界面的截图, 你只需要读出对方(对方)发来的消息内容。\n"
            + "要求:\n"
            + "1. 找出对方最新发的那条消息原文, 原样抄写, 不要改写、不要翻译、不要总结;\n"
            + "2. 如果截图里有对方发的图片/表情/视频, 在 visual 里简短描述一下;\n"
            + "3. 如果截图里根本没有聊天内容, latest 留空, topic 写 \"看不到聊天内容\";\n"
            + "4. 严格只输出 JSON, 不要任何解释。\n"
            + "格式: {\"latest\":\"对方最新消息原文\",\"topic\":\"聊天话题,10字内\",\"visual\":\"画面补充,20字内,没有就留空\"}";

    /** 阶段2: 纯文本模型, 基于读出的文本生成回复 */
    private static final String WRITE_SYSTEM =
            "你是一个中文恋爱聊天助手。你要读懂对方发来的消息, 判断它的类型与意图, 并写出可以直接发出去的回复。\n"
            + "要求:\n"
            + "1. 严格只输出 JSON 数组, 不要任何解释或多余文字;\n"
            + "2. 回复要口语化、像真人打字, 不要书面语, 不要解释自己在开玩笑, 不要用括号表情说明;\n"
            + "3. 回复不要复述对方原话, 不要以『我』开头说教, 不要长篇大论;\n"
            + "4. 分寸感: 对方冷淡就留台阶, 对方热络就接住热度, 不要过度热情或油腻;\n"
            + "5. 绝不给对方承诺做不到的事, 不编造事实。";

    public static Result analyze(Config cfg, String peerMsg) throws Exception {
        return analyze(cfg, peerMsg, null, null);
    }

    /**
     * 带上下文的分析
     * @param context  聊天记录(最近若干条, 从旧到新), 可为空
     * @param shotJpeg 预处理后的截图(JPEG), 可为空; 非空则走视觉通道
     */
    public static Result analyze(Config cfg, String peerMsg, java.util.List<String> context,
                                 byte[] shotJpeg) throws Exception {
        return analyze(cfg, peerMsg, context, shotJpeg, false);
    }

    /**
     * @param retryFull  true = 整屏重试(裁剪裁错了时用)
     */
    public static Result analyze(Config cfg, String peerMsg, java.util.List<String> context,
                                 byte[] shotJpeg, boolean retryFull) throws Exception {
        int n = Math.max(1, Math.min(5, cfg.replyCount));
        Result r = new Result();

        // ---------- 阶段1: 有截图 → 视觉模型读出对方原话 ----------
        String effectiveMsg = peerMsg == null ? "" : peerMsg.trim();
        boolean viaVision = shotJpeg != null && shotJpeg.length > 1000;
        if (viaVision) {
            long t0 = System.currentTimeMillis();
            try {
                String readOut = Glm.chat(cfg.glmKey, cfg.visionModel, READ_SYSTEM,
                        "请读出这张截图里对方发来的消息。", shotJpeg, 1500);
                r.msVision = System.currentTimeMillis() - t0;
                String latest = jsonStr(readOut, "latest");
                String topic = jsonStr(readOut, "topic");
                String visual = jsonStr(readOut, "visual");
                r.readMsg = latest;
                Logx.i("阶段1读图 " + (r.msVision / 1000.0f) + "s: " + brief(latest)
                        + (topic.isEmpty() ? "" : " | 话题:" + topic)
                        + (visual.isEmpty() ? "" : " | 画面:" + visual));
                // 视觉读到的比剪贴板/节点树的更接近"对方原话", 优先采用
                if (!latest.isEmpty() && looksLikeUiNoise(latest)) {
                    Logx.i("读到的像界面文案而非聊天内容: " + brief(latest));
                    r.noContent = true;
                } else if (!latest.isEmpty()) {
                    effectiveMsg = latest;
                } else {
                    // 截图里没有聊天内容: 标记出来, 阶段2 不该硬编
                    r.noContent = true;
                    Logx.i("阶段1未读到聊天内容(截图里可能没有聊天窗口)");
                }
                if (!visual.isEmpty()) {
                    // 画面信息作为上下文的一部分
                    if (context == null) context = new ArrayList<String>();
                    context = new ArrayList<String>(context);
                    context.add("[对方发了图片/表情] " + visual);
                }
                if (topic.isEmpty()) topic = "";
                else context = addTopic(context, topic);
            } catch (Throwable t) {
                r.msVision = System.currentTimeMillis() - t0;
                Logx.e("阶段1读图失败, 降级为纯文本", t);
            }
        }

        // ---------- 阶段2: 纯文本模型分类 + 写回复(快且便宜) ----------
        long t1 = System.currentTimeMillis();
        String out;
        if (r.noContent && effectiveMsg.startsWith("(请从截图")) {
            // 截图里确实没有聊天内容, 没有任何可分析的素材 -> 不硬编
            out = "";
            Logx.i("没有可分析的素材, 跳过生成");
        } else {
            out = writeReplies(cfg, effectiveMsg, context, n);
        }
        r.msText = System.currentTimeMillis() - t1;

        Result parsed = parse(out, n);
        parsed.noContent = r.noContent;
        parsed.readMsg = r.readMsg;
        parsed.msVision = r.msVision;
        parsed.msText = r.msText;
        return parsed;
    }

    private static java.util.List<String> addTopic(java.util.List<String> ctx, String topic) {
        if (ctx == null) return ctx;
        java.util.List<String> out = new ArrayList<String>(ctx);
        out.add("[聊天话题] " + topic);
        return out;
    }

    /** 阶段2: 用纯文本模型生成回复 */
    private static String writeReplies(Config cfg, String peerMsg, java.util.List<String> context,
                                       int n) throws Exception {
        String types = cfg.types == null || cfg.types.trim().isEmpty()
                ? "幽默型,搭讪型,温柔型,高冷型,直球型,共情型,反撩型,暧昧试探型,冷淡敷衍,求助型" : cfg.types.trim();
        // 悬浮窗下拉框选定了具体类型就只用那一个
        if (cfg.currentType != null && !cfg.currentType.trim().isEmpty()) {
            types = cfg.currentType.trim();
        }

        StringBuilder sb = new StringBuilder();
        if (context != null && !context.isEmpty()) {
            sb.append("这是你们最近的聊天记录(从早到晚):\n");
            int n2 = Math.min(context.size(), 12);
            for (int k = context.size() - n2; k < context.size(); k++) {
                sb.append("- ").append(context.get(k)).append("\n");
            }
            sb.append("\n");
        }
        sb.append("对方最新发来的消息:\n").append(peerMsg).append("\n\n");
        sb.append("请结合上面的聊天记录, 判断对方现在的状态与意图, 并写出可以直接发出去的回复。\n");
        sb.append("输出格式为 JSON 数组, 每项:\n");
        sb.append("{\"type\":\"类型\",\"intent\":\"对方意图,不超过15字\",\"replies\":[");
        for (int k = 0; k < n; k++) sb.append(k > 0 ? ",\"" : "\"");
        sb.append("回复1\",\"回复2\",\"回复3\"]}\n");
        sb.append("type 必须从这些里选一个: ").append(types).append("\n");
        sb.append("replies 数量为 ").append(n).append(", 每条 15-40 字, 风格各不相同(有的俏皮、有的温柔、有的简短)\n");
        sb.append("要接住上文已经聊过的话题, 不要重复问对方已经说过的事。\n");
        if (cfg.style != null && !cfg.style.trim().isEmpty()) {
            sb.append("额外风格要求: ").append(cfg.style.trim()).append("\n");
        }
        sb.append("严格只输出 JSON。");

        // 有截图时用快速通道(不思考, 拿不到就退回完整通道)
        String out = Glm.chatFast(cfg.glmKey, cfg.model, WRITE_SYSTEM, sb.toString());
        if (out == null || out.trim().isEmpty()) {
            Logx.i("快速通道无输出, 退回完整通道");
            out = Glm.chat(cfg.glmKey, cfg.model, WRITE_SYSTEM, sb.toString());
        }
        return out;
    }

    /**
     * 判断读出来的是不是「界面文案」而不是对方说的话
     * 抖音/小红书这类 App 顶部有搜索提示、底部有导航词, 裁剪不当就会读到它们
     */
    static boolean looksLikeUiNoise(String s) {
        if (s == null || s.trim().isEmpty()) return true;
        String t = s.trim();
        String[] noise = {
                "大家都在搜", "综合", "视频", "用户", "推荐", "关注", "搜索",
                "直播", "团购", "同城", "经验", "热点", "首页", "朋友", "消息", "我",
                "AI 解析", "评论", "回复", "点赞", "收藏", "分享", "更多", "点击",
                "输入", "发送", "搜索框", "登录", "关注作者"};
        for (String k : noise) {
            if (t.equals(k) || t.length() <= 4 && t.contains(k)) return true;
        }
        // 明显是界面词组(全是导航词拼接)
        if (t.length() <= 12) {
            int hit = 0;
            for (String k : noise) if (t.contains(k)) hit++;
            if (hit >= 2) return true;
        }
        return false;
    }

    /** 从模型输出里取某个字符串字段 */
    private static String jsonStr(String raw, String key) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.startsWith("```")) {
            int f = s.indexOf('\n');
            int e = s.lastIndexOf("```");
            if (f > 0 && e > f) s = s.substring(f + 1, e).trim();
        }
        try {
            org.json.JSONObject o = new org.json.JSONObject(s);
            String v = o.optString(key, "");
            return v == null ? "" : v.trim();
        } catch (Throwable e) {
            // 退化: 正则找
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(s);
            if (m.find()) return m.group(1).trim();
            return "";
        }
    }

    private static String brief(String s) {
        if (s == null) return "";
        return s.length() <= 40 ? s : s.substring(0, 40) + "…";
    }

    /** 容错解析: 修全角标点 -> JSON -> 失败则正则兜底 */
    static Result parse(String raw, int n) {
        Result r = new Result();
        r.raw = raw == null ? "" : raw;
        String json = r.raw.trim();
        // 去掉 markdown 代码块包裹
        if (json.startsWith("```")) {
            int f = json.indexOf('\n');
            int e = json.lastIndexOf("```");
            if (f > 0 && e > f) json = json.substring(f + 1, e).trim();
        }
        int a = json.indexOf('[');
        int b = json.lastIndexOf(']');
        if (a >= 0 && b > a) json = json.substring(a, b + 1);
        json = json.replace('：', ':').replace('，', ',').replace('“', '"')
                .replace('”', '"').replace('｛', '{').replace('｝', '}')
                .replace('［', '[').replace('］', ']');

        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                r.type = o.optString("type", "");
                r.intent = o.optString("intent", "");
                JSONArray reps = o.optJSONArray("replies");
                if (reps != null) {
                    for (int k = 0; k < reps.length(); k++) {
                        String s = reps.optString(k, "").trim();
                        if (!s.isEmpty()) r.replies.add(s);
                    }
                }
            }
            if (!r.type.isEmpty() || !r.replies.isEmpty()) return r;
        } catch (Throwable e) {
            Logx.i("解析失败, 改用容错提取; 原始输出: " + head(r.raw, 300));
        }
        // 正则兜底
        java.util.regex.Matcher mm = java.util.regex.Pattern.compile("\\{[^}]*\\}").matcher(json);
        while (mm.find()) {
            String one = mm.group();
            String type = str(one, "type");
            String intent = str(one, "intent");
            if (!type.isEmpty()) r.type = type;
            if (!intent.isEmpty()) r.intent = intent;
            // 只取 replies 数组里的字符串
            int idx = one.indexOf("replies");
            if (idx > 0) {
                String tail = one.substring(idx);
                java.util.regex.Matcher rm = java.util.regex.Pattern
                        .compile(":\\s*\"([^\"]{2,120})\"").matcher(tail);
                while (rm.find()) {
                    String s = rm.group(1).trim();
                    if (!s.isEmpty() && !s.equals("replies")) r.replies.add(s);
                }
            }
        }
        return r;
    }

    private static String head(String s, int n) {
        if (s == null) return "(null)";
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }

    private static String str(String s, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(s);
        return m.find() ? m.group(1) : "";
    }
}
