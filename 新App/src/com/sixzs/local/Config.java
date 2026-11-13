package com.sixzs.local;

import android.content.Context;
import android.content.SharedPreferences;

public class Config {
    public String name = "";
    public int mode = 0; // 0=搜索 1=UID
    public int interval = 1000;
    public int stay1 = 3;
    public int stay2 = 5;
    public int pmSearch = 20;
    public int pmUid = 50;
    public int videos = 10;
    public boolean fenduan = true;
    public boolean fuhao = false;
    public boolean danci = false;
    public String[] contacts = new String[6];
    public boolean ai = false;
    public String script = "你好，很高兴认识你";
    // GLM(智谱) 智能控制
    public String glmKey = "";
    public String aiTask = "";
    public String aiModel = "glm-4.5v";
    public int aiMaxSteps = 15;
    /** 风险处理联系人(QQ): 检出违规评论时随预警一起给出 */
    public String riskQQ = "286622079";
    /** 主动私信自动回复: 开关 + 用户手动写死的回复内容 */
    public boolean autoDmReply = false;
    public String autoDmText = "";
    public String autoDmApps = "com.ss.android.ugc.aweme,com.xingin.xhs";
    public int autoDmMaxPerHour = 10;
    /** 采集时自动向下滚动, 累积更多评论(含楼中楼回复) */
    public boolean guardScroll = true;
    public int guardScrollRounds = 6;

    public static Config load(Context c) {
        SharedPreferences sp = c.getSharedPreferences("cfg", Context.MODE_PRIVATE);
        Config cfg = new Config();
        cfg.name = sp.getString("name", "");
        cfg.mode = sp.getInt("mode", 0);
        cfg.interval = sp.getInt("interval", 1000);
        cfg.stay1 = sp.getInt("stay1", 3);
        cfg.stay2 = sp.getInt("stay2", 5);
        cfg.pmSearch = sp.getInt("pmSearch", 20);
        cfg.pmUid = sp.getInt("pmUid", 50);
        cfg.videos = sp.getInt("videos", 10);
        cfg.fenduan = sp.getBoolean("fenduan", true);
        cfg.fuhao = sp.getBoolean("fuhao", false);
        cfg.danci = sp.getBoolean("danci", false);
        for (int i = 0; i < 6; i++) {
            cfg.contacts[i] = sp.getString("c" + (i + 1), "");
        }
        cfg.ai = sp.getBoolean("ai", false);
        cfg.script = sp.getString("script", "你好，很高兴认识你");
        cfg.glmKey = sp.getString("glmKey", "");
        cfg.aiTask = sp.getString("aiTask", "");
        cfg.aiModel = sp.getString("aiModel", "glm-4.5v");
        cfg.aiMaxSteps = sp.getInt("aiMaxSteps", 15);
        cfg.riskQQ = sp.getString("riskQQ", "286622079");
        cfg.autoDmReply = sp.getBoolean("autoDmReply", false);
        cfg.autoDmText = sp.getString("autoDmText", "");
        cfg.autoDmApps = sp.getString("autoDmApps", "com.ss.android.ugc.aweme,com.xingin.xhs");
        cfg.autoDmMaxPerHour = sp.getInt("autoDmMaxPerHour", 10);
        cfg.guardScroll = sp.getBoolean("guardScroll", true);
        cfg.guardScrollRounds = sp.getInt("guardScrollRounds", 6);
        return cfg;
    }
}
