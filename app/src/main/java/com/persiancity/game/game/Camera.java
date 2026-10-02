package com.persiancity.game.game;

import android.graphics.Canvas;

/**
 * دوربین بازی — دنبال کردن بازیکن با نرمی و محدود به مرزهای شهر
 */
public class Camera {
    public float x, y;          // مرکز دید
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
        clampTo();
    }

    private void clampTo() {
        if (G.WORLD_W > halfW * 2f) {
            x = G.clamp(x, halfW, G.WORLD_W - halfW);
        } else {
            x = G.WORLD_W / 2f;
        }
        if (G.WORLD_H > halfH * 2f) {
            y = G.clamp(y, halfH, G.WORLD_H - halfH);
        } else {
            y = G.WORLD_H / 2f;
        }
    }

    public void apply(Canvas c) {
        c.translate(halfW - x, halfH - y);
    }
}
