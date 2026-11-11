package com.sixzs.local;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Host {

    public static Context appContext;

    private static final Handler UI = new Handler(Looper.getMainLooper());

    private static JSONArray args(String json) {
        try {
            return new JSONArray(json == null ? "[]" : json);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static String s(JSONArray a, int i) {
        return a.optString(i, "");
    }

    private static int n(JSONArray a, int i) {
        return a.optInt(i, 0);
    }

    private static long l(JSONArray a, int i) {
        return a.optLong(i, 0);
    }

    private static String js(Object o) {
        return o == null ? "null" : String.valueOf(o);
    }

    public static String dispatch(String method, String jsonArgs) {
        final JSONArray a = args(jsonArgs);
        try {
            if ("sleep".equals(method)) {
                long ms = l(a, 0);
                long end = System.currentTimeMillis() + ms;
                while (System.currentTimeMillis() < end) {
                    if (LuaHost.stopping) throw new RuntimeException("__stop__");
                    Thread.sleep(Math.min(100, Math.max(1, end - System.currentTimeMillis())));
                }
                return "true";
            }
            if ("toast".equals(method)) {
                final String t = s(a, 0);
                UI.post(new Runnable() {
                    public void run() {
                        Toast.makeText(appContext, t, Toast.LENGTH_SHORT).show();
                    }
                });
                return "true";
            }
            if ("log".equals(method) || "hud".equals(method)) {
                Logx.i(s(a, 0));
                return "true";
            }
            if ("tickCount".equals(method)) {
                return String.valueOf(System.currentTimeMillis());
            }
            if ("getDisplaySize".equals(method)) {
                android.util.DisplayMetrics dm = appContext.getResources().getDisplayMetrics();
                JSONArray r = new JSONArray();
                r.put(dm.widthPixels);
                r.put(dm.heightPixels);
                return r.toString();
            }
            if ("keyPress".equals(method)) {
                String k = s(a, 0);
                int act = "home".equals(k) ? AccessService.GLOBAL_ACTION_HOME : AccessService.GLOBAL_ACTION_BACK;
                return js(AccessService.globalAction(act));
            }
            if ("tap".equals(method)) {
                return js(AccessService.tap((float) a.optDouble(0, 0), (float) a.optDouble(1, 0)));
            }
            if ("swipe".equals(method)) {
                return js(AccessService.swipe((float) a.optDouble(0, 0), (float) a.optDouble(1, 0),
                        (float) a.optDouble(2, 0), (float) a.optDouble(3, 0), l(a, 4)));
            }
            if ("inputText".equals(method)) {
                String t = s(a, 0);
                if (NodeUtil.inputFocused(t)) return "true";
                if (LocalImeService.ready()) {
                    LocalImeService.commit(t);
                    return "true";
                }
                try {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager)
                            appContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("t", t));
                } catch (Exception e) {
                }
                return js(NodeUtil.pasteFocused());
            }
            if ("imeOpen".equals(method)) {
                Intent it = new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS);
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                appContext.startActivity(it);
                return "true";
            }
            if ("imePicker".equals(method)) {
                UI.post(new Runnable() {
                    public void run() {
                        try {
                            ((android.view.inputmethod.InputMethodManager)
                                    appContext.getSystemService(Context.INPUT_METHOD_SERVICE)).showInputMethodPicker();
                        } catch (Exception e) {
                        }
                    }
                });
                return "true";
            }
            if ("runIntent".equals(method)) {
                String action = s(a, 0);
                String uri = s(a, 1);
                Intent it = new Intent(action.isEmpty() ? Intent.ACTION_VIEW : action);
                if (!uri.isEmpty()) it.setData(Uri.parse(uri));
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                appContext.startActivity(it);
                return "true";
            }
            if ("launchApp".equals(method)) {
                Intent it = appContext.getPackageManager().getLaunchIntentForPackage(s(a, 0));
                if (it == null) return "false";
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                appContext.startActivity(it);
                return "true";
            }
            if ("httpGet".equals(method)) {
                return http("GET", s(a, 0), null, 20, null);
            }
            if ("httpPost".equals(method)) {
                long to = a.optLong(2, 20);
                return http("POST", s(a, 0), s(a, 1), to <= 0 ? 20 : to, s(a, 3));
            }
            if ("md5".equals(method)) {
                return md5(s(a, 0).getBytes("UTF-8"));
            }
            if ("encodeUrl".equals(method)) {
                return URLEncoder.encode(s(a, 0), "UTF-8");
            }
            if ("decodeUrl".equals(method)) {
                return URLDecoder.decode(s(a, 0), "UTF-8");
            }
            if ("fileMD5".equals(method)) {
                File f = new File(s(a, 0));
                if (!f.exists()) return "";
                MessageDigest md = MessageDigest.getInstance("MD5");
                FileInputStream in = new FileInputStream(f);
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) md.update(buf, 0, len);
                in.close();
                return md5(md.digest());
            }
            if ("getWorkPath".equals(method)) {
                return appContext.getFilesDir().getAbsolutePath();
            }
            if ("getConfig".equals(method)) {
                return getConfig();
            }
            if ("ocrInit".equals(method)) {
                return js(Ocr.init(appContext));
            }
            if ("pdocr".equals(method)) {
                return ocrRegion(n(a, 0), n(a, 1), n(a, 2), n(a, 3));
            }
            if ("ocrEx".equals(method)) {
                int w = n(a, 0), t = n(a, 1), l = n(a, 2), b = n(a, 3);
                return ocrRegion(l, t, Math.min(l + Math.max(w, 1), 10000), b);
            }
            if ("findMultiColor".equals(method)) {
                return findColor(a);
            }
            if ("shRead".equals(method)) {
                File f = new File(s(a, 0));
                if (!f.exists()) return "null";
                FileInputStream in = new FileInputStream(f);
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) bo.write(buf, 0, len);
                in.close();
                JSONArray r = new JSONArray();
                r.put(new String(bo.toByteArray(), "UTF-8"));
                return r.get(0).toString();
            }
            if ("shWrite".equals(method)) {
                File f = new File(s(a, 0));
                File p = f.getParentFile();
                if (p != null && !p.exists()) p.mkdirs();
                FileOutputStream out = new FileOutputStream(f, false);
                out.write(s(a, 1).getBytes("UTF-8"));
                out.close();
                return "true";
            }
            if ("fileExist".equals(method)) {
                return js(new File(s(a, 0)).exists());
            }
            if ("mkdir".equals(method)) {
                return js(new File(s(a, 0)).mkdirs());
            }
            if ("extractAssets".equals(method)) {
                return js(extractAssets(s(a, 0), s(a, 1)));
            }
            if ("nodeUpdate".equals(method)) {
                NodeUtil.clear();
                return "true";
            }
            if ("nodeFindOne".equals(method)) {
                return nodeFindOne(a);
            }
            if ("nodeFindAll".equals(method)) {
                JSONArray r = NodeUtil.findAll(n(a, 0), n(a, 1), n(a, 2), n(a, 3),
                        a.optJSONObject(4), "1".equals(s(a, 5)) || a.optBoolean(5, false));
                return r.toString();
            }
            if ("nodeClick".equals(method)) {
                return js(NodeUtil.click(n(a, 0)));
            }
            if ("nodeSetText".equals(method)) {
                return js(NodeUtil.setText(n(a, 0), s(a, 1)));
            }
            if ("nodeParent".equals(method)) {
                return js(NodeUtil.parent(n(a, 0)));
            }
            if ("nodePreNode".equals(method)) {
                return js(NodeUtil.preNode(n(a, 0)));
            }
            if ("nodeFindIn".equals(method)) {
                return js(NodeUtil.findIn(n(a, 0), a.optJSONObject(1)));
            }
            if ("stop".equals(method)) {
                LuaHost.stopping = true;
                return "true";
            }
            return "null";
        } catch (Throwable t) {
            Logx.e("host call fail: " + method, t);
            return "null";
        }
    }

    private static String getConfig() {
        try {
            android.content.SharedPreferences sp =
                    appContext.getSharedPreferences("cfg", Context.MODE_PRIVATE);
            JSONObject o = new JSONObject();
            o.put("多选框_主配置", "0");
            StringBuilder sel = new StringBuilder();
            if (sp.getBoolean("fenduan", true)) sel.append(sel.length() == 0 ? "0" : ",0");
            if (sp.getBoolean("fuhao", false)) sel.append(sel.length() == 0 ? "1" : ",1");
            if (sp.getBoolean("danci", false)) sel.append(sel.length() == 0 ? "2" : ",2");
            o.put("多选框_联系", sel.toString());
            o.put("单选框_模式", sp.getInt("mode", 0) == 1 ? "1" : "0");
            o.put("运行间隔", String.valueOf(sp.getInt("interval", 1000)));
            o.put("停留1", String.valueOf(sp.getInt("stay1", 3)));
            o.put("停留2", String.valueOf(sp.getInt("stay2", 5)));
            o.put("私信控制_搜索", String.valueOf(sp.getInt("pmSearch", 20)));
            o.put("私信控制_UID", String.valueOf(sp.getInt("pmUid", 50)));
            o.put("刷视频", String.valueOf(sp.getInt("videos", 10)));
            o.put("输入框_名字", sp.getString("name", ""));
            for (int i = 1; i <= 6; i++) o.put("联系" + i, sp.getString("c" + i, ""));
            o.put("AI话术", sp.getBoolean("ai", false) ? "1" : "0");
            return o.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    private static String ocrRegion(int l, int t, int r, int b) {
        android.graphics.Bitmap bm = AccessService.screenshot(l, t, r, b);
        JSONArray res = Ocr.detect(bm, 0.45f, 0.55f);
        if (bm != null) bm.recycle();
        JSONArray out = new JSONArray();
        if (res != null) {
            for (int i = 0; i < res.length(); i++) {
                JSONObject o = res.optJSONObject(i);
                if (o == null) continue;
                try {
                    JSONObject x = new JSONObject();
                    x.put("text", o.optString("LaBeL", ""));
                    x.put("score", o.optDouble("ProB", 0));
                    double x0 = o.optDouble("X0", 0), y0 = o.optDouble("Y0", 0);
                    double x2 = o.optDouble("X2", x0), y2 = o.optDouble("Y2", y0);
                    JSONObject bd = new JSONObject();
                    bd.put("l", (int) x0);
                    bd.put("t", (int) y0);
                    bd.put("r", (int) x2);
                    bd.put("b", (int) y2);
                    x.put("bounds", bd);
                    out.put(x);
                } catch (Exception e) {
                }
            }
        }
        return out.toString();
    }

    private static int parseColor(String hex) {
        try {
            return 0xFF000000 | Integer.parseInt(hex.trim(), 16);
        } catch (Exception e) {
            return -1;
        }
    }

    private static boolean colorMatch(int c1, int c2, int tol) {
        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        return Math.abs(r1 - r2) <= tol && Math.abs(g1 - g2) <= tol && Math.abs(b1 - b2) <= tol;
    }

    private static String findColor(JSONArray a) {
        int l = n(a, 0), t = n(a, 1), r = n(a, 2), b = n(a, 3);
        String first = s(a, 4);
        String offs = s(a, 5);
        int dir = n(a, 6);
        double sim = a.optDouble(7, 0.9);
        JSONArray out = new JSONArray();
        android.graphics.Bitmap bm = AccessService.screenshot(l, t, r, b);
        if (bm == null) {
            out.put(-1);
            out.put(-1);
            return out.toString();
        }
        int tol = (int) Math.round((1.0 - sim) * 255.0);
        if (tol < 0) tol = 0;
        if (tol > 255) tol = 255;
        int anchor = parseColor(first);
        java.util.List<int[]> pts = new java.util.ArrayList<int[]>();
        if (offs != null && offs.length() > 0) {
            String[] parts = offs.replace(" ", "").split("\\|");
            for (int i = 0; i + 2 < parts.length + 1 && i + 2 <= parts.length - 1; i += 3) {
                try {
                    int c = parseColor(parts[i + 2]);
                    if (c >= 0) pts.add(new int[]{Integer.parseInt(parts[i]), Integer.parseInt(parts[i + 1]), c});
                } catch (Exception e) {
                }
            }
        }
        int W = bm.getWidth(), H = bm.getHeight();
        if (anchor >= 0) {
            if (dir == 1) {
                for (int y = H - 1; y >= 0; y--)
                    for (int x = W - 1; x >= 0; x--)
                        if (colorMatch(bm.getPixel(x, y), anchor, tol) && checkPts(bm, x, y, pts, tol, W, H)) {
                            out.put(x + l);
                            out.put(y + t);
                            bm.recycle();
                            return out.toString();
                        }
            } else {
                for (int y = 0; y < H; y++)
                    for (int x = 0; x < W; x++)
                        if (colorMatch(bm.getPixel(x, y), anchor, tol) && checkPts(bm, x, y, pts, tol, W, H)) {
                            out.put(x + l);
                            out.put(y + t);
                            bm.recycle();
                            return out.toString();
                        }
            }
        }
        bm.recycle();
        out.put(-1);
        out.put(-1);
        return out.toString();
    }

    private static boolean checkPts(android.graphics.Bitmap bm, int x, int y,
                                    java.util.List<int[]> pts, int tol, int W, int H) {
        for (int[] p : pts) {
            int xx = x + p[0], yy = y + p[1];
            if (xx < 0 || yy < 0 || xx >= W || yy >= H) return false;
            if (!colorMatch(bm.getPixel(xx, yy), p[2], tol)) return false;
        }
        return true;
    }

    private static String nodeFindOne(JSONArray a) {
        JSONObject r = NodeUtil.findOne(n(a, 0), n(a, 1), n(a, 2), n(a, 3), a.optJSONObject(4));
        if (r != null && a.optBoolean(5, false)) {
            NodeUtil.click(r.optInt("nid", 0));
        }
        return js(r);
    }

    private static String http(String method, String url, String body, long timeout, String headers) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout((int) (timeout * 1000));
            c.setReadTimeout((int) (timeout * 1000));
            c.setRequestMethod(method);
            if (headers != null && !headers.isEmpty()) {
                for (String line : headers.split("\n")) {
                    int i = line.indexOf(':');
                    if (i > 0) c.setRequestProperty(line.substring(0, i).trim(), line.substring(i + 1).trim());
                }
            }
            if ("POST".equals(method)) {
                c.setDoOutput(true);
                if (c.getRequestProperty("Content-Type") == null) {
                    c.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                }
                OutputStream out = c.getOutputStream();
                out.write((body == null ? "" : body).getBytes("UTF-8"));
                out.close();
            }
            int code = c.getResponseCode();
            InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            if (in != null) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) bo.write(buf, 0, len);
                in.close();
            }
            JSONArray r = new JSONArray();
            r.put(new String(bo.toByteArray(), "UTF-8"));
            r.put(code);
            return r.toString();
        } catch (Exception e) {
            JSONArray r = new JSONArray();
            r.put("");
            r.put(0);
            return r.toString();
        }
    }

    private static boolean extractAssets(String name, String dir) {
        try {
            File d = new File(dir);
            if (!d.exists()) d.mkdirs();
            String assetPath = name;
            InputStream in = appContext.getAssets().open(assetPath);
            byte[] head = new byte[4];
            int n = in.read(head);
            if (n == 4 && head[0] == 'P' && head[1] == 'K') {
                in.close();
                File tmp = new File(d, "__tmp_rc.zip");
                InputStream in2 = appContext.getAssets().open(assetPath);
                FileOutputStream out = new FileOutputStream(tmp);
                byte[] buf = new byte[8192];
                int len;
                while ((len = in2.read(buf)) > 0) out.write(buf, 0, len);
                in2.close();
                out.close();
                ZipFile zf = new ZipFile(tmp);
                Enumeration<? extends ZipEntry> en = zf.entries();
                while (en.hasMoreElements()) {
                    ZipEntry ze = en.nextElement();
                    if (ze.isDirectory()) continue;
                    String base = new File(ze.getName()).getName();
                    if (base.isEmpty()) continue;
                    InputStream zi = zf.getInputStream(ze);
                    FileOutputStream zo = new FileOutputStream(new File(d, base));
                    while ((len = zi.read(buf)) > 0) zo.write(buf, 0, len);
                    zi.close();
                    zo.close();
                }
                zf.close();
                tmp.delete();
                return true;
            }
            FileOutputStream out = new FileOutputStream(new File(d, new File(assetPath).getName()));
            out.write(head, 0, n);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();
            return true;
        } catch (Exception e) {
            Logx.e("extractAssets fail " + name, e);
            return false;
        }
    }

    private static String md5(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data);
        byte[] d = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : d) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
}
