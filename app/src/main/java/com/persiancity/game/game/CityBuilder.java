package com.persiancity.game.game;

import java.util.ArrayList;
import java.util.Random;

/**
 * سازنده شهر بزرگ «شهر شادی»
 * جاده کمربندی + شبکه خیابان‌ها + ۲۰+ ساختمان + پارک بزرگ
 */
public class CityBuilder {

    private final World w;
    private final Random rnd = new Random(42);

    public CityBuilder(World world) {
        this.w = world;
    }

    public void build() {
        // ۱) چمن پایه
        for (int y = 0; y < G.MAP_H; y++) {
            for (int x = 0; x < G.MAP_W; x++) {
                w.tileType[y][x] = World.T_GRASS;
            }
        }

        // ۲) جاده‌ها
        int[][] vRoads = {{4, 6}, {22, 24}, {40, 42}, {58, 60}, {76, 78}, {94, 96}};
        int[][] hRoads = {{4, 6}, {18, 20}, {32, 34}, {46, 48}, {60, 62}, {72, 74}};

        for (int[] r : vRoads) {
            for (int y = 0; y < G.MAP_H; y++) {
                for (int x = r[0]; x <= r[1]; x++) {
                    w.tileType[y][x] = World.T_ROAD;
                    w.roadAxis[y][x] |= 2;
                }
            }
        }
        for (int[] r : hRoads) {
            for (int y = r[0]; y <= r[1]; y++) {
                for (int x = 0; x < G.MAP_W; x++) {
                    w.tileType[y][x] = World.T_ROAD;
                    w.roadAxis[y][x] |= 1;
                }
            }
        }

        // ۳) پیاده‌رو کنار جاده‌ها
        for (int y = 1; y < G.MAP_H - 1; y++) {
            for (int x = 1; x < G.MAP_W - 1; x++) {
                if (w.tileType[y][x] == World.T_ROAD) continue;
                boolean near = w.tileType[y - 1][x] == World.T_ROAD || w.tileType[y + 1][x] == World.T_ROAD
                        || w.tileType[y][x - 1] == World.T_ROAD || w.tileType[y][x + 1] == World.T_ROAD;
                if (near) w.tileType[y][x] = World.T_SIDEWALK;
            }
        }

        // ۴) پارک بزرگ (وسط شهر، دو محله)
        paintPark(43, 21, 57, 45);

        // ۵) ساختمان‌ها
        buildBuildings();

        // ۶) درخت‌ها و تزئینات
        placeTrees();

        // ۷) چراغ خیابان
        placeStreetlights(vRoads, hRoads);

        // ۸) جای پارک خودرو
        placeParking();

        // ۹) ساکنان شهر
        spawnCityNpcs();

        // ۱۰) ترافیک شهری
        spawnTraffic(vRoads, hRoads);
    }

