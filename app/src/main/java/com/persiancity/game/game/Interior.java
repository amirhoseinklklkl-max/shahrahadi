package com.persiancity.game.game;

import java.util.ArrayList;

/**
 * محیط داخلی مغازه‌ها (رستوران و سوپرمارکت)
 * یک اتاق کوچک با در خروج در پایین
 */
public class Interior {

    public final Building from;             // ساختمان مبدأ
    public final float roomW, roomH;        // اندازه اتاق (پیکسل)
    public final String floorType;          // نوع کف

    // مبلمان: مستطیل‌های برخوردی (کابینت، قفسه، میز ...)
    public final ArrayList<float[]> furniture = new ArrayList<>();
    public final ArrayList<int[]> furnitureStyle = new ArrayList<>(); // رنگ + نوع

    // نقاط کلیدی
    public float doorX, doorY;              // در خروج
    public float counterX, counterY;        // پیشخوان/آشپزخانه
    public final ArrayList<float[]> tableSpots = new ArrayList<>();  // جای میزها
    public final ArrayList<float[]> standSpots = new ArrayList<>();  // جای ایستادن مشتری‌ها

    public Interior(Building from, int tilesW, int tilesH, String floorType) {
        this.from = from;
        this.roomW = tilesW * G.TILE;
        this.roomH = tilesH * G.TILE;
        this.floorType = floorType;
        this.doorX = roomW / 2f;
        this.doorY = roomH - G.TILE * 0.8f;
    }

    public void addFurniture(float tx, float ty, float tw, float th, int color, int style) {
        furniture.add(new float[]{tx * G.TILE, ty * G.TILE, tw * G.TILE, th * G.TILE});
        furnitureStyle.add(new int[]{color, style});
    }

    public boolean collides(float x, float y, float r) {
        if (x - r < G.TILE * 0.2f || x + r > roomW - G.TILE * 0.2f) return true;
        if (y - r < G.TILE * 0.2f || y + r > roomH - G.TILE * 0.2f) return true;
        for (float[] f : furniture) {
            if (x + r > f[0] && x - r < f[0] + f[2] && y + r > f[1] && y - r < f[1] + f[3]) {
                return true;
            }
        }
        return false;
    }

    /**
     * ساخت محیط رستوران «زنجبیل»
     */
    public static Interior makeRestaurant(Building b) {
        Interior in = new Interior(b, 13, 9, "restaurant");
        // آشپزخانه (چپ)
        in.addFurniture(0.8f, 1.2f, 3.2f, 1.4f, 0xFFB0BEC5, 0);
        in.counterX = 2.4f * G.TILE;
        in.counterY = 3.4f * G.TILE;
        // صندوق (راست)
        in.addFurniture(10.2f, 1.2f, 2f, 1.2f, 0xFF8D6E63, 1);
        // میزها
        in.addFurniture(5.2f, 1.6f, 1.6f, 1.6f, 0xFFFF8A65, 2);
        in.addFurniture(8.0f, 3.2f, 1.6f, 1.6f, 0xFFFF8A65, 2);
        in.addFurniture(4.6f, 5.0f, 1.6f, 1.6f, 0xFFFF8A65, 2);
        in.tableSpots.add(new float[]{7.2f * G.TILE, 2.4f * G.TILE});
        in.tableSpots.add(new float[]{10.0f * G.TILE, 4.0f * G.TILE});
        in.tableSpots.add(new float[]{6.6f * G.TILE, 5.8f * G.TILE});
        return in;
    }

    /**
     * ساخت محیط سوپرمارکت «فراوان»
     */
    public static Interior makeMarket(Building b) {
        Interior in = new Interior(b, 13, 9, "market");
        // قفسه‌ها
        in.addFurniture(1.0f, 1.2f, 4.5f, 1.0f, 0xFF90CAF9, 3);
        in.addFurniture(7.5f, 1.2f, 4.5f, 1.0f, 0xFF90CAF9, 3);
        in.addFurniture(1.0f, 3.4f, 4.5f, 1.0f, 0xFFA5D6A7, 3);
        in.addFurniture(7.5f, 3.4f, 4.5f, 1.0f, 0xFFA5D6A7, 3);
        // صندوق فروش (پشتش مدیر می‌ایستد)
        in.addFurniture(9.6f, 5.6f, 2.4f, 1.2f, 0xFF8D6E63, 1);
        in.counterX = 10.8f * G.TILE;
        in.counterY = 6.2f * G.TILE;
        in.standSpots.add(new float[]{10.8f * G.TILE, 4.7f * G.TILE});
        return in;
    }
}
