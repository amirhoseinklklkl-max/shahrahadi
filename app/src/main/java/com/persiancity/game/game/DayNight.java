package com.persiancity.game.game;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * چرخه روز و شب — آسمان آرام تغییر رنگ می‌دهد و شب چراغ‌ها روشن می‌شود
 */
public class DayNight {
    public float minutes = 8f * 60f;   // شروع از صبح ۸

    private final Paint p = new Paint();

    public void update(float dt) {
        minutes += dt * G.GAME_MIN_PER_SEC;
        if (minutes >= G.DAY_MINUTES) minutes -= G.DAY_MINUTES;
    }

    public boolean isNight() {
        return minutes < 6f * 60f || minutes > 19f * 60f;
    }

    public boolean isEvening() {
        return (minutes > 17.5f * 60f && minutes <= 19f * 60f) || (minutes >= 5f * 60f && minutes < 6f * 60f);
    }

    public String clockText() {
        int h = (int) (minutes / 60f);
        int m = (int) (minutes % 60f);
        return G.fa(String.format(java.util.Locale.US, "%02d:%02d", h, m));
    }

    /**
     * پرده تیره روی صفحه هنگام شب
     */
    public void drawOverlay(Canvas c, float viewW, float viewH) {
        float dark = 0f;
        if (minutes >= 19f * 60f) {
            float t = (minutes - 19f * 60f) / 90f;
            dark = 0.55f * Math.min(t, 1f);
        } else if (minutes >= 18f * 60f) {
            float t = (minutes - 18f * 60f) / 60f;
            dark = 0.55f * t;
        } else if (minutes < 5.5f * 60f) {
            dark = 0.5f;
        } else if (minutes < 6.5f * 60f) {
            float t = 1f - (minutes - 5.5f * 60f) / 60f;
            dark = 0.5f * t;
        }
        if (dark > 0.01f) {
            p.setColor(((int) (dark * 255f) << 24) | 0x0A1030);
            c.drawRect(0, 0, viewW, viewH, p);
        }
    }
}
