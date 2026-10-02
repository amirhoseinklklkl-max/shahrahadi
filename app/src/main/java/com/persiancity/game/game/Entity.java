package com.persiancity.game.game;

/**
 * موجودیت پایه دنیای بازی
 */
public abstract class Entity {

    public float x, y;   // مختصات مرکز
    public float w, h;   // اندازه جعبه

    public Entity(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public boolean overlaps(Entity other, float margin) {
        return Math.abs(x - other.x) < (w + other.w) / 2f + margin
                && Math.abs(y - other.y) < (h + other.h) / 2f + margin;
    }

    public float distanceTo(Entity other) {
        return G.dist(x, y, other.x, other.y);
    }
}
