package com.sixzs.local;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

public class Logx {
    public static final String TAG = "DMSX";
    private static final int MAX_LINES = 80;
    private static final ArrayDeque<String> LINES = new ArrayDeque<String>();
    private static final SimpleDateFormat TS = new SimpleDateFormat("HH:mm:ss", Locale.US);

    private static synchronized void add(String s) {
        LINES.addLast("[" + TS.format(new Date()) + "] " + s);
        while (LINES.size() > MAX_LINES) LINES.removeFirst();
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = LINES.iterator();
        while (it.hasNext()) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(it.next());
        }
        FloatService.setLog(sb.toString());
    }

    public static void i(String s) {
        Log.i(TAG, s);
        add(s);
    }

    public static void e(String s, Throwable t) {
        Log.e(TAG, s, t);
        add(s + (t == null ? "" : ": " + t));
    }

    public static void e(String s) {
        e(s, null);
    }

    public static synchronized void clear() {
        LINES.clear();
        FloatService.setLog("");
    }
}
