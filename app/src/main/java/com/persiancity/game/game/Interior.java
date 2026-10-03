package com.persiancity.game.game;

import java.util.ArrayList;

/**
 * محیط داخلی مغازه‌ها: رستوران، کافه، سوپرمارکت، خانه، سینما (با صندلی و پرده!)
 * و باغ‌وحش (با حیوان‌ها!)
 */
public class Interior {
    public final Building building;
    public final float roomW, roomH;        // اندازه اتاق (پیکسل)
    public final String floorType;          // restaurant / market / zoo / cinema / home / ...
    public float doorX;                     // مرکز در خروج

    // مبلمان: مستطیل‌های (x, y, w, h)
    public final ArrayList<float[]> furniture = new ArrayList<>();
    public final ArrayList<int[]> furnitureStyle = new ArrayList<>(); // [رنگ، نوع]
    public final ArrayList<float[]> seatSpots = new ArrayList<>();    // صندلی‌های سینما

    // حیوانات باغ‌وحش
    public final ArrayList<Integer> animalTypes = new ArrayList<>();
    public final ArrayList<float[]> animalPos = new ArrayList<>();

    public static final int F_TABLE = 0;
    public static final int F_COUNTER = 1;
    public static final int F_SHELF = 2;
    public static final int F_PLANT = 3;
    public static final int F_BED = 4;
    public static final int F_TV = 5;
    public static final int F_RUG = 6;
    public static final int F_FENCE = 8;
    public static final int F_SCREEN = 9;    // پرده سینما
    public static final int F_BOOKCASE = 10;
    public static final int F_DESK = 11;

    public Interior(Building b, int tilesW, int tilesH, String floorType) {
        this.building = b;
        this.roomW = tilesW * G.TILE;
        this.roomH = tilesH * G.TILE;
        this.floorType = floorType;
        this.doorX = roomW / 2f;
    }

    public void addFurniture(float x, float y, float w, float h, int color, int styleType) {
        furniture.add(new float[]{x, y, w, h});
        furnitureStyle.add(new int[]{color, styleType});
    }

    /**
     * برخورد با مبلمان
     */
    public boolean blocked(float x, float y, float r) {
        for (int i = 0; i < furniture.size(); i++) {
            int[] st = furnitureStyle.get(i);
            if (st[1] == F_RUG) continue;   // فرش مانع نیست
            float[] f = furniture.get(i);
            if (x + r > f[0] && x - r < f[0] + f[2] && y + r > f[1] - 6f && y - r < f[1] + f[3]) {
                return true;
            }
        }
        return false;
    }

    /**
     * ساخت محیط داخلی مناسب هر ساختمان
     */
    public static Interior createFor(Building b) {
        if (b == null) return null;
        switch (b.type) {
            case Building.RESTAURANT: return buildRestaurant(b);
            case Building.CAFE:       return buildCafe(b);
            case Building.BAKERY:     return buildBakery(b);
            case Building.MARKET:     return buildMarket(b);
            case Building.HOME:       return buildHome(b);
            case Building.CINEMA:     return buildCinema(b);
            case Building.ZOO:        return buildZoo(b);
            case Building.LIBRARY:    return buildLibrary(b);
            case Building.SCHOOL:     return buildSchool(b);
            case Building.BANK:       return buildBank(b);
            case Building.HOSPITAL:   return buildHospital(b);
            case Building.TOYSTORE:   return buildToystore(b);
            case Building.CLOTHES:    return buildClothes(b);
            case Building.AIRPORT:    return buildAirport(b);
            default: return null;
        }
    }

    private static Interior buildRestaurant(Building b) {
        Interior in = new Interior(b, 16, 10, "restaurant");
        in.addFurniture(60, 60, 260, 56, 0xFF8D6E63, F_COUNTER);           // کانتر آشپزخانه
        in.addFurniture(70, 130, 150, 40, 0xFFFFB74D, F_TABLE);            // میزها
        in.addFurniture(380, 130, 150, 40, 0xFFFFB74D, F_TABLE);
        in.addFurniture(700, 130, 150, 40, 0xFFFFB74D, F_TABLE);
        in.addFurniture(70, 300, 150, 40, 0xFFFFB74D, F_TABLE);
        in.addFurniture(380, 300, 150, 40, 0xFFFFB74D, F_TABLE);
        in.addFurniture(700, 300, 150, 40, 0xFFFFB74D, F_TABLE);
        in.addFurniture(480, 40, 90, 90, 0xFF66BB6A, F_PLANT);
        return in;
    }

