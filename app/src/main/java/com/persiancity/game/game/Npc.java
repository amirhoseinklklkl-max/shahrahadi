package com.persiancity.game.game;

import java.util.ArrayList;
import java.util.Random;

/**
 * شهروندهای شهر شادی — این‌ور و آن‌ور می‌روند و حرف می‌زنند
 */
public class Npc extends Entity {
    public final int gender;         // ۰=آقا ۱=خانم
    public final int shirt;
    public final int pants;
    public final int hairStyle;
    public final int skin;

    private float wanderTimer = 0f;
    private float vx = 0f, vy = 0f;
    public String bubble = null;
    public float bubbleTimer = 0f;

    private static final Random rnd = new Random();
    private static final int[] SHIRTS_M = {0xFF42A5F5, 0xFF66BB6A, 0xFFFF7043, 0xFFAB47BC, 0xFFFFCA28};
    private static final int[] SHIRTS_F = {0xFFF06292, 0xFFBA68C8, 0xFF4DD0E1, 0xFFFFB74D, 0xFFAED581};
    private static final int[] PANTS = {0xFF3949AB, 0xFF5D4037, 0xFF37474F, 0xFF6D4C41};

    public Npc(float x, float y, int gender) {
        super(x, y);
        this.gender = gender;
        this.shirt = gender == 1 ? SHIRTS_F[rnd.nextInt(SHIRTS_F.length)] : SHIRTS_M[rnd.nextInt(SHIRTS_M.length)];
        this.pants = PANTS[rnd.nextInt(PANTS.length)];
        this.hairStyle = gender == 1 ? 2 : rnd.nextInt(2);
        this.skin = rnd.nextBoolean() ? G.COL_SKIN : G.COL_SKIN2;
    }

    public void update(float dt, World world) {
        if (bubbleTimer > 0f) {
            bubbleTimer -= dt;
            if (bubbleTimer <= 0f) bubble = null;
        }

        wanderTimer -= dt;
        if (wanderTimer <= 0f) {
            wanderTimer = 1.5f + rnd.nextFloat() * 3f;
            if (rnd.nextInt(3) == 0) {
                vx = 0f;
                vy = 0f;
            } else {
                float a = rnd.nextFloat() * (float) Math.PI * 2f;
                vx = (float) Math.cos(a) * G.NPC_SPEED;
                vy = (float) Math.sin(a) * G.NPC_SPEED;
            }
        }

        if (vx != 0f || vy != 0f) {
            float step = dt;
            float tryX = x + vx * step;
            if (!world.isBlocked(tryX, y, 14f)) x = tryX;
            else vx = -vx;
            float tryY = y + vy * step;
            if (!world.isBlocked(x, tryY, 14f)) y = tryY;
            else vy = -vy;

            if (Math.abs(vx) > Math.abs(vy)) dir = vx > 0 ? 3 : 1;
            else dir = vy > 0 ? 0 : 2;
            anim += dt * 1.4f;
        } else {
            anim = 0f;
        }
    }

    public void say(String text) {
        bubble = text;
        bubbleTimer = 3.5f;
    }

    /**
     * حرف تصادفی شاد
     */
    public static String randomTalk() {
        String[] talks = {
            "چه روز قشنگی!",
            "قطار شهری را دیدی؟ خیلی باحاله!",
            "برو سینما ستاره، کارتون‌هاش عالیه!",
            "من امروز بستنی خوردم!",
            "باغ‌وحش شادی را رفتی؟ زرافه‌اش خیلی بلنده!",
            "سلام قهرمان! خوش بگذرد!",
            "هوای امروز عالیه!",
            "راننده تاکسی هستی؟ من مسافرم!",
            "کتابخانه نور قصه‌های خوبی دارد.",
            "مراقب ماشین‌ها باش ها!"
        };
        return talks[rnd.nextInt(talks.length)];
    }

    public static void scatter(ArrayList<Npc> list, World world, int count, float cx, float cy, float range) {
        for (int i = 0; i < count; i++) {
            float[] p = world.randomWalkableNear(cx, cy, range, false);
            if (p != null) {
                list.add(new Npc(p[0], p[1], rnd.nextInt(2)));
            }
        }
    }
}
