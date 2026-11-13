package com.sixzs.lovekb;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** 智谱(清言) API 客户端 */
public class Glm {

    private static final String URL_CHAT = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    /** 默认视觉模型(实测最快) */
    public static final String DEFAULT_VISION = "glm-4.6v";

    /** 单次对话(纯文本) */
    public static String chat(String key, String model, String system, String user) throws Exception {
        return chat(key, model, system, user, null, 0);
    }

    /**
     * 单次对话(可带截图, 走多模态视觉模型)
     *
     * @param maxTokensOverride &lt;=0 时自动取值(带图 2500 / 纯文本 1200)
     */
    public static String chat(String key, String model, String system, String user, byte[] jpeg,
                              int maxTokensOverride) throws Exception {
        JSONArray messages = new JSONArray();
        messages.put(msg("system", system));

        boolean withImage = jpeg != null && jpeg.length > 1000;
        if (withImage) {
            JSONArray content = new JSONArray();
            JSONObject img = new JSONObject();
            JSONObject iu = new JSONObject();
            iu.put("url", "data:image/jpeg;base64," + android.util.Base64.encodeToString(jpeg, android.util.Base64.NO_WRAP));
            img.put("image_url", iu);
            img.put("type", "image_url");
            content.put(img);
            JSONObject txt = new JSONObject();
            txt.put("type", "text");
            txt.put("text", user);
            content.put(txt);
            JSONObject um = new JSONObject();
            um.put("role", "user");
            um.put("content", content);
            messages.put(um);
        } else {
            messages.put(msg("user", user));
        }

        int maxTokens = maxTokensOverride > 0 ? maxTokensOverride : (withImage ? 2500 : 1200);
        String vModel = withImage ? visionModel(model) : (model == null || model.trim().isEmpty() ? "glm-4-flash" : model.trim());
        if (withImage) Logx.i("带截图 → 视觉模型 " + vModel);

        for (int round = 0; round < 2; round++) {
            JSONObject body = new JSONObject();
            body.put("model", vModel);
            body.put("messages", messages);
            body.put("temperature", 0.85);
            body.put("max_tokens", maxTokens);

            String raw = send(key, body);
            JSONObject o = new JSONObject(raw);
            JSONObject msgObj = o.getJSONArray("choices").getJSONObject(0).getJSONObject("message");
            String content = msgObj.optString("content", "");
            String reasoning = msgObj.optString("reasoning_content", "");
            if (content != null && content.trim().length() > 0) return content;

            // 思考型模型: 思考吃光了 token, 把思考带上再问一次要最终答案
            Logx.i("模型正文为空(思考型模型), 第" + (round + 1) + "轮续写");
            if (reasoning != null && reasoning.trim().length() > 0) {
                messages.put(msg("assistant", reasoning));
                messages.put(msg("user", "不要再分析, 现在直接只输出最终的 JSON 数组结果。"));
            }
            maxTokens = Math.max(maxTokens, 2500);
            if (reasoning == null || reasoning.trim().isEmpty()) break;
        }
        return "";
    }

    /**
     * 纯文本快速对话: 用于阶段2(基于已读出的文本生成回复)
     * 不做思考型续写, 拿不到正文就直接返回, 避免白等
     */
    public static String chatFast(String key, String model, String system, String user) throws Exception {
        JSONArray messages = new JSONArray();
        messages.put(msg("system", system));
        messages.put(msg("user", user));
        JSONObject body = new JSONObject();
        body.put("model", model == null || model.trim().isEmpty() ? "glm-4-flash" : model.trim());
        body.put("messages", messages);
        body.put("temperature", 0.85);
        body.put("max_tokens", 1000);
        String raw = send(key, body);
        try {
            JSONObject o = new JSONObject(raw);
            JSONObject m = o.getJSONArray("choices").getJSONObject(0).getJSONObject("message");
            String c = m.optString("content", "");
            if (c != null && c.trim().length() > 0) return c;
        } catch (Throwable t) {
            Logx.e("快速对话解析失败", t);
        }
        return "";
    }

    /**
     * 统一的 HTTP 发送: 成功返回原始响应体, 失败抛异常
     */
    private static String send(String key, JSONObject body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(URL_CHAT).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(20000);
        c.setReadTimeout(120000);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("Authorization", "Bearer " + (key == null ? "" : key.trim()));
        OutputStream os = c.getOutputStream();
        os.write(body.toString().getBytes("UTF-8"));
        os.flush();
        os.close();
        int code = c.getResponseCode();
        String raw = code == 200 ? readAll(c.getInputStream()) : readAll(c.getErrorStream());
        if (code != 200) throw new Exception("HTTP " + code + " " + tail(raw, 160));
        return raw;
    }

    /**
     * 带图时确保用支持视觉的模型
     *
     * 实测(同一张真实截图, 同一段提示词, 2026-11):
     *   glm-4.5v          40-57 秒, 输入 2元/百万
     *   glm-4.6v           5.8-8.4 秒, 输入 1元/百万  ← 默认
     *   GLM-4.6V-Flash     9-12 秒, 免费但偶发 429
     * 换 4.6v 比 4.5v 快 7 倍且便宜一半。
     */
    static String visionModel(String model) {
        String m = model == null ? "" : model.trim().toLowerCase();
        if (m.contains("4v") || m.contains("4.5v") || m.contains("4.6v") || m.contains("vision")
                || m.contains("-v-") || m.contains("4o") || m.contains("5v")) {
            return model;
        }
        return DEFAULT_VISION;
    }

    private static JSONObject msg(String role, String content) throws Exception {
        JSONObject o = new JSONObject();
        o.put("role", role);
        o.put("content", content);
        return o;
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        in.close();
        return new String(bo.toByteArray(), "UTF-8");
    }

    private static String tail(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(s.length() - n);
    }
}
