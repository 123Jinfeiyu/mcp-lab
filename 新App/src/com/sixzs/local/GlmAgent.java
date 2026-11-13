package com.sixzs.local;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GLM(智谱) 智能体: 截图 -> 视觉模型决策 -> 解析动作 -> 无障碍/输入法执行
 * Open-AutoGLM 风格, 但完全跑在手机本地, 不依赖电脑。
 */
public class GlmAgent {

    public static final String SYSTEM =
            "你是安卓手机智能控制助手(与 Open-AutoGLM 同类)。用户给你一个【操作任务】和当前屏幕截图,\n"
                    + "你的职责是通过每一步一个动作, 亲手把任务做完, 而不是描述界面。\n\n"
                    + "输出格式(严格两行, 不要输出其它内容):\n"
                    + "思考: <一句话说明当前看到了什么、下一步为什么这么做>\n"
                    + "动作: <恰好一个动作调用>\n\n"
                    + "铁律:\n"
                    + "1. 任务尚未完成时, 严禁输出 finished(...)。必须先给出能推进任务的动作。\n"
                    + "2. 不要只描述界面内容, 必须给出可执行的动作。\n"
                    + "3. 坐标一律用【0-1000 的整数】表示: 整张图的宽和高分别映射到 0-1000,\n"
                    + "   即 0=图最左边/最上边, 500=正中间, 1000=图最右边/最下边。不要用像素。\n"
                    + "4. 每步只输出一个动作, 需要多步就下一步继续。\n"
                    + "5. 元素看不清就先 swipe 浏览, 找到目标再 tap。\n\n"
                    + "可用动作:\n"
                    + "tap(px, py)                    点击(px/py 为 0-1000 归一化坐标)\n"
                    + "swipe(px1, py1, px2, py2, 300)  滑动(前四个为 0-1000 归一化坐标)\n"
                    + "text(\"内容\")                   输入文本\n"
                    + "back()                         返回\n"
                    + "home()                         回主屏\n"
                    + "launch(\"包名\")                 启动App(包名必须用下面表里的, 不要自己猜)\n"
                    + "wait(1000)                     等待毫秒\n"
                    + "finished(\"任务结果说明\")        仅当任务确实完成时使用\n\n"
                    + "常用App包名对照:\n"
                    + "抖音 = com.ss.android.ugc.aweme\n"
                    + "小红书 = com.xingin.xhs\n"
                    + "快手 = com.smile.gifmaker\n"
                    + "微信 = com.tencent.mm\n"
                    + "QQ = com.tencent.mobileqq\n"
                    + "本应用(AI私信助手本地版) = com.sixzs.local\n"
                    + "微博 = com.sina.weibo\n"
                    + "淘宝 = com.taobao.taobao";

    private static final String URL_CHAT = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    private static final int MAX_EDGE = 1080; // 送模型前的长边上限

    private static volatile boolean running = false;
    private static volatile boolean stopFlag = false;
    private static volatile int step = 0;
    private static volatile String lastAction = "";
    private static Thread worker;
    private static Context ctx;

    public static boolean isRunning() { return running; }
    public static int getStep() { return step; }
    public static String getLastAction() { return lastAction; }

    /** 供宿主注入 Context (MainActivity / FloatService 调用) */
    public static void setContext(Context c) {
        if (c != null) ctx = c.getApplicationContext();
    }

    // ------------------------------------------------------------ 生命周期

    public static synchronized void start(Context c, final String task, final String key,
                                          final String model, final int maxSteps) {
        if (running) return;
        if (key == null || key.trim().isEmpty()) {
            Logx.i("AI: 未填 API Key");
            return;
        }
        if (task == null || task.trim().isEmpty()) {
            Logx.i("AI: 未填任务");
            return;
        }
        ctx = c.getApplicationContext();
        stopFlag = false;
        running = true;
        FloatService.setOverlayVisible(false); // 隐藏悬浮窗, 免得遮挡屏幕/吃掉点击
        worker = new Thread(new Runnable() {
            public void run() {
                try {
                    loop(task, key, model, maxSteps);
                } catch (Throwable t) {
                    Logx.e("AI异常", t);
                } finally {
                    running = false;
                    step = 0;
                    FloatService.setOverlayVisible(true);
                    Logx.i("AI 已停止");
                }
            }
        }, "glm-agent");
        worker.start();
    }

    public static synchronized void stop() {
        stopFlag = true;
        Logx.i("AI: 收到停止指令");
    }

