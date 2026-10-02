package com.persiancity.game.game;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.Random;

/**
 * دنیای بازی — شهر بزرگ + محیط داخلی مغازه‌ها
 */
public class World {

    // انواع تایل
    public static final int T_BUILDING = 0;
    public static final int T_ROAD = 1;
    public static final int T_GRASS = 2;
    public static final int T_PARK = 3;
    public static final int T_WATER = 4;
    public static final int T_SIDEWALK = 5;
    public static final int T_PATH = 6;

    public int[][] tileType = new int[G.MAP_H][G.MAP_W];
    public int[][] roadAxis = new int[G.MAP_H][G.MAP_W]; // 0=هیچ 1=افقی 2=عمودی 3=چهارراه
    public boolean[][] solid = new boolean[G.MAP_H][G.MAP_W];

    public final ArrayList<Building> buildings = new ArrayList<>();
    public final ArrayList<float[]> trees = new ArrayList<>();          // x, y, size
    public final ArrayList<float[]> streetlights = new ArrayList<>();   // x, y
    public final ArrayList<float[]> parkingSpots = new ArrayList<>();   // x, y, angleDeg
    public final ArrayList<float[]> grassTufts = new ArrayList<>();     // تزئین چمن

    public final ArrayList<Npc> cityNpcs = new ArrayList<>();
    public final ArrayList<Npc> interiorNpcs = new ArrayList<>();
    public final ArrayList<Vehicle> vehicles = new ArrayList<>();

    public Interior interior = null;   // اگر مخالف null باشد داخل مغازه‌ایم
    public DayNight dayNight = new DayNight();
    public float fountainX, fountainY;

    private final Paint paint = new Paint();
    private final Random rnd = new Random();
    public CityBuilder builder;

    public World() {
        builder = new CityBuilder(this);
        builder.build();
    }

    // ---------------- برخورد و مسیر ----------------

    public boolean collides(float x, float y, float r) {
        if (interior != null) {
            return interior.collides(x, y, r);
        }
        if (x - r < 0 || y - r < 0 || x + r >= G.WORLD_W || y + r >= G.WORLD_H) return true;
        int x0 = (int) ((x - r) / G.TILE), x1 = (int) ((x + r) / G.TILE);
        int y0 = (int) ((y - r) / G.TILE), y1 = (int) ((y + r) / G.TILE);
        for (int ty = y0; ty <= y1; ty++) {
            for (int tx = x0; tx <= x1; tx++) {
                if (solid[ty][tx]) return true;
            }
        }
        return false;
    }

    public boolean isRoadPoint(float x, float y) {
        int tx = (int) (x / G.TILE), ty = (int) (y / G.TILE);
        if (tx < 0 || ty < 0 || tx >= G.MAP_W || ty >= G.MAP_H) return false;
        return tileType[ty][tx] == T_ROAD;
    }

    public boolean walkableTile(int tx, int ty, boolean allowRoad) {
        if (tx < 0 || ty < 0 || tx >= G.MAP_W || ty >= G.MAP_H) return false;
        if (solid[ty][tx]) return false;
        int t = tileType[ty][tx];
        if (t == T_ROAD) return allowRoad;
        return t == T_GRASS || t == T_PARK || t == T_SIDEWALK || t == T_PATH;
    }

    /**
     * نقطه قابل راه‌رفن تصادفی نزدیک (برای NPCها)
     */
    public float[] randomWalkableNear(float x, float y, float maxDist, boolean allowRoad) {
        for (int i = 0; i < 24; i++) {
            float a = rnd.nextFloat() * (float) (Math.PI * 2);
            float d = 60f + rnd.nextFloat() * maxDist;
            float nx = x + (float) Math.cos(a) * d;
            float ny = y + (float) Math.sin(a) * d;
            int tx = (int) (nx / G.TILE), ty = (int) (ny / G.TILE);
            if (walkableTile(tx, ty, allowRoad)) {
                return new float[]{tx * G.TILE + G.TILE / 2f, ty * G.TILE + G.TILE / 2f};
            }
        }
        return null;
    }

