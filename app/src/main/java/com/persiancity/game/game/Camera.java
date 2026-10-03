package com.persiancity.game.game;

import android.graphics.Canvas;

/**
 * دوربین بازی — دنبال کردن بازیکن با نرمی، محدود به مرزهای شهر
 * + زوم: حین راه رفتن نزدیک‌تر است (درخواست کاربر) و حین رانندگی کمی دورتر
 */
public class Camera {
    public float x, y;          // مرکز دید
    public float zoom = 1.55f;  // بزرگ‌نمایی فعلی
    public float zoomTarget = 1.55f;
    private float halfW, halfH;

    public void setViewport(float w, float h) {
        this.halfW = w / 2f;
        this.halfH = h / 2f;
        clampTo();
    }

    public void snap(float tx, float ty) {
        this.x = tx;
        this.y = ty;
        clampTo();
    }

    public void follow(float tx, float ty, float dt) {
        float k = 1f - (float) Math.pow(0.0001, dt);
        x += (tx - x) * k;
        y += (ty - y) * k;
        // زوم نرم به سمت هدف
        zoom += (zoomTarget - zoom) * Math.min(1f, dt * 2.2f);
        clampTo();
    }

    private void clampTo() {
        float hw = halfW / zoom, hh = halfH / zoom;
        if (G.WORLD_W > hw * 2f) {
            x = G.clamp(x, hw, G.WORLD_W - hw);
        } else {
            x = G.WORLD_W / 2f;
        }
        if (G.WORLD_H > hh * 2f) {
            y = G.clamp(y, hh, G.WORLD_H - hh);
        } else {
            y = G.WORLD_H / 2f;
        }
    }

    public void apply(Canvas c) {
        c.translate(halfW, halfH);
        c.scale(zoom, zoom);
        c.translate(-x, -y);
    }
}
