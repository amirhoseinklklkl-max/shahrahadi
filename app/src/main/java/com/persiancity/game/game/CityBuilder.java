package com.persiancity.game.game;

import java.util.Random;

/**
 * سازنده شهر بزرگ «شهر شادی»
 * کمربندی قابل رفت‌وآمد + خیابان‌های داخلی + پارک با آبشار + ریل دور شهر
 */
public final class CityBuilder {

    private CityBuilder() {}

    private static final Random rnd = new Random();

    public static void build(World w) {
        // ۱) همه چمن
        for (int y = 0; y < G.MAP_H; y++) {
            for (int x = 0; x < G.MAP_W; x++) {
                w.tileType[y][x] = World.T_GRASS;
            }
        }

        // ۲) کمربندی دور شهر (دو لاین — قابل رفت‌وآمد)
        ring(w, 4, 135);

        // ۳) خیابان‌های داخلی (افقی و عمودی)
        streetH(w, 34, 6, 133);
        streetH(w, 66, 6, 133);
        streetV(w, 34, 6, 93);
        streetV(w, 69, 6, 93);
        streetV(w, 104, 6, 93);

        // ۴) پارک بزرگ با حوض و آبشار
        buildPark(w, 11, 38, 19, 24);

        // ۵) ساختمان‌ها
        buildBuildings(w);

        // ۶) پیاده‌روها (حاشیه جاده‌ها)
        sidewalks(w);

        // ۷) درخت‌ها
        scatterTrees(w);

        // ۸) ریل دور شهر + ایستگاه + قطار
        buildRail(w);

        // ۹) ترافیک و شهروندها
        buildTraffic(w);
        buildNpcs(w);
    }

    // ---------------- جاده‌ها ----------------

    private static void ring(World w, int a, int b) {
        for (int x = a; x <= b; x++) {
            setRoad(w, x, a);
            setRoad(w, x, a + 1);
            setRoad(w, x, b - 1);
            setRoad(w, x, b);
        }
        for (int y = a; y <= b; y++) {
            setRoad(w, a, y);
            setRoad(w, a + 1, y);
            setRoad(w, b - 1, y);
            setRoad(w, b, y);
        }
    }

    private static void streetH(World w, int ty, int x0, int x1) {
        for (int x = x0; x <= x1; x++) {
            setRoad(w, x, ty);
            setRoad(w, x, ty + 1);
        }
    }

    private static void streetV(World w, int tx, int y0, int y1) {
        for (int y = y0; y <= y1; y++) {
            setRoad(w, tx, y);
            setRoad(w, tx + 1, y);
        }
    }

    private static void setRoad(World w, int x, int y) {
        if (x >= 0 && y >= 0 && x < G.MAP_W && y < G.MAP_H) {
            w.tileType[y][x] = World.T_ROAD;
        }
    }

    private static void sidewalks(World w) {
        for (int y = 1; y < G.MAP_H - 1; y++) {
            for (int x = 1; x < G.MAP_W - 1; x++) {
                if (w.tileType[y][x] != World.T_GRASS) continue;
                boolean nearRoad =
                        w.tileType[y][x - 1] == World.T_ROAD || w.tileType[y][x + 1] == World.T_ROAD ||
                        w.tileType[y - 1][x] == World.T_ROAD || w.tileType[y + 1][x] == World.T_ROAD;
                if (nearRoad) w.tileType[y][x] = World.T_SIDEWALK;
            }
        }
    }

    // ---------------- پارک ----------------

    private static void buildPark(World w, int tx, int ty, int tw, int th) {
        for (int y = ty; y < ty + th; y++) {
            for (int x = tx; x < tx + tw; x++) {
                w.tileType[y][x] = World.T_PARK;
            }
        }
        // مسیرهای پارک
        for (int x = tx; x < tx + tw; x++) {
            w.tileType[ty + th / 2][x] = World.T_PATH;
        }
        for (int y = ty; y < ty + th; y++) {
            w.tileType[y][tx + tw / 2] = World.T_PATH;
        }
        // حوض آب
        int cx = tx + tw / 2, cy = ty + th / 2;
        for (int y = cy - 3; y <= cy + 3; y++) {
            for (int x = cx - 4; x <= cx + 4; x++) {
                int dx = x - cx, dy = y - cy;
                if (dx * dx * 2 + dy * dy * 3 <= 26) {
                    w.tileType[y][x] = World.T_WATER;
                }
            }
        }
        w.fountainX = (cx + 0.5f) * G.TILE;
        w.fountainY = (cy + 0.5f) * G.TILE;
        w.waterfallX = w.fountainX - 4.4f * G.TILE;
        w.waterfallY = (cy - 2.2f) * G.TILE;
    }

    // ---------------- ساختمان‌ها ----------------