    // ------------------------------------------------------------ 主循环

    private static void loop(String task, String key, String model, int maxSteps) {
        Logx.i("AI 开始任务: " + task + " (最多" + maxSteps + "步)");
        JSONArray messages = new JSONArray();
        try {
            messages.put(msg("system", SYSTEM));
        } catch (Exception e) {
        }
        for (int i = 1; i <= maxSteps; i++) {
            if (stopFlag) { Logx.i("AI 已手动停止"); return; }
            step = i;

            byte[] jpeg = shotJpeg();
            if (jpeg == null) {
                Logx.i("AI: 截屏失败, 请先点「截图授权」(API<30 需要)");
                return;
            }
            try {
                JSONArray content = new JSONArray();
                JSONObject img = new JSONObject();
                JSONObject iu = new JSONObject();
                iu.put("url", "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.NO_WRAP));
                img.put("image_url", iu);
                img.put("type", "image_url");
                content.put(img);

                // 附上系统节点树的可操作元素清单, 让模型直接用精确坐标, 不靠目测
                StringBuilder prompt = new StringBuilder();
                prompt.append("任务: ").append(task);
                prompt.append("\n当前是第 ").append(i).append(" 步, 请输出思考与动作。");
                int sw = screenW(), sh = screenH();
                prompt.append("\n屏幕尺寸: ").append(sw).append("x").append(sh).append(", 坐标一律用 0-1000 归一化(左上=0,0 右下=1000,1000)。");
                String els = "";
                try {
                    els = UiElements.describe(sw, sh, 60);
                } catch (Throwable e) {
                }
                if (!els.isEmpty()) {
                    prompt.append("\n\n当前界面上可操作的元素(编号 文本 @归一化中心坐标), "
                            + "点击这些元素时请直接使用清单里的坐标, 不要自己目测:\n").append(els);
                }
                JSONObject txt = new JSONObject();
                txt.put("type", "text");
                txt.put("text", prompt.toString());
                content.put(txt);
                JSONObject um = new JSONObject();
                um.put("role", "user");
                um.put("content", content);
                messages.put(um);

                String reply = callChat(key, model, messages);
                String think = pick(reply, "思考");
                String action = pick(reply, "动作");

                // 模型只描述不动手 -> 追问一次, 逼它给出动作
                if (action.isEmpty() || action.startsWith("finished")) {
                    try {
                        JSONObject um2 = new JSONObject();
                        um2.put("role", "user");
                        um2.put("content", "任务还没完成。请只输出一行, 以 动作: 开头, 内容为一个动作调用, 不要描述界面。");
                        messages.put(um2);
                        String reply2 = callChat(key, model, messages);
                        String a2 = pick(reply2, "动作");
                        if (!a2.isEmpty() && !a2.startsWith("finished")) {
                            reply = reply2;
                            think = pick(reply2, "思考");
                            action = a2;
                        }
                    } catch (Exception e) {
                        Logx.i("AI 追问失败: " + e.getMessage());
                    }
                }

                try {
                    messages.put(msg("assistant", reply));
                } catch (Exception ignored) {
                }
                Logx.i("AI第" + i + "步 思考: " + (think.isEmpty() ? "(无)" : think));
                if (action.isEmpty()) {
                    Logx.i("AI 未输出动作: " + tail(reply, 120));
                    return;
                }
                lastAction = action;
                Logx.i("AI第" + i + "步 动作: " + action);

                String done = exec(action);
                if (done != null) {
                    Logx.i("AI 任务完成: " + done);
                    return;
                }
                sleep(1200);
            } catch (Exception e) {
                Logx.i("AI 调用失败: " + e.getMessage());
                return;
            }
        }
        Logx.i("AI 达到最大步数, 任务未确认完成");
    }

    // ------------------------------------------------------------ 能力封装

    /** 截图 -> JPEG(缩放) */
    static byte[] shotJpeg() {
        Bitmap bmp = AccessService.screenshot(0, 0, 0, 0);
        if (bmp == null) return null;
        try {
            int w = bmp.getWidth(), h = bmp.getHeight();
            int edge = Math.max(w, h);
            Bitmap small = bmp;
            if (edge > MAX_EDGE) {
                float k = (float) MAX_EDGE / edge;
                small = Bitmap.createScaledBitmap(bmp, Math.round(w * k), Math.round(h * k), true);
                bmp.recycle();
            }
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            small.compress(Bitmap.CompressFormat.JPEG, 78, bo);
            small.recycle();
            byte[] jpg = bo.toByteArray();
            saveDebugShot(jpg);
            return jpg;
        } catch (Throwable t) {
            return null;
        }
    }

    /** 把 AI 实际看到的画面存下来, 便于核对识别是否准确 */
    static void saveDebugShot(byte[] jpg) {
        try {
            Context c = ctx;
            java.io.File dir = null;
            if (c != null) dir = c.getExternalFilesDir(null); // 免权限, 可直接 adb pull
            if (dir == null && c != null) dir = c.getFilesDir();
            if (dir == null) return;
            java.io.File f = new java.io.File(dir, "ai_last.jpg");
            java.io.FileOutputStream out = new java.io.FileOutputStream(f);
            out.write(jpg);
            out.close();
            Logx.i("AI 画面已存: " + f.getAbsolutePath());
        } catch (Throwable t) {
            Logx.e("存AI画面失败", t);
        }
    }

    /** 调 GLM, 返回文本 */
    static String callChat(String key, String model, JSONArray messages) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", model == null || model.trim().isEmpty() ? "glm-4.5v" : model.trim());
        body.put("messages", messages);
        body.put("temperature", 0.1);
        HttpURLConnection c = (HttpURLConnection) new URL(URL_CHAT).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(20000);
        c.setReadTimeout(120000);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("Authorization", "Bearer " + key.trim());
        OutputStream os = c.getOutputStream();
        os.write(body.toString().getBytes("UTF-8"));
        os.flush();
        os.close();
        int code = c.getResponseCode();
        String raw;
        if (code == 200) {
            raw = readAll(c.getInputStream());
        } else {
            raw = readAll(c.getErrorStream());
            throw new Exception("HTTP " + code + " " + tail(raw, 150));
        }
        JSONObject o = new JSONObject(raw);
        return o.getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content");
    }

