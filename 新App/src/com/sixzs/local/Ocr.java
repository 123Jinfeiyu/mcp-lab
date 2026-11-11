package com.sixzs.local;

import android.content.Context;
import android.graphics.Bitmap;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

public class Ocr {

    private static boolean tried = false;
    private static boolean ok = false;
    private static Method mDetect;

    public static synchronized boolean init(Context c) {
        if (tried) return ok;
        tried = true;
        try {
            File dir = new File(c.getFilesDir(), "ocr");
            if (!dir.exists()) dir.mkdirs();
            copy(c, "ocr/classes.dex", new File(dir, "plugin.dex"));
            String[] models = {"PaddLeOCR_Db.param", "PaddLeOCR_Db.bin",
                    "PaddLeOCR_Rec.param", "PaddLeOCR_Rec.bin", "LaBeL.txt"};
            for (String m : models) copy(c, "ocr/" + m, new File(dir, m));

            dalvik.system.DexClassLoader cl = new dalvik.system.DexClassLoader(
                    new File(dir, "plugin.dex").getAbsolutePath(),
                    dir.getAbsolutePath(), null, Ocr.class.getClassLoader());
            Class<?> main = cl.loadClass("com.LR.JZ.PaddLeOCR.Main");

            Method mInit = main.getMethod("IniTiaLModeL", Context.class);
            Object r = mInit.invoke(null, c);
            if (!(r instanceof Boolean) || !((Boolean) r).booleanValue()) {
                Logx.i("OCR 插件初始化返回失败");
                return false;
            }
            Method mLoad = main.getMethod("LoadModeL", String.class, String.class, String.class,
                    String.class, String.class, int.class, int.class, boolean.class,
                    String.class, String.class);
            Object lr = mLoad.invoke(null, dir + "/LaBeL.txt",
                    dir + "/PaddLeOCR_Db.param", dir + "/PaddLeOCR_Db.bin",
                    dir + "/PaddLeOCR_Rec.param", dir + "/PaddLeOCR_Rec.bin",
                    640, 2, false, "", "");
            if (lr instanceof String && ((String) lr).length() > 0) {
                Logx.i("OCR 模型加载返回: " + lr);
            }
            mDetect = main.getMethod("Detect", Bitmap.class, float.class, float.class);
            ok = true;
            Logx.i("OCR 插件加载成功");
        } catch (Throwable t) {
            Logx.e("OCR 初始化异常", t);
        }
        return ok;
    }

    public static JSONArray detect(Bitmap b, float conf, float prob) {
        if (b == null) return null;
        if (Host.appContext == null) return null;
        if (!init(Host.appContext)) return null;
        try {
            String s = (String) mDetect.invoke(null, b, conf, prob);
            if (s == null || s.length() == 0) return new JSONArray();
            return new JSONArray(s);
        } catch (Throwable t) {
            Logx.e("OCR 识别异常", t);
            return null;
        }
    }

    private static void copy(Context c, String asset, File dst) throws IOException {
        if (dst.exists() && dst.length() > 0) return;
        InputStream in = c.getAssets().open(asset);
        FileOutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        in.close();
        out.close();
    }
}