    private static void addB(World w, int type, int tx, int ty, int tw, int th, int wall, int roof) {
        Building b = new Building(type, tx * G.TILE, ty * G.TILE, tw * G.TILE, th * G.TILE, wall, roof);
        w.buildings.add(b);
        for (int y = ty; y < ty + th; y++) {
            for (int x = tx; x < tx + tw; x++) {
                if (x >= 0 && y >= 0 && x < G.MAP_W && y < G.MAP_H) {
                    w.tileType[y][x] = World.T_BUILDING;
                }
            }
        }
        return;
    }

    private static void buildBuildings(World w) {
        // بلوک شمالی (بین ریل و خیابان اول) — در رو به جنوب
        addB(w, Building.BANK, 14, 20, 8, 7, 0xFFFFF3E0, 0xFF8D6E63);
        addB(w, Building.SCHOOL, 25, 20, 9, 7, 0xFFE3F2FD, 0xFF1565C0);
        addB(w, Building.LIBRARY, 37, 20, 8, 7, 0xFFF3E5F5, 0xFF6A1B9A);
        addB(w, Building.HOSPITAL, 48, 19, 10, 8, 0xFFFFFFFF, 0xFFE53935);
        addB(w, Building.POLICE, 76, 20, 8, 7, 0xFFECEFF1, 0xFF37474F);
        addB(w, Building.FIRE, 87, 20, 8, 7, 0xFFFFEBEE, 0xFFC62828);
        addB(w, Building.TOYSTORE, 98, 20, 8, 7, 0xFFFFF9C4, 0xFFF9A825);

        // بلوک میانی (بین دو خیابان افقی) — پارک سمت چپ است
        addB(w, Building.CAFE, 38, 42, 7, 6, 0xFFFFF8E1, 0xFF795548);
        addB(w, Building.RESTAURANT, 48, 41, 9, 7, 0xFFFFECB3, 0xFFE65100);
        addB(w, Building.MARKET, 74, 42, 8, 6, 0xFFE8F5E9, 0xFF2E7D32);
        addB(w, Building.CINEMA, 86, 41, 12, 7, 0xFFEDE7F6, 0xFF4A148C);
        addB(w, Building.ZOO, 104, 40, 14, 9, 0xFFDCEDC8, 0xFF33691E);

        // بلوک جنوبی (بین خیابان دوم و ریل جنوبی) — خانه‌ها و بقیه
        addB(w, Building.HOME, 14, 72, 6, 6, 0xFFFFFDE7, 0xFFEF6C00);
        addB(w, Building.HOME, 23, 72, 6, 6, 0xFFE1F5FE, 0xFF0277BD);
        addB(w, Building.HOME, 32, 72, 6, 6, 0xFFFFEBEE, 0xFFC2185B);
        addB(w, Building.HOME, 14, 80, 6, 6, 0xFFF1F8E9, 0xFF558B2F);
        addB(w, Building.HOME, 23, 80, 6, 6, 0xFFFBE9E7, 0xFFBF360C);
        addB(w, Building.HOME, 32, 80, 6, 6, 0xFFE8EAF6, 0xFF283593);

        // هلی‌پورت (فضای باز) جنوب شرق
        addB(w, Building.HELIPORT, 84, 74, 10, 9, 0xFFB2DFDB, 0xFF00695C);

        // ایستگاه قطار — کنار ریل جنوبی
        Building st = new Building(Building.TRAIN_STATION, 58 * G.TILE, 91 * G.TILE,
                9 * G.TILE, 3 * G.TILE, 0xFFFFF3E0, 0xFF5D4037);
        st.doorY = st.y - 24f;   // در رو به ریل (سکو)
        w.buildings.add(st);
        for (int y = 91; y < 94; y++) {
            for (int x = 58; x < 67; x++) {
                w.tileType[y][x] = World.T_BUILDING;
            }
        }

        // نقطه سکوی ایستگاه (روی ریل جنوبی)
        w.trainStationX = st.doorX;
        if (w.railPath != null) {
            w.trainStationY = w.railPath.bottom();
        } else {
            w.trainStationY = 89.5f * G.TILE;
        }

        // خانه بازیکن = خانه اول
        w.spawnX = 17 * G.TILE;
        w.spawnY = 79.5f * G.TILE;
    }

    // ---------------- درخت‌ها ----------------

    private static void scatterTrees(World w) {
        // داخل پارک
        for (int i = 0; i < 14; i++) {
            float x = (12 + rnd.nextInt(17)) * G.TILE;
            float y = (39 + rnd.nextInt(22)) * G.TILE;
            if (w.tileAt(x, y) == World.T_PARK || w.tileAt(x, y) == World.T_GRASS) {
                w.trees.add(new float[]{x, y, 1f + rnd.nextFloat() * 0.5f});
            }
        }
        // حاشیه کمربندی داخل
        for (int i = 0; i < 26; i++) {
            float x = (8 + rnd.nextInt(124)) * G.TILE;
            float y = (8 + rnd.nextInt(84)) * G.TILE;
            int t = w.tileAt(x, y);
            if ((t == World.T_GRASS || t == World.T_PARK) && !w.isBlocked(x, y, 30f)) {
                w.trees.add(new float[]{x, y, 0.9f + rnd.nextFloat() * 0.5f});
            }
        }
    }

