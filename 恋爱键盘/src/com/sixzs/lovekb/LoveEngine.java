package com.sixzs.lovekb;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** 悬浮窗与输入法共用的状态与生成逻辑 */
public class LoveEngine {

    private static final String P = "engine";

    /** 多条文本之间的分隔符(不可见控制字符) */
    private static final String SEP = "\u0001";

    /** 对方发来的消息 */
    public static void setPeerMsg(Context c, String msg) {
        c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putString("peerMsg", msg).apply();
    }

    public static String peerMsg(Context c) {
        return c.getSharedPreferences(P, Context.MODE_PRIVATE).getString("peerMsg", "");
    }

    public static void setReplies(Context c, List<String> replies) {
        SharedPreferences.Editor ed = c.getSharedPreferences(P, Context.MODE_PRIVATE).edit();
        ed.putString("replies", String.join(SEP, replies == null ? new ArrayList<String>() : replies));
        ed.apply();
    }

    public static List<String> replies(Context c) {
        String s = c.getSharedPreferences(P, Context.MODE_PRIVATE).getString("replies", "");
        List<String> out = new ArrayList<String>();
        if (s == null || s.isEmpty()) return out;
        for (String x : s.split(SEP)) {
            if (!x.trim().isEmpty()) out.add(x.trim());
        }
        return out;
    }

    /** 聊天上下文 */
    public static void setContext(Context c, java.util.List<String> ctx) {
        c.getSharedPreferences(P, Context.MODE_PRIVATE).edit()
                .putString("context", String.join(SEP, ctx == null ? new ArrayList<String>() : ctx)).apply();
    }

    /**
     * 清空全部会话状态(对方消息 / 上下文 / 候选回复)
     * 用于「清上下文」按钮: 换人聊天时把上一个会话的痕迹清干净
     */
    public static void clearAll(Context c) {
        c.getSharedPreferences(P, Context.MODE_PRIVATE).edit()
                .remove("peerMsg")
                .remove("context")
                .remove("replies")
                .apply();
    }

    /** 当前上下文条数 */
    public static int contextCount(Context c) {
        return context(c).size();
    }

    public static List<String> context(Context c) {
        String s = c.getSharedPreferences(P, Context.MODE_PRIVATE).getString("context", "");
        List<String> out = new ArrayList<String>();
        if (s == null || s.isEmpty()) return out;
        for (String x : s.split(SEP)) {
            if (!x.trim().isEmpty()) out.add(x.trim());
        }
        return out;
    }

    /** 调 AI 分类并生成回复(带上下文) */
    public static LoveAI.Result analyze(Context c, String msg) throws Exception {
        return LoveAI.analyze(Config.load(c), msg, context(c), null);
    }

    /**
     * 读剪贴板, 多策略:
     *  1) 直接读(前台/输入法身份有效)
     *  2) 让输入法服务读(它有剪贴板权限)
     *  3) 空
     */
    public static String readClipboard(Context c) {
        String t = Clip.read(c);
        if (!t.isEmpty()) return t;
        LoveImeService ime = LoveImeService.I;
        if (ime != null) {
            try {
                android.content.ClipboardManager cm =
                        (android.content.ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    android.content.ClipData d = cm.getPrimaryClip();
                    if (d != null && d.getItemCount() > 0) {
                        CharSequence s = d.getItemAt(0).getText();
                        if (s != null && !s.toString().trim().isEmpty()) return s.toString().trim();
                    }
                }
            } catch (Throwable e) {
            }
        }
        return "";
    }
}
