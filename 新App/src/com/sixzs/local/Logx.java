package com.sixzs.local;

import android.util.Log;

public class Logx {
    public static final String TAG = "DMSX";

    public static void i(String s) {
        Log.i(TAG, s);
        FloatService.setStatus(s);
    }

    public static void e(String s, Throwable t) {
        Log.e(TAG, s, t);
    }
}
