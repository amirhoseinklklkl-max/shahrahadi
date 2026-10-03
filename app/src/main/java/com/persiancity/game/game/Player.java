package com.persiancity.game.game;

import org.json.JSONObject;

/**
 * بازیکن — شخصیت اصلی شهر شادی
 */
public class Player extends Entity {
    public String name = "قهرمان";
    public int gender = 0;        // ۰ = پسر، ۱ = دختر
    public int money = G.START_MONEY;
    public float hunger = 100f;   // سیری
    public float energy = 100f;   // انرژی
    public Vehicle driving = null;   // خودرو/موتور/هلی/قطار/اسب در حال راندن
    public boolean swimming = false; // در آب دریاچه شنا می‌کند
    public int outfit = 0;           // استایل لباس (اندیس OUTFITS)

    // بسته شدن سفر با قطار (مسافری)
    public boolean ridingTrain = false;
    public float trainBoardDist = 0f;

    public void eat(float hungerAdd, float energyAdd) {
        hunger = Math.min(hunger + hungerAdd, 100f);
        energy = Math.min(energy + energyAdd, 100f);
    }

    public void addMoney(int amount) {
        money = Math.max(0, money + amount);
    }

    public boolean isTired() {
        return energy < 25f;
    }

    /**
     * حرکت با برخورد — dx/dy از اهرم
     */
    public void update(float dt, World world, float dx, float dy) {
        if (driving != null || ridingTrain) {
            anim += dt;
            return;
        }
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        boolean moving = len > 0.12f;
        if (moving) {
            float speed = swimming ? G.SWIM_SPEED
                    : (isTired() ? G.WALK_SPEED_TIRED : G.WALK_SPEED);
            float nx = dx / len, ny = dy / len;
            float step = speed * dt;

            if (swimming) {
                // 🏊 شنا — روی آب آزاد است، فقط ساختمان مانع است
                float tryX = x + nx * step;
                if (!world.isBlockedForSwim(tryX, y)) x = tryX;
                float tryY = y + ny * step;
                if (!world.isBlockedForSwim(x, tryY)) y = tryY;
                energy = Math.max(0f, energy - dt * 1.3f);
                hunger = Math.max(0f, hunger - dt * 0.7f);
            } else {
                // برخورد محور به محور تا گیر نکند
                float tryX = x + nx * step;
                if (!world.isBlocked(tryX, y, 14f) && !world.vehicleBlocks(tryX, y, 14f)) {
                    x = tryX;
                }
                float tryY = y + ny * step;
                if (!world.isBlocked(x, tryY, 14f) && !world.vehicleBlocks(x, tryY, 14f)) {
                    y = tryY;
                }
                energy = Math.max(0f, energy - dt * 0.55f);
                hunger = Math.max(0f, hunger - dt * 0.4f);
                if (hunger <= 0f) energy = Math.max(0f, energy - dt * 1.2f);
            }
            x = G.clamp(x, 60f, G.WORLD_W - 60f);
            y = G.clamp(y, 60f, G.WORLD_H - 60f);

            // جهت نگاه
            if (Math.abs(nx) > Math.abs(ny)) {
                dir = nx > 0 ? 3 : 1;
            } else {
                dir = ny > 0 ? 0 : 2;
            }
            anim += dt * (swimming ? 1.0f : 1.6f);
        } else {
            anim = 0f;
            if (!swimming) {
                energy = Math.min(100f, energy + dt * 0.8f);
            }
        }

        // رسیدن به خشکی = پایان شنا
        if (swimming && world.tileAt(x, y) != World.T_WATER) {
            swimming = false;
        }
    }

    /**
     * داخل ساختمان — حرکت آزاد در اتاق + خروج از درِ پایین
     */
    public void updateInterior(float dt, World world, float dx, float dy, Interior room) {
        if (driving != null || ridingTrain) return;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        boolean moving = len > 0.12f;
        if (moving) {
            float speed = G.WALK_SPEED * 0.85f;
            float nx = dx / len, ny = dy / len;
            float step = speed * dt;
            float tryX = x + nx * step;
            if (tryX > 20f && tryX < room.roomW - 20f) x = tryX;
            float tryY = y + ny * step;
            if (tryY > 30f && tryY < room.roomH - 14f) y = tryY;
            if (Math.abs(nx) > Math.abs(ny)) dir = nx > 0 ? 3 : 1;
            else dir = ny > 0 ? 0 : 2;
            anim += dt * 1.6f;
            energy = Math.max(0f, energy - dt * 0.3f);
        } else {
            anim = 0f;
        }

        // ✅ رسیدن به در خروج = خارج شدن از ساختمان (رفع باگ قفل‌شدن داخل رستوران/باغ‌وحش/سینما)
        if (y >= room.roomH - 30f && Math.abs(x - room.doorX) < 78f) {
            world.exitInterior(this);
        }
    }

    public void writeJson(JSONObject o) {
        try {
            o.put("name", name);
            o.put("gender", gender);
            o.put("money", money);
            o.put("hunger", hunger);
            o.put("energy", energy);
            o.put("px", x);
            o.put("py", y);
            o.put("dir", dir);
            o.put("outfit", outfit);
        } catch (Exception e) {
        }
    }

    public void readJson(JSONObject o) {
        try {
            if (o.has("name")) name = o.getString("name");
            if (o.has("gender")) gender = o.getInt("gender");
            money = o.optInt("money", G.START_MONEY);
            hunger = (float) o.optDouble("hunger", 100.0);
            energy = (float) o.optDouble("energy", 100.0);
            x = (float) o.optDouble("px", G.WORLD_W / 2);
            y = (float) o.optDouble("py", G.WORLD_H / 2);
            dir = o.optInt("dir", 0);
            outfit = o.optInt("outfit", gender == 1 ? 5 : 0);
        } catch (Exception e) {
        }
    }
}
