package com.persiancity.game.game;

/**
 * ثابت‌های سراسری بازی «شهر شادی»
 */
public final class G {

    private G() {}

    // اندازه زمین بازی (شهر خیلی بزرگ + روستا + دریاچه)
    public static final float TILE = 64f;
    public static final int MAP_W = 168;   // تعداد تایل افقی
    public static final int MAP_H = 128;   // تعداد تایل عمودی
    public static final float WORLD_W = MAP_W * TILE;
    public static final float WORLD_H = MAP_H * TILE;

    // سرعت‌ها (پیکسل بر ثانیه)
    public static final float WALK_SPEED = 215f;
    public static final float WALK_SPEED_TIRED = 110f;
    public static final float NPC_SPEED = 90f;

    // چرخه روز و شب: هر ثانیه واقعی = چند دقیقه بازی
    public static final float GAME_MIN_PER_SEC = 2.2f;
    public static final float DAY_MINUTES = 24f * 60f;

    // پول شروع
    public static final int START_MONEY = 5000;

    // رنگ‌های کارتونی
    public static final int COL_GRASS = 0xFF7CC24E;
    public static final int COL_GRASS2 = 0xFF6DB644;
    public static final int COL_ROAD = 0xFF5A616B;
    public static final int COL_ROAD_LINE = 0xFFF5F5F5;
    public static final int COL_SIDEWALK = 0xFFC9CFD6;
    public static final int COL_HEDGE = 0xFF3E8E41;
    public static final int COL_TREE = 0xFF388E3C;
    public static final int COL_TRUNK = 0xFF795548;
    public static final int COL_WATER = 0xFF64B5F6;
    public static final int COL_PATH = 0xFFE6C99A;

    // پالت پوست
    public static final int COL_SKIN = 0xFFF7C99B;
    public static final int COL_SKIN2 = 0xFFE0AC7E;

    /**
     * تبدیل ارقام انگلیسی به فارسی
     */
    public static String fa(int n) {
        return fa(String.valueOf(n));
    }

    public static String fa(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append((char) ('۰' + (c - '0')));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * فاصله بین دو نقطه
     */
    public static float dist(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * محدود کردن مقدار بین حداقل و حداکثر
     */
    public static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