    /** 纯文本调用(不带图), 供内容审核等场景使用 */
    static String callChatText(String key, String model, String prompt) throws Exception {
        JSONArray messages = new JSONArray();
        messages.put(msg("system", "你是一个严谨的中文内容审核助手, 只按要求输出结果, 不要多余解释。"));
        messages.put(msg("user", prompt));
        return callChat(key, model, messages);
    }

    /** 单次问答(不执行动作), 供测试按钮与 Lua 调用 */
    public static String askOnce(String key, String model, String prompt) throws Exception {
        JSONArray messages = new JSONArray();
        messages.put(msg("system", SYSTEM));
        byte[] jpeg = shotJpeg();
        if (jpeg != null) {
            JSONArray content = new JSONArray();
            JSONObject img = new JSONObject();
            JSONObject iu = new JSONObject();
            iu.put("url", "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.NO_WRAP));
            img.put("image_url", iu);
            img.put("type", "image_url");
            content.put(img);
            JSONObject txt = new JSONObject();
            txt.put("type", "text");
            txt.put("text", prompt);
            content.put(txt);
            JSONObject um = new JSONObject();
            um.put("role", "user");
            um.put("content", content);
            messages.put(um);
        } else {
            JSONObject um = new JSONObject();
            um.put("role", "user");
            um.put("content", "【注意: 未获取到手机画面, 请只根据文字回答】\n" + prompt);
            messages.put(um);
            Logx.i("AI: 未获取到画面, 请先点「截图授权」(API<30 需要)");
        }
        return callChat(key, model, messages);
    }

    // ------------------------------------------------------------ 动作解析与执行

    /** 执行动作, 返回非 null 表示任务结束 */
    static String exec(String action) {
        String a = action.trim();
        Matcher m;

        m = Pattern.compile("^tap\\s*\\(\\s*(-?[\\d.]+)\\s*,\\s*(-?[\\d.]+)\\s*\\)$").matcher(a);
        if (m.matches()) {
            float[] p = pct(Float.parseFloat(m.group(1)), Float.parseFloat(m.group(2)));
            AccessService.tap(p[0], p[1]);
            return null;
        }
        m = Pattern.compile("^swipe\\s*\\(\\s*(-?[\\d.]+)\\s*,\\s*(-?[\\d.]+)\\s*,\\s*(-?[\\d.]+)\\s*,\\s*(-?[\\d.]+)\\s*(?:,\\s*(\\d+)\\s*)?\\)$").matcher(a);
        if (m.matches()) {
            float[] p1 = pct(Float.parseFloat(m.group(1)), Float.parseFloat(m.group(2)));
            float[] p2 = pct(Float.parseFloat(m.group(3)), Float.parseFloat(m.group(4)));
            long dur = m.group(5) == null ? 300 : Long.parseLong(m.group(5));
            AccessService.swipe(p1[0], p1[1], p2[0], p2[1], dur);
            return null;
        }
        m = Pattern.compile("^text\\s*\\(\\s*[\"'](.*)[\"']\\s*\\)$", Pattern.DOTALL).matcher(a);
        if (m.matches()) {
            String t = m.group(1);
            if (LocalImeService.ready()) {
                LocalImeService.commit(t);
            } else {
                NodeUtil.inputFocused(t);
            }
            Logx.i("AI 输入: " + t);
            return null;
        }
        if (a.matches("^back\\s*\\(\\s*\\)$")) {
            AccessService.globalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
            return null;
        }
        if (a.matches("^home\\s*\\(\\s*\\)$")) {
            AccessService.globalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);
            return null;
        }
        m = Pattern.compile("^launch\\s*\\(\\s*[\"']([^\"']+)[\"']\\s*\\)$").matcher(a);
        if (m.matches()) {
            String pkg = m.group(1);
            try {
                Intent it = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
                if (it != null) {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(it);
                    Logx.i("AI 启动: " + pkg);
                } else {
                    Logx.i("AI 启动失败, 未安装: " + pkg);
                }
            } catch (Throwable t) {
                Logx.i("AI 启动异常: " + t.getMessage());
            }
            return null;
        }
        m = Pattern.compile("^wait\\s*\\(\\s*(\\d+)\\s*\\)$").matcher(a);
        if (m.matches()) {
            sleep(Integer.parseInt(m.group(1)));
            return null;
        }
        m = Pattern.compile("^finished\\s*\\(\\s*[\"'](.*)[\"']\\s*\\)$", Pattern.DOTALL).matcher(a);
        if (m.matches()) return m.group(1);

        Logx.i("AI 动作无法识别: " + a);
        return null;
    }

