package com.persiancity.game.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;

/**
 * نقشه کوچک شهر — گوشه بالای صفحه
 * نشانگرها: خانه، رستوران، سینما، بیمارستان، ایستگاه قطار، قطار متحرک، هلیکوپتر
 */
public class MiniMap {
    private Bitmap mapBitmap = null;
    private final Paint paint = new Paint();
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final ArrayList<float[]> markers = new ArrayList<>();
    private final ArrayList<Integer> markerColors = new ArrayList<>();
    private final ArrayList<Integer> markerTypes = new ArrayList<>();   // ۰=نقطه ۱=قطار (متحرک)

    public float mapX, mapY, mapW, mapH;

    public MiniMap(World world) {
        buildFromWorld(world);
    }

    public void buildFromWorld(World world) {
        try {
            int[][] tileType = world.tileType;
            int w = G.MAP_W, h = G.MAP_H;
            Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(bm);

            int[] colors = new int[]{
                0xFFB0A48C,   // ۰ ساختمان
                0xFF6B7280,   // ۱ جاده
                0xFF7CC24E,   // ۲ چمن
                0xFF5DAE45,   // ۳ پارک
                0xFF64B5F6,   // ۴ آب
                0xFFC9CFD6,   // ۵ پیاده‌رو
                0xFFE6C99A,   // ۶ مسیر پارک
                0xFFC8A66B,   // ۷ جاده خاکی روستا
                0xFF51585F    // ۸ باند فرودگاه
            };

            Paint tp = new Paint();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int t = tileType[y][x];
                    if (t < 0 || t >= colors.length) t = World.T_GRASS;   // محافظ ضد کرش
                    tp.setColor(colors[t]);
                    cv.drawPoint(x, y, tp);
                }
            }
            mapBitmap = bm;
        } catch (Throwable t) {
            mapBitmap = null;   // نقشه کوچک حیاتی نیست
        }
    }

    /**
     * افزودن نشانگر ثابت (مختصات دنیا)
     * kind: ۰ = نقطه ثابت، ۱ = متحرک (پره‌دار)
     */
    public void addMarker(float wx, float wy, int color, int kind) {
        markers.add(new float[]{wx, wy});
        markerColors.add(color);
        markerTypes.add(kind);
    }

    public void clearMarkers() {
        markers.clear();
        markerColors.clear();
        markerTypes.clear();
    }

    // نشانگرهای پویا (هر فریم تنظیم می‌شوند: قطار، ماشین‌های بازیکن)
    private final ArrayList<float[]> dynMarkers = new ArrayList<>();
    private final ArrayList<Integer> dynColors = new ArrayList<>();

    public void clearDynamic() {
        dynMarkers.clear();
        dynColors.clear();
    }

    public void addDynamic(float wx, float wy, int color) {
        dynMarkers.add(new float[]{wx, wy});
        dynColors.add(color);
    }

    /**
     * رسم در گوشه صفحه — برمی‌گرداند ارتفاع واقعی
     */
    public float draw(Canvas c, float left, float top, float width, float px, float py) {
        mapX = left;
        mapY = top;
        mapW = width;
        float height = width * G.WORLD_H / G.WORLD_W;
        mapH = height;

        borderPaint.setColor(0xF0FFFFFF);
        c.drawRoundRect(left - 5f, top - 5f, left + width + 5f, top + height + 5f, 10f, 10f, borderPaint);

        if (mapBitmap != null) {
            paint.setFilterBitmap(true);
            paint.setAlpha(235);
            c.drawBitmap(mapBitmap, null, new android.graphics.RectF(left, top, left + width, top + height), paint);
            paint.setAlpha(255);
        } else {
            paint.setColor(0xFF7CC24E);
            c.drawRect(left, top, left + width, top + height, paint);
        }

        // نشانگرهای ثابت
        for (int i = 0; i < markers.size(); i++) {
            float[] m = markers.get(i);
            float mx = left + m[0] / G.WORLD_W * width;
            float my = top + m[1] / G.WORLD_H * height;
            int color = markerColors.get(i);
            int kind = markerTypes.get(i);
            if (kind == 1) {
                dotPaint.setColor(color);
                c.drawCircle(mx, my, 5f, dotPaint);
                dotPaint.setColor(0xFFFFFFFF);
                c.drawCircle(mx, my, 2f, dotPaint);
            } else {
                dotPaint.setColor(0xFFFFFFFF);
                c.drawCircle(mx, my, 6f, dotPaint);
                dotPaint.setColor(color);
                c.drawCircle(mx, my, 4.5f, dotPaint);
            }
        }

        // نشانگرهای پویا (قطار متحرک، ماشین‌های کاربر)
        for (int i = 0; i < dynMarkers.size(); i++) {
            float[] m = dynMarkers.get(i);
            float mx = left + m[0] / G.WORLD_W * width;
            float my = top + m[1] / G.WORLD_H * height;
            mx = G.clamp(mx, left + 3f, left + width - 3f);
            my = G.clamp(my, top + 3f, top + height - 3f);
            dotPaint.setColor(0xFFFFFFFF);
            c.drawCircle(mx, my, 6.5f, dotPaint);
            dotPaint.setColor(dynColors.get(i));
            c.drawCircle(mx, my, 4.5f, dotPaint);
            dotPaint.setColor(0xFF212121);
            c.drawCircle(mx, my, 1.8f, dotPaint);
        }

        // بازیکن: نقطه سفید درشت
        float px2 = left + px / G.WORLD_W * width;
        float py2 = top + py / G.WORLD_H * height;
        px2 = G.clamp(px2, left + 4f, left + width - 4f);
        py2 = G.clamp(py2, top + 4f, top + height - 4f);
        dotPaint.setColor(0xFF212121);
        c.drawCircle(px2, py2, 7f, dotPaint);
        dotPaint.setColor(0xFFFFEB3B);
        c.drawCircle(px2, py2, 5f, dotPaint);
        return height;
    }
}
