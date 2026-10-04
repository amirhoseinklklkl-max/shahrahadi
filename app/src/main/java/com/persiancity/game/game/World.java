package com.persiancity.game.game;

import com.persiancity.game.SoundManager;

import android.graphics.Canvas;
import android.graphics.Paint;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;

/**
 * دنیای بازی: تایل‌ها، ساختمان‌ها، شهروندها، خودروها، دود، محیط داخلی
 */
public class World {
    // نوع تایل‌ها
    public static final int T_BUILDING = 0;
    public static final int T_ROAD = 1;
    public static final int T_GRASS = 2;
    public static final int T_PARK = 3;
    public static final int T_WATER = 4;
    public static final int T_SIDEWALK = 5;
    public static final int T_PATH = 6;
    public static final int T_DIRT = 7;
    public static final int T_RUNWAY = 8;   // باند فرودگاه

    public int[][] tileType = new int[G.MAP_H][G.MAP_W];

    public final ArrayList<Building> buildings = new ArrayList<>();
    public final ArrayList<Npc> cityNpcs = new ArrayList<>();
    public final ArrayList<Npc> interiorNpcs = new ArrayList<>();
    public final ArrayList<Vehicle> vehicles = new ArrayList<>();
    public final ArrayList<float[]> smoke = new ArrayList<>();      // x,y,vx,vy,life,lifeMax,size
    public final ArrayList<float[]> trees = new ArrayList<>();      // x,y,size
    public final ArrayList<float[]> trafficLanes = new ArrayList<>(); // axis,coord,min,max

    public Interior interior = null;
    public final DayNight dayNight = new DayNight();

    public RailPath railPath = null;
    public RailPath railPath2 = null;   // حلقه شرق (فرودگاه، شهر ستاره، شهر گلاب)
    public float trainStationX = -1f, trainStationY = -1f;   // نقطه سکوی ایستگاه اصلی

    /**
     * حلقه ریل شماره i (۰ = شهر اصلی، ۱ = شرق)
     */
    public RailPath rail(int i) {
        return i == 0 ? railPath : railPath2;
    }

    // ایستگاه‌های قطار: x، y سکو، شماره حلقه
    public final ArrayList<float[]> trainStations = new ArrayList<>();

    // ایستگاه‌های اتوبوس بین‌شهری + اسم‌ها
    public final ArrayList<float[]> busStops = new ArrayList<>();
    public final ArrayList<String> busStopNames = new ArrayList<>();

    public float fountainX = 0f, fountainY = 0f;
    public float waterfallX = 0f, waterfallY = 0f;

    // شهربازی
    public float wheelX = 0f, wheelY = 0f;         // چرخ‌وفلک
    public float carouselX = 0f, carouselY = 0f;   // سرسیر

    // دریاچه و روستا
    public final ArrayList<float[]> fishSpots = new ArrayList<>();        // x,y,نوع ماهی
    public final ArrayList<float[]> villageAnimals = new ArrayList<>();   // x,y,نوع (۰=گاو ۱=گوسفند)

    // هواپیمای فرودگاه — خودکار بلند می‌شود و فرود می‌آید
    public float planeX = 0f, planeY = 0f, planeAngle = 0f;
    public float planeAlt = 0f, planeSpeed = 0f;
    public int planeState = 0;
    private float planeTimer = 5f;
    private float planeCircAng = 0f, planeCircStart = 0f;

    // خانه بازیکن و مرجع بازیکن برای ترمز ماشین‌ها
    public Building playerHome = null;
    public Player playerRef = null;

    public float spawnX = G.WORLD_W / 2f, spawnY = G.WORLD_H / 2f;

    public final Paint paint = new Paint();
    private final Random rnd = new Random();
    private final float[] tmpPoint = new float[2];

    // ================= پرس‌وجو =================

