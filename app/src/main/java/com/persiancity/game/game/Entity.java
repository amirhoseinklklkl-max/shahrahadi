package com.persiancity.game.game;

/**
 * کلاس پایه همه موجودیت‌های متحرک
 */
public class Entity {
    public float x, y;
    public int dir = 0;      // ۰=پایین ۱=چپ ۲=بالا ۳=راست
    public float anim = 0f;

    public Entity() {}

    public Entity(float x, float y) {
        this.x = x;
        this.y = y;
    }
}
