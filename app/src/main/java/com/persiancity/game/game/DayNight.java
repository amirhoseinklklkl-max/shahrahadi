package com.persiancity.game.game;

/**
 * چرخه روز و شب — ساعت درون‌بازی
 */
public class DayNight {

    public float minutes;   // دقیقه از نیمه‌شب
    public int dayCount = 1;

    public DayNight() {
        minutes = 8f * 60f; // شروع: ۸ صبح
    }

    public void update(float dt) {
        minutes += dt * G.GAME_MIN_PER_SEC;
        if (minutes >= G.DAY_MINUTES) {
            minutes -= G.DAY_MINUTES;
            dayCount++;
        }
    }

    public void setTime(float m) {
        minutes = G.clamp(m, 0, G.DAY_MINUTES - 1);
    }

    public int getHour() {
        return (int) (minutes / 60f) % 24;
    }

    public int getMinute() {
        return (int) minutes % 60;
    }

    /**
     * متن ساعت به فارسی مثل «۸:۳۰ صبح»
     */
    public String timeText() {
        int h = getHour();
        int m = getMinute();
        String part;
        if (h >= 5 && h < 12) part = "صبح";
        else if (h >= 12 && h < 17) part = "بعدازظهر";
        else if (h >= 17 && h < 20) part = "عصر";
        else part = "شب";

        int h12 = h % 12;
        if (h12 == 0) h12 = 12;
        return G.fa(h12 + ":" + String.format("%02d", m) + " " + part);
    }

    /**
     * سطح تاریکی محیط ۰ تا ۱
     */
    public float darkness() {
        float h = minutes / 60f;
        if (h >= 7 && h < 18) return 0f;             // روز
        if (h >= 18 && h < 20.5f) return (h - 18f) / 2.5f * 0.62f; // غروب
        if (h >= 20.5f || h < 5f) return 0.62f;      // شب
        return (1f - (h - 5f) / 2f) * 0.62f;         // سپیده‌دم
    }

    public boolean isNight() {
        return darkness() > 0.35f;
    }

    /**
     * رنگ آسمان برای پس‌زمینه
     */
    public int skyColor() {
        float h = minutes / 60f;
        if (h >= 6 && h < 17) return 0xFF87CEEB;                 // آبی روز
        if (h >= 17 && h < 19.5f) return lerpColor(0xFF87CEEB, 0xFFB57EDC, (h - 17f) / 2.5f); // غروب
        if (h >= 19.5f || h < 4.5f) return 0xFF1A2340;           // شب
        return lerpColor(0xFF1A2340, 0xFF87CEEB, (h - 4.5f) / 1.5f); // سپیده
    }

    /**
     * نام بخش روز
     */
    public String phaseName() {
        int h = getHour();
        if (h >= 5 && h < 12) return "صبح";
        if (h >= 12 && h < 17) return "بعدازظهر";
        if (h >= 17 && h < 20) return "عصر";
        return "شب";
    }

    private static int lerpColor(int c1, int c2, float t) {
        t = G.clamp(t, 0f, 1f);
        int r = (int) (((c1 >> 16) & 0xFF) * (1 - t) + ((c2 >> 16) & 0xFF) * t);
        int g = (int) (((c1 >> 8) & 0xFF) * (1 - t) + ((c2 >> 8) & 0xFF) * t);
        int b = (int) ((c1 & 0xFF) * (1 - t) + (c2 & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
