package com.persiancity.game.game;

import java.util.Random;

/**
 * سازنده دنیای خیلی بزرگ «شهر شادی» — نسخه ۲.۴
 * شهر اصلی با جاده‌های پهن‌تر + شهر ستاره (شرق) + شهر گلاب (جنوب شرق)
 * + اتوبان بین‌شهری + خط اتوبوس با ایستگاه + دو خط ریل قطار
 * + فرودگاه با باند و هواپیمای خودکار + دو روستا با اسب + دریاچه بزرگ
 */
public final class CityBuilder {

    private CityBuilder() {}

    private static final Random rnd = new Random();

    // مرز حلقه‌های ریل (تایل)
    private static final float R1_L = 10.5f, R1_T = 10.5f, R1_R = 157.5f, R1_B = 113.5f;
    private static final float R2_L = 178.5f, R2_T = 8.5f, R2_R = 254.5f, R2_B = 158.5f;

    public static void build(World w) {
        // ۱) همه چمن
        for (int y = 0; y < G.MAP_H; y++) {
            for (int x = 0; x < G.MAP_W; x++) {
                w.tileType[y][x] = World.T_GRASS;
            }
        }

        // ۲) کمربندی پهن شهر اصلی (۴ لاین)
        ring4(w, 4, 167, 4, 123);

        // ۳) خیابان‌های داخلی پهن (۳ لاین)
        streetH3(w, 36, 8, 163);
        streetH3(w, 68, 8, 163);
        streetH3(w, 100, 8, 97);      // غرب — تا قبل اسکله و دریاچه
        streetH3(w, 100, 152, 163);   // شرق دریاچه
        streetV3(w, 36, 8, 119);
        streetV3(w, 72, 8, 119);
        streetV3(w, 108, 8, 96);      // قبل از دریاچه تمام می‌شود
        streetV3(w, 140, 8, 96);

        // ۴) اتوبان شرقی بین‌شهری (۴ لاین) + اتصال جنوب به شهر گلاب
        streetH4(w, 56, 164, 258);
        for (int y = 120; y <= 123; y++) {
            for (int x = 164; x <= 177; x++) {
                setRoad(w, x, y);
            }
        }

        // ۵) شهر ستاره (شرق)
        ring2(w, 218, 246, 10, 50);
        streetH3(w, 28, 220, 244);
        streetV3(w, 236, 12, 48);

        // ۶) شهر گلاب (جنوب شرق)
        ring2(w, 176, 252, 122, 156);
        streetH3(w, 138, 178, 250);
        streetV3(w, 210, 124, 154);

        // ۷) باند و پیش‌باند فرودگاه (شرق ریل شرقی — ریل از ۱۷۸٫۵ می‌گذرد)
        for (int y = 70; y <= 75; y++) {
            for (int x = 181; x <= 212; x++) {
                setTile(w, x, y, World.T_RUNWAY);
            }
        }
        for (int y = 76; y <= 80; y++) {
            for (int x = 180; x <= 214; x++) {
                setTile(w, x, y, World.T_RUNWAY);
            }
        }

        // ۸) پارک بزرگ + شهربازی + دریاچه + روستا
        buildPark(w, 12, 42, 21, 23);
        buildAmusement(w, 146, 12, 16, 18);
        buildLake(w);
        buildVillage(w);
        buildStarCityPark(w);
        buildGolabPark(w);

        // ۹) ساختمان‌ها (شهر اصلی + شهرهای دیگر + ایستگاه‌ها + فرودگاه)
        buildBuildings(w);

        // ۱۰) پیاده‌روها و درخت‌ها
        sidewalks(w);
        scatterTrees(w);

        // ۱۱) دو خط ریل + دو قطار + ایستگاه‌ها
        buildRail(w);

        // ۱۲) ایستگاه‌های اتوبوس بین‌شهری
        buildBusStops(w);

        // ۱۳) ترافیک و شهروندها
        buildTraffic(w);
        buildNpcs(w);
    }

    // ---------------- جاده‌ها ----------------