    public boolean isBlocked(float x, float y, float r) {
        int x0 = (int) ((x - r) / G.TILE), x1 = (int) ((x + r) / G.TILE);
        int y0 = (int) ((y - r) / G.TILE), y1 = (int) ((y + r) / G.TILE);
        if (x - r < 0 || y - r < 0 || x + r >= G.WORLD_W || y + r >= G.WORLD_H) return true;
        for (int ty = y0; ty <= y1; ty++) {
            for (int tx = x0; tx <= x1; tx++) {
                if (tx < 0 || ty < 0 || tx >= G.MAP_W || ty >= G.MAP_H) return true;
                int t = tileType[ty][tx];
                if (t == T_BUILDING || t == T_WATER) return true;
            }
        }
        return false;
    }

    /**
     * برخورد با خودروها — بازیکن نمی‌تواند از روی ماشین‌ها رد شود.
     * قطار در ۴ نقطه (لوکوموتیو + ۳ واگن) مانع است.
     */
    public boolean vehicleBlocks(float x, float y, float r) {
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle v = vehicles.get(i);
            if (!v.isActive()) continue;
            if (v.isFlying()) continue;   // هلیکوپتر در حال پرواز مانع نیست
            if (v.mode == Vehicle.MODE_PLAYER) continue;   // وسیله خودت مانع نیست

            if (v.type == Vehicle.CAR_TRAIN) {
                for (int w = 0; w < 4; w++) {
                    v.pointAt(this, w, tmpPoint);
                    if (G.dist(x, y, tmpPoint[0], tmpPoint[1]) < 46f + r) return true;
                }
            } else {
                float br = v.blockRadius();
                if (Math.abs(v.x - x) < br + r && Math.abs(v.y - y) < br + r) return true;
            }
        }
        return false;
    }

    public void addSmoke(float x, float y, float size) {
        smoke.add(new float[]{
            x, y,
            (rnd.nextFloat() - 0.5f) * 24f, -26f - rnd.nextFloat() * 22f,
            2.0f, 2.0f, size
        });
        if (smoke.size() > 90) smoke.remove(0);
    }

    public Building buildingByType(int type) {
        for (Building b : buildings) {
            if (b.type == type) return b;
        }
        return null;
    }

    /**
     * نزدیک‌ترین ساختمانی که درِاش نزدیک بازیکن است
     */
    public Building buildingNearDoor(float x, float y, float maxDist) {
        Building best = null;
        float bestD = maxDist;
        for (Building b : buildings) {
            float d = G.dist(x, y, b.doorX, b.doorY + 34f);
            if (d < bestD) {
                bestD = d;
                best = b;
            }
        }
        return best;
    }

    public float[] randomWalkableNear(float x, float y, float range, boolean onRoad) {
        for (int i = 0; i < 24; i++) {
            float px = G.clamp(x + (rnd.nextFloat() - 0.5f) * 2f * range, 80f, G.WORLD_W - 80f);
            float py = G.clamp(y + (rnd.nextFloat() - 0.5f) * 2f * range, 80f, G.WORLD_H - 80f);
            int t = tileAt(px, py);
            boolean ok = onRoad ? (t == T_ROAD) : (t == T_GRASS || t == T_SIDEWALK || t == T_PARK || t == T_PATH);
            if (ok && !isBlocked(px, py, 16f)) {
                return new float[]{px, py};
            }
        }
        return null;
    }

    public int tileAt(float x, float y) {
        int tx = (int) (x / G.TILE), ty = (int) (y / G.TILE);
        if (tx < 0 || ty < 0 || tx >= G.MAP_W || ty >= G.MAP_H) return T_BUILDING;
        return tileType[ty][tx];
    }

    /**
     * آیا نقطه‌ای روی آب است؟ (برای قایق)
     */
    public boolean isWaterAt(float x, float y) {
        return tileAt(x, y) == T_WATER;
    }

    /**
     * برخورد هنگام شنا — فقط ساختمان‌ها مانع‌اند (آب آزاد است)
     */
    public boolean isBlockedForSwim(float x, float y) {
        if (x < 60f || y < 60f || x >= G.WORLD_W - 60f || y >= G.WORLD_H - 60f) return true;
        return tileAt(x, y) == T_BUILDING;
    }

    /**
     * جستجوی نزدیک‌ترین نقطه قابل پیاده‌روی (برای پیاده شدن از قایق)
     */
    public float[] findWalkableNear(float x, float y) {
        for (float r = 60f; r <= 280f; r += 55f) {
            for (int a = 0; a < 12; a++) {
                float ang = (float) (a * Math.PI / 6.0);
                float px = x + (float) Math.cos(ang) * r;
                float py = y + (float) Math.sin(ang) * r;
                if (!isBlocked(px, py, 16f)) return new float[]{px, py};
            }
        }
        return null;
    }

    /**
     * اسپاون تصادفی خودرو در یکی از لاین‌های ترافیک (دور از بازیکن)
     */
    public void randomLaneSpawn(Vehicle v) {
        if (trafficLanes.isEmpty()) return;
        float[] lane = trafficLanes.get(rnd.nextInt(trafficLanes.size()));
        v.axis = lane[0] > 0.5f ? 'V' : 'H';
        v.laneDir = rnd.nextBoolean() ? 1f : -1f;
        v.laneMin = lane[2];
        v.laneMax = lane[3];
        float along = lane[2] + rnd.nextFloat() * (lane[3] - lane[2]);
        if (v.axis == 'H') {
            v.y = lane[1];
            v.x = along;
        } else {
            v.x = lane[1];
            v.y = along;
        }
    }

    // ================= ورود و خروج محیط =================

    public void enterInterior(Building b, Player p) {
        interior = Interior.createFor(b);
        if (interior == null) return;
        p.x = interior.doorX;
        p.y = interior.roomH - 64f;   // کمی بالاتر از در تا خودکار خارج نشود
        p.driving = null;
        p.ridingTrain = false;
        p.swimming = false;
        interiorNpcs.clear();
        // چند شهروند داخل مغازه‌ها
        if (interior.floorType.equals("restaurant") || interior.floorType.equals("market") || interior.floorType.equals("cafe")) {
            Npc.scatter(interiorNpcs, this, 3, interior.roomW / 2f, interior.roomH / 2f, interior.roomW);
        }
        // ✅ مدرسه: معلم + چند دانش‌آموز با کوله‌پشتی داخل سالن (درخواست کاربر)
        if (interior.floorType.equals("school")) {
            Npc teacher = new Npc(interior.roomW / 2f, 150f, 0);
            teacher.dir = 0;
            interiorNpcs.add(teacher);
            // بچه‌ها پشت نیمکت‌ها (۴ نیمکت)
            for (int i = 0; i < 4; i++) {
                Npc kid = Npc.schoolKid(135f + i * 160f, 300f);
                kid.dir = 2;
                interiorNpcs.add(kid);
            }
            // یکی هم قدم می‌زند
            interiorNpcs.add(Npc.schoolKid(interior.roomW * 0.5f, interior.roomH * 0.72f));
        }
        SoundManager.play("door");
    }

    public void exitInterior(Player p) {
        if (interior == null) return;
        Building b = interior.building;
        interior = null;
        interiorNpcs.clear();
        if (b != null) {
            p.x = b.doorX;
            p.y = b.doorY + 40f;
        } else {
            p.x = spawnX;
            p.y = spawnY;
        }
        SoundManager.play("door");
    }

    // ================= به‌روزرسانی =================

    public void update(float dt, Player p) {
        dayNight.update(dt);

        // ✈ هواپیمای فرودگاه
        updatePlane(dt);

        // دود
        for (int i = smoke.size() - 1; i >= 0; i--) {
            float[] s = smoke.get(i);
            s[0] += s[2] * dt;
            s[1] += s[3] * dt;
            s[4] -= dt;
            s[6] += dt * 6f;
            if (s[4] <= 0f) smoke.remove(i);
        }

        if (interior != null) {
            for (int i = 0; i < interiorNpcs.size(); i++) {
                interiorNpcs.get(i).update(dt, this);
            }
            return;   // داخل ساختمان ترافیک به‌روز نمی‌شود
        }

        // شهروندها
        for (int i = 0; i < cityNpcs.size(); i++) {
            cityNpcs.get(i).update(dt, this);
        }

        // خودروها و قطار
        for (int i = 0; i < vehicles.size(); i++) {
            vehicles.get(i).update(dt, this);
        }

        // تصادف خودروها با هم: دود + محو ۳ ثانیه + ظاهر شدن جای دیگر
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle a = vehicles.get(i);
            if (a.mode != Vehicle.MODE_TRAFFIC || !a.isActive()) continue;
            for (int j = i + 1; j < vehicles.size(); j++) {
                Vehicle b = vehicles.get(j);
                if (b.mode != Vehicle.MODE_TRAFFIC || !b.isActive()) continue;
                if (Math.abs(a.x - b.x) < 54f && Math.abs(a.y - b.y) < 54f) {
                    float mx = (a.x + b.x) / 2f, my = (a.y + b.y) / 2f;
                    for (int k = 0; k < 6; k++) {
                        addSmoke(mx + (rnd.nextFloat() - 0.5f) * 40f, my + (rnd.nextFloat() - 0.5f) * 30f, 12f);
                    }
                    SoundManager.play("crash");
                    a.deadTimer = 3f;
                    b.deadTimer = 3f;
                }
            }
        }
    }

    // ================= رسم شهر =================

    public void draw(Canvas c, SpriteLib sprites, float cx, float cy, float vw, float vh) {
        int tx0 = Math.max(0, (int) ((cx - vw / 2f) / G.TILE) - 1);
        int ty0 = Math.max(0, (int) ((cy - vh / 2f) / G.TILE) - 1);
        int tx1 = Math.min(G.MAP_W - 1, (int) ((cx + vw / 2f) / G.TILE) + 1);
        int ty1 = Math.min(G.MAP_H - 1, (int) ((cy + vh / 2f) / G.TILE) + 1);

        // تایل‌ها
        for (int ty = ty0; ty <= ty1; ty++) {
            for (int tx = tx0; tx <= tx1; tx++) {
                drawTile(c, tx, ty, tileType[ty][tx]);
            }
        }

        // ریل قطار (هر دو حلقه)
        if (railPath != null) {
            sprites.drawRails(c, railPath);
        }
        if (railPath2 != null) {
            sprites.drawRails(c, railPath2);
        }

        // درخت‌ها
        for (int i = 0; i < trees.size(); i++) {
            float[] t = trees.get(i);
            if (t[0] < tx0 * G.TILE - 80f || t[0] > (tx1 + 1) * G.TILE + 80f) continue;
            if (t[1] < ty0 * G.TILE - 80f || t[1] > (ty1 + 1) * G.TILE + 80f) continue;
            sprites.drawTree(c, t[0], t[1], t[2]);
        }

        // فواره و آبشار پارک (آبشار داخل آب)
        if (fountainX > 0f) {
            sprites.drawFountain(c, fountainX, fountainY, dayNight.minutes);
        }
        if (waterfallX > 0f) {
            sprites.drawWaterfall(c, waterfallX, waterfallY, dayNight.minutes);
        }

        // ماهی‌های دریاچه (زیر قایق، روی آب)
        for (int i = 0; i < fishSpots.size(); i++) {
            float[] f = fishSpots.get(i);
            sprites.drawFish(c, f[0], f[1], (int) f[2], dayNight.minutes + i * 1.7f);
        }

        // ساختمان‌ها
        for (int i = 0; i < buildings.size(); i++) {
            Building b = buildings.get(i);
            if (b.x > (tx1 + 1) * G.TILE + 60f || b.x + b.w < tx0 * G.TILE - 60f) continue;
            if (b.y > (ty1 + 1) * G.TILE + 60f || b.y + b.h < ty0 * G.TILE - 60f) continue;
            sprites.drawBuilding(c, b, dayNight.minutes);
        }

        // شهروندها
        for (int i = 0; i < cityNpcs.size(); i++) {
            Npc n = cityNpcs.get(i);
            sprites.drawPerson(c, n.x, n.y, n.dir, n.anim,
                    n.shirt, n.pants, n.skin, 0xFF3E2723, n.hairStyle, -1, false, n.gender);
            if (n.backpack) sprites.drawBackpack(c, n.x, n.y, n.dir, n.backpackColor);
        }

        // خودروها (قطار با واگن‌هایش)
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle v = vehicles.get(i);
            if (!v.isActive()) continue;
            if (v.type == Vehicle.CAR_TRAIN) {
                sprites.drawTrain(c, v, railPath, dayNight.minutes);
            } else {
                sprites.drawVehicle(c, v, dayNight.minutes);
            }
        }

        // حباب حرف NPCها
        sprites.drawBubbles(c, cityNpcs);

        // چرخ‌وفلک و سرسیر شهربازی
        if (wheelX > 0f) {
            sprites.drawFerrisWheel(c, wheelX, wheelY, dayNight.minutes);
        }
        if (carouselX > 0f) {
            sprites.drawCarousel(c, carouselX, carouselY, dayNight.minutes);
        }

        // حیوانات روستا (گاو و گوسفند)
        for (int i = 0; i < villageAnimals.size(); i++) {
            float[] a = villageAnimals.get(i);
            sprites.drawFarmAnimal(c, a[0], a[1], (int) a[2], dayNight.minutes + i * 2.1f);
        }

        // 🚌 ایستگاه‌های اتوبوس
        for (int i = 0; i < busStops.size(); i++) {
            float[] bs = busStops.get(i);
            if (bs[0] < tx0 * G.TILE - 120f || bs[0] > (tx1 + 1) * G.TILE + 120f) continue;
            if (bs[1] < ty0 * G.TILE - 120f || bs[1] > (ty1 + 1) * G.TILE + 120f) continue;
            sprites.drawBusStop(c, bs[0], bs[1],
                    i < busStopNames.size() ? busStopNames.get(i) : "ایستگاه", dayNight.minutes);
        }

        // ✈ هواپیمای فرودگاه
        if (planeX > 0f) {
            sprites.drawPlane(c, planeX, planeY - planeAlt * 0.8f, planeAngle, planeAlt, dayNight.minutes);
        }

        // دود
        drawSmoke(c);
    }

    private void drawTile(Canvas c, int tx, int ty, int t) {
        float x = tx * G.TILE, y = ty * G.TILE;
        switch (t) {
            case T_ROAD:
                paint.setColor(G.COL_ROAD);
                c.drawRect(x, y, x + G.TILE, y + G.TILE, paint);
                break;
            case T_WATER:
                paint.setColor(G.COL_WATER);
                c.drawRect(x, y, x + G.TILE, y + G.TILE, paint);
                paint.setColor(0x66FFFFFF);
                float wave = (float) Math.sin((tx + ty) * 1.3f + dayNight.minutes * 0.5f) * 3f;
                c.drawRect(x + 8f, y + 24f + wave, x + 30f, y + 28f + wave, paint);
                c.drawRect(x + 34f, y + 42f - wave, x + 56f, y + 46f - wave, paint);
                break;
            case T_PARK:
                paint.setColor(0xFF5DAE45);
                c.drawRect(x, y, x + G.TILE, y + G.TILE, paint);
                break;
            case T_SIDEWALK:
                paint.setColor(G.COL_SIDEWALK);
                c.drawRect(x, y, x + G.TILE, y + G.TILE, paint);
                paint.setColor(0x33000000);
                c.drawLine(x, y + G.TILE / 2f, x + G.TILE, y + G.TILE / 2f, paint);
                c.drawLine(x + G.TILE / 2f, y, x + G.TILE / 2f, y + G.TILE, paint);
                break;
            case T_PATH:
                paint.setColor(G.COL_PATH);
                c.drawRect(x, y, x + G.TILE, y + G.TILE, paint);
                paint.setColor(0x22000000);
                c.drawCircle(x + G.TILE * 0.3f, y + G.TILE * 0.6f, 2.5f, paint);
                c.drawCircle(x + G.TILE * 0.7f, y + G.TILE * 0.3f, 2f, paint);
                break;
            case T_DIRT:
                // جاده خاکی روستا
                paint.setColor(0xFFC8A66B);
                c.drawRect(x, y, x + G.TILE, y + G.TILE, paint);
                paint.setColor(0x33000000);
                c.drawCircle(x + G.TILE * 0.25f, y + G.TILE * 0.35f, 2.5f, paint);
                c.drawCircle(x + G.TILE * 0.65f, y + G.TILE * 0.7f, 3f, paint);
                c.drawCircle(x + G.TILE * 0.8f, y + G.TILE * 0.2f, 2f, paint);
                break;
            case T_RUNWAY:
                // باند فرودگاه و پیش‌باند
                paint.setColor(0xFF4A5058);
                c.drawRect(x, y, x + G.TILE, y + G.TILE, paint);
                paint.setColor(0x88FFFFFF);
                c.drawRect(x + G.TILE * 0.4f, y + G.TILE * 0.44f, x + G.TILE * 0.6f, y + G.TILE * 0.56f, paint);
                break;
            case T_BUILDING:
                paint.setColor(0xFF90A4AE);
                c.drawRect(x, y, x + G.TILE, y + G.TILE, paint);
                break;
            default:
                paint.setColor((tx + ty) % 2 == 0 ? G.COL_GRASS : G.COL_GRASS2);
                c.drawRect(x, y, x + G.TILE, y + G.TILE, paint);
                break;
        }

        // خطوط جاده زیباتر: خط‌چین وسط + خط عابر + ا方面
        if (t == T_ROAD) {
            boolean hRoad = tx > 0 && tileType[ty][tx - 1] == T_ROAD && tx < G.MAP_W - 1 && tileType[ty][tx + 1] == T_ROAD;
            boolean vRoad = ty > 0 && tileType[ty - 1][tx] == T_ROAD && ty < G.MAP_H - 1 && tileType[ty + 1][tx] == T_ROAD;
            paint.setColor(G.COL_ROAD_LINE);
            paint.setStrokeWidth(2.5f);
            if (hRoad && !vRoad) {
                float dash = (tx % 2 == 0) ? G.TILE * 0.4f : 0f;
                if (dash > 0f) c.drawLine(x + G.TILE * 0.1f, y + G.TILE / 2f, x + G.TILE * 0.1f + dash, y + G.TILE / 2f, paint);
            } else if (vRoad && !hRoad) {
                float dash = (ty % 2 == 0) ? G.TILE * 0.4f : 0f;
                if (dash > 0f) c.drawLine(x + G.TILE / 2f, y + G.TILE * 0.1f, x + G.TILE / 2f, y + G.TILE * 0.1f + dash, paint);
            }
            // چاهک ملایم
            if ((tx * 7 + ty * 13) % 29 == 0) {
                paint.setColor(0x44000000);
                c.drawCircle(x + G.TILE * 0.5f, y + G.TILE * 0.5f, 5f, paint);
            }
        }
    }

    private void drawSmoke(Canvas c) {
        for (int i = 0; i < smoke.size(); i++) {
            float[] s = smoke.get(i);
            float a = Math.max(0f, s[4] / s[5]);
            int alpha = (int) (a * 150f);
            float size = s[6] * (2f - a);
            paint.setColor((alpha << 24) | 0x5A5A5A);
            c.drawCircle(s[0], s[1], size, paint);
            paint.setColor(((alpha / 2) << 24) | 0x8A8A8A);
            c.drawCircle(s[0], s[1], size * 0.6f, paint);
        }
    }

    // ================= هواپیمای خودکار فرودگاه ✈ =================

    /**
     * چرخه کامل هواپیما: ترمینال ← تاکسی ← برخاست ← گشت دور نقشه ← فرود ← ترمینال
     */
    private void updatePlane(float dt) {
        planeTimer -= dt;
        switch (planeState) {
            case 0:   // پارک در ترمینال
                if (planeTimer <= 0f) planeState = 1;
                break;

            case 1: {   // تاکسی به ابتدای باند
                float tx = 182f * G.TILE, ty = 78.5f * G.TILE;
                float d = G.dist(planeX, planeY, tx, ty);
                if (d < 14f) {
                    planeState = 2;
                    planeSpeed = 0f;
                    planeAngle = 0f;
                } else {
                    planeAngle = (float) Math.atan2(ty - planeY, tx - planeX);
                    planeX += (float) Math.cos(planeAngle) * 85f * dt;
                    planeY += (float) Math.sin(planeAngle) * 85f * dt;
                }
                break;
            }

            case 2:   // شتاب روی باند و برخاست
                planeSpeed = Math.min(planeSpeed + 160f * dt, 330f);
                planeX += planeSpeed * dt;
                planeAngle = 0f;
                if (planeX > 214f * G.TILE) {
                    planeState = 3;
                    planeCircStart = -(float) Math.PI / 2f;
                    planeCircAng = planeCircStart;
                    SoundManager.play("jet");
                }
                break;

            case 3: {   // پرواز دایره‌ای دور منطقه شرقی
                planeAlt = Math.min(planeAlt + 70f * dt, 240f);
                planeCircAng += 0.2f * dt;
                planeX = 216f * G.TILE + (float) Math.cos(planeCircAng) * 40f * G.TILE;
                planeY = 96f * G.TILE + (float) Math.sin(planeCircAng) * 29f * G.TILE;
                planeAngle = (float) Math.atan2(
                        (float) Math.cos(planeCircAng) * 29f,
                        -(float) Math.sin(planeCircAng) * 40f);
                if (planeCircAng >= planeCircStart + (float) (Math.PI * 2.0)) {
                    planeState = 4;
                }
                break;
            }

            case 4: {   // نزدیک شدن برای فرود (به انتهای شرقی باند)
                planeAlt = Math.max(planeAlt - 55f * dt, 30f);
                float tx = 208f * G.TILE, ty = 78.5f * G.TILE;
                float d = G.dist(planeX, planeY, tx, ty);
                planeAngle = (float) Math.atan2(ty - planeY, tx - planeX);
                planeX += (float) Math.cos(planeAngle) * 290f * dt;
                planeY += (float) Math.sin(planeAngle) * 290f * dt;
                if (d < 90f) {
                    planeState = 5;
                    planeSpeed = 330f;
                    planeAngle = (float) Math.PI;   // فرود در جهت غرب
                    SoundManager.play("jet");
                }
                break;
            }

            case 5:   // نشستن روی باند و ترمز
                planeAlt = Math.max(planeAlt - 130f * dt, 0f);
                planeSpeed = Math.max(planeSpeed - 80f * dt, 95f);
                planeX -= planeSpeed * dt;
                planeAngle = (float) Math.PI;
                if (planeX < 186f * G.TILE) {
                    planeState = 6;
                }
                break;

            case 6: {   // تاکسی به ترمینال
                planeAlt = 0f;
                float tx = 188f * G.TILE, ty = 71.5f * G.TILE;
                float d = G.dist(planeX, planeY, tx, ty);
                if (d < 14f) {
                    planeState = 0;
                    planeTimer = 8f;
                } else {
                    planeAngle = (float) Math.atan2(ty - planeY, tx - planeX);
                    planeX += (float) Math.cos(planeAngle) * 85f * dt;
                    planeY += (float) Math.sin(planeAngle) * 85f * dt;
                }
                break;
            }
        }
    }

    // ================= رسم محیط داخلی =================

    public void drawInterior(Canvas c, SpriteLib sprites) {
        // دیوار اطراف اتاق (به‌جای آسمان آبی!)
        paint.setColor(interior.floorType.equals("zoo") ? 0xFFA5D6A7 : 0xFF6D4C41);
        c.drawRect(-3000f, -3000f, interior.roomW + 3000f, interior.roomH + 3000f, paint);

        // کف
        int floorColor;
        switch (interior.floorType) {
            case "restaurant": floorColor = 0xFFF5E1C8; break;
            case "zoo":        floorColor = 0xFFC8E6C9; break;
            case "cinema":     floorColor = 0xFF37474F; break;
            case "home":       floorColor = 0xFFFFE0B2; break;
            default:           floorColor = 0xFFE8EEF2; break;
        }
        paint.setColor(floorColor);
        c.drawRect(0, 0, interior.roomW, interior.roomH, paint);

        // خطوط کاشی
        paint.setColor(0x22000000);
        paint.setStrokeWidth(1.5f);
        for (float x = 0; x <= interior.roomW; x += G.TILE) {
            c.drawLine(x, 0, x, interior.roomH, paint);
        }
        for (float y = 0; y <= interior.roomH; y += G.TILE) {
            c.drawLine(0, y, interior.roomW, y, paint);
        }

        // دیوار پایین و در خروج
        paint.setColor(0xFF6D4C41);
        c.drawRect(0, interior.roomH - 14f, interior.roomW, interior.roomH, paint);
        paint.setColor(0xFF4E342A);
        c.drawRect(interior.doorX - G.TILE * 0.7f, interior.roomH - 14f,
                interior.doorX + G.TILE * 0.7f, interior.roomH, paint);
        paint.setColor(0xFFFFF59D);
        c.drawRect(interior.doorX - G.TILE * 0.5f, interior.roomH - 20f,
                interior.doorX + G.TILE * 0.5f, interior.roomH - 14f, paint);

        // حیوانات باغ‌وحش
        if (interior.floorType.equals("zoo")) {
            for (int i = 0; i < interior.animalTypes.size(); i++) {
                float[] ap = interior.animalPos.get(i);
                sprites.drawAnimal(c, ap[0], ap[1], interior.animalTypes.get(i), dayNight.minutes);
            }
        }

        // مبلمان
        for (int i = 0; i < interior.furniture.size(); i++) {
            float[] f = interior.furniture.get(i);
            int[] st = interior.furnitureStyle.get(i);
            if (st[1] == 9) {
                // پرده سینما: قاب تیره + صفحه روشن
                paint.setColor(0xFF212121);
                c.drawRoundRect(f[0] - 8f, f[1] - 8f, f[0] + f[2] + 8f, f[1] + f[3] + 8f, 10f, 10f, paint);
                paint.setColor(0xFFF5F5F5);
                c.drawRoundRect(f[0], f[1], f[0] + f[2], f[1] + f[3], 6f, 6f, paint);
                paint.setColor(0x33000000);
                c.drawRoundRect(f[0], f[1], f[0] + f[2], f[1] + f[3] * 0.25f, 6f, 6f, paint);
            } else {
                sprites.drawFurniture(c, f[0], f[1], f[2], f[3], st[0], st[1], dayNight.minutes);
            }
        }

        // صندلی‌های سینما
        if (interior.floorType.equals("cinema")) {
            for (int i = 0; i < interior.seatSpots.size(); i++) {
                float[] s = interior.seatSpots.get(i);
                sprites.drawCinemaSeat(c, s[0], s[1]);
            }
        }

        // شهروندهای داخل
        for (int i = 0; i < interiorNpcs.size(); i++) {
            Npc n = interiorNpcs.get(i);
            sprites.drawPerson(c, n.x, n.y, n.dir, n.anim,
                    n.shirt, n.pants, n.skin, 0xFF3E2723, n.hairStyle, -1, false, n.gender);
            if (n.backpack) sprites.drawBackpack(c, n.x, n.y, n.dir, n.backpackColor);
        }

        sprites.drawBubbles(c, interiorNpcs);
    }

    // ================= ذخیره و بارگذاری =================

    public void writeTime(JSONObject o) {
        try {
            o.put("time", dayNight.minutes);
        } catch (Exception e) {
        }
    }

    public void readTime(JSONObject o) {
        try {
            dayNight.minutes = (float) o.optDouble("time", 8f * 60f);
        } catch (Exception e) {
        }
    }
}
