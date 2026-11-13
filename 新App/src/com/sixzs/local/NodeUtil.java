package com.sixzs.local;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class NodeUtil {

    private static final SparseArray<AccessibilityNodeInfo> CACHE = new SparseArray<AccessibilityNodeInfo>();
    private static int NEXT_ID = 1;

    public static void clear() {
        CACHE.clear();
        NEXT_ID = 1;
    }

    public static int put(AccessibilityNodeInfo n) {
        int id = NEXT_ID++;
        CACHE.put(id, n);
        return id;
    }

    public static AccessibilityNodeInfo get(int nid) {
        return CACHE.get(nid);
    }

    private static boolean inRegion(AccessibilityNodeInfo n, int l, int t, int r, int b) {
        if (r <= l || b <= t) return true;
        Rect rc = new Rect();
        n.getBoundsInScreen(rc);
        return rc.right > l && rc.left < r && rc.bottom > t && rc.top < b;
    }

    private static boolean match(AccessibilityNodeInfo n, JSONObject sel) {
        if (sel == null) return true;
        String cls = sel.optString("class", null);
        String id = sel.optString("id", null);
        String text = sel.optString("text", null);
        String desc = sel.optString("desc", null);
        if (cls != null && !cls.isEmpty()) {
            CharSequence c = n.getClassName();
            if (c == null || !c.toString().equals(cls)) return false;
        }
        if (id != null && !id.isEmpty()) {
            String v = n.getViewIdResourceName();
            if (v == null) return false;
            if (!(v.equals(id) || v.endsWith(id) || id.endsWith(v))) return false;
        }
        if (text != null && !text.isEmpty()) {
            CharSequence c = n.getText();
            if (c == null) return false;
            String v = c.toString();
            if (!v.equals(text) && !v.contains(text)) return false;
        }
        if (desc != null && !desc.isEmpty()) {
            CharSequence c = n.getContentDescription();
            if (c == null) return false;
            String v = c.toString();
            if (!v.equals(desc) && !v.contains(desc)) return false;
        }
        return true;
    }

    public static JSONObject toJson(AccessibilityNodeInfo n, int nid) {
        try {
            JSONObject o = new JSONObject();
            o.put("nid", nid);
            o.put("text", n.getText() == null ? "" : n.getText().toString());
            o.put("desc", n.getContentDescription() == null ? "" : n.getContentDescription().toString());
            o.put("cls", n.getClassName() == null ? "" : n.getClassName().toString());
            o.put("id", n.getViewIdResourceName() == null ? "" : n.getViewIdResourceName());
            o.put("pkg", n.getPackageName() == null ? "" : n.getPackageName().toString());
            Rect rc = new Rect();
            n.getBoundsInScreen(rc);
            JSONObject b = new JSONObject();
            b.put("l", rc.left);
            b.put("t", rc.top);
            b.put("r", rc.right);
            b.put("b", rc.bottom);
            o.put("bounds", b);
            return o;
        } catch (Exception e) {
            return null;
        }
    }

    private static List<AccessibilityNodeInfo> all() {
        List<AccessibilityNodeInfo> list = new ArrayList<AccessibilityNodeInfo>();
        AccessibilityNodeInfo root = AccessService.root();
        if (root == null) return list;
        ArrayDeque<AccessibilityNodeInfo> q = new ArrayDeque<AccessibilityNodeInfo>();
        q.add(root);
        int guard = 0;
        while (!q.isEmpty() && guard++ < 5000) {
            AccessibilityNodeInfo n = q.poll();
            list.add(n);
            for (int i = 0; i < n.getChildCount(); i++) {
                AccessibilityNodeInfo c = n.getChild(i);
                if (c != null) q.add(c);
            }
        }
        return list;
    }

    public static JSONObject findOne(int l, int t, int r, int b, JSONObject sel) {
        for (AccessibilityNodeInfo n : all()) {
            if (inRegion(n, l, t, r, b) && match(n, sel)) {
                return toJson(n, put(n));
            }
        }
        return null;
    }

    public static JSONArray findAll(int l, int t, int r, int b, JSONObject sel, boolean onlyVisible) {
        JSONArray arr = new JSONArray();
        for (AccessibilityNodeInfo n : all()) {
            if (!inRegion(n, l, t, r, b)) continue;
            if (!match(n, sel)) continue;
            if (onlyVisible && !n.isVisibleToUser()) continue;
            JSONObject o = toJson(n, put(n));
            if (o != null) arr.put(o);
        }
        return arr;
    }

    public static JSONObject parent(int nid) {
        AccessibilityNodeInfo n = get(nid);
        if (n == null) return null;
        AccessibilityNodeInfo p = n.getParent();
        if (p == null) return null;
        return toJson(p, put(p));
    }

    public static JSONObject preNode(int nid) {
        AccessibilityNodeInfo n = get(nid);
        if (n == null) return null;
        AccessibilityNodeInfo p = n.getParent();
        if (p == null) return null;
        for (int i = 0; i < p.getChildCount(); i++) {
            AccessibilityNodeInfo c = p.getChild(i);
            if (c == null) continue;
            if (c.equals(n)) {
                return i > 0 ? toJson(p.getChild(i - 1), put(p.getChild(i - 1))) : null;
            }
        }
        return null;
    }

    public static JSONObject findIn(int nid, JSONObject sel) {
        AccessibilityNodeInfo n = get(nid);
        if (n == null) return null;
        ArrayDeque<AccessibilityNodeInfo> q = new ArrayDeque<AccessibilityNodeInfo>();
        q.add(n);
        int guard = 0;
        while (!q.isEmpty() && guard++ < 3000) {
            AccessibilityNodeInfo x = q.poll();
            if (!x.equals(n) && match(x, sel)) return toJson(x, put(x));
            for (int i = 0; i < x.getChildCount(); i++) {
                AccessibilityNodeInfo c = x.getChild(i);
                if (c != null) q.add(c);
            }
        }
        return null;
    }

    public static boolean click(int nid) {
        AccessibilityNodeInfo n = get(nid);
        if (n == null) return false;
        AccessibilityNodeInfo cur = n;
        for (int depth = 0; depth < 6 && cur != null; depth++) {
            if (cur.isClickable() && cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
            cur = cur.getParent();
        }
        Rect rc = new Rect();
        n.getBoundsInScreen(rc);
        return AccessService.tap(rc.centerX(), rc.centerY());
    }

    public static boolean setText(int nid, String text) {
        AccessibilityNodeInfo n = get(nid);
        if (n == null) return false;
        Bundle b = new Bundle();
        b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        return n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b);
    }

    public static boolean inputFocused(String text) {
        AccessibilityNodeInfo root = AccessService.root();
        if (root == null) return false;
        AccessibilityNodeInfo f = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (f == null) return false;
        Bundle b = new Bundle();
        b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        return f.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b);
    }

    public static boolean pasteFocused() {
        AccessibilityNodeInfo root = AccessService.root();
        if (root == null) return false;
        AccessibilityNodeInfo f = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (f == null) return false;
        f.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        return f.performAction(AccessibilityNodeInfo.ACTION_PASTE);
    }
}
