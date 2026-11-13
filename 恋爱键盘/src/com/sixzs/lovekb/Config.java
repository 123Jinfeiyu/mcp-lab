package com.sixzs.lovekb;

import android.content.Context;
import android.content.SharedPreferences;

/** 配置: API Key / 模型 / 风格偏好 / 条数 */
public class Config {
    public String glmKey = "";
    public String model = "glm-4-flash";     // 纯文本, 用 flash 更快更省
    /** 额外风格偏好, 会拼进提示词, 例: 温柔一点 / 更幽默 / 短句 */
    public String style = "";
    /** 每种类型给几条回复 */
    public int replyCount = 3;
    /** 分类体系: 逗号分隔 */
    public String types = "幽默型,搭讪型,温柔型,高冷型,直球型,共情型,反撩型,暧昧试探型,冷淡敷衍,求助型";
    /** 目标 App 包名, 填入时只允许在这些 App 里操作(空=不限制) */
    public String targetApps = "";
    /** 点候选回复后是否直接发送(关=只填入输入框) */
    public boolean autoSend = true;

    /** 视觉模型(带截图时用)。实测 glm-4.6v 比 glm-4.5v 快 7 倍 */
    public String visionModel = "glm-4.6v";

    /** 截图长边上限(px), 0=不缩放。1280 是清晰度与体积的平衡点 */
    public int shotMaxEdge = 1280;

    /** 截图 JPEG 质量 */
    public int shotQuality = 70;

    /** 是否把截图裁剪到聊天区域(省 token, 减少干扰) */
    public boolean shotCrop = true;

    /** 当前选中的回复类型(悬浮窗下拉框), 空=自动判断(用全部类型) */
    public String currentType = "";

    private static final String P = "cfg";

    public static Config load(Context c) {
        Config cfg = new Config();
        SharedPreferences sp = c.getSharedPreferences(P, Context.MODE_PRIVATE);
        cfg.glmKey = sp.getString("glmKey", "");
        cfg.model = sp.getString("model", "glm-4-flash");
        cfg.style = sp.getString("style", "");
        cfg.replyCount = sp.getInt("replyCount", 3);
        cfg.types = sp.getString("types", cfg.types);
        cfg.targetApps = sp.getString("targetApps", "");
        cfg.autoSend = sp.getBoolean("autoSend", true);
        cfg.visionModel = sp.getString("visionModel", "glm-4.6v");
        cfg.shotMaxEdge = sp.getInt("shotMaxEdge", 1280);
        cfg.shotQuality = sp.getInt("shotQuality", 70);
        cfg.shotCrop = sp.getBoolean("shotCrop", true);
        cfg.currentType = sp.getString("currentType", "");
        return cfg;
    }

    public void save(Context c) {
        SharedPreferences.Editor ed = c.getSharedPreferences(P, Context.MODE_PRIVATE).edit();
        ed.putString("glmKey", glmKey);
        ed.putString("model", model);
        ed.putString("style", style);
        ed.putInt("replyCount", replyCount);
        ed.putString("types", types);
        ed.putString("targetApps", targetApps);
        ed.putBoolean("autoSend", autoSend);
        ed.putString("visionModel", visionModel);
        ed.putInt("shotMaxEdge", shotMaxEdge);
        ed.putInt("shotQuality", shotQuality);
        ed.putBoolean("shotCrop", shotCrop);
        ed.putString("currentType", currentType);
        ed.apply();
    }
}