    private static Interior buildCafe(Building b) {
        Interior in = new Interior(b, 12, 9, "cafe");
        in.addFurniture(50, 50, 220, 50, 0xFF795548, F_COUNTER);
        in.addFurniture(90, 180, 120, 36, 0xFFFFCC80, F_TABLE);
        in.addFurniture(420, 180, 120, 36, 0xFFFFCC80, F_TABLE);
        in.addFurniture(90, 330, 120, 36, 0xFFFFCC80, F_TABLE);
        in.addFurniture(420, 330, 120, 36, 0xFFFFCC80, F_TABLE);
        in.addFurniture(520, 40, 80, 80, 0xFF66BB6A, F_PLANT);
        return in;
    }

    private static Interior buildBakery(Building b) {
        Interior in = new Interior(b, 12, 8, "bakery");
        in.addFurniture(50, 50, 260, 50, 0xFF8D6E63, F_COUNTER);   // پیشخوان نان
        in.addFurniture(60, 200, 200, 46, 0xFF5D4037, F_SHELF);    // قفسه نان
        in.addFurniture(360, 200, 200, 46, 0xFF5D4037, F_SHELF);
        in.addFurniture(120, 330, 140, 40, 0xFFFFCC80, F_TABLE);
        in.addFurniture(430, 330, 140, 40, 0xFFFFCC80, F_TABLE);
        return in;
    }

    private static Interior buildMarket(Building b) {
        Interior in = new Interior(b, 16, 10, "market");
        in.addFurniture(40, 60, 200, 46, 0xFF455A64, F_SHELF);
        in.addFurniture(300, 60, 200, 46, 0xFF455A64, F_SHELF);
        in.addFurniture(560, 60, 200, 46, 0xFF455A64, F_SHELF);
        in.addFurniture(40, 200, 200, 46, 0xFF455A64, F_SHELF);
        in.addFurniture(300, 200, 200, 46, 0xFF455A64, F_SHELF);
        in.addFurniture(560, 200, 200, 46, 0xFF455A64, F_SHELF);
        in.addFurniture(640, 330, 200, 60, 0xFFFF9800, F_COUNTER);   // صندوق
        return in;
    }

    private static Interior buildHome(Building b) {
        Interior in = new Interior(b, 12, 9, "home");
        in.addFurniture(60, 60, 150, 80, 0xFFE91E63, F_BED);
        in.addFurniture(380, 50, 160, 46, 0xFF37474F, F_TV);
        in.addFurniture(200, 260, 180, 90, 0xFFFF7043, F_RUG);
        in.addFurniture(480, 240, 110, 50, 0xFF8D6E63, F_TABLE);
        in.addFurniture(620, 60, 70, 70, 0xFF66BB6A, F_PLANT);
        return in;
    }