    // ---------------- ریل و قطار ----------------

    private static void buildRail(World w) {
        // حلقه ریل داخل کمربندی
        w.railPath = new RailPath(10.5f * G.TILE, 10.5f * G.TILE,
                129.5f * G.TILE, 89.5f * G.TILE);

        // قطار شادی — همیشه در حرکت
        Vehicle train = new Vehicle(Vehicle.CAR_TRAIN, 10.5f * G.TILE, 10.5f * G.TILE);
        train.mode = Vehicle.MODE_RAIL;
        train.trackPos = w.railPath.distOfPoint(w.trainStationX, w.trainStationY) + 500f;
        w.vehicles.add(train);
    }

    // ---------------- ترافیک ----------------

    private static void buildTraffic(World w) {
        // لاین‌های کمربندی (حلقه کامل دور شهر)
        float ringMin = 140f, ringMaxX = G.WORLD_W - 140f, ringMaxY = G.WORLD_H - 140f;
        w.trafficLanes.add(new float[]{0, 4.5f * G.TILE, ringMin, ringMaxX});    // شمال شرق‌رو
        w.trafficLanes.add(new float[]{0, 5.5f * G.TILE, ringMin, ringMaxX});    // شمال غرب‌رو
        w.trafficLanes.add(new float[]{0, 94.5f * G.TILE, ringMin, ringMaxX});   // جنوب
        w.trafficLanes.add(new float[]{0, 95.5f * G.TILE, ringMin, ringMaxX});
        w.trafficLanes.add(new float[]{1, 4.5f * G.TILE, ringMin, ringMaxY});    // غرب
        w.trafficLanes.add(new float[]{1, 5.5f * G.TILE, ringMin, ringMaxY});
        w.trafficLanes.add(new float[]{1, 134.5f * G.TILE, ringMin, ringMaxY});  // شرق
        w.trafficLanes.add(new float[]{1, 135.5f * G.TILE, ringMin, ringMaxY});

        // لاین خیابان‌های داخلی
        float ix0 = 6.5f * G.TILE, ix1 = 133.5f * G.TILE, iy0 = 6.5f * G.TILE, iy1 = 93.5f * G.TILE;
        w.trafficLanes.add(new float[]{0, 34.5f * G.TILE, ix0, ix1});
        w.trafficLanes.add(new float[]{0, 35.5f * G.TILE, ix0, ix1});
        w.trafficLanes.add(new float[]{0, 66.5f * G.TILE, ix0, ix1});
        w.trafficLanes.add(new float[]{0, 67.5f * G.TILE, ix0, ix1});
        w.trafficLanes.add(new float[]{1, 34.5f * G.TILE, iy0, iy1});
        w.trafficLanes.add(new float[]{1, 35.5f * G.TILE, iy0, iy1});
        w.trafficLanes.add(new float[]{1, 69.5f * G.TILE, iy0, iy1});
        w.trafficLanes.add(new float[]{1, 70.5f * G.TILE, iy0, iy1});
        w.trafficLanes.add(new float[]{1, 104.5f * G.TILE, iy0, iy1});
        w.trafficLanes.add(new float[]{1, 105.5f * G.TILE, iy0, iy1});

        // ۱۸ خودرو در شهر
        int[] types = {Vehicle.CAR_SEDAN, Vehicle.CAR_TAXI, Vehicle.CAR_SPORT, Vehicle.CAR_PICKUP,
                       Vehicle.CAR_BUS, Vehicle.MOTOR};
        for (int i = 0; i < 18; i++) {
            Vehicle v = new Vehicle(types[i % types.length], 0, 0);
            w.randomLaneSpawn(v);
            w.vehicles.add(v);
        }

        // هلیکوپتر پارک‌شده در هلی‌پورت
        Building hp = w.buildingByType(Building.HELIPORT);
        if (hp != null) {
            Vehicle heli = new Vehicle(Vehicle.CAR_HELICOPTER, hp.doorX, hp.y + hp.h * 0.42f);
            heli.mode = Vehicle.MODE_PARKED;
            heli.angle = 0f;
            w.vehicles.add(heli);
        }
    }

    // ---------------- شهروندها ----------------

    private static void buildNpcs(World w) {
        Npc.scatter(w.cityNpcs, w, 26, G.WORLD_W / 2f, G.WORLD_H / 2f, G.WORLD_W / 2.4f);
    }
}
