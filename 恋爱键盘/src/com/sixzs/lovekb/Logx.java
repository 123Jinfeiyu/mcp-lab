package com.sixzs.lovekb;

import android.util.Log;

/** 日志: 同时进 logcat 和悬浮窗面板 */
public class Logx {
    private static final String TAG = "LOVEKB";
    public static volatile Consumer sink = null;

    public static void i(String s) {
        Log.i(TAG, s);
        Consumer c = sink;
        if (c != null) {
            try {
                c.accept(s);
            } catch (Throwable t) {
            }
        }
    }

    /**
     * 只进 logcat, 不显示在悬浮窗状态区
     * 用于技术性日志(类型切换、内存回收等), 免得把状态区刷满
     */
    public static void quiet(String s) {
        Log.i(TAG, s);
    }

    public static void e(String s, Throwable t) {
        Log.e(TAG, s, t);
        i(s + (t == null ? "" : ": " + t.getMessage()));
    }

    public interface Consumer {
        void accept(String s);
    }
}
