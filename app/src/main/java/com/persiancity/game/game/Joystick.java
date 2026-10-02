package com.persiancity.game.game;

/**
 * جوی‌استیک مجازی سمت چپ صفحه
 */
public class Joystick {

    public float baseX, baseY;   // مرکز جوی‌استیک (مختصات صفحه)
    public float knobX, knobY;   // محل دسته
    public boolean active = false;

    public float outX = 0f, outY = 0f;  // خروجی -۱ تا ۱

    private final float radius;

    public Joystick(float radius) {
        this.radius = radius;
    }

    /**
     * شروع لمس — اگر داخل ناحیه چپ باشد فعال می‌شود
     */
    public boolean onTouchDown(float x, float y, float viewW, float viewH) {
        if (x < viewW * 0.45f) {
            active = true;
            baseX = x;
            baseY = y;
            knobX = x;
            knobY = y;
            return true;
        }
        return false;
    }

    public boolean onTouchMove(float x, float y) {
        if (!active) return false;
        float dx = x - baseX;
        float dy = y - baseY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > radius) {
            dx = dx / len * radius;
            dy = dy / len * radius;
        }
        knobX = baseX + dx;
        knobY = baseY + dy;
        outX = dx / radius;
        outY = dy / radius;
        return true;
    }

    public boolean onTouchUp() {
        if (!active) return false;
        active = false;
        knobX = baseX;
        knobY = baseY;
        outX = 0f;
        outY = 0f;
        return true;
    }

    public float getRadius() {
        return radius;
    }

    /**
     * ریست کامل جوی‌استیک (موقع باز شدن منوها)
     */
    public void reset() {
        active = false;
        outX = 0f;
        outY = 0f;
        knobX = baseX;
        knobY = baseY;
    }
}