    private static Interior buildCinema(Building b) {
        Interior in = new Interior(b, 17, 11, "cinema");
        // پرده بزرگ سینما (بالا)
        in.addFurniture(3.2f * G.TILE, 0.5f * G.TILE, 10.5f * G.TILE, 2.4f * G.TILE, 0xFF6A1B9A, F_SCREEN);
        // جلوه‌های تاج پرده
        in.addFurniture(3.4f * G.TILE, 0.45f * G.TILE, 10.2f * G.TILE, 0.35f * G.TILE, 0xFF6A1B9A, 6);
        // صندلی‌ها: ۳ ردیف × ۶
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 6; col++) {
                float sx = (3.6f + col * 1.75f) * G.TILE;
                float sy = (4.4f + row * 1.5f) * G.TILE;
                in.seatSpots.add(new float[]{sx, sy});
            }
        }
        in.addFurniture(0.4f * G.TILE, 0.5f * G.TILE, 100f, 60f, 0xFF8D6E63, F_COUNTER);   // پاپ‌کورن
        return in;
    }

    private static Interior buildZoo(Building b) {
        Interior in = new Interior(b, 26, 13, "zoo");
        // ✅ ۱۰ قلمرو نرده‌ای با ۱۰ حیوان (قبلاً ۶ تا بود)
        int[] types = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        // شیر، فیل، میمون، گورخر، پنگوئن، زرافه، خرس، ببر، پاندا، شتر
        int pen = 0;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 5; col++) {
                float px = (0.5f + col * 5.0f) * G.TILE;
                float py = (0.7f + row * 5.9f) * G.TILE;
                in.addFurniture(px, py, 4.5f * G.TILE, 5.1f * G.TILE, 0xFF8D6E63, F_FENCE);
                in.animalTypes.add(types[pen]);
                in.animalPos.add(new float[]{px + 2.25f * G.TILE, py + 2.8f * G.TILE});
                pen++;
            }
        }
        return in;
    }

    private static Interior buildClothes(Building b) {
        Interior in = new Interior(b, 13, 9, "clothes");
        in.addFurniture(40, 60, 220, 46, 0xFFF06292, F_SHELF);
        in.addFurniture(320, 60, 220, 46, 0xFF42A5F5, F_SHELF);
        in.addFurniture(600, 60, 220, 46, 0xFFFFB74D, F_SHELF);
        in.addFurniture(60, 240, 90, 150, 0xFFB0BEC5, F_COUNTER);   // مانتین
        in.addFurniture(500, 240, 160, 44, 0xFF8D6E63, F_TABLE);
        in.addFurniture(700, 240, 80, 80, 0xFF66BB6A, F_PLANT);
        return in;
    }

    private static Interior buildAirport(Building b) {
        Interior in = new Interior(b, 15, 8, "airport");
        in.addFurniture(60, 60, 300, 56, 0xFF455A64, F_COUNTER);   // پیشخوان پرواز
        for (int i = 0; i < 4; i++) {
            in.addFurniture(80 + i * 170f, 260, 120, 44, 0xFF90A4AE, F_TABLE);
        }
        in.addFurniture(620, 50, 130, 84, 0xFF37474F, F_TV);       // تخته پرواز
        return in;
    }

    private static Interior buildLibrary(Building b) {
        Interior in = new Interior(b, 14, 9, "library");
        in.addFurniture(40, 60, 220, 50, 0xFF5D4037, F_BOOKCASE);
        in.addFurniture(320, 60, 220, 50, 0xFF5D4037, F_BOOKCASE);
        in.addFurniture(600, 60, 220, 50, 0xFF5D4037, F_BOOKCASE);
        in.addFurniture(120, 240, 160, 44, 0xFF8D6E63, F_TABLE);
        in.addFurniture(480, 240, 160, 44, 0xFF8D6E63, F_TABLE);
        return in;
    }

    private static Interior buildSchool(Building b) {
        Interior in = new Interior(b, 14, 9, "school");
        in.addFurniture(300, 50, 240, 46, 0xFF455A64, F_DESK);   // میز معلم
        for (int i = 0; i < 4; i++) {
            in.addFurniture(80 + i * 160f, 220, 110, 44, 0xFFFFB74D, F_DESK);
        }
        in.addFurniture(620, 60, 70, 70, 0xFF66BB6A, F_PLANT);
        return in;
    }

    private static Interior buildBank(Building b) {
        Interior in = new Interior(b, 14, 9, "bank");
        in.addFurniture(60, 60, 480, 60, 0xFF455A64, F_COUNTER);
        in.addFurniture(140, 240, 120, 44, 0xFF8D6E63, F_TABLE);
        in.addFurniture(460, 240, 120, 44, 0xFF8D6E63, F_TABLE);
        return in;
    }

    private static Interior buildHospital(Building b) {
        Interior in = new Interior(b, 15, 9, "hospital");
        in.addFurniture(60, 50, 200, 46, 0xFFE0E0E0, F_COUNTER);
        in.addFurniture(80, 220, 140, 70, 0xFFF5F5F5, F_BED);
        in.addFurniture(360, 220, 140, 70, 0xFFF5F5F5, F_BED);
        in.addFurniture(640, 220, 140, 70, 0xFFF5F5F5, F_BED);
        return in;
    }

    private static Interior buildToystore(Building b) {
        Interior in = new Interior(b, 13, 9, "toystore");
        in.addFurniture(40, 60, 200, 46, 0xFFF06292, F_SHELF);
        in.addFurniture(300, 60, 200, 46, 0xFF42A5F5, F_SHELF);
        in.addFurniture(560, 60, 200, 46, 0xFFFFB74D, F_SHELF);
        in.addFurniture(200, 240, 160, 44, 0xFF66BB6A, F_TABLE);
        return in;
    }
}
