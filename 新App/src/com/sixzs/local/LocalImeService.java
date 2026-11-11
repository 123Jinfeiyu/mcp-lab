package com.sixzs.local;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.inputmethodservice.InputMethodService;

public class LocalImeService extends InputMethodService {

    public static volatile LocalImeService I;
    private static final Handler UI = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        I = this;
        Logx.i("本地输入法已启动");
    }

    @Override
    public void onDestroy() {
        I = null;
        super.onDestroy();
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @Override
    public View onCreateInputView() {
        return null;
    }

    public static boolean ready() {
        LocalImeService s = I;
        try {
            return s != null && s.getCurrentInputConnection() != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean commit(String text) {
        final LocalImeService s = I;
        if (s == null) return false;
        final String t = text == null ? "" : text;
        UI.post(new Runnable() {
            public void run() {
                try {
                    InputConnection ic = s.getCurrentInputConnection();
                    if (ic != null) ic.commitText(t, t.length());
                } catch (Exception e) {
                }
            }
        });
        return true;
    }

    public static boolean deleteOne() {
        final LocalImeService s = I;
        if (s == null) return false;
        UI.post(new Runnable() {
            public void run() {
                try {
                    InputConnection ic = s.getCurrentInputConnection();
                    if (ic != null) ic.deleteSurroundingText(1, 0);
                } catch (Exception e) {
                }
            }
        });
        return true;
    }

    public static boolean isEnabled(Context c) {
        try {
            InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
            for (InputMethodInfo i : imm.getEnabledInputMethodList()) {
                if (i.getPackageName().equals(c.getPackageName())) return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    public static boolean isSelected(Context c) {
        try {
            String id = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
            return id != null && id.startsWith(c.getPackageName() + "/");
        } catch (Exception e) {
            return false;
        }
    }
}
