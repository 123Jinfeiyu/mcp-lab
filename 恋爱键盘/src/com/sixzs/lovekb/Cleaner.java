package com.sixzs.lovekb;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * 内存清理
 *
 * 恋爱键盘会攒下几样占内存的东西:
 *   ① 截图 Bitmap      —— 一张 1080x2400 ARGB 约 10MB
 *   ② 候选回复卡片      —— 每次生成都新建 TextView, 不释放就一直挂着
 *   ③ 聊天上下文/消息   —— 换人聊天时上一个会话的痕迹
 *   ④ 后台录屏会话      —— VirtualDisplay + ImageReader 常驻(最大的一块)
 *
 * 「清理」按钮做 ①②③ + 主动 GC;
 * 「清上下文」只做 ③(换人聊天时用, 不影响录屏会话)。
 */
public class Cleaner {

    /** 清理结果 */
    public static class Report {
        public int freedBytes = 0;
        public int clearedCards = 0;
        public boolean stoppedCapture = false;
        public boolean clearedContext = false;

        public String text() {
            StringBuilder sb = new StringBuilder();
            if (freedBytes > 0) sb.append("释放 ").append(freedBytes / 1024).append("KB");
            if (clearedCards > 0) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("清掉 ").append(clearedCards).append(" 条卡片");
            }
            if (clearedContext) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("上下文已清");
            }
            if (stoppedCapture) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("录屏已停");
            }
            if (sb.length() == 0) sb.append("没有可释放的内容");
            return sb.toString();
        }
    }

    /**
     * 清理候选回复区(把 TextView 从父容器摘掉并回收)
     * @return 摘掉的卡片数
     */
    public static int clearCards(LinearLayout box) {
        if (box == null) return 0;
        int n = box.getChildCount();
        for (int i = 0; i < n; i++) {
            View v = box.getChildAt(i);
            if (v == null) continue;
            try {
                if (v instanceof TextView) {
                    TextView tv = (TextView) v;
                    tv.setText("");
                    CharSequence cs = tv.getText();
                    if (cs != null && cs instanceof String) {
                        // 断开引用, 让 String 也能回收
                        tv.setText("");
                    }
                }
                v.setOnClickListener(null);
                v.setBackgroundResource(0);
                if (v.getParent() instanceof ViewGroup) {
                    ((ViewGroup) v.getParent()).removeView(v);
                }
            } catch (Throwable t) {
            }
        }
        box.removeAllViews();
        return n;
    }

    /**
     * 完整清理: 截图 + 卡片 + 上下文 (+可选停止录屏会话)
     */
    public static Report cleanup(FloatService fs, boolean stopCapture) {
        Report r = new Report();

        // ① 截图
        try {
            r.freedBytes += ScreenCap.releaseShot();
        } catch (Throwable t) {
        }

        // ② 候选卡片
        if (fs != null) {
            try {
                r.clearedCards += clearCards(fs.repliesBox());
            } catch (Throwable t) {
            }
            try {
                fs.clearRepliesView();
            } catch (Throwable t) {
            }
        }

        // ③ 上下文 / 对方消息 / 候选(持久化)
        if (fs != null) {
            try {
                r.clearedContext = true;
                LoveEngine.clearAll(fs);
                fs.clearPeerMessage();
            } catch (Throwable t) {
            }
        }

        // ④ 录屏会话(最大的一块内存: VirtualDisplay + ImageReader)
        if (stopCapture) {
            try {
                ScreenCap.stop(fs);
                r.stoppedCapture = true;
            } catch (Throwable t) {
            }
        }

        // 主动回收
        try {
            Runtime rt = Runtime.getRuntime();
            rt.gc();
            try {
                System.runFinalization();
            } catch (Throwable t) {
            }
            r.freedBytes += 0;
        } catch (Throwable t) {
        }
        return r;
    }

    /** 只清上下文(换人聊天时用) */
    public static Report clearContextOnly(FloatService fs) {
        Report r = new Report();
        if (fs != null) {
            try {
                LoveEngine.clearAll(fs);
                fs.clearPeerMessage();
                r.clearedContext = true;
            } catch (Throwable t) {
            }
            try {
                r.clearedCards += clearCards(fs.repliesBox());
                fs.clearRepliesView();
            } catch (Throwable t) {
            }
        }
        return r;
    }

    /** 当前内存占用摘要(字节) */
    public static String memoryText() {
        try {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) / 1024;
            long max = rt.maxMemory() / 1024;
            int shot = ScreenCap.shotBytes() / 1024;
            return "内存 " + used + "KB/" + max + "KB"
                    + (shot > 0 ? " · 截图 " + shot + "KB" : "");
        } catch (Throwable t) {
            return "";
        }
    }
}
