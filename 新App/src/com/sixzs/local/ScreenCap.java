package com.sixzs.local;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.nio.ByteBuffer;

public class ScreenCap {

    private static MediaProjection projection;
    private static ImageReader reader;
    private static VirtualDisplay display;
    private static int w, h, dpi;

    public static synchronized boolean init(Context ctx, int resultCode, Intent data) {
        if (reader != null) return true;
        try {
            MediaProjectionManager mpm = (MediaProjectionManager)
                    ctx.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            projection = mpm.getMediaProjection(resultCode, data);
            if (projection == null) return false;
            WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics dm = new DisplayMetrics();
            if (Build.VERSION.SDK_INT >= 17) {
                wm.getDefaultDisplay().getRealMetrics(dm);
            } else {
                wm.getDefaultDisplay().getMetrics(dm);
            }
            w = dm.widthPixels;
            h = dm.heightPixels;
            dpi = dm.densityDpi;
            reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2);
            display = projection.createVirtualDisplay("dmcap", w, h, dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    reader.getSurface(), null, null);
            Logx.i("截图(MediaProjection)就绪 " + w + "x" + h);
            return true;
        } catch (Throwable t) {
            Logx.e("MediaProjection 初始化失败", t);
            return false;
        }
    }

    public static boolean ready() {
        return reader != null;
    }

    public static synchronized Bitmap capture() {
        ImageReader r = reader;
        if (r == null) return null;
        Image img = null;
        for (int i = 0; i < 3 && img == null; i++) {
            try {
                img = r.acquireLatestImage();
            } catch (Throwable t) {
            }
            if (img == null) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }
        }
        if (img == null) return null;
        try {
            Image.Plane[] planes = img.getPlanes();
            ByteBuffer buf = planes[0].getBuffer();
            int rowStride = planes[0].getRowStride();
            int pixelStride = planes[0].getPixelStride();
            int rowPadding = rowStride - pixelStride * w;
            Bitmap bmp = Bitmap.createBitmap(w + rowPadding / pixelStride, h, Bitmap.Config.ARGB_8888);
            buf.rewind();
            bmp.copyPixelsFromBuffer(buf);
            Bitmap cropped = Bitmap.createBitmap(bmp, 0, 0, w, h);
            bmp.recycle();
            return cropped;
        } catch (Throwable t) {
            return null;
        } finally {
            img.close();
        }
    }

    public static synchronized void stop() {
        try {
            if (display != null) display.release();
        } catch (Throwable t) {
        }
        try {
            if (reader != null) reader.close();
        } catch (Throwable t) {
        }
        try {
            if (projection != null) projection.stop();
        } catch (Throwable t) {
        }
        display = null;
        reader = null;
        projection = null;
    }
}
