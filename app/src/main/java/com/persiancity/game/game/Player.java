package com.persiancity.game.game;

/**
 * بازیکن — شخصیت اصلی بازی
 */
public class Player extends Entity {

    // جهت نگاه: ۰=پایین ۱=بالا ۲=چپ ۳=راست
    public int facing = 0;
    public float animTime = 0f;
    public boolean moving = false;

    // ظاهر
    public int outfit = 0;   // مدل لباس
    public int hair = 0;     // مدل مو

    // وضعیت
    public int money = G.START_MONEY;
    public float hunger = 100f;   // ۱۰۰ = سیر کامل
    public float energy = 100f;   // ۱۰۰ = پر انرژی

    // رانندگی
    public Vehicle driving = null;

    // حمل بار (برای شغل گارسونی: کد غذا، برای پیک: بسته)
    public int carrying = -1;     // -1 = چیزی حمل نمی‌کند

    public Player(float x, float y) {
        super(x, y, 40, 40);
    }

    public void update(float dt, float joyX, float joyY, World world) {
        if (driving != null) return; // وقتی ماشین سوار است حرکت مستقیم ندارد

        float len = (float) Math.sqrt(joyX * joyX + joyY * joyY);
        moving = len > 0.15f;

        float speed = (energy > 15f ? G.WALK_SPEED : G.WALK_SPEED_TIRED);

        if (moving) {
            float nx = joyX / Math.max(1f, len);
            float ny = joyY / Math.max(1f, len);
            if (len > 1f) {
                nx = joyX / len;
                ny = joyY / len;
            }

            float dx = nx * speed * dt;
            float dy = ny * speed * dt;

            // حرکت جدا برای لغزش روی دیوارها
            if (!world.collides(x + dx, y, 16f)) {
                x += dx;
            }
            if (!world.collides(x, y + dy, 16f)) {
                y += dy;
            }

            // جهت نگاه
            if (Math.abs(nx) > Math.abs(ny)) {
                facing = nx < 0 ? 2 : 3;
            } else {
                facing = ny < 0 ? 1 : 0;
            }

            animTime += dt;
        } else {
            animTime = 0f;
        }

        // گرسنگی و انرژی
        float hungerRate = 0.25f;      // در ثانیه واقعی
        float energyRate = 0.30f;
        if (moving) energyRate += 0.10f;
        if (hunger <= 0f) energyRate += 0.35f; // وقتی گشنه‌ای زود خسته می‌شی

        hunger = Math.max(0f, hunger - hungerRate * dt);
        energy = Math.max(0f, energy - energyRate * dt);
    }

    public boolean isTired() {
        return energy <= 15f;
    }

    public void eat(float hungerAdd, float energyAdd) {
        hunger = Math.min(100f, hunger + hungerAdd);
        energy = Math.min(100f, energy + energyAdd);
    }

    public void sleep() {
        energy = 100f;
        hunger = Math.max(20f, hunger - 30f);
    }

    public boolean canAfford(int price) {
        return money >= price;
    }

    public void pay(int amount) {
        money = Math.max(0, money - amount);
    }

    public void earn(int amount) {
        money += amount;
    }
}