    /**
     * نزدیک‌ترین جای پارک به یک نقطه
     */
    public float[] nearestParking(float x, float y) {
        float[] best = null;
        float bestD = Float.MAX_VALUE;
        for (float[] p : parkingSpots) {
            float d = G.dist(x, y, p[0], p[1]);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    /**
     * ساختمانی که جلوی درش ایستاده‌ایم
     */
    public Building buildingNearDoor(float x, float y) {
        for (Building b : buildings) {
            if (G.dist(x, y, b.doorX, b.doorY) < G.TILE * 1.4f) return b;
        }
        return null;
    }

    public Building buildingByType(int type) {
        for (Building b : buildings) {
            if (b.type == type) return b;
        }
        return null;
    }

    // ---------------- ورود و خروج مغازه ----------------

    public void enterInterior(Building b, Player p) {
        if (b.type == Building.RESTAURANT) {
            interior = Interior.makeRestaurant(b);
            Npc manager = new Npc(interior.counterX, interior.counterY + G.TILE * 1.2f, "آقای زنجبیل",
                    Npc.ROLE_RESTAURANT_MANAGER, rnd);
            manager.outfitColor = 0xFF8D6E63;
            interiorNpcs.add(manager);
        } else if (b.type == Building.MARKET) {
            interior = Interior.makeMarket(b);
            Npc manager = new Npc(interior.counterX, interior.counterY + G.TILE * 1.1f, "خانم فراوان",
                    Npc.ROLE_MARKET_MANAGER, rnd);
            manager.outfitColor = 0xFF26A69A;
            interiorNpcs.add(manager);
        } else {
            return;
        }
        p.x = interior.doorX;
        p.y = interior.doorY - G.TILE * 1.2f;
    }

    public void exitInterior(Player p) {
        if (interior == null) return;
        Building b = interior.from;
        interior = null;
        interiorNpcs.clear();
        p.x = b.doorX;
        p.y = b.doorY;
    }

    // ---------------- به‌روزرسانی ----------------

    public void update(float dt, Player player) {
        dayNight.update(dt);

        if (interior != null) {
            for (Npc n : interiorNpcs) n.update(dt, this);
        } else {
            for (Npc n : cityNpcs) n.update(dt, this);
            for (Vehicle v : vehicles) {
                if (v.mode == Vehicle.MODE_TRAFFIC) {
                    Entity playerEnt = player.driving != null ? player.driving : player;
                    v.trafficUpdate(dt, this, playerEnt);
                }
            }
        }
    }

    // ---------------- ترسیم ----------------

    public void draw(Canvas c, SpriteLib sprites, Camera cam, float viewW, float viewH) {
        if (interior != null) {
            drawInterior(c, sprites);
            drawEntities(c, sprites, false);
            return;
        }

        float s = cam.scale;
        float halfW = viewW / (2f * s);
        float halfH = viewH / (2f * s);
        int tx0 = Math.max(0, (int) ((cam.x - halfW) / G.TILE) - 1);
        int tx1 = Math.min(G.MAP_W - 1, (int) ((cam.x + halfW) / G.TILE) + 1);
        int ty0 = Math.max(0, (int) ((cam.y - halfH) / G.TILE) - 1);
        int ty1 = Math.min(G.MAP_H - 1, (int) ((cam.y + halfH) / G.TILE) + 1);

        // زمین
        for (int ty = ty0; ty <= ty1; ty++) {
            for (int tx = tx0; tx <= tx1; tx++) {
                drawTile(c, tx, ty);
            }
        }

        // جاده: خط‌چین وسط
        paint.setColor(G.COL_ROAD_LINE);
        paint.setStrokeWidth(4f);
        for (int ty = ty0; ty <= ty1; ty++) {
            for (int tx = tx0; tx <= tx1; tx++) {
                if (tileType[ty][tx] != T_ROAD) continue;
                int ax = roadAxis[ty][tx];
                float px = tx * G.TILE, py = ty * G.TILE;
                if ((ax & 1) != 0 && (tx % 2 == 0)) {
                    c.drawLine(px, py + G.TILE / 2f, px + G.TILE, py + G.TILE / 2f, paint);
                }
                if ((ax & 2) != 0 && (ty % 2 == 0)) {
                    c.drawLine(px + G.TILE / 2f, py, px + G.TILE / 2f, py + G.TILE, paint);
                }
            }
        }

        // ساختمان‌ها
        for (Building b : buildings) {
            if (b.px > (tx1 + 1) * G.TILE || b.px + b.pw < tx0 * G.TILE) continue;
            if (b.py > (ty1 + 1) * G.TILE || b.py + b.ph < ty0 * G.TILE) continue;
            sprites.drawBuilding(c, b);
        }

        // چراغ‌های خیابان
        for (float[] l : streetlights) {
            if (l[0] < tx0 * G.TILE - 60f || l[0] > (tx1 + 1) * G.TILE + 60f) continue;
            if (l[1] < ty0 * G.TILE - 60f || l[1] > (ty1 + 1) * G.TILE + 60f) continue;
            sprites.drawStreetlight(c, l[0], l[1]);
        }

        drawEntities(c, sprites, true);

        // فواره پارک
        drawFountain(c, sprites);
    }

    private void drawTile(Canvas c, int tx, int ty) {
        int t = tileType[ty][tx];
        float px = tx * G.TILE, py = ty * G.TILE;
        switch (t) {
            case T_ROAD:
                paint.setColor(G.COL_ROAD);
                c.drawRect(px, py, px + G.TILE, py + G.TILE, paint);
                break;
            case T_SIDEWALK:
                paint.setColor(G.COL_SIDEWALK);
                c.drawRect(px, py, px + G.TILE, py + G.TILE, paint);
                paint.setColor(0xFFB0B7BF);
                paint.setStrokeWidth(2f);
                c.drawLine(px, py + G.TILE, px + G.TILE, py + G.TILE, paint);
                break;
            case T_PARK:
                paint.setColor(0xFF8BC34A);
                c.drawRect(px, py, px + G.TILE, py + G.TILE, paint);
                break;
            case T_PATH:
                paint.setColor(G.COL_PATH);
                c.drawRect(px, py, px + G.TILE, py + G.TILE, paint);
                break;
            case T_WATER:
                paint.setColor(G.COL_WATER);
                c.drawRect(px, py, px + G.TILE, py + G.TILE, paint);
                break;
            default: // چمن و زیر ساختمان
                paint.setColor(((tx + ty) % 2 == 0) ? G.COL_GRASS : G.COL_GRASS2);
                c.drawRect(px, py, px + G.TILE, py + G.TILE, paint);
                break;
        }
    }

    private void drawEntities(Canvas c, SpriteLib sprites, boolean city) {
        sprites.nightMode = dayNight.isNight();

        if (city) {
            // وسایل نقلیه
            for (Vehicle v : vehicles) v.draw(c, sprites);
        }

        // NPCها
        if (city) {
            for (Npc n : cityNpcs) {
                sprites.drawPerson(c, n.x, n.y, n.facing, n.animTime,
                        n.outfitColor, n.pantsColor, n.skinColor, n.hairColor, n.hairStyle,
                        -1, false);
            }
        } else {
            for (Npc n : interiorNpcs) {
                sprites.drawPerson(c, n.x, n.y, n.facing, n.animTime,
                        n.outfitColor, n.pantsColor, n.skinColor, n.hairColor, n.hairStyle,
                        -1, false);
            }
        }

        // درخت‌ها (روی همه چیز برای عمق صحنه)
        if (city) {
            for (float[] t : trees) {
                sprites.drawTree(c, t[0], t[1], t[2]);
            }
        }

        // حباب حرف NPCها
        sprites.drawBubbles(c, city ? cityNpcs : interiorNpcs);
    }

    private void drawFountain(Canvas c, SpriteLib sprites) {
        if (tileType[33][50] != T_PATH) return; // فقط اگر پارک هست
        sprites.drawFountain(c, fountainX, fountainY, dayNight.minutes);
    }

    public void drawInterior(Canvas c, SpriteLib sprites) {
        // کف
        int floorColor = interior.floorType.equals("restaurant") ? 0xFFF5E1C8 : 0xFFE8EEF2;
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

        // مبلمان
        for (int i = 0; i < interior.furniture.size(); i++) {
            float[] f = interior.furniture.get(i);
            int[] st = interior.furnitureStyle.get(i);
            paint.setColor(st[0]);
            c.drawRoundRect(f[0], f[1], f[0] + f[2], f[1] + f[3], 10f, 10f, paint);
            paint.setColor(0x33000000);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f);
            c.drawRoundRect(f[0], f[1], f[0] + f[2], f[1] + f[3], 10f, 10f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }
}
