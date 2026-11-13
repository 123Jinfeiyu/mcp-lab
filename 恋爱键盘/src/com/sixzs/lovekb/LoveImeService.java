package com.sixzs.lovekb;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * 恋爱键盘(输入法)
 *
 * 为什么做成输入法:
 *   Android 10+ 只有「默认输入法」和「前台应用」能读剪贴板,
 *   普通悬浮窗读不到别人复制的内容。成为输入法后才有读取权限,
 *   而且候选回复可以直接 commitText 写进聊天框。
 */
public class LoveImeService extends InputMethodService {

    public static volatile LoveImeService I;

    private TextView imeStatus;
    private LinearLayout imeReplies;
    /** 候选回复(与悬浮窗共享) */
    public static final List<String> SHARED = new ArrayList<String>();

    @Override
    public void onCreate() {
        super.onCreate();
        I = this;
        Logx.i("恋爱键盘输入法已启动");
    }

    @Override
    public void onDestroy() {
        I = null;
        super.onDestroy();
    }

    @Override
    public View onCreateInputView() {
        View v = getLayoutInflater().inflate(R.layout.ime_panel, null);
        imeStatus = (TextView) v.findViewById(R.id.imeStatus);
        imeReplies = (LinearLayout) v.findViewById(R.id.imeReplies);

        v.findViewById(R.id.imeRead).setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                readAndGenerate();
            }
        });
        v.findViewById(R.id.imeGen).setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                generate();
            }
        });
        v.findViewById(R.id.imeSwitch).setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                switchIme();
            }
        });
        renderShared();
        return v;
    }

    @Override
    public void onStartInputView(android.view.inputmethod.EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        renderShared();
    }

    /** 作为输入法, 剪贴板可读 */
    private String readClip() {
        try {
            android.content.ClipboardManager cm =
                    (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null) return "";
            android.content.ClipData d = cm.getPrimaryClip();
            if (d == null || d.getItemCount() == 0) return "";
            CharSequence t = d.getItemAt(0).getText();
            return t == null ? "" : t.toString().trim();
        } catch (Throwable t) {
            return "";
        }
    }

    /** 供广播/脚本调用: 读剪贴板并生成 */
    public void readAndGeneratePublic() {
        readAndGenerate();
    }

    /** 供广播/脚本调用: 把第 n 条候选写进当前输入框 */
    public void commitIndex(int index) {
        List<String> list = new ArrayList<String>(SHARED);
        if (list.isEmpty()) {
            Logx.i("输入法还没有候选回复");
            status("还没有候选回复");
            return;
        }
        int i = Math.max(0, Math.min(list.size() - 1, index));
        commit(list.get(i));
    }

    /** 供广播/脚本调用: 读剪贴板并生成 */
    private void readAndGenerate() {
        String msg = readClip();
        if (msg.isEmpty()) {
            status("剪贴板是空的, 先复制对方的消息");
            return;
        }
        status("已读取: " + brief(msg));
        Logx.i("输入法读取剪贴板: " + brief(msg));
        LoveEngine.setPeerMsg(this, msg);
        generate();
    }

    private void generate() {
        String msg = LoveEngine.peerMsg(this);
        if (msg.isEmpty()) {
            msg = readClip();
            LoveEngine.setPeerMsg(this, msg);
        }
        if (msg.isEmpty()) {
            status("没有可分析的消息");
            return;
        }
        status("AI 分析中...");
        final Context ctx = LoveImeService.this;
        final String msg2 = msg;
        new Thread(new Runnable() {
            public void run() {
                try {
                    final LoveAI.Result r = LoveEngine.analyze(ctx, msg2);
                    SHARED.clear();
                    SHARED.addAll(r.replies);
                    LoveEngine.setReplies(ctx, r.replies);
                    final String t = "【" + r.type + "】" + r.intent;
                    post(new Runnable() {
                        public void run() {
                            status(t);
                            renderShared();
                        }
                    });
                } catch (final Throwable e) {
                    post(new Runnable() {
                        public void run() {
                            status("生成失败: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    /** 展示候选, 点一下直接写进输入框 */
    private void renderShared() {
        if (imeReplies == null) return;
        imeReplies.removeAllViews();
        final List<String> list = new ArrayList<String>(SHARED);
        for (final String rep : list) {
            TextView tv = new TextView(this);
            tv.setText(rep);
            tv.setTextSize(14);
            tv.setTextColor(0xFF333333);
            tv.setBackgroundResource(R.drawable.bg_reply);
            tv.setPadding(12, 10, 12, 10);
            tv.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    commit(rep);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = 6;
            imeReplies.addView(tv, lp);
        }
    }

    /** 由悬浮窗调用: 直接写入并触发发送(本输入法是当前输入法时最可靠) */
    public boolean commitAndSend(String text) {
        try {
            android.view.inputmethod.InputConnection ic = getCurrentInputConnection();
            if (ic == null) return false;
            ic.commitText(text, text.length());
            Logx.i("输入法已写入: " + brief(text));
            android.view.inputmethod.EditorInfo info = getCurrentInputEditorInfo();
            int action = info != null && info.actionId != 0
                    ? info.actionId : android.view.inputmethod.EditorInfo.IME_ACTION_SEND;
            boolean ok = ic.performEditorAction(action);
            Logx.i(ok ? "输入法已触发发送动作" : "输入法写入完成(未触发发送动作)");
            return ok;
        } catch (Throwable t) {
            return false;
        }
    }

    private void commit(String text) {
        try {
            android.view.inputmethod.InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.commitText(text, text.length());
                Logx.i("输入法已写入: " + brief(text));
                status("已输入, 可直接发送");
                // 开了自动发送 → 触发对方的"发送"动作
                try {
                    if (Config.load(this).autoSend) {
                        android.view.inputmethod.EditorInfo info = getCurrentInputEditorInfo();
                        int action = info != null ? info.actionId
                                : android.view.inputmethod.EditorInfo.IME_ACTION_SEND;
                        if (ic.performEditorAction(action)) {
                            Logx.i("输入法已触发发送动作");
                            status("已发送");
                        }
                    }
                } catch (Throwable e) {
                }
            }
        } catch (Throwable t) {
            status("写入失败");
        }
    }

    private void switchIme() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
        } catch (Throwable t) {
        }
    }

    private void status(final String s) {
        Logx.i("输入法: " + s);
        if (imeStatus == null) return;
        post(new Runnable() {
            public void run() {
                try {
                    imeStatus.setText(s);
                } catch (Throwable t) {
                }
            }
        });
    }

    private void post(Runnable r) {
        try {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(r);
        } catch (Throwable t) {
        }
    }

    private static String brief(String s) {
        if (s == null) return "";
        return s.length() <= 20 ? s : s.substring(0, 20) + "…";
    }
}