    /** کمربندی پهن ۴ لاینه */
    private static void ring4(World w, int x0, int x1, int y0, int y1) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y0 + 3; y++) setRoad(w, x, y);
            for (int y = y1 - 3; y <= y1; y++) setRoad(w, x, y);
        }
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x0 + 3; x++) setRoad(w, x, y);
            for (int x = x1 - 3; x <= x1; x++) setRoad(w, x, y);
        }
    }

    /** کمربندی باریک ۲ لاینه (شهرهای کوچک‌تر) */
    private static void ring2(World w, int x0, int x1, int y0, int y1) {
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

    private static void streetH3(World w, int ty, int x0, int x1) {
        for (int x = x0; x <= x1; x++) {
            setRoad(w, x, ty);
            setRoad(w, x, ty + 1);
            setRoad(w, x, ty + 2);
        }
    }

    private static void streetV3(World w, int tx, int y0, int y1) {
        for (int y = y0; y <= y1; y++) {
            setRoad(w, tx, y);
            setRoad(w, tx + 1, y);
            setRoad(w, tx + 2, y);
        }
    }

    private static void streetH4(World w, int ty, int x0, int x1) {
        for (int x = x0; x <= x1; x++) {
            for (int y = ty; y <= ty + 3; y++) setRoad(w, x, y);
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
                if (w.tileType[y][x] != World.T_ROAD) setTile(w, x, y, World.T_PARK);
            }
        }
        int cx = tx + tw / 2, cy = ty + th / 2;
        // حوض آب
        for (int y = cy - 4; y <= cy + 4; y++) {
            for (int x = cx - 5; x <= cx + 5; x++) {
                int dx = x - cx, dy = y - cy;
                if (dx * dx * 2 + dy * dy * 3 <= 40) {
                    setTile(w, x, y, World.T_WATER);
                }
            }
        }
        // مسیرهای پارک (روی آب و جاده نمی‌روند)
        for (int x = tx; x < tx + tw; x++) {
            int t = w.tileType[cy][x];
            if (t != World.T_WATER && t != World.T_ROAD) setTile(w, x, cy, World.T_PATH);
        }
        for (int y = ty; y < ty + th; y++) {
            int t = w.tileType[y][cx];
            if (t != World.T_WATER && t != World.T_ROAD) setTile(w, cx, y, World.T_PATH);
        }
        w.fountainX = (cx + 0.5f) * G.TILE;
        w.fountainY = (cy + 0.5f) * G.TILE;
        // ✅ آبشار داخلِ آب
        w.waterfallX = w.fountainX - 2.0f * G.TILE;
        w.waterfallY = (cy - 1.0f) * G.TILE;
    }

    // ---------------- شهربازی ----------------

    private static void buildAmusement(World w, int tx, int ty, int tw, int th) {
        for (int y = ty; y < ty + th; y++) {
            for (int x = tx; x < tx + tw; x++) {
                if (w.tileType[y][x] != World.T_ROAD) setTile(w, x, y, World.T_PARK);
            }
        }
        for (int x = tx; x < tx + tw; x++) {
            setTile(w, x, ty + 8, World.T_PATH);
        }
        for (int y = ty; y < ty + th; y++) {
            setTile(w, tx + 7, y, World.T_PATH);
        }
        w.wheelX = (tx + 7.5f) * G.TILE;
        w.wheelY = (ty + 5.0f) * G.TILE;
        w.carouselX = (tx + 2.5f) * G.TILE;
        w.carouselY = (ty + 13.5f) * G.TILE;
    }

    // ---------------- پارک‌های شهرهای دیگر ----------------

    private static void buildStarCityPark(World w) {
        for (int y = 40; y <= 48; y++) {
            for (int x = 220; x <= 234; x++) {
                if (w.tileType[y][x] != World.T_ROAD) setTile(w, x, y, World.T_PARK);
            }
        }
        for (int x = 220; x <= 234; x++) {
            if (w.tileType[44][x] != World.T_ROAD) setTile(w, x, 44, World.T_PATH);
        }
    }

    private static void buildGolabPark(World w) {
        for (int y = 124; y <= 153; y++) {
            for (int x = 234; x <= 250; x++) {
                if (w.tileType[y][x] != World.T_ROAD) setTile(w, x, y, World.T_PARK);
            }
        }
        for (int y = 124; y <= 153; y++) {
            if (w.tileType[y][242] != World.T_ROAD) setTile(w, 242, y, World.T_PATH);
        }
        for (int x = 234; x <= 250; x++) {
            if (w.tileType[148][x] != World.T_ROAD) setTile(w, x, 148, World.T_PATH);
        }
    }

    // ---------------- دریاچه بزرگ ----------------

    private static void buildLake(World w) {
        float cx = 129f, cy = 107f, rx = 22f, ry = 5.5f;
        for (int y = 99; y <= 116; y++) {
            for (int x = 104; x <= 154; x++) {
                float dx = (x + 0.5f - cx) / rx;
                float dy = (y + 0.5f - cy) / ry;
                float wiggle = (float) Math.sin(x * 0.9f) * 0.06f + (float) Math.cos(y * 1.3f) * 0.05f;
                if (dx * dx + dy * dy <= 1f + wiggle) {
                    setTile(w, x, y, World.T_WATER);
                }
            }
        }
        // ماهی‌های دریاچه
        for (int i = 0; i < 14; i++) {
            float a = rnd.nextFloat() * (float) Math.PI * 2f;
            float rr = (float) Math.sqrt(rnd.nextFloat()) * 0.8f;
            float fx = cx + (float) Math.cos(a) * rx * rr;
            float fy = cy + (float) Math.sin(a) * ry * rr;
            w.fishSpots.add(new float[]{fx * G.TILE, fy * G.TILE, rnd.nextInt(3)});
        }
        // اسکله چوبی کنار دریاچه
        addB(w, Building.DOCK, 103, 100, 4, 4, 0xFFD7CCC8, 0xFF8D6E63);
    }

    // ---------------- روستا ----------------

    private static void buildVillage(World w) {
        // جاده خاکی از خیابان جنوبی به دل روستا
        for (int y = 103; y <= 115; y++) {
            setTile(w, 30, y, World.T_DIRT);
            setTile(w, 31, y, World.T_DIRT);
        }
        // جاده خاکی افقی روستا (از خیابان شهری عبور نمی‌کند)
        for (int x = 13; x <= 50; x++) {
            if (x >= 36 && x <= 38) continue;   // خیابان شهری دست نخورد
            setTile(w, x, 110, World.T_DIRT);
        }

        // خانه‌های روستایی
        addB(w, Building.VILLAGE_HOME, 14, 104, 5, 4, 0xFFFFF3E0, 0xFFBFA05A);
        addB(w, Building.VILLAGE_HOME, 21, 104, 5, 4, 0xFFEFEBE9, 0xFF8D6E63);
        addB(w, Building.VILLAGE_HOME, 32, 104, 4, 4, 0xFFE8F5E9, 0xFF6D8B4E);
        addB(w, Building.VILLAGE_HOME, 14, 112, 5, 4, 0xFFE3F2FD, 0xFF795548);
        addB(w, Building.VILLAGE_HOME, 21, 112, 5, 4, 0xFFFBE9E7, 0xFFA1523B);

        // نانوایی و مزرعه
        addB(w, Building.BAKERY, 40, 104, 6, 5, 0xFFFFF8E1, 0xFFC77A3A);
        addB(w, Building.FARM, 40, 111, 6, 4, 0xFFF1F8E9, 0xFF9E9D24);

        // حیوانات روستا: گاو و گوسفندها در حال چرا
        w.villageAnimals.add(new float[]{38.5f * G.TILE, 109.5f * G.TILE, 0f});   // گاو
        w.villageAnimals.add(new float[]{26f * G.TILE, 103.3f * G.TILE, 1f});     // گوسفند
        w.villageAnimals.add(new float[]{49f * G.TILE, 103.5f * G.TILE, 1f});     // گوسفند دوم
        w.villageAnimals.add(new float[]{46f * G.TILE, 116.5f * G.TILE, 0f});     // گاو دوم
    }

    // ---------------- ساختمان‌ها ----------------

    private static void addB(World w, int type, int tx, int ty, int tw, int th, int wall, int roof) {
        Building b = new Building(type, tx * G.TILE, ty * G.TILE, tw * G.TILE, th * G.TILE, wall, roof);
        w.buildings.add(b);
        for (int y = ty; y < ty + th; y++) {
            for (int x = tx; x < tx + tw; x++) {
                // هلی‌پورت سکوی بتنی باز است — قابل راه‌رفتن
                setTile(w, x, y, type == Building.HELIPORT ? World.T_SIDEWALK : World.T_BUILDING);
            }
        }
    }

    private static void buildBuildings(World w) {
        // ===== شهر اصلی =====

        // بلوک شمالی — در رو به جنوب
        addB(w, Building.BANK, 14, 20, 8, 7, 0xFFFFF3E0, 0xFF8D6E63);
        addB(w, Building.SCHOOL, 25, 20, 9, 7, 0xFFE3F2FD, 0xFF1565C0);
        addB(w, Building.LIBRARY, 40, 20, 8, 7, 0xFFF3E5F5, 0xFF6A1B9A);
        addB(w, Building.HOSPITAL, 50, 19, 10, 8, 0xFFFFFFFF, 0xFFE53935);
        addB(w, Building.POLICE, 76, 20, 8, 7, 0xFFECEFF1, 0xFF37474F);
        addB(w, Building.FIRE, 87, 20, 8, 7, 0xFFFFEBEE, 0xFFC62828);
        addB(w, Building.TOYSTORE, 98, 20, 8, 7, 0xFFFFF9C4, 0xFFF9A825);
        addB(w, Building.CARSHOP, 112, 20, 12, 7, 0xFFE0F7FA, 0xFF00695C);

        // بلوک میانی — پارک سمت چپ است
        addB(w, Building.CAFE, 40, 42, 7, 6, 0xFFFFF8E1, 0xFF795548);
        addB(w, Building.RESTAURANT, 50, 41, 9, 7, 0xFFFFECB3, 0xFFE65100);
        addB(w, Building.CLOTHES, 61, 42, 7, 6, 0xFFFCE4EC, 0xFFAD1457);
        addB(w, Building.MARKET, 76, 42, 8, 6, 0xFFE8F5E9, 0xFF2E7D32);
        addB(w, Building.CINEMA, 88, 41, 11, 7, 0xFFEDE7F6, 0xFF4A148C);
        addB(w, Building.ZOO, 99, 40, 9, 9, 0xFFDCEDC8, 0xFF33691E);

        // بلوک جنوبی — خانه‌ها و هلی‌پورت
        addB(w, Building.HOME, 14, 72, 6, 6, 0xFFFFFDE7, 0xFFEF6C00);
        addB(w, Building.HOME, 22, 72, 6, 6, 0xFFE1F5FE, 0xFF0277BD);
        addB(w, Building.HOME, 30, 72, 6, 6, 0xFFFFEBEE, 0xFFC2185B);
        addB(w, Building.HOME, 14, 80, 6, 6, 0xFFF1F8E9, 0xFF558B2F);
        addB(w, Building.HOME, 22, 80, 6, 6, 0xFFFBE9E7, 0xFFBF360C);
        addB(w, Building.HOME, 30, 80, 6, 6, 0xFFE8EAF6, 0xFF283593);
        addB(w, Building.HELIPORT, 84, 74, 10, 9, 0xFFB2DFDB, 0xFF00695C);

        // ورودی شهربازی
        addB(w, Building.AMUSEMENT, 148, 30, 7, 3, 0xFFFFFDE7, 0xFFE91E63);

        // ===== شهر ستاره (شرق) =====
        addB(w, Building.HOME, 222, 14, 5, 5, 0xFFFFFDE7, 0xFFF9A825);
        addB(w, Building.HOME, 228, 14, 5, 5, 0xFFE1F5FE, 0xFF0277BD);
        Building starSchool = addBRet(w, Building.SCHOOL, 239, 14, 6, 6, 0xFFE3F2FD, 0xFF1565C0);
        starSchool.label = "دبستان ستاره";
        addB(w, Building.MARKET, 220, 33, 7, 6, 0xFFE8F5E9, 0xFF2E7D32);
        addB(w, Building.CAFE, 228, 33, 6, 6, 0xFFFFF8E1, 0xFF795548);
        addB(w, Building.BAKERY, 239, 33, 6, 5, 0xFFFFF8E1, 0xFFC77A3A);

        // ===== شهر گلاب (جنوب شرق) =====
        addB(w, Building.VILLAGE_HOME, 179, 126, 5, 4, 0xFFFFF3E0, 0xFFBFA05A);
        addB(w, Building.VILLAGE_HOME, 186, 126, 5, 4, 0xFFEFEBE9, 0xFF8D6E63);
        addB(w, Building.VILLAGE_HOME, 193, 126, 5, 4, 0xFFE8F5E9, 0xFF6D8B4E);
        addB(w, Building.FARM, 179, 131, 6, 4, 0xFFF1F8E9, 0xFF9E9D24);
        addB(w, Building.BAKERY, 191, 131, 6, 5, 0xFFFFF8E1, 0xFFC77A3A);
        addB(w, Building.VILLAGE_HOME, 179, 143, 5, 4, 0xFFE3F2FD, 0xFF795548);
        addB(w, Building.VILLAGE_HOME, 186, 143, 5, 4, 0xFFFBE9E7, 0xFFA1523B);
        addB(w, Building.HOME, 216, 126, 6, 6, 0xFFFFFDE7, 0xFFEF6C00);
        addB(w, Building.HOME, 224, 126, 6, 6, 0xFFE1F5FE, 0xFF0277BD);
        addB(w, Building.CAFE, 216, 143, 6, 6, 0xFFFFF8E1, 0xFF795548);
        addB(w, Building.MARKET, 224, 143, 7, 6, 0xFFE8F5E9, 0xFF2E7D32);

        // ===== فرودگاه =====
        addB(w, Building.AIRPORT, 182, 64, 12, 6, 0xFFECEFF1, 0xFF0277BD);

        // ===== ایستگاه‌های قطار =====
        // ایستگاه اصلی — جنوب ریل حلقه ۱ (در رو به شمال/سکو)
        Building stA = new Building(Building.TRAIN_STATION, 58 * G.TILE, 115 * G.TILE,
                9 * G.TILE, 3 * G.TILE, 0xFFFFF3E0, 0xFF5D4037);
        stA.doorY = stA.y - 24f;   // در رو به ریل
        stA.doorSide = 1;          // شمال
        w.buildings.add(stA);
        for (int y = 115; y < 118; y++) {
            for (int x = 58; x < 67; x++) {
                setTile(w, x, y, World.T_BUILDING);
            }
        }
        w.trainStationX = stA.doorX;
        w.trainStationY = R1_B * G.TILE;
        w.trainStations.add(new float[]{w.trainStationX, w.trainStationY, 0f});

        // ایستگاه شهر ستاره — بالای ریل حلقه ۲ (در رو به جنوب/سکو)
        Building stB = new Building(Building.TRAIN_STATION, 230 * G.TILE, 4 * G.TILE,
                8 * G.TILE, 4 * G.TILE, 0xFFE3F2FD, 0xFF1565C0);
        stB.label = "ایستگاه شهر ستاره";
        w.buildings.add(stB);
        for (int y = 4; y < 8; y++) {
            for (int x = 230; x < 238; x++) {
                setTile(w, x, y, World.T_BUILDING);
            }
        }
        w.trainStations.add(new float[]{stB.doorX, R2_T * G.TILE, 1f});

        // ایستگاه شهر گلاب — جنوب ریل حلقه ۲ (در رو به سکو/شمال... ریل در جنوب است → در پیش‌فرض جنوبی)
        Building stC = new Building(Building.TRAIN_STATION, 214 * G.TILE, 151 * G.TILE,
                8 * G.TILE, 4 * G.TILE, 0xFFF1F8E9, 0xFF558B2F);
        stC.label = "ایستگاه شهر گلاب";
        w.buildings.add(stC);
        for (int y = 151; y < 155; y++) {
            for (int x = 214; x < 222; x++) {
                setTile(w, x, y, World.T_BUILDING);
            }
        }
        w.trainStations.add(new float[]{stC.doorX, R2_B * G.TILE, 1f});

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

    private static Building addBRet(World w, int type, int tx, int ty, int tw, int th, int wall, int roof) {
        Building b = new Building(type, tx * G.TILE, ty * G.TILE, tw * G.TILE, th * G.TILE, wall, roof);
        w.buildings.add(b);
        for (int y = ty; y < ty + th; y++) {
            for (int x = tx; x < tx + tw; x++) {
                setTile(w, x, y, World.T_BUILDING);
            }
        }
        return b;
    }

    // ---------------- درخت‌ها ----------------

    private static void scatterTrees(World w) {
        // داخل پارک اصلی
        for (int i = 0; i < 18; i++) {
            float x = (13 + rnd.nextInt(19)) * G.TILE;
            float y = (43 + rnd.nextInt(20)) * G.TILE;
            addTree(w, x, y);
        }
        // شهربازی
        for (int i = 0; i < 8; i++) {
            float x = (147 + rnd.nextInt(14)) * G.TILE;
            float y = (13 + rnd.nextInt(16)) * G.TILE;
            addTree(w, x, y);
        }
        // روستا
        for (int i = 0; i < 12; i++) {
            float x = (12 + rnd.nextInt(46)) * G.TILE;
            float y = (100 + rnd.nextInt(16)) * G.TILE;
            addTree(w, x, y);
        }
        // پارک شهر ستاره
        for (int i = 0; i < 6; i++) {
            float x = (221 + rnd.nextInt(13)) * G.TILE;
            float y = (41 + rnd.nextInt(7)) * G.TILE;
            addTree(w, x, y);
        }
        // پارک شهر گلاب
        for (int i = 0; i < 9; i++) {
            float x = (235 + rnd.nextInt(15)) * G.TILE;
            float y = (125 + rnd.nextInt(28)) * G.TILE;
            addTree(w, x, y);
        }
        // حاشیه شهرها و نقشه
        for (int i = 0; i < 60; i++) {
            float x = (8 + rnd.nextInt(246)) * G.TILE;
            float y = (8 + rnd.nextInt(162)) * G.TILE;
            addTree(w, x, y);
        }
    }

    private static void addTree(World w, float x, float y) {
        int t = w.tileAt(x, y);
        if (t != World.T_GRASS && t != World.T_PARK) return;
        if (w.isBlocked(x, y, 30f)) return;
        // روی ریل نیفتد
        if (nearRail(x, y)) return;
        w.trees.add(new float[]{x, y, 0.9f + rnd.nextFloat() * 0.6f});
    }

    private static boolean nearRail(float x, float y) {
        float t = G.TILE;
        // حلقه ۱
        if (Math.abs(x - R1_L * t) < 70f || Math.abs(x - R1_R * t) < 70f) {
            if (y > R1_T * t - 70f && y < R1_B * t + 70f) return true;
        }
        if (Math.abs(y - R1_T * t) < 70f || Math.abs(y - R1_B * t) < 70f) {
            if (x > R1_L * t - 70f && x < R1_R * t + 70f) return true;
        }
        // حلقه ۲
        if (Math.abs(x - R2_L * t) < 70f || Math.abs(x - R2_R * t) < 70f) {
            if (y > R2_T * t - 70f && y < R2_B * t + 70f) return true;
        }
        if (Math.abs(y - R2_T * t) < 70f || Math.abs(y - R2_B * t) < 70f) {
            if (x > R2_L * t - 70f && x < R2_R * t + 70f) return true;
        }
        return false;
    }

    // ---------------- ریل و قطار ----------------

    private static void buildRail(World w) {
        // حلقه ۱ — دور شهر اصلی
        RailPath r1 = new RailPath(R1_L * G.TILE, R1_T * G.TILE, R1_R * G.TILE, R1_B * G.TILE);
        w.railPath = r1;
        // حلقه ۲ — دور فرودگاه، شهر ستاره و شهر گلاب
        RailPath r2 = new RailPath(R2_L * G.TILE, R2_T * G.TILE, R2_R * G.TILE, R2_B * G.TILE);
        w.railPath2 = r2;

        // قطار شادی ۱ — همیشه در حرکت
        Vehicle train1 = new Vehicle(Vehicle.CAR_TRAIN, r1.left(), r1.top());
        train1.mode = Vehicle.MODE_RAIL;
        train1.railIndex = 0;
        train1.speed = 150f;
        train1.trackPos = r1.distOfPoint(w.trainStationX, w.trainStationY) + 500f;
        w.vehicles.add(train1);

        // قطار شادی ۲ — خط شرق
        float[] stB = w.trainStations.get(1);
        Vehicle train2 = new Vehicle(Vehicle.CAR_TRAIN, r2.left(), r2.top());
        train2.mode = Vehicle.MODE_RAIL;
        train2.railIndex = 1;
        train2.speed = 150f;
        train2.trackPos = r2.distOfPoint(stB[0], stB[1]) + 500f;
        w.vehicles.add(train2);
    }

    // ---------------- ایستگاه‌های اتوبوس ----------------

    private static void buildBusStops(World w) {
        // {تایل x، تایل y} — همه روی پیاده‌رو کنار جاده
        addBusStop(w, 39f, 71f, "ایستگاه جنوب شهر");
        addBusStop(w, 76f, 67f, "ایستگاه مرکز شهر");
        addBusStop(w, 20f, 99f, "ایستگاه روستا");
        addBusStop(w, 222f, 55f, "ایستگاه شهر ستاره");
        addBusStop(w, 206f, 121f, "ایستگاه شهر گلاب");
    }

    private static void addBusStop(World w, float tx, float ty, String name) {
        w.busStops.add(new float[]{tx * G.TILE, ty * G.TILE});
        w.busStopNames.add(name);
    }

    // ---------------- ترافیک ----------------

    private static void buildTraffic(World w) {
        float t = G.TILE;

        // لاین‌های کمربندی شهر اصلی (پهن — ۴ ردیف)
        w.trafficLanes.add(new float[]{0, 5.5f * t, 6 * t, 162 * t});
        w.trafficLanes.add(new float[]{0, 6.5f * t, 6 * t, 162 * t});
        w.trafficLanes.add(new float[]{0, 121.5f * t, 6 * t, 175 * t});
        w.trafficLanes.add(new float[]{0, 122.5f * t, 6 * t, 175 * t});
        w.trafficLanes.add(new float[]{1, 5.5f * t, 6 * t, 122 * t});
        w.trafficLanes.add(new float[]{1, 6.5f * t, 6 * t, 122 * t});
        w.trafficLanes.add(new float[]{1, 165.5f * t, 6 * t, 122 * t});
        w.trafficLanes.add(new float[]{1, 166.5f * t, 6 * t, 122 * t});

        // خیابان‌های داخلی ۳ لاینه
        w.trafficLanes.add(new float[]{0, 36.5f * t, 8.5f * t, 162.5f * t});
        w.trafficLanes.add(new float[]{0, 37.5f * t, 8.5f * t, 162.5f * t});
        w.trafficLanes.add(new float[]{0, 38.5f * t, 8.5f * t, 162.5f * t});
        w.trafficLanes.add(new float[]{0, 68.5f * t, 8.5f * t, 162.5f * t});
        w.trafficLanes.add(new float[]{0, 69.5f * t, 8.5f * t, 162.5f * t});
        w.trafficLanes.add(new float[]{0, 70.5f * t, 8.5f * t, 162.5f * t});
        w.trafficLanes.add(new float[]{0, 100.5f * t, 8.5f * t, 96.5f * t});
        w.trafficLanes.add(new float[]{0, 101.5f * t, 8.5f * t, 96.5f * t});
        w.trafficLanes.add(new float[]{0, 102.5f * t, 8.5f * t, 96.5f * t});
        w.trafficLanes.add(new float[]{0, 100.5f * t, 152.5f * t, 162.5f * t});
        w.trafficLanes.add(new float[]{0, 101.5f * t, 152.5f * t, 162.5f * t});
        w.trafficLanes.add(new float[]{1, 36.5f * t, 8.5f * t, 119.5f * t});
        w.trafficLanes.add(new float[]{1, 37.5f * t, 8.5f * t, 119.5f * t});
        w.trafficLanes.add(new float[]{1, 38.5f * t, 8.5f * t, 119.5f * t});
        w.trafficLanes.add(new float[]{1, 72.5f * t, 8.5f * t, 119.5f * t});
        w.trafficLanes.add(new float[]{1, 73.5f * t, 8.5f * t, 119.5f * t});
        w.trafficLanes.add(new float[]{1, 74.5f * t, 8.5f * t, 119.5f * t});
        w.trafficLanes.add(new float[]{1, 108.5f * t, 8.5f * t, 96.5f * t});
        w.trafficLanes.add(new float[]{1, 109.5f * t, 8.5f * t, 96.5f * t});
        w.trafficLanes.add(new float[]{1, 140.5f * t, 8.5f * t, 96.5f * t});
        w.trafficLanes.add(new float[]{1, 141.5f * t, 8.5f * t, 96.5f * t});

        // اتوبان شرقی بین‌شهری
        w.trafficLanes.add(new float[]{0, 57.5f * t, 168 * t, 257 * t});
        w.trafficLanes.add(new float[]{0, 58.5f * t, 168 * t, 257 * t});

        // لاین‌های شهر ستاره
        w.trafficLanes.add(new float[]{0, 10.5f * t, 219 * t, 245 * t});
        w.trafficLanes.add(new float[]{0, 49.5f * t, 219 * t, 245 * t});
        w.trafficLanes.add(new float[]{1, 218.5f * t, 11 * t, 49 * t});
        w.trafficLanes.add(new float[]{1, 245.5f * t, 11 * t, 49 * t});
        w.trafficLanes.add(new float[]{0, 28.5f * t, 220.5f * t, 243.5f * t});
        w.trafficLanes.add(new float[]{0, 29.5f * t, 220.5f * t, 243.5f * t});
        w.trafficLanes.add(new float[]{1, 236.5f * t, 12.5f * t, 47.5f * t});
        w.trafficLanes.add(new float[]{1, 237.5f * t, 12.5f * t, 47.5f * t});

        // لاین‌های شهر گلاب
        w.trafficLanes.add(new float[]{0, 122.5f * t, 177 * t, 251 * t});
        w.trafficLanes.add(new float[]{0, 155.5f * t, 177 * t, 251 * t});
        w.trafficLanes.add(new float[]{1, 176.5f * t, 123 * t, 155 * t});
        w.trafficLanes.add(new float[]{1, 251.5f * t, 123 * t, 155 * t});
        w.trafficLanes.add(new float[]{0, 138.5f * t, 178.5f * t, 249.5f * t});
        w.trafficLanes.add(new float[]{0, 139.5f * t, 178.5f * t, 249.5f * t});
        w.trafficLanes.add(new float[]{1, 210.5f * t, 124.5f * t, 153.5f * t});
        w.trafficLanes.add(new float[]{1, 211.5f * t, 124.5f * t, 153.5f * t});

        // ۳۶ خودرو در دنیای بزرگ
        int[] types = {Vehicle.CAR_SEDAN, Vehicle.CAR_TAXI, Vehicle.CAR_SPORT, Vehicle.CAR_PICKUP,
                       Vehicle.CAR_BUS, Vehicle.MOTOR};
        for (int i = 0; i < 36; i++) {
            Vehicle v = new Vehicle(types[i % types.length], 0, 0);
            w.randomLaneSpawn(v);
            w.vehicles.add(v);
        }

        // 🚓 دو ماشین پلیس که در شهر گشت می‌زنند
        for (int i = 0; i < 2; i++) {
            Vehicle police = new Vehicle(Vehicle.CAR_POLICE, 0, 0);
            w.randomLaneSpawn(police);
            w.vehicles.add(police);
        }

        // 🚌 دو اتوبوس بین‌شهری روی اتوبان
        Vehicle bus1 = new Vehicle(Vehicle.CAR_BUS, 170 * t, 57.5f * t);
        bus1.axis = 'H';
        bus1.laneDir = 1f;
        bus1.laneMin = 168 * t;
        bus1.laneMax = 257 * t;
        w.vehicles.add(bus1);
        Vehicle bus2 = new Vehicle(Vehicle.CAR_BUS, 250 * t, 58.5f * t);
        bus2.axis = 'H';
        bus2.laneDir = -1f;
        bus2.laneMin = 168 * t;
        bus2.laneMax = 257 * t;
        w.vehicles.add(bus2);

        // هلیکوپتر پارک‌شده در هلی‌پورت
        Building hp = w.buildingByType(Building.HELIPORT);
        if (hp != null) {
            Vehicle heli = new Vehicle(Vehicle.CAR_HELICOPTER, hp.doorX, hp.y + hp.h * 0.42f);
            heli.mode = Vehicle.MODE_PARKED;
            heli.angle = 0f;
            w.vehicles.add(heli);
        }

        // قایق عمومی اسکله — روی آب کنار اسکله
        Vehicle boat = new Vehicle(Vehicle.CAR_BOAT, 110.5f * t, 107f * t);
        boat.mode = Vehicle.MODE_PARKED;
        boat.angle = 0f;
        boat.owned = true;
        w.vehicles.add(boat);

        // 🐴 اسب‌های روستا و شهر گلاب (چرا می‌کنند، سوار شدن آزاد)
        float[][] horseSpots = {
            {17f, 108.5f}, {46f, 109f}, {35f, 113.5f},            // روستای اصلی
            {199f, 128.5f}, {188f, 136.5f}, {204f, 147f}          // شهر گلاب
        };
        for (float[] hs : horseSpots) {
            Vehicle horse = new Vehicle(Vehicle.CAR_HORSE, hs[0] * t, hs[1] * t);
            horse.mode = Vehicle.MODE_GRAZE;
            horse.owned = true;
            horse.angle = rnd.nextFloat() * (float) Math.PI * 2f;
            w.vehicles.add(horse);
        }

        // ✈ هواپیمای فرودگاه در حالت پارک
        w.planeX = 188f * t;
        w.planeY = 71.5f * t;
        w.planeAngle = 0f;
        w.planeState = 0;
    }

    // ---------------- شهروندها ----------------

    private static void buildNpcs(World w) {
        // شهر اصلی
        Npc.scatter(w.cityNpcs, w, 30, 84f * G.TILE, 60f * G.TILE, 60f * G.TILE);
        // روستا
        Npc.scatter(w.cityNpcs, w, 6, 30f * G.TILE, 108f * G.TILE, 8f * G.TILE);
        // شهر ستاره
        Npc.scatter(w.cityNpcs, w, 8, 232f * G.TILE, 30f * G.TILE, 10f * G.TILE);
        // شهر گلاب
        Npc.scatter(w.cityNpcs, w, 8, 213f * G.TILE, 139f * G.TILE, 12f * G.TILE);

        // ✅ دانش‌آموزها با کوله‌پشتی در حیاط هر دو مدرسه (درخواست کاربر)
        // مدرسه دانش (شهر اصلی) — جلوی حیاط
        Building school = w.buildingByType(Building.SCHOOL);
        if (school != null) {
            for (int i = 0; i < 4; i++) {
                float kx = school.doorX + (i - 1.5f) * 78f;
                float ky = school.doorY + 60f + (i % 2) * 46f;
                if (!w.isBlocked(kx, ky, 14f)) w.cityNpcs.add(Npc.schoolKid(kx, ky));
            }
        }
        // دبستان ستاره (شهر شرق)
        for (Building b : w.buildings) {
            if (b.type == Building.SCHOOL && b != school) {
                for (int i = 0; i < 3; i++) {
                    float kx = b.doorX + (i - 1f) * 70f;
                    float ky = b.doorY + 55f;
                    if (!w.isBlocked(kx, ky, 14f)) w.cityNpcs.add(Npc.schoolKid(kx, ky));
                }
            }
        }
    }
}
