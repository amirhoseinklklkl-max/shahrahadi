package com.persiancity.game.game;

import java.util.Random;

/**
 * شخصیت‌های داخل بازی (NPC) — گردش می‌کنند و با آن‌ها حرف می‌زنیم
 */
public class Npc extends Entity {

    // نقش‌ها
    public static final int ROLE_CITIZEN = 0;        // شهروند عادی
    public static final int ROLE_RESTAURANT_MANAGER = 1; // مدیر رستوران
    public static final int ROLE_MARKET_MANAGER = 2;     // مدیر سوپرمارکت
    public static final int ROLE_TABLE_CUSTOMER = 3;     // مشتری رستوران
    public static final int ROLE_QUEUE_CUSTOMER = 4;     // مشتری صندوق
    public static final int ROLE_TAXI_PASSENGER = 5;     // مسافر تاکسی

    public final String name;
    public final int role;
    public final int outfitColor, pantsColor, hairColor, hairStyle, skinColor;

    // هوش مصنوعی حرکت
    private static final int ST_IDLE = 0, ST_WALK = 1;
    private int state = ST_IDLE;
    private float targetX, targetY;
    private float idleTimer = 0f;
    public int facing = 0;
    public float animTime = 0f;

    // حباب حرف
    public String bubble = null;
    private float bubbleTimer = 0f;

    private final Random rnd;

    public Npc(float x, float y, String name, int role, Random rnd) {
        super(x, y, 40, 40);
        this.name = name;
        this.role = role;
        this.rnd = rnd;
        this.targetX = x;
        this.targetY = y;

        // ظاهر تصادفی بامزه
        int[] shirtColors = {0xFFEF5350, 0xFF42A5F5, 0xFF66BB6A, 0xFFFFCA28, 0xFFAB47BC, 0xFFFF7043, 0xFF26C6DA};
        int[] pantsColors = {0xFF3949AB, 0xFF5D4037, 0xFF37474F, 0xFF00695C, 0xFF6D4C41};
        int[] hairColors = {0xFF3E2723, 0xFF212121, 0xFF6D4C41, 0xFF8D6E63, 0xFF4E342E};
        this.outfitColor = shirtColors[rnd.nextInt(shirtColors.length)];
        this.pantsColor = pantsColors[rnd.nextInt(pantsColors.length)];
        this.hairColor = hairColors[rnd.nextInt(hairColors.length)];
        this.hairStyle = rnd.nextInt(4);
        this.skinColor = rnd.nextBoolean() ? G.COL_SKIN : G.COL_SKIN2;
    }

    public void update(float dt, World world) {
        if (bubble != null) {
            bubbleTimer -= dt;
            if (bubbleTimer <= 0f) bubble = null;
        }

        // مشتری‌های داخل محیط را JobSystem مدیریت می‌کند
        if (role == ROLE_TABLE_CUSTOMER || role == ROLE_QUEUE_CUSTOMER || role == ROLE_TAXI_PASSENGER) {
            walkToward(dt, world);
            return;
        }

        switch (state) {
            case ST_IDLE:
                idleTimer -= dt;
                if (idleTimer <= 0f) {
                    // انتخاب مقصد جدید نزدیک محل فعلی
                    float[] t = world.randomWalkableNear(x, y, 8f * G.TILE, false);
                    if (t != null) {
                        targetX = t[0];
                        targetY = t[1];
                        state = ST_WALK;
                    } else {
                        idleTimer = 2f;
                    }
                }
                break;

            case ST_WALK:
                float d = G.dist(x, y, targetX, targetY);
                if (d < 10f) {
                    state = ST_IDLE;
                    idleTimer = 1.5f + rnd.nextFloat() * 4.5f;
                    // گاهی حباب حرف بامزه
                    if (rnd.nextInt(6) == 0) {
                        say(Dialogues.randomAmbient(rnd));
                    }
                } else {
                    float nx = (targetX - x) / d;
                    float ny = (targetY - y) / d;
                    float step = G.NPC_SPEED * dt;
                    float newX = x + nx * step;
                    float newY = y + ny * step;
                    if (!world.collides(newX, y, 14f)) x = newX;
                    if (!world.collides(x, newY, 14f)) y = newY;
                    facing = Math.abs(nx) > Math.abs(ny) ? (nx < 0 ? 2 : 3) : (ny < 0 ? 1 : 0);
                    animTime += dt;
                }
                break;
        }
    }

    /**
     * حرکت به سمت هدف مشخص (برای مشتری‌ها)
     */
    private void walkToward(float dt, World world) {
        float d = G.dist(x, y, targetX, targetY);
        if (d < 8f) return;
        float nx = (targetX - x) / d;
        float ny = (targetY - y) / d;
        float step = G.NPC_SPEED * 1.2f * dt;
        float newX = x + nx * step;
        float newY = y + ny * step;
        if (!world.collides(newX, y, 14f)) x = newX;
        if (!world.collides(x, newY, 14f)) y = newY;
        facing = Math.abs(nx) > Math.abs(ny) ? (nx < 0 ? 2 : 3) : (ny < 0 ? 1 : 0);
        animTime += dt;
    }

    public void setTarget(float tx, float ty) {
        this.targetX = tx;
        this.targetY = ty;
    }

    public boolean atTarget() {
        return G.dist(x, y, targetX, targetY) < 10f;
    }

    public void say(String text, float seconds) {
        bubble = text;
        bubbleTimer = seconds;
    }

    public void say(String text) {
        say(text, 3f);
    }
}
