package com.sixzs.local;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 导出当前界面上"可操作元素"清单(文本 + 归一化中心坐标),
 * 交给 GLM 挑选, 这样点击位置来自系统节点树, 而不是模型目测, 精度高得多。
 */
public class UiElements {

    public static class Item {
        public String text = "";
        public String cls = "";
        public String id = "";
        public int x1, y1, x2, y2;   // 屏幕像素
        public float cx, cy;         // 0-1000 归一化中心
    }

    public static List<Item> collect(int max) {
        List<Item> out = new ArrayList<Item>();
        AccessibilityNodeInfo root = AccessService.root();
        if (root == null) return out;
        walk(root, out, max);
        return out;
    }

    private static void walk(AccessibilityNodeInfo n, List<Item> out, int max) {
        if (n == null || out.size() >= max) return;
        try {
            boolean clickable = n.isClickable();
            CharSequence t = n.getText();
            CharSequence d = n.getContentDescription();
            String text = t == null ? "" : t.toString().trim();
            String desc = d == null ? "" : d.toString().trim();
            String id = "";
            try {
                if (n.getViewIdResourceName() != null) {
                    id = n.getViewIdResourceName();
                    int k = id.lastIndexOf('/');
                    if (k >= 0) id = id.substring(k + 1);
                }
            } catch (Throwable e) {
            }
            // 可点击的控件 / 有文字的文本 / 有描述的图标, 都要收进来
            boolean useful = clickable || !text.isEmpty() || (!desc.isEmpty() && desc.length() <= 30);
            if (useful) {
                Rect b = new Rect();
                n.getBoundsInScreen(b);
                if (b.width() > 0 && b.height() > 0) {
                    Item it = new Item();
                    it.text = text.isEmpty() ? desc : text;
                    it.cls = n.getClassName() == null ? "" : n.getClassName().toString();
                    it.id = id;
                    it.x1 = b.left; it.y1 = b.top; it.x2 = b.right; it.y2 = b.bottom;
                    out.add(it);
                }
            }
            for (int i = 0; i < n.getChildCount() && out.size() < max; i++) {
                AccessibilityNodeInfo c = n.getChild(i);
                if (c != null) {
                    walk(c, out, max);
                    c.recycle();
                }
            }
        } catch (Throwable t) {
        }
    }

    /** 生成给模型看的元素清单文本(0-1000 归一化坐标) */
    public static String describe(int screenW, int screenH, int max) {
        List<Item> items = collect(max);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Item it : items) {
            if (it.text.isEmpty() && it.id.isEmpty()) continue;
            it.cx = screenW > 0 ? (it.x1 + it.x2) / 2f * 1000f / screenW : 0;
            it.cy = screenH > 0 ? (it.y1 + it.y2) / 2f * 1000f / screenH : 0;
            String label = it.text.isEmpty() ? it.id : it.text;
            sb.append(i++).append(". ").append(label);
            if (!it.text.isEmpty() && !it.id.isEmpty()) sb.append(" (").append(it.id).append(")");
            sb.append(" @(").append(Math.round(it.cx)).append(",").append(Math.round(it.cy)).append(")\n");
            if (i >= 40) break;
        }
        return sb.toString();
    }
}
