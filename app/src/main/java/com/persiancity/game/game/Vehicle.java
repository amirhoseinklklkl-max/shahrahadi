package com.persiancity.game.game;

import com.persiancity.game.SoundManager;

import java.util.Random;

/**
 * وسایل نقلیه شهر شادی:
 * ماشین‌ها و موتورها در خیابان‌ها رفت‌وآمد می‌کنند (برخورد = دود و محو!)
 * هلیکوپتر پرواز آزاد دارد و قطار روی ریل دور شهر می‌چرخد.
 */
public class Vehicle extends Entity {
    public static final int CAR_SEDAN = 0;
    public static final int CAR_TAXI = 1;
    public static final int CAR_SPORT = 2;
    public static final int CAR_PICKUP = 3;
    public static final int CAR_BUS = 4;
    public static final int MOTOR = 5;
    public static final int CAR_HELICOPTER = 6;
    public static final int CAR_TRAIN = 7;

    public static final int MODE_TRAFFIC = 0;   // در ترافیک شهر
    public static final int MODE_PARKED = 1;    // پارک شده (قابل خرید/سوار شدن)
    public static final int MODE_PLAYER = 2;    // توسط بازیکن رانده می‌شود
    public static final int MODE_RAIL = 3;      // قطار روی ریل

    public int type;
    public int mode = MODE_TRAFFIC;
    public float speed = 0f;
    public float angle = 0f;          // رادیان
    public int color = 0xFFE53935;

    // ترافیک
    public char axis = 'H';           // 'H' افقی / 'V' عمودی
    public float laneDir = 1f;        // +۱ یا -۱
    public float laneMin = 140f;      // محدوده حرکت روی لاین
    public float laneMax = G.WORLD_W - 140f;

    // برخورد و دود
    public float deadTimer = 0f;      // بعد از تصادف ۳ ثانیه نامرئی
    public float smokeTimer = 0f;

    // قطار
    public float trackPos = 0f;       // فاصله روی ریل
    public float hornCooldown = 0f;

    private static final Random rnd = new Random();
    private static final float[] tmp = new float[3];

    public static final int[] CAR_COLORS = {
        0xFFE53935, 0xFF1E88E5, 0xFFFDD835, 0xFF43A047, 0xFF8E24AA, 0xFFFF7043, 0xFF00ACC1
    };

    public Vehicle(int type, float x, float y) {
        this.type = type;
        this.x = x;
        this.y = y;
        if (type == CAR_TRAIN) {
            this.color = 0xFFD32F2F;
            this.speed = 95f;
        } else if (type == MOTOR) {
            this.color = CAR_COLORS[rnd.nextInt(CAR_COLORS.length)];
            this.speed = 150f;
        } else if (type == CAR_BUS) {
            this.color = 0xFFFB8C00;
            this.speed = 85f;
        } else {
            this.color = CAR_COLORS[rnd.nextInt(CAR_COLORS.length)];
            this.speed = 105f + rnd.nextFloat() * 45f;
        }
    }

    public boolean isFlying() {
        return type == CAR_HELICOPTER && mode == MODE_PLAYER;
    }

    public boolean isActive() {
        return deadTimer <= 0f;
    }

    /**
     * شعاع برخورد این وسیله
     */
    public float blockRadius() {
        switch (type) {
            case MOTOR: return 20f;
            case CAR_BUS: return 44f;
            case CAR_HELICOPTER: return 40f;
            default: return 32f;
        }
    }

    /**
     * مکان واگن i ام قطار (۰ = لوکوموتیو)
     */
    public void pointAt(World world, int i, float[] out) {
        if (world.railPath == null) {
            out[0] = x;
            out[1] = y;
            return;
        }
        world.railPath.posAt(trackPos - i * 95f, tmp);
        out[0] = tmp[0];
        out[1] = tmp[1];
        if (i == 0) {
            out[0] = x;
            out[1] = y;
        }
    }

    /**
     * به‌روزرسانی رفتار
     */
    public void update(float dt, World world) {
        if (deadTimer > 0f) {
            deadTimer -= dt;
            if (deadTimer <= 0f) respawn(world);
            return;
        }

        switch (mode) {
            case MODE_TRAFFIC:
                updateTraffic(dt);
                break;

            case MODE_RAIL:
                updateRail(dt, world);
                break;

            case MODE_PARKED:
            case MODE_PLAYER:
            default:
                break;
        }
    }

    private void updateTraffic(float dt) {
        float v = speed * laneDir * dt;
        if (axis == 'H') {
            x += v;
            angle = laneDir > 0 ? 0f : (float) Math.PI;
            // رسیدن به آخر مسیر = دور همان مسیر می‌زند
            if (x > laneMax) x = laneMin;
            if (x < laneMin) x = laneMax;
        } else {
            y += v;
            angle = laneDir > 0 ? (float) Math.PI / 2f : (float) -Math.PI / 2f;
            if (y > laneMax) y = laneMin;
            if (y < laneMin) y = laneMax;
        }
    }

    private void updateRail(float dt, World world) {
        if (world.railPath == null) return;
        trackPos += speed * dt;
        world.railPath.posAt(trackPos, tmp);
        x = tmp[0];
        y = tmp[1];
        angle = tmp[2];

        // دود دودکش
        smokeTimer -= dt;
        if (smokeTimer <= 0f) {
            smokeTimer = 0.38f;
            float sx = x - (float) Math.cos(angle) * 34f;
            float sy = y - (float) Math.sin(angle) * 34f - 46f;
            world.addSmoke(sx, sy, 9f);
        }

        // بوق نزدیک ایستگاه
        hornCooldown -= dt;
        if (hornCooldown <= 0f && world.trainStationX > 0f) {
            if (G.dist(x, y, world.trainStationX, world.trainStationY) < 260f) {
                SoundManager.play("train");
                hornCooldown = 40f;
            }
        }
    }

    /**
     * بعد از تصادف یا رسیدن به آخر شهر: جای دیگر ظاهر شو
     */
    public void respawn(World world) {
        deadTimer = 0f;
        if (type == CAR_TRAIN || mode == MODE_RAIL) return;   // قطار هرگز نمی‌میرد
        if (mode == MODE_PLAYER) return;
        world.randomLaneSpawn(this);
        speed = type == MOTOR ? 150f : 105f + rnd.nextFloat() * 45f;
    }

    /**
     * جهت حرکت برای رسم
     */
    public float drawAngle() {
        return angle;
    }
}
