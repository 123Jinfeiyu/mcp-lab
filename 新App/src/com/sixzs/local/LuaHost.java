package com.sixzs.local;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class LuaHost {

    static {
        System.loadLibrary("lua_host");
    }

    public static volatile boolean stopping = false;
    private static Thread worker;

    private static native void nativeInit(String prelude);

    private static native String nativeRun(String code);

    /** 由 JNI 回调的宿主方法入口 */
    public static String call(String method, String jsonArgs) {
        return Host.dispatch(method, jsonArgs);
    }

    private static String readAsset(Context ctx, String path) throws Exception {
        InputStream in = ctx.getAssets().open(path);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) > 0) bo.write(buf, 0, len);
        in.close();
        return new String(bo.toByteArray(), "UTF-8");
    }

    private static void extractLua(Context ctx) throws Exception {
        java.io.File dir = new java.io.File(ctx.getFilesDir(), "lua");
        if (!dir.exists()) dir.mkdirs();
        String[] names = ctx.getAssets().list("lua");
        if (names == null) return;
        for (String name : names) {
            java.io.InputStream in = ctx.getAssets().open("lua/" + name);
            java.io.FileOutputStream out = new java.io.FileOutputStream(new java.io.File(dir, name));
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();
        }
    }

    public static synchronized void start(final Context ctx, final String luaName) {
        if (worker != null) return;
        stopping = false;
        Host.appContext = ctx.getApplicationContext();
        worker = new Thread(new Runnable() {
            public void run() {
                try {
                    extractLua(ctx);
                    nativeInit(readAsset(ctx, "lua/prelude.lua"));
                    String code = readAsset(ctx, "lua/" + luaName);
                    String err = nativeRun(code);
                    if (err != null && err.length() > 0) Logx.i("脚本退出: " + err);
                } catch (Throwable t) {
                    Logx.e("LuaHost 异常", t);
                } finally {
                    worker = null;
                    Logx.i("脚本已停止");
                    FloatService.setRunning(false);
                }
            }
        }, "lua-host");
        worker.start();
    }

    public static synchronized void stop() {
        stopping = true;
        Thread w = worker;
        worker = null;
        if (w != null) w.interrupt();
    }

    public static boolean isRunning() {
        return worker != null;
    }
}