    /**
     * 截图可能被缩放过, 坐标要还原到屏幕真实像素。
     * 这里通过当前屏幕尺寸与截图尺寸的比例换算。
     */
    private static int screenW() {
        try {
            android.util.DisplayMetrics dm = null;
            if (ctx != null) dm = ctx.getResources().getDisplayMetrics();
            else if (AccessService.I != null) dm = AccessService.I.getResources().getDisplayMetrics();
            if (dm != null) return dm.widthPixels;
        } catch (Throwable t) {
        }
        return 0;
    }

    private static int screenH() {
        try {
            android.util.DisplayMetrics dm = null;
            if (ctx != null) dm = ctx.getResources().getDisplayMetrics();
            else if (AccessService.I != null) dm = AccessService.I.getResources().getDisplayMetrics();
            if (dm != null) return dm.heightPixels;
        } catch (Throwable t) {
        }
        return 0;
    }

    /**
     * 归一化坐标(0-1000) -> 屏幕真实像素。
     * GLM-4V 系列原生按 0-1000 输出 grounding 坐标, 这样不受图片缩放影响。
     */
    private static float[] pct(float px, float py) {
        float w = screenW();
        float h = screenH();
        if (w <= 0 || h <= 0) {
            Bitmap b = AccessService.screenshot(0, 0, 0, 0);
            if (b != null) {
                w = b.getWidth();
                h = b.getHeight();
                b.recycle();
            }
        }
        if (w <= 0 || h <= 0) return new float[]{px, py};
        float x = px / 1000f * w;
        float y = py / 1000f * h;
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > w) x = w;
        if (y > h) y = h;
        return new float[]{x, y};
    }

    // ------------------------------------------------------------ 工具

    private static JSONObject msg(String role, String content) throws Exception {
        JSONObject o = new JSONObject();
        o.put("role", role);
        o.put("content", content);
        return o;
    }

    private static String pick(String reply, String key) {
        for (String line : reply.split("\n")) {
            String l = line.trim();
            if (l.startsWith(key)) {
                int i = l.indexOf(':');
                if (i < 0) i = l.indexOf('：');
                if (i > 0) return l.substring(i + 1).trim();
            }
        }
        return "";
    }

    private static String tail(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(s.length() - n);
    }

    private static String readAll(java.io.InputStream in) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) > 0) bo.write(buf, 0, len);
        in.close();
        return bo.toString("UTF-8");
    }

    private static void sleep(long ms) {
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) {
            if (stopFlag) return;
            try {
                Thread.sleep(Math.min(100, end - System.currentTimeMillis()));
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
