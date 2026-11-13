package com.sixzs.local;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class LuaHost {

    /** Lua 引擎是否可用(本机架构缺少对应 native 库时为 false) */
    public static volatile boolean nativeReady = false;

    static {
        try {
            System.loadLibrary("lua_host");
            nativeReady = true;
        } catch (Throwable t) {
            nativeReady = false;
            Logx.e("Lua引擎加载失败(本机架构无对应 native 库, 例如 x86 模拟器缺 armeabi 库)", t);
        }
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
            java.io.File dst = new java.io.File(dir, name);
            if (dst.exists()) continue; // 已释放过则跳过, 保留本地修改
            java.io.InputStream in = ctx.getAssets().open("lua/" + name);
            java.io.FileOutputStream out = new java.io.FileOutputStream(dst);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();
        }
    }

    /** 外部脚本覆盖目录: /sdcard/AI私信助手数据/lua/  (改动无需重打包) */
    public static java.io.File overlayDir() {
        return new java.io.File(
                android.os.Environment.getExternalStorageDirectory(), "AI私信助手数据/lua");
    }

    /** 把 sdcard 覆盖目录里的 *.lua 拷进 filesDir/lua, 同名覆盖; 无权限/无文件则跳过 */
    private static void overlayLua(Context ctx) {
        try {
            java.io.File od = overlayDir();
            java.io.File[] fs = od.listFiles();
            if (fs == null) return;
            java.io.File dir = new java.io.File(ctx.getFilesDir(), "lua");
            if (!dir.exists()) dir.mkdirs();
            for (java.io.File f : fs) {
                if (!f.getName().endsWith(".lua")) continue;
                java.io.InputStream in = new java.io.FileInputStream(f);
                java.io.FileOutputStream out = new java.io.FileOutputStream(new java.io.File(dir, f.getName()));
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                in.close();
                out.close();
                Logx.i("热加载: " + f.getName());
            }
        } catch (Throwable t) {
            Logx.e("overlayLua: " + t);
        }
    }

    public static synchronized void start(final Context ctx, final String luaName) {
        if (worker != null) return;
        if (!nativeReady) {
            Logx.i("Lua引擎不可用: 当前设备架构没有对应的 native 库, 固定脚本模式无法运行(AI/风控/私信守候不受影响)");
            return;
        }
        stopping = false;
        Host.appContext = ctx.getApplicationContext();
        worker = new Thread(new Runnable() {
            public void run() {
                try {
                    extractLua(ctx);
                    overlayLua(ctx); // sdcard 覆盖 > APK 内置
                    nativeInit(readAsset(ctx, "lua/prelude.lua"));
                    String code = null;
                    java.io.File local = new java.io.File(
                            new java.io.File(ctx.getFilesDir(), "lua"), luaName);
                    if (local.exists()) {
                        java.io.FileInputStream in = new java.io.FileInputStream(local);
                        ByteArrayOutputStream bo = new ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = in.read(buf)) > 0) bo.write(buf, 0, len);
                        in.close();
                        code = new String(bo.toByteArray(), "UTF-8");
                    } else {
                        code = readAsset(ctx, "lua/" + luaName);
                    }
                    String err = nativeRun(code);
                    if (err != null && err.length() > 0) {
                        if (stopping || err.contains("__stop__")) {
                            // 手动停止 / exitScript(): 正常流程, 不打印错误栈
                        } else {
                            Logx.i("脚本退出: " + err);
                        }
                    }
                } catch (Throwable t) {
                    if (!stopping) Logx.e("LuaHost 异常", t);
                } finally {
                    worker = null;
                    Logx.i("脚本已停止");
                    FloatService.setRunning(false);
                }
            }
        }, "lua-host");
        worker.start();
    }

    /** 一键重启: 停止当前脚本并重新加载(会重新读 sdcard 覆盖目录) */
    public static synchronized void restart(final Context ctx, final String luaName) {
        if (worker == null) {
            start(ctx, luaName);
            return;
        }
        stop();
        new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(300); } catch (Exception e) { }
                start(ctx, luaName);
            }
        }, "lua-restart").start();
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
