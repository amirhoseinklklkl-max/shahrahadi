package com.persiancity.game.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

/**
 * مینی‌مپ — نقشه کوچک شهر گوشه صفحه
 */
public class MiniMap {

    private Bitmap mapBitmap;
    private final Paint paint = new Paint();
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    public float w, h;

    // نقاط نشانگر (مختصات دنیا)
    private final List<float[]> markers = new ArrayList<>();
    private final List<Integer> markerColors = new ArrayList<>();

    public MiniMap() {
        paint.setAntiAlias(true);
    }

    public void buildFromWorld(int[][] tileType, float padding) {
        // tileType: 0=ساختمان 1=جاده 2=چمن 3=پارک 4=آب 5=پیاده‌رو
        int pw = G.MAP_W;
        int ph = G.MAP_H;
        int[] colors = {
                0xFFEF9A9A, // ساختمان
                0xFF757575, // جاده
                0xFFA5D6A7, // چمن
                0xFF66BB6A, // پارک
                0xFF64B5F6, // آب
                0xFFBDBDBD  // پیاده‌رو
        };
        Bitmap bmp = Bitmap.createBitmap(pw, ph, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        for (int y = 0; y < ph; y++) {
            for (int x = 0; x < pw; x++) {
                int t = tileType[y][x];
                paint.setColor(colors[t]);
                c.drawPoint(x, y, paint);
            }
        }
        mapBitmap = bmp;
    }

    public void clearMarkers() {
        markers.clear();
        markerColors.clear();
    }

    public void addMarker(float wx, float wy, int color) {
        markers.add(new float[]{wx, wy});
        markerColors.add(color);
    }

    public void draw(Canvas c, float px, float py, float screenX, float screenY, float width) {
        if (mapBitmap == null) return;
        float scale = width / G.WORLD_W;
        w = G.WORLD_W * scale;
        h = G.WORLD_H * scale;

        // پس‌زمینه
        dotPaint.setColor(0xCC37474F);
        c.drawRoundRect(screenX - 6, screenY - 6, screenX + w + 6, screenY + h + 6, 10, 10, dotPaint);

        dotPaint.setAlpha(230);
        c.drawBitmap(mapBitmap, null, new android.graphics.RectF(screenX, screenY, screenX + w, screenY + h), dotPaint);
        dotPaint.setAlpha(255);

        // نشانگرها
        for (int i = 0; i < markers.size(); i++) {
            float[] m = markers.get(i);
            float mx = screenX + m[0] * scale;
            float my = screenY + m[1] * scale;
            dotPaint.setColor(markerColors.get(i));
            c.drawCircle(mx, my, 4f, dotPaint);
            dotPaint.setColor(0xFFFFFFFF);
            dotPaint.setStyle(Paint.Style.STROKE);
            dotPaint.setStrokeWidth(1.5f);
            c.drawCircle(mx, my, 4f, dotPaint);
            dotPaint.setStyle(Paint.Style.FILL);
        }

        // بازیکن
        float pxs = screenX + px * scale;
        float pys = screenY + py * scale;
        dotPaint.setColor(0xFFFFEB3B);
        c.drawCircle(pxs, pys, 5f, dotPaint);
        dotPaint.setColor(0xFF000000);
        dotPaint.setStyle(Paint.Style.STROKE);
        dotPaint.setStrokeWidth(1.5f);
        c.drawCircle(pxs, pys, 5f, dotPaint);
        dotPaint.setStyle(Paint.Style.FILL);
    }
}
