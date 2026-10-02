package com.persiancity.game.game;

/**
 * دوربین بازی — بازیکن را نرم دنبال می‌کند
 */
public class Camera {

    public float x, y;        // مرکز نگاه دوربین (مختصات دنیا)
    public float scale = 1f;  // بزرگ‌نمایی
    private float targetScale = 1f;

    public void follow(float tx, float ty, float dt, boolean driving) {
        targetScale = driving ? 0.82f : 1f;
        scale += (targetScale - scale) * Math.min(1f, dt * 3f);

        float lerp = Math.min(1f, dt * 8f);
        x += (tx - x) * lerp;
        y += (ty - y) * lerp;

        clampToMap();
    }

    public void snapTo(float tx, float ty) {
        x = tx;
        y = ty;
        clampToMap();
    }

    private void clampToMap() {
        // محدوده دید صفحه‌ای بعداً در رندر لحاظ می‌شود؛ اینجا حداقلی محدود می‌کنیم
        x = G.clamp(x, 0, G.WORLD_W);
        y = G.clamp(y, 0, G.WORLD_H);
    }

    public float screenToWorldX(float screenX, float viewW) {
        return (screenX - viewW / 2f) / scale + x;
    }

    public float screenToWorldY(float screenY, float viewH) {
        return (screenY - viewH / 2f) / scale + y;
    }
}
