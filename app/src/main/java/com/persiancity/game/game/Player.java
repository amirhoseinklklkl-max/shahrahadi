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
    public Vehicle driving = null;   // خودرو/موتور/هلی/قطار در حال راندن

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
            float speed = isTired() ? G.WALK_SPEED_TIRED : G.WALK_SPEED;
            float nx = dx / len, ny = dy / len;
            float step = speed * dt;

            // برخورد محور به محور تا گیر نکند
            float tryX = x + nx * step;
            if (!world.isBlocked(tryX, y, 14f) && !world.vehicleBlocks(tryX, y, 14f)) {
                x = tryX;
            }
            float tryY = y + ny * step;
            if (!world.isBlocked(x, tryY, 14f) && !world.vehicleBlocks(x, tryY, 14f)) {
                y = tryY;
            }
            x = G.clamp(x, 100f, G.WORLD_W - 100f);
            y = G.clamp(y, 100f, G.WORLD_H - 100f);

            // جهت نگاه
            if (Math.abs(nx) > Math.abs(ny)) {
                dir = nx > 0 ? 3 : 1;
            } else {
                dir = ny > 0 ? 0 : 2;
            }
            anim += dt * 1.6f;

            // انرژی و گرسنگی
            energy = Math.max(0f, energy - dt * 0.55f);
            hunger = Math.max(0f, hunger - dt * 0.4f);
            if (hunger <= 0f) energy = Math.max(0f, energy - dt * 1.2f);
        } else {
            anim = 0f;
            energy = Math.min(100f, energy + dt * 0.8f);
        }
    }

    /**
     * داخل ساختمان — حرکت آزاد در اتاق
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
            if (tryY > 30f && tryY < room.roomH - 20f) y = tryY;
            if (Math.abs(nx) > Math.abs(ny)) dir = nx > 0 ? 3 : 1;
            else dir = ny > 0 ? 0 : 2;
            anim += dt * 1.6f;
            energy = Math.max(0f, energy - dt * 0.3f);
        } else {
            anim = 0f;
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
        } catch (Exception e) {
        }
    }
}
