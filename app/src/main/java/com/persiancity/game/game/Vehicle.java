package com.persiancity.game.game;

import android.graphics.Canvas;

/**
 * وسایل نقلیه — ماشین و موتور با فیزیک ساده و بامزه
 */
public class Vehicle extends Entity {

    // مدل‌ها
    public static final int CAR_ABI = 0;      // آبی ابری (سدان)
    public static final int CAR_TANDE = 1;    // تندر نارنجی (اسپرت)
    public static final int CAR_VAN = 2;      // ون خانواده
    public static final int CAR_VANT = 3;     // وانت باری
    public static final int CAR_CLASSIC = 4;  // کلاسیک قرمز
    public static final int BIKE_SKOOTER = 5; // اسکوتر ملایم
    public static final int BIKE_SHETAB = 6;  // موتور شتاب
    public static final int BIKE_NEON = 7;    // موتور نئون

    // حالت
    public static final int MODE_PARKED = 0;
    public static final int MODE_PLAYER = 1;
    public static final int MODE_TRAFFIC = 2;

    public static class Model {
        public final String name;
        public final int price;
        public final float maxSpeed;
        public final int baseColor;
        public final boolean bike;
        public final String desc;

        public Model(String name, int price, float maxSpeed, int baseColor, boolean bike, String desc) {
            this.name = name;
            this.price = price;
            this.maxSpeed = maxSpeed;
            this.baseColor = baseColor;
            this.bike = bike;
            this.desc = desc;
        }
    }

    public static final Model[] MODELS = {
            new Model("آبی ابری", 15000, 300f, 0xFF42A5F5, false, "یک سدان آروم و مطمئن"),
            new Model("تندر نارنجی", 32000, 385f, 0xFFFF7043, false, "اسپرت و خفن! برای شغل تاکسی عالیه"),
            new Model("ون خانواده", 22000, 265f, 0xFF9CCC65, false, "جادار برای دورهمی‌ها"),
            new Model("وانت باری", 18000, 280f, 0xFFFFB300, false, "برای حمل بار و کار"),
            new Model("کلاسیک قرمز", 28000, 320f, 0xFFEF5350, false, "قدیمی ولی شیک"),
            new Model("اسکوتر ملایم", 4000, 265f, 0xFF4DD0E1, true, "اسکوتر بامزه برای پیک"),
            new Model("موتور شتاب", 7500, 360f, 0xFFAB47BC, true, "تند و تیز!"),
            new Model("موتور نئون", 9000, 400f, 0xFF7C4DFF, true, "با نورهای رنگی شبانه"),
    };

    public final int model;
    public int paint;          // رنگ بدنه (قابل تغییر در گاراژ)
    public int engineLevel;    // ۰ تا ۳
    public boolean spoiler;    // بال اسپرت
    public int neonColor;      // ۰ = ندارد
    public int mode = MODE_PARKED;
    public boolean hasPassenger = false;   // مسافر تاکسی

    // فیزیک
    public float angle = 0f;   // رادیان (۰ = شرق)
    public float speed = 0f;   // پیکسل بر ثانیه (منفی = دنده عقب)

    // هوش مصنوعی ترافیک
    public int trafficAxis = 0;   // ۰=افقی ۱=عمودی
    public int trafficSign = 1;   // ۱ یا -۱

    public Vehicle(int model, float x, float y) {
        super(x, y, 0, 0);
        this.model = model;
        this.paint = MODELS[model].baseColor;
        if (isBike()) {
            w = 34;
            h = 60;
        } else if (model == CAR_VAN || model == CAR_VANT) {
            w = 76;
            h = 130;
        } else {
            w = 70;
            h = 124;
        }
    }

    public boolean isBike() {
        return MODELS[model].bike;
    }

    public float maxSpeed() {
        return MODELS[model].maxSpeed * (1f + 0.12f * engineLevel);
    }

    public float accelPower() {
        boolean sport = model == CAR_TANDE || model == BIKE_SHETAB || model == BIKE_NEON;
        return (isBike() ? 330f : 240f) * (sport ? 1.25f : 1f);
    }

    /**
     * رانندگی بازیکن با جوی‌استیک
     */
    public void drive(float dt, float joyX, float joyY, World world) {
        float throttle = -joyY;   // بالا = گاز
        float steer = joyX;

        if (throttle > 0.1f) {
            speed += accelPower() * throttle * dt;
        } else if (throttle < -0.1f) {
            // ترمز / دنده عقب
            speed -= accelPower() * 0.9f * (-throttle) * dt;
        } else {
            // اصطکاک
            speed *= (1f - 1.4f * dt);
            if (Math.abs(speed) < 8f) speed = 0f;
        }

        float max = maxSpeed();
        float maxRev = -max * 0.35f;
        speed = G.clamp(speed, maxRev, max);

        // فرمان
        float speedNorm = Math.abs(speed) / maxSpeed();
        if (Math.abs(speed) > 15f) {
            float turnRate = 2.4f * (isBike() ? 1.25f : 1f);
            float t = steer * turnRate * dt * (0.45f + 0.55f * speedNorm);
            if (speed < 0) t = -t;
            angle += t;
        }

        moveWithCollision(dt, world, 20f);
    }

    /**
     * حرکت ترافیک شهری روی جاده‌ها
     */
    public void trafficUpdate(float dt, World world, Entity playerEntity) {
        float targetSpeed = 150f + (model % 3) * 20f;

        // ترمز برای بازیکن
        float lookAhead = 110f;
        float fx = x + (float) Math.cos(angle) * lookAhead;
        float fy = y + (float) Math.sin(angle) * lookAhead;
        if (playerEntity != null && G.dist(fx, fy, playerEntity.x, playerEntity.y) < 95f) {
            targetSpeed = 0f;
        }

        if (speed < targetSpeed) speed = Math.min(targetSpeed, speed + 220f * dt);
        else if (speed > targetSpeed) speed = Math.max(targetSpeed, speed - 400f * dt);

        // چک جاده بودن جلو
        float nx = x + (float) Math.cos(angle) * (isBike() ? 30f : 45f);
        float ny = y + (float) Math.sin(angle) * (isBike() ? 30f : 45f);
        if (!world.isRoadPoint(nx, ny)) {
            // آخر جاده: دور بزن
            angle += (float) Math.PI;
            trafficSign = -trafficSign;
        }

        moveWithCollision(dt, world, 18f);
    }

    private void moveWithCollision(float dt, World world, float radius) {
        float dx = (float) Math.cos(angle) * speed * dt;
        float dy = (float) Math.sin(angle) * speed * dt;

        float nx = x + dx;
        float ny = y + dy;

        if (!world.collides(nx + (float) Math.cos(angle) * 20f, ny + (float) Math.sin(angle) * 20f, radius * 0.6f)
                && !world.collides(nx, ny, radius)) {
            x = nx;
            y = ny;
        } else {
            // برخورد: ایست با کوچک پسرفت
            speed *= -0.25f;
        }

        // محدود به دنیا
        x = G.clamp(x, 60f, G.WORLD_W - 60f);
        y = G.clamp(y, 60f, G.WORLD_H - 60f);
    }

    public float speedNorm() {
        return G.clamp(Math.abs(speed) / maxSpeed(), 0f, 1f);
    }

    public String displayName() {
        return MODELS[model].name;
    }

    public void draw(Canvas c, SpriteLib sprites) {
        if (isBike()) {
            sprites.drawMotorcycle(c, this);
        } else {
            sprites.drawCar(c, this);
        }
    }
}
