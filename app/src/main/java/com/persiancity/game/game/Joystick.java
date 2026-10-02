package com.persiancity.game.game;

/**
 * اهرم کنترل مجازی (سمت چپ صفحه)
 */
public class Joystick {
    public float baseX, baseY;      // مرکز اهرم
    public float knobX, knobY;      // محل دست
    public boolean active = false;
    private float radius = 110f;

    public float getDx() {
        if (!active) return 0f;
        float d = knobX - baseX;
        return G.clamp(d / radius, -1f, 1f);
    }

    public float getDy() {
        if (!active) return 0f;
        float d = knobY - baseY;
        return G.clamp(d / radius, -1f, 1f);
    }

    public void start(float x, float y) {
        active = true;
        baseX = x;
        baseY = y;
        knobX = x;
        knobY = y;
    }

    public void move(float x, float y) {
        if (!active) return;
        float dx = x - baseX, dy = y - baseY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > radius) {
            dx = dx / len * radius;
            dy = dy / len * radius;
        }
        knobX = baseX + dx;
        knobY = baseY + dy;
    }

    public void release() {
        active = false;
        knobX = baseX;
        knobY = baseY;
    }

    public void reset() {
        active = false;
        knobX = baseX;
        knobY = baseY;
    }

    public float getRadius() {
        return radius;
    }

    public boolean isMoved() {
        return active && (Math.abs(getDx()) > 0.15f || Math.abs(getDy()) > 0.15f);
    }
}
