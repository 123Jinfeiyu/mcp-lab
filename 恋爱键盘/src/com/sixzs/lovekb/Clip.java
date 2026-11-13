package com.sixzs.lovekb;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

/** 剪贴板: 读对方发来的消息 / 写回复 */
public class Clip {

    public static String read(Context c) {
        try {
            ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null) return "";
            ClipData d = cm.getPrimaryClip();
            if (d == null || d.getItemCount() == 0) return "";
            CharSequence t = d.getItemAt(0).getText();
            return t == null ? "" : t.toString().trim();
        } catch (Throwable t) {
            return "";
        }
    }

    public static boolean write(Context c, String text) {
        try {
            ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null) return false;
            cm.setPrimaryClip(ClipData.newPlainText("reply", text));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
