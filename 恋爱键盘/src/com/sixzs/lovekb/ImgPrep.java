package com.sixzs.lovekb;

import android.graphics.Bitmap;
import android.graphics.Rect;

import java.io.ByteArrayOutputStream;

/**
 * 图像预处理: 给视觉模型前先把图「瘦身」
 *
 * 为什么要做这一步(实测数据):
 *   真机 1080x2400 原图 JPEG q70   → 120KB, base64 161KB
 *   长边压到 1280 (576x1280) q70   →  59KB, base64  79KB   (-51%)
 *   再裁掉上下无关区域            →  38KB, base64  51KB   (-68%)
 *   base64 体积直接决定请求体大小和上传耗时, 而聊天区域文字在
 *   1280 长边下依然清晰可读(模型 OCR 足够)。
 *
 * 三步:
 *   ① 裁剪: 只留聊天区域(去掉状态栏/导航栏/输入框), 减少无关像素干扰
 *   ② 缩放: 长边压到 maxEdge(默认 1280)
 *   ③ 压缩: JPEG 质量可调(默认 70), 并记录压缩前后体积
 */
public class ImgPrep {

    /** 默认长边上限 */
    public static final int DEFAULT_MAX_EDGE = 1280;
    /** 默认 JPEG 质量 */
    public static final int DEFAULT_QUALITY = 70;

    /** 预处理结果 */
    public static class Out {
        public byte[] jpeg;
        public int srcW, srcH;      // 原图尺寸
        public int outW, outH;      // 处理后尺寸
        public int srcBytes;        // 原图 PNG/JPEG 体积(估)
        public int outBytes;        // 处理后体积
        public boolean cropped;     // 是否裁剪过
        public String ratio = "";   // 体积比描述

        public String sizeText() {
            return outW + "x" + outH + " / " + (outBytes / 1024) + "KB";
        }

        public String detail() {
            StringBuilder sb = new StringBuilder();
            sb.append(srcW).append("x").append(srcH).append(" → ").append(outW)
                    .append("x").append(outH).append(", ").append(outBytes / 1024).append("KB");
            if (!ratio.isEmpty()) sb.append(" (").append(ratio).append(")");
            return sb.toString();
        }
    }

    /**
     * 预处理截图
     * @param bmp      原始截图
     * @param focus    聊天区域(屏幕坐标); 为 null 则不裁剪
     * @param maxEdge  长边上限, <=0 表示不缩放
     * @param quality  JPEG 质量 1-100
     */
    public static Out prepare(Bitmap bmp, Rect focus, int maxEdge, int quality) {
        Out o = new Out();
        if (bmp == null) return o;
        o.srcW = bmp.getWidth();
        o.srcH = bmp.getHeight();

        Bitmap work = bmp;
        try {
            // ① 裁剪到聊天区域
            if (focus != null && focus.width() > 40 && focus.height() > 40) {
                int l = Math.max(0, focus.left);
                int t = Math.max(0, focus.top);
                int r = Math.min(o.srcW, focus.right);
                int bo = Math.min(o.srcH, focus.bottom);
                if (r - l > 40 && bo - t > 40) {
                    Bitmap c = Bitmap.createBitmap(work, l, t, r - l, bo - t);
                    if (c != null) {
                        work = c;
                        o.cropped = true;
                    }
                }
            }

            // ② 缩放: 长边压到 maxEdge
            int w = work.getWidth(), h = work.getHeight();
            if (maxEdge > 0 && Math.max(w, h) > maxEdge) {
                float s = maxEdge / (float) Math.max(w, h);
                int nw = Math.max(1, Math.round(w * s));
                int nh = Math.max(1, Math.round(h * s));
                Bitmap sc = Bitmap.createScaledBitmap(work, nw, nh, true);
                if (sc != null) {
                    if (work != bmp) work.recycle();
                    work = sc;
                }
            }

            // ③ JPEG 压缩
            int q = quality <= 0 ? DEFAULT_QUALITY : Math.min(95, Math.max(30, quality));
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            work.compress(Bitmap.CompressFormat.JPEG, q, bo);
            o.jpeg = bo.toByteArray();
            o.outW = work.getWidth();
            o.outH = work.getHeight();
            o.outBytes = o.jpeg.length;

            // 估算原始体积(PNG 级别): w*h*4 是 ARGB 的裸大小
            o.srcBytes = o.srcW * o.srcH * 4;
            if (o.srcBytes > 0) {
                int pct = o.outBytes * 100 / o.srcBytes;
                o.ratio = "体积 " + pct + "%";
            }
        } catch (Throwable t) {
            Logx.e("图像预处理失败", t);
        } finally {
            if (work != null && work != bmp) {
                try { work.recycle(); } catch (Throwable t) { }
            }
        }
        return o;
    }

    /** 便捷: 只缩放不裁剪 */
    public static Out prepare(Bitmap bmp) {
        return prepare(bmp, null, DEFAULT_MAX_EDGE, DEFAULT_QUALITY);
    }
}
