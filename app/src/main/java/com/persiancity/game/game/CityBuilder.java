package com.persiancity.game.game;

import java.util.Random;

/**
 * سازنده شهر خیلی بزرگ «شهر شادی»
 * کمربندی کامل (چهار طرف!) + خیابان‌های داخلی + پارک با آبشار وسط دریاچه
 * + روستا + دریاچه بزرگ با ماهی + شهربازی + فروشگاه ماشین + ریل دور شهر
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

        // ۲) کمربندی کامل دور شهر (چهار طرف — شامل جنوب!)
        ring(w, 4, 163, 4, 123);

        // ۳) خیابان‌های داخلی (افقی و عمودی)
        streetH(w, 34, 6, 161);
        streetH(w, 66, 6, 161);
        streetH(w, 98, 6, 161);
        streetV(w, 34, 6, 121);
        streetV(w, 69, 6, 121);
        streetV(w, 104, 6, 93);   // قبل از دریاچه تمام می‌شود
        streetV(w, 139, 6, 93);   // قبل از دریاچه تمام می‌شود

        // ۴) پارک بزرگ با حوض و آبشار (آبشار وسط آب!)
        buildPark(w, 11, 38, 19, 24);

        // ۵) شهربازی (فضای باز با چرخ‌وفلک و سرسیر)
        buildAmusement(w, 142, 13, 15, 18);

        // ۶) دریاچه بزرگ روستایی با ماهی
        buildLake(w);

        // ۷) روستا
        buildVillage(w);

        // ۸) ساختمان‌ها
        buildBuildings(w);

        // ۹) پیاده‌روها (حاشیه جاده‌ها)
        sidewalks(w);

        // ۱۰) درخت‌ها
        scatterTrees(w);

        // ۱۱) ریل دور شهر + ایستگاه + قطار
        buildRail(w);

        // ۱۲) ترافیک و شهروندها
        buildTraffic(w);
        buildNpcs(w);
    }

    // ---------------- جاده‌ها ----------------

    /**
     * کمربندی مستطیلی — همه اضلاع داخل نقشه (رفع باگ جاده جنوبی جاافتاده)
     */
    private static void ring(World w, int x0, int x1, int y0, int y1) {
        for (int x = x0; x <= x1; x++) {
            setRoad(w, x, y0);
            setRoad(w, x, y0 + 1);
            setRoad(w, x, y1 - 1);
            setRoad(w, x, y1);
        }
        for (int y = y0; y <= y1; y++) {
            setRoad(w, x0, y);
            setRoad(w, x0 + 1, y);
            setRoad(w, x1 - 1, y);
            setRoad(w, x1, y);
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

    private static void setTile(World w, int x, int y, int t) {
        if (x >= 0 && y >= 0 && x < G.MAP_W && y < G.MAP_H) {
            w.tileType[y][x] = t;
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
                setTile(w, x, y, World.T_PARK);
            }
        }
        // مسیرهای پارک
        for (int x = tx; x < tx + tw; x++) {
            setTile(w, x, ty + th / 2, World.T_PATH);
        }
        for (int y = ty; y < ty + th; y++) {
            setTile(w, tx + tw / 2, y, World.T_PATH);
        }
        // حوض آب
        int cx = tx + tw / 2, cy = ty + th / 2;
        for (int y = cy - 3; y <= cy + 3; y++) {
            for (int x = cx - 4; x <= cx + 4; x++) {
                int dx = x - cx, dy = y - cy;
                if (dx * dx * 2 + dy * dy * 3 <= 26) {
                    setTile(w, x, y, World.T_WATER);
                }
            }
        }
        w.fountainX = (cx + 0.5f) * G.TILE;
        w.fountainY = (cy + 0.5f) * G.TILE;
        // ✅ آبشار داخلِ آب — سمت چپ حوض (قبلاً بیرون حوض بود!)
        w.waterfallX = w.fountainX - 2.0f * G.TILE;
        w.waterfallY = (cy - 1.0f) * G.TILE;
    }

    // ---------------- شهربازی ----------------

    private static void buildAmusement(World w, int tx, int ty, int tw, int th) {
        for (int y = ty; y < ty + th; y++) {
            for (int x = tx; x < tx + tw; x++) {
                setTile(w, x, y, World.T_PARK);
            }
        }
        // مسیر شهربازی
        for (int x = tx; x < tx + tw; x++) {
            setTile(w, x, ty + 8, World.T_PATH);
        }
        for (int y = ty; y < ty + th; y++) {
            setTile(w, tx + 7, y, World.T_PATH);
        }
        // چرخ‌وفلک بزرگ و سرسیر کاروسل
        w.wheelX = (tx + 7.5f) * G.TILE;
        w.wheelY = (ty + 5.0f) * G.TILE;
        w.carouselX = (tx + 2.5f) * G.TILE;
        w.carouselY = (ty + 12.5f) * G.TILE;
    }

    // ---------------- دریاچه بزرگ ----------------

    private static void buildLake(World w) {
        // دریاچه بیضی بزرگ در جنوب شرق (داخل ریل — دور از خیابان‌ها)
        float cx = 128f, cy = 106.5f, rx = 22f, ry = 5.8f;
        for (int y = 99; y <= 116; y++) {
            for (int x = 103; x <= 153; x++) {
                float dx = (x + 0.5f - cx) / rx;
                float dy = (y + 0.5f - cy) / ry;
                float wiggle = (float) Math.sin(x * 0.9f) * 0.06f + (float) Math.cos(y * 1.3f) * 0.05f;
                if (dx * dx + dy * dy <= 1f + wiggle) {
                    setTile(w, x, y, World.T_WATER);
                }
            }
        }
        // ماهی‌های دریاچه (برای رسم)
        for (int i = 0; i < 12; i++) {
            float a = rnd.nextFloat() * (float) Math.PI * 2f;
            float rr = (float) Math.sqrt(rnd.nextFloat()) * 0.8f;
            float fx = cx + (float) Math.cos(a) * rx * rr;
            float fy = cy + (float) Math.sin(a) * ry * rr;
            w.fishSpots.add(new float[]{fx * G.TILE, fy * G.TILE, rnd.nextInt(3)});
        }
        // اسکله چوبی کنار دریاچه
        addB(w, Building.DOCK, 101, 102, 4, 4, 0xFFD7CCC8, 0xFF8D6E63);
    }

    // ---------------- روستا ----------------

    private static void buildVillage(World w) {
        // جاده خاکی از خیابان جنوبی به دل روستا (از ردیف ۱۰۰ — جاده اصلی دست نخورد)
        for (int y = 100; y <= 113; y++) {
            setTile(w, 30, y, World.T_DIRT);
            setTile(w, 31, y, World.T_DIRT);
        }
        // جاده خاکی افقی روستا
        for (int x = 13; x <= 56; x++) {
            setTile(w, x, 107, World.T_DIRT);
        }

        // خانه‌های روستایی (دو ردیف)
        addB(w, Building.VILLAGE_HOME, 14, 102, 5, 4, 0xFFFFF3E0, 0xFFBFA05A);
        addB(w, Building.VILLAGE_HOME, 22, 102, 5, 4, 0xFFEFEBE9, 0xFF8D6E63);
        addB(w, Building.VILLAGE_HOME, 38, 102, 5, 4, 0xFFE8F5E9, 0xFF6D8B4E);
        addB(w, Building.VILLAGE_HOME, 14, 109, 5, 4, 0xFFE3F2FD, 0xFF795548);
        addB(w, Building.VILLAGE_HOME, 22, 109, 5, 4, 0xFFFBE9E7, 0xFFA1523B);

        // نانوایی و مزرعه
        addB(w, Building.BAKERY, 44, 101, 6, 5, 0xFFFFF8E1, 0xFFC77A3A);
        addB(w, Building.FARM, 44, 109, 6, 4, 0xFFF1F8E9, 0xFF9E9D24);

        // حیوانات روستا: گاو و گوسفندها در حال چرا
        w.villageAnimals.add(new float[]{39.5f * G.TILE, 100.8f * G.TILE, 0f});   // گاو
        w.villageAnimals.add(new float[]{21f * G.TILE, 101.3f * G.TILE, 1f});     // گوسفند
        w.villageAnimals.add(new float[]{52.5f * G.TILE, 99.8f * G.TILE, 1f});    // گوسفند دوم
    }

    // ---------------- ساختمان‌ها ----------------

    private static void addB(World w, int type, int tx, int ty, int tw, int th, int wall, int roof) {
        Building b = new Building(type, tx * G.TILE, ty * G.TILE, tw * G.TILE, th * G.TILE, wall, roof);
        w.buildings.add(b);
        for (int y = ty; y < ty + th; y++) {
            for (int x = tx; x < tx + tw; x++) {
                setTile(w, x, y, World.T_BUILDING);
            }
        }
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
        addB(w, Building.CARSHOP, 110, 20, 12, 7, 0xFFE0F7FA, 0xFF00695C);

        // بلوک میانی (بین دو خیابان افقی) — پارک سمت چپ است
        addB(w, Building.CAFE, 38, 42, 7, 6, 0xFFFFF8E1, 0xFF795548);
        addB(w, Building.RESTAURANT, 48, 41, 9, 7, 0xFFFFECB3, 0xFFE65100);
        addB(w, Building.MARKET, 74, 42, 8, 6, 0xFFE8F5E9, 0xFF2E7D32);
        addB(w, Building.CINEMA, 86, 41, 12, 7, 0xFFEDE7F6, 0xFF4A148C);
        addB(w, Building.ZOO, 104, 40, 14, 9, 0xFFDCEDC8, 0xFF33691E);

        // بلوک جنوبی — خانه‌ها و هلی‌پورت
        addB(w, Building.HOME, 14, 72, 6, 6, 0xFFFFFDE7, 0xFFEF6C00);
        addB(w, Building.HOME, 23, 72, 6, 6, 0xFFE1F5FE, 0xFF0277BD);
        addB(w, Building.HOME, 32, 72, 6, 6, 0xFFFFEBEE, 0xFFC2185B);
        addB(w, Building.HOME, 14, 80, 6, 6, 0xFFF1F8E9, 0xFF558B2F);
        addB(w, Building.HOME, 23, 80, 6, 6, 0xFFFBE9E7, 0xFFBF360C);
        addB(w, Building.HOME, 32, 80, 6, 6, 0xFFE8EAF6, 0xFF283593);

        // هلی‌پورت (فضای باز) جنوب
        addB(w, Building.HELIPORT, 84, 74, 10, 9, 0xFFB2DFDB, 0xFF00695C);

        // ورودی شهربازی — کنار خیابان ۱۳۹
        addB(w, Building.AMUSEMENT, 142, 31, 7, 3, 0xFFFFFDE7, 0xFFE91E63);

        // ایستگاه قطار — جنوب ریل (در رو به سکو/شمال)
        Building st = new Building(Building.TRAIN_STATION, 58 * G.TILE, 115 * G.TILE,
                9 * G.TILE, 3 * G.TILE, 0xFFFFF3E0, 0xFF5D4037);
        st.doorY = st.y - 24f;   // در رو به ریل (سکو)
        w.buildings.add(st);
        for (int y = 115; y < 118; y++) {
            for (int x = 58; x < 67; x++) {
                setTile(w, x, y, World.T_BUILDING);
            }
        }

        // نقطه سکوی ایستگاه (روی ریل جنوبی)
        w.trainStationX = st.doorX;
        if (w.railPath != null) {
            w.trainStationY = w.railPath.bottom();
        } else {
            w.trainStationY = 113.5f * G.TILE;
        }

        // خانه بازیکن = خانه اول (با اسم مخصوص)
        Building firstHome = null;
        for (Building b : w.buildings) {
            if (b.type == Building.HOME) { firstHome = b; break; }
        }
        if (firstHome != null) {
            firstHome.label = "خانه تو";
            w.playerHome = firstHome;
        }

        w.spawnX = 17 * G.TILE;
        w.spawnY = 79.5f * G.TILE;
    }

    // ---------------- درخت‌ها ----------------

    private static void scatterTrees(World w) {
        // داخل پارک
        for (int i = 0; i < 16; i++) {
            float x = (12 + rnd.nextInt(17)) * G.TILE;
            float y = (39 + rnd.nextInt(22)) * G.TILE;
            if (w.tileAt(x, y) == World.T_PARK || w.tileAt(x, y) == World.T_GRASS) {
                w.trees.add(new float[]{x, y, 1f + rnd.nextFloat() * 0.5f});
            }
        }
        // شهربازی
        for (int i = 0; i < 8; i++) {
            float x = (142 + rnd.nextInt(15)) * G.TILE;
            float y = (13 + rnd.nextInt(18)) * G.TILE;
            if (w.tileAt(x, y) == World.T_PARK || w.tileAt(x, y) == World.T_GRASS) {
                w.trees.add(new float[]{x, y, 0.9f + rnd.nextFloat() * 0.5f});
            }
        }
        // روستا
        for (int i = 0; i < 12; i++) {
            float x = (12 + rnd.nextInt(45)) * G.TILE;
            float y = (99 + rnd.nextInt(15)) * G.TILE;
            int t = w.tileAt(x, y);
            if ((t == World.T_GRASS || t == World.T_PARK) && !w.isBlocked(x, y, 30f)) {
                w.trees.add(new float[]{x, y, 0.9f + rnd.nextFloat() * 0.6f});
            }
        }
        // حاشیه شهر
        for (int i = 0; i < 34; i++) {
            float x = (8 + rnd.nextInt(152)) * G.TILE;
            float y = (8 + rnd.nextInt(112)) * G.TILE;
            int t = w.tileAt(x, y);
            if ((t == World.T_GRASS || t == World.T_PARK) && !w.isBlocked(x, y, 30f)) {
                w.trees.add(new float[]{x, y, 0.9f + rnd.nextFloat() * 0.5f});
            }
        }
    }

    // ---------------- ریل و قطار ----------------

    private static void buildRail(World w) {
        // حلقه ریل داخل کمربندی — دور کل شهر بزرگ
        w.railPath = new RailPath(10.5f * G.TILE, 10.5f * G.TILE,
                157.5f * G.TILE, 113.5f * G.TILE);

        // قطار شادی — همیشه در حرکت
        Vehicle train = new Vehicle(Vehicle.CAR_TRAIN, 10.5f * G.TILE, 10.5f * G.TILE);
        train.mode = Vehicle.MODE_RAIL;
        train.trackPos = w.railPath.distOfPoint(w.trainStationX, w.trainStationY) + 500f;
        w.vehicles.add(train);
    }

    // ---------------- ترافیک ----------------

    private static void buildTraffic(World w) {
        // لاین‌های کمربندی (حلقه کامل — شامل جنوب!)
        float ringMin = 140f, ringMaxX = G.WORLD_W - 140f, ringMaxY = G.WORLD_H - 140f;
        w.trafficLanes.add(new float[]{0, 4.5f * G.TILE, ringMin, ringMaxX});      // شمال
        w.trafficLanes.add(new float[]{0, 5.5f * G.TILE, ringMin, ringMaxX});
        w.trafficLanes.add(new float[]{0, 122.5f * G.TILE, ringMin, ringMaxX});    // جنوب ✅
        w.trafficLanes.add(new float[]{0, 123.5f * G.TILE, ringMin, ringMaxX});
        w.trafficLanes.add(new float[]{1, 4.5f * G.TILE, ringMin, ringMaxY});      // غرب
        w.trafficLanes.add(new float[]{1, 5.5f * G.TILE, ringMin, ringMaxY});
        w.trafficLanes.add(new float[]{1, 162.5f * G.TILE, ringMin, ringMaxY});    // شرق
        w.trafficLanes.add(new float[]{1, 163.5f * G.TILE, ringMin, ringMaxY});

        // لاین خیابان‌های داخلی
        float ix0 = 6.5f * G.TILE, ix1 = 161.5f * G.TILE, iy0 = 6.5f * G.TILE, iy1 = 121.5f * G.TILE;
        w.trafficLanes.add(new float[]{0, 34.5f * G.TILE, ix0, ix1});
        w.trafficLanes.add(new float[]{0, 35.5f * G.TILE, ix0, ix1});
        w.trafficLanes.add(new float[]{0, 66.5f * G.TILE, ix0, ix1});
        w.trafficLanes.add(new float[]{0, 67.5f * G.TILE, ix0, ix1});
        w.trafficLanes.add(new float[]{0, 98.5f * G.TILE, ix0, ix1});
        w.trafficLanes.add(new float[]{0, 99.5f * G.TILE, ix0, ix1});
        w.trafficLanes.add(new float[]{1, 34.5f * G.TILE, iy0, iy1});
        w.trafficLanes.add(new float[]{1, 35.5f * G.TILE, iy0, iy1});
        w.trafficLanes.add(new float[]{1, 69.5f * G.TILE, iy0, iy1});
        w.trafficLanes.add(new float[]{1, 70.5f * G.TILE, iy0, iy1});
        w.trafficLanes.add(new float[]{1, 104.5f * G.TILE, iy0, 93.5f * G.TILE});   // قبل دریاچه
        w.trafficLanes.add(new float[]{1, 105.5f * G.TILE, iy0, 93.5f * G.TILE});
        w.trafficLanes.add(new float[]{1, 139.5f * G.TILE, iy0, 93.5f * G.TILE});
        w.trafficLanes.add(new float[]{1, 140.5f * G.TILE, iy0, 93.5f * G.TILE});

        // ۲۶ خودرو در شهر بزرگ
        int[] types = {Vehicle.CAR_SEDAN, Vehicle.CAR_TAXI, Vehicle.CAR_SPORT, Vehicle.CAR_PICKUP,
                       Vehicle.CAR_BUS, Vehicle.MOTOR};
        for (int i = 0; i < 26; i++) {
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

        // قایق عمومی اسکله — روی آب کنار اسکله
        Vehicle boat = new Vehicle(Vehicle.CAR_BOAT, 107.5f * G.TILE, 104.8f * G.TILE);
        boat.mode = Vehicle.MODE_PARKED;
        boat.angle = 0f;
        boat.owned = true;   // قایق اسکله برای همه آزاد است
        w.vehicles.add(boat);
    }

    // ---------------- شهروندها ----------------

    private static void buildNpcs(World w) {
        Npc.scatter(w.cityNpcs, w, 32, G.WORLD_W / 2f, G.WORLD_H / 2f, G.WORLD_W / 2.6f);
        // شهروندهای روستا
        Npc.scatter(w.cityNpcs, w, 6, 30f * G.TILE, 106f * G.TILE, 8f * G.TILE);
    }
}
