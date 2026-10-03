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
    public static final int CAR_BOAT = 8;
    public static final int CAR_POLICE = 9;
    public static final int CAR_HORSE = 10;

    public static final int MODE_TRAFFIC = 0;   // در ترافیک شهر
    public static final int MODE_PARKED = 1;    // پارک شده (قابل خرید/سوار شدن)
    public static final int MODE_PLAYER = 2;    // توسط بازیکن رانده می‌شود
    public static final int MODE_RAIL = 3;      // قطار روی ریل
    public static final int MODE_GRAZE = 4;     // چرای آزاد (اسب روستا)

    public int type;
    public int mode = MODE_TRAFFIC;
    public float speed = 0f;
    public float angle = 0f;          // رادیان
    public int color = 0xFFE53935;
    public boolean owned = false;     // متعلق به بازیکن است؟

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
    public int railIndex = 0;         // حلقه ریل (۰ = شهر اصلی، ۱ = شرق)
    public float hornCooldown = 0f;

    // چرا (اسب)
    private float grazeTimer = 0f;
    private float grazeX = Float.NaN, grazeY = Float.NaN;

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
            this.speed = 150f;
        } else if (type == MOTOR) {
            this.color = CAR_COLORS[rnd.nextInt(CAR_COLORS.length)];
            this.speed = 150f;
        } else if (type == CAR_BUS) {
            this.color = 0xFFFB8C00;
            this.speed = 85f;
        } else if (type == CAR_POLICE) {
            this.color = 0xFFECEFF1;
            this.speed = 118f;
        } else if (type == CAR_HORSE) {
            this.color = 0xFF8D5524;
            this.speed = 34f;
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
     * شعاع برخورد این وسیله (ماشین‌ها بزرگ‌تر شدند)
     */
    public float blockRadius() {
        switch (type) {
            case MOTOR: return 26f;
            case CAR_BUS: return 54f;
            case CAR_HELICOPTER: return 40f;
            case CAR_BOAT: return 40f;
            case CAR_HORSE: return 32f;
            default: return 40f;
        }
    }

    /**
     * مکان واگن i ام قطار (۰ = لوکوموتیو)
     */
    public void pointAt(World world, int i, float[] out) {
        RailPath path = world.rail(railIndex);
        if (path == null) {
            out[0] = x;
            out[1] = y;
            return;
        }
        path.posAt(trackPos - i * 95f, tmp);
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
                updateTraffic(dt, world);
                break;

            case MODE_RAIL:
                updateRail(dt, world);
                break;

            case MODE_GRAZE:
                updateGraze(dt, world);
                break;

            case MODE_PARKED:
            case MODE_PLAYER:
            default:
                break;
        }
    }

    /**
     * چرای آزاد اسب — آهسته این‌ور و آن‌ور می‌رود
     */
    private void updateGraze(float dt, World world) {
        grazeTimer -= dt;
        if (grazeTimer <= 0f) {
            grazeTimer = 4f + rnd.nextFloat() * 6f;
            if (rnd.nextBoolean()) {
                grazeX = Float.NaN;
            } else {
                float a = rnd.nextFloat() * (float) Math.PI * 2f;
                float r = 60f + rnd.nextFloat() * 140f;
                float tx = x + (float) Math.cos(a) * r;
                float ty = y + (float) Math.sin(a) * r;
                if (!world.isBlocked(tx, ty, 26f)) {
                    grazeX = tx;
                    grazeY = ty;
                } else {
                    grazeX = Float.NaN;
                }
            }
        }
        if (!Float.isNaN(grazeX)) {
            float dx = grazeX - x, dy = grazeY - y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d < 8f) {
                grazeX = Float.NaN;
                speed = 0f;
            } else {
                speed = 34f;
                float step = speed * dt;
                float nx = x + dx / d * step;
                float ny = y + dy / d * step;
                if (!world.isBlocked(nx, y, 24f)) x = nx;
                if (!world.isBlocked(x, ny, 24f)) y = ny;
                angle = (float) Math.atan2(dy, dx);
                anim += dt * 1.3f;
            }
        } else {
            speed = 0f;
        }
    }

    /**
     * بعد از پیاده شدن، اسب دوباره شروع به چرا می‌کند
     */
    public void resetGraze() {
        grazeTimer = 0.5f;
        grazeX = Float.NaN;
    }

    private void updateTraffic(float dt, World world) {
        // 🚓 آژیر پلیس نزدیک بازیکن
        if (type == CAR_POLICE) {
            hornCooldown -= dt;
            if (hornCooldown <= 0f) {
                hornCooldown = 11f;
                if (world.playerRef != null
                        && G.dist(x, y, world.playerRef.x, world.playerRef.y) < 950f) {
                    SoundManager.play("siren");
                }
            }
        }

        // 🛑 ترمز: اگر بازیکن یا عابری جلوی ماشین باشد، ماشین وایستد
        // (رفع حس «رد شدن از روی ماشین / له شدن عابر»)
        boolean brake = false;
        if (world.playerRef != null) {
            brake = someoneAhead(world.playerRef.x, world.playerRef.y, world);
        }
        if (!brake) {
            for (int i = 0; i < world.cityNpcs.size() && !brake; i++) {
                Npc n = world.cityNpcs.get(i);
                brake = someoneAhead(n.x, n.y, null);
            }
        }
        if (brake) {
            return;   // می‌ایستد ولی سرعت کروز حفظ می‌شود
        }

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

    private boolean someoneAhead(float tx, float ty, World world) {
        float dx = tx - x, dy = ty - y;
        if (axis == 'H') {
            if (Math.abs(dy) > 55f) return false;
            float along = dx * laneDir;
            return along > 10f && along < 130f;
        } else {
            if (Math.abs(dx) > 55f) return false;
            float along = dy * laneDir;
            return along > 10f && along < 130f;
        }
    }

    private void updateRail(float dt, World world) {
        RailPath path = world.rail(railIndex);
        if (path == null) return;
        trackPos += speed * dt;
        path.posAt(trackPos, tmp);
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

        // بوق نزدیک ایستگاه (هر ایستگاه روی حلقه خودش)
        hornCooldown -= dt;
        if (hornCooldown <= 0f) {
            for (int i = 0; i < world.trainStations.size(); i++) {
                float[] st = world.trainStations.get(i);
                if ((int) st[2] != railIndex) continue;
                if (G.dist(x, y, st[0], st[1]) < 300f) {
                    SoundManager.play("train");
                    hornCooldown = 40f;
                    break;
                }
            }
            if (hornCooldown <= 0f) hornCooldown = 3f;
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