    private void paintPark(int x0, int y0, int x1, int y1) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                w.tileType[y][x] = World.T_PARK;
            }
        }
        // مسیرهای پارک
        for (int y = y0; y <= y1; y++) w.tileType[y][50] = World.T_PATH;
        for (int x = x0; x <= x1; x++) w.tileType[33][x] = World.T_PATH;

        // حوض و فواره وسط پارک
        w.fountainX = 50.5f * G.TILE;
        w.fountainY = 26.5f * G.TILE;
        for (int y = 25; y <= 28; y++) {
            for (int x = 49; x <= 51; x++) {
                w.tileType[y][x] = World.T_WATER;
                w.solid[y][x] = true;
            }
        }
    }

    private void addB(int x, int y, int bw, int bh, int type, String name,
                      int wall, int roof, int accent, boolean doorUp) {
        Building b = new Building(x, y, bw, bh, type, name, wall, roof, accent, doorUp);
        w.buildings.add(b);
        for (int ty = y; ty < y + bh; ty++) {
            for (int tx = x; tx < x + bw; tx++) {
                w.tileType[ty][tx] = World.T_BUILDING;
                w.solid[ty][tx] = true;
            }
        }
    }

    private void buildBuildings() {
        // ---- ردیف بالا (شمال) ----
        addB(8, 8, 6, 6, Building.HOME, "خانه ما", 0xFFFFF3E0, 0xFFEF5350, 0xFFFFCA28, false);
        addB(15, 8, 5, 6, Building.HOUSE, "خانه همسایه", 0xFFE1F5FE, 0xFF8D6E63, 0xFF90CAF9, false);
        addB(26, 8, 7, 6, Building.CLOTHES, "فروشگاه لباس شیک‌پوش", 0xFFF8BBD0, 0xFFAD1457, 0xFFFF80AB, false);
        addB(34, 8, 5, 6, Building.HOUSE, "خانه", 0xFFFFF9C4, 0xFF8D6E63, 0xFFFFD54F, false);
        addB(44, 8, 6, 6, Building.BARBER, "آرایشگاه آفتاب", 0xFFB3E5FC, 0xFF0277BD, 0xFFE53935, false);
        addB(51, 8, 5, 6, Building.HOUSE, "خانه", 0xFFFFCCBC, 0xFF6D4C41, 0xFFFF8A65, false);
        addB(62, 8, 6, 6, Building.CAFE, "کافه شکلات", 0xFFFFE0B2, 0xFFBF360C, 0xFF8D6E63, false);
        addB(69, 8, 5, 6, Building.HOUSE, "خانه", 0xFFDCEDC8, 0xFF5D4037, 0xFFAED581, false);
        addB(80, 8, 8, 6, Building.CINEMA, "سینما ستاره", 0xFFE1BEE7, 0xFF6A1B9A, 0xFFFFD54F, false);
        addB(89, 8, 4, 6, Building.HOUSE, "خانه", 0xFFB2DFDB, 0xFF00695C, 0xFF80CBC4, false);

        // ---- ردیف دوم ----
        addB(8, 22, 9, 6, Building.SCHOOL, "مدرسه دانش", 0xFFFFF59D, 0xFF33691E, 0xFFAFB42B, false);
        addB(18, 22, 3, 6, Building.GAS, "پمپ بنزین برق", 0xFFFFF3E0, 0xFFEF6C00, 0xFFFFB74D, false);
        addB(26, 22, 8, 6, Building.RESTAURANT, "رستوران زنجبیل", 0xFFFFE0B2, 0xFFE64A19, 0xFFFFAB91, false);
        addB(35, 22, 4, 6, Building.HOUSE, "خانه", 0xFFE1BEE7, 0xFF7B1FA2, 0xFFCE93D8, false);
        addB(62, 22, 8, 6, Building.MARKET, "سوپرمارکت فراوان", 0xFFDCEDC8, 0xFF558B2F, 0xFF9CCC65, false);
        addB(71, 22, 4, 6, Building.HOUSE, "خانه", 0xFFFFF9C4, 0xFF8D6E63, 0xFFFFD54F, false);
        addB(80, 22, 9, 6, Building.CARSHOP, "نمایشگاه ماشین تندر", 0xFFB0BEC5, 0xFF37474F, 0xFF29B6F6, false);

        // ---- ردیف سوم ----
        addB(8, 36, 8, 6, Building.HOSPITAL, "بیمارستان مهربانی", 0xFFFFFFFF, 0xFFE53935, 0xFFEF9A9A, false);
        addB(17, 36, 4, 6, Building.HOUSE, "خانه", 0xFFFFCCBC, 0xFF6D4C41, 0xFFFF8A65, false);
        addB(26, 36, 7, 6, Building.BANK, "بانک سپهر", 0xFFFFF8E1, 0xFFC9A227, 0xFFFFD54F, false);
        addB(34, 36, 5, 6, Building.HOUSE, "خانه", 0xFFDCEDC8, 0xFF4E342E, 0xFFAED581, false);
        addB(62, 36, 7, 6, Building.GARAGE, "گاراژ تیونینگ توربو", 0xFFCFD8DC, 0xFF455A64, 0xFFFF7043, false);
        addB(70, 36, 5, 6, Building.HOUSE, "خانه", 0xFFB3E5FC, 0xFF546E7A, 0xFF81D4FA, false);
        addB(80, 36, 8, 6, Building.BIKESHOP, "موتور برق‌وباد", 0xFFD1C4E9, 0xFF512DA8, 0xFFB388FF, false);

        // ---- ردیف چهارم ----
        addB(8, 50, 8, 6, Building.JOBCENTER, "اداره مشاغل کارینا", 0xFFB2DFDB, 0xFF00695C, 0xFF80CBC4, false);
        addB(17, 50, 4, 6, Building.HOUSE, "خانه", 0xFFFFF9C4, 0xFF6D4C41, 0xFFFFD54F, false);
        addB(26, 50, 8, 6, Building.TOYSTORE, "فروشگاه فرفره", 0xFFFFF176, 0xFFF9A825, 0xFFFF6F00, false);
        addB(35, 50, 4, 6, Building.HOUSE, "خانه", 0xFFE1F5FE, 0xFF455A64, 0xFF90CAF9, false);
        addB(44, 50, 6, 5, Building.TAXISTAND, "ایستگاه تاکسی", 0xFFFFECB3, 0xFFFFB300, 0xFF212121, false);
        addB(52, 50, 4, 4, Building.KIOSK, "دکه روزنامه", 0xFFFFAB91, 0xFFD84315, 0xFFFFCCBC, false);
        addB(62, 50, 7, 6, Building.POLICE, "پلیس‌خانه امنیت", 0xFFE3F2FD, 0xFF1565C0, 0xFF64B5F6, false);
        addB(70, 50, 5, 6, Building.HOUSE, "خانه", 0xFFFFCCBC, 0xFF5D4037, 0xFFFF8A65, false);
        addB(80, 50, 9, 8, Building.STADIUM, "ورزشگاه تلاش", 0xFFECEFF1, 0xFF607D8B, 0xFF4CAF50, false);
        addB(90, 50, 3, 6, Building.LIBRARY, "کتابخانه کتاب", 0xFFFFF8E1, 0xFF795548, 0xFFBCAAA4, false);

        // ---- ردیف پایین (در رو به شمال) ----
        addB(8, 66, 5, 5, Building.HOUSE, "خانه", 0xFFDCEDC8, 0xFF8D6E63, 0xFFAED581, true);
        addB(14, 66, 5, 5, Building.HOUSE, "خانه", 0xFFFFF9C4, 0xFF795548, 0xFFFFD54F, true);
        addB(26, 66, 6, 5, Building.HOUSE, "خانه", 0xFFB2DFDB, 0xFF00695C, 0xFF80CBC4, true);
        addB(44, 66, 8, 5, Building.MOSQUE, "مسجد نور", 0xFFE0F2F1, 0xFF00897B, 0xFFFFD54F, true);
        addB(61, 66, 5, 5, Building.HOUSE, "خانه", 0xFFE1BEE7, 0xFF6A1B9A, 0xFFCE93D8, true);
        addB(67, 66, 5, 5, Building.HOUSE, "خانه", 0xFFFFCCBC, 0xFF8D6E63, 0xFFFF8A65, true);
        addB(80, 66, 6, 5, Building.HOUSE, "خانه", 0xFFDCEDC8, 0xFF5D4037, 0xFFAED581, true);
        addB(87, 66, 5, 5, Building.HOUSE, "خانه", 0xFFFFF9C4, 0xFF6D4C41, 0xFFFFD54F, true);
    }

    private void placeTree(int tx, int ty) {
        if (tx < 1 || ty < 1 || tx >= G.MAP_W - 1 || ty >= G.MAP_H - 1) return;
        if (w.solid[ty][tx]) return;
        if (w.tileType[ty][tx] != World.T_GRASS && w.tileType[ty][tx] != World.T_PARK) return;
        w.solid[ty][tx] = true;
        w.trees.add(new float[]{tx * G.TILE + G.TILE / 2f, ty * G.TILE + G.TILE / 2f,
                0.8f + rnd.nextFloat() * 0.5f});
    }

    private void placeTrees() {
        // دیوار درختی دور شهر (بعد از جاده کمربندی)
        for (int x = 0; x < G.MAP_W; x += 2) {
            placeTree(x, 1);
            placeTree(x + 1, 2);
            placeTree(x, 76);
            placeTree(x + 1, 75);
        }
        for (int y = 0; y < G.MAP_H; y += 2) {
            placeTree(1, y);
            placeTree(2, y + 1);
            placeTree(98, y);
            placeTree(97, y + 1);
        }

        // درخت‌های محله‌ها (دستچین تا جلوی درها را نگیرند)
        int[][] spots = {
                {8, 16}, {12, 16}, {19, 15}, {27, 16}, {37, 16}, {45, 16}, {53, 16}, {63, 16}, {73, 16}, {81, 16}, {90, 16},
                {8, 30}, {13, 30}, {28, 30}, {37, 30}, {63, 30}, {73, 30}, {90, 30},
                {8, 44}, {13, 44}, {28, 44}, {38, 44}, {63, 44}, {72, 44}, {90, 44},
                {8, 58}, {13, 58}, {28, 58}, {38, 58}, {54, 57}, {63, 58}, {72, 58}, {91, 59},
                {9, 64}, {20, 64}, {27, 64}, {38, 64}, {54, 64}, {62, 64}, {74, 64}, {81, 64}, {93, 64},
                // پارک
                {44, 23}, {46, 30}, {56, 23}, {55, 30}, {44, 38}, {47, 44}, {56, 38}, {53, 44}, {44, 42},
        };
        for (int[] s : spots) placeTree(s[0], s[1]);
    }

    private void placeStreetlights(int[][] vRoads, int[][] hRoads) {
        for (int[] r : hRoads) {
            float ly = (r[0] - 1) * G.TILE + G.TILE / 2f;
            for (int x = 10; x < G.MAP_W - 8; x += 9) {
                w.streetlights.add(new float[]{x * G.TILE + G.TILE / 2f, ly});
            }
        }
        for (int[] r : vRoads) {
            float lx = (r[0] - 1) * G.TILE + G.TILE / 2f;
            for (int y = 12; y < G.MAP_H - 8; y += 11) {
                w.streetlights.add(new float[]{lx, y * G.TILE + G.TILE / 2f});
            }
        }
    }

    private void placeParking() {
        addSpot(90, 23, -90);
        addSpot(92, 23, -90);
        addSpot(90, 26, -90);
        addSpot(90, 38, -90);
        addSpot(92, 38, -90);
        addSpot(90, 41, -90);
        // جلوی خانه ما
        addSpot(9, 16, 0);
        addSpot(12, 16, 0);
        // جلوی رستوران و سوپرمارکت
        addSpot(27, 30, 0);
        addSpot(31, 30, 0);
        addSpot(64, 30, 0);
        addSpot(67, 30, 0);
        // جلوی گاراژ
        addSpot(63, 44, 0);
        addSpot(66, 44, 0);
        // ایستگاه تاکسی
        addSpot(45, 57, 0);
        addSpot(48, 57, 0);
    }

    private void addSpot(int tx, int ty, float angleDeg) {
        if (w.solid[ty][tx]) return;
        w.parkingSpots.add(new float[]{tx * G.TILE + G.TILE / 2f, ty * G.TILE + G.TILE / 2f, angleDeg});
    }

    private void spawnCityNpcs() {
        String[] names = {
                "آرش", "سارا", "نیما", "الهام", "باباحیدر", "خانم رحیمی", "پارسا", "مریم",
                "کیان", "درسا", "امیر", "شیوا", "عمو رضا", "خاله مینا", "سینا", "هستی",
                "بردیا", "نگار", "فرهاد", "یاسمن", "کاوه", "ترانه", "مهدی", "پریسا",
                "آیدا", "رامین", "گلنار", "سهیل", "مینا", "بهنام"
        };
        int count = 0;
        for (int tries = 0; tries < 4000 && count < 26; tries++) {
            int tx = rnd.nextInt(G.MAP_W);
            int ty = rnd.nextInt(G.MAP_H);
            int t = w.tileType[ty][tx];
            if ((t == World.T_SIDEWALK || t == World.T_PARK || t == World.T_PATH) && !w.solid[ty][tx]) {
                Npc n = new Npc(tx * G.TILE + G.TILE / 2f, ty * G.TILE + G.TILE / 2f,
                        names[count % names.length], Npc.ROLE_CITIZEN, rnd);
                w.cityNpcs.add(n);
                count++;
            }
        }
    }

    private void spawnTraffic(int[][] vRoads, int[][] hRoads) {
        for (int i = 0; i < 8; i++) {
            int model = rnd.nextInt(5);
            if (i % 2 == 0) {
                // جاده افقی
                int[] r = hRoads[rnd.nextInt(hRoads.length)];
                float cy = (r[0] + 1.5f) * G.TILE;
                int sign = rnd.nextBoolean() ? 1 : -1;
                float x = (8 + rnd.nextInt(80)) * G.TILE;
                Vehicle v = new Vehicle(model, x, cy + sign * G.TILE * 0.55f);
                v.angle = sign > 0 ? 0f : (float) Math.PI;
                v.mode = Vehicle.MODE_TRAFFIC;
                v.trafficAxis = 0;
                v.trafficSign = sign;
                w.vehicles.add(v);
            } else {
                // جاده عمودی
                int[] r = vRoads[rnd.nextInt(vRoads.length)];
                float cx = (r[0] + 1.5f) * G.TILE;
                int sign = rnd.nextBoolean() ? 1 : -1;
                float y = (8 + rnd.nextInt(60)) * G.TILE;
                Vehicle v = new Vehicle(model, cx - sign * G.TILE * 0.55f, y);
                v.angle = sign > 0 ? (float) Math.PI / 2f : (float) -Math.PI / 2f;
                v.mode = Vehicle.MODE_TRAFFIC;
                v.trafficAxis = 1;
                v.trafficSign = sign;
                w.vehicles.add(v);
            }
        }
    }
}
