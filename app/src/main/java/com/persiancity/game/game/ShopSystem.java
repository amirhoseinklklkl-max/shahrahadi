package com.persiancity.game.game;

import java.util.ArrayList;

/**
 * سیستم فروشگاه‌ها و خدمات شهر — منوهای خرید و امکانات
 */
public class ShopSystem {

    private final GameView view;
    private final World world;
    private final Player player;
    private final UIManager ui;

    // خوراکی‌ها: نام، قیمت، سیری، انرژی
    private static final Object[][] RESTAURANT_FOODS = {
            {"ساندویچ مانی", 250, 35f, 15f},
            {"پیتزا مخصوص", 450, 50f, 20f},
            {"سالاد تازه", 180, 25f, 25f},
            {"آب پرتقال", 100, 10f, 12f},
    };
    private static final Object[][] CAFE_FOODS = {
            {"شیرکاکائو", 120, 8f, 18f},
            {"دونات خوشمزه", 150, 18f, 8f},
            {"بستنی شکلاتی", 120, 12f, 8f},
            {"کیک خونمایی", 200, 20f, 10f},
    };
    private static final Object[][] MARKET_FOODS = {
            {"نان و پنیر", 90, 20f, 10f},
            {"میوه‌ی روز", 90, 15f, 10f},
            {"تن‌ماهی", 200, 30f, 12f},
            {"شکلات", 80, 8f, 6f},
    };

    public ShopSystem(GameView view) {
        this.view = view;
        this.world = view.world;
        this.player = view.player;
        this.ui = view.ui;
    }

    private boolean pay(int price) {
        if (player.money < price) {
            ui.toast("پول کافی نداری! (" + UIManager.faMoney(price) + " تومان لازمه)");
            view.fx("click");
            return false;
        }
        player.pay(price);
        view.fx("buy");
        return true;
    }

    // ================= منوی کلی ساختمان =================

    public void openBuildingMenu(Building b) {
        switch (b.type) {
            case Building.HOME: openHomeMenu(); break;
            case Building.CLOTHES: openClothesMenu(); break;
            case Building.BARBER: openBarberMenu(); break;
            case Building.CAFE: openFoodMenu("کافه شکلات", CAFE_FOODS, "یه چیزی بخوریم!"); break;
            case Building.CINEMA: openCinemaMenu(); break;
            case Building.HOSPITAL: openHospitalMenu(); break;
            case Building.BANK: openBankMenu(); break;
            case Building.TOYSTORE: openToyMenu(); break;
            case Building.CARSHOP: openCarShop(); break;
            case Building.BIKESHOP: openBikeShop(); break;
            case Building.GARAGE: openGarageMenu(); break;
            case Building.JOBCENTER: openJobCenter(); break;
            case Building.TAXISTAND: openTaxiStand(); break;
            case Building.RESTAURANT:
            case Building.MARKET:
                world.enterInterior(b, player);
                view.fx("door");
                break;
            default:
                ui.toast(b.flavorText());
                break;
        }
    }

    // ================= خانه =================

    private void openHomeMenu() {
        UIManager.Menu m = new UIManager.Menu("خانه ما — آخیش، رسیدم خونه!");
        m.items.add(new UIManager.MenuItem("خوابیدن تا صبح", "انرژی پر می‌شه و روز بعد شروع می‌شه",
                () -> {
                    view.sleepUntilMorning();
                    ui.closeMenu();
                }));
        m.items.add(new UIManager.MenuItem("میان‌وعده خوردن", "۵۰ تومان — کمی سیر و سرحال می‌شی",
                () -> {
                    if (pay(50)) {
                        player.eat(15f, 10f);
                        view.fx("eat");
                        ui.toast("وای چه خوشمزه!");
                    }
                }));
        m.items.add(new UIManager.MenuItem("آوردن خودروها جلوی خانه", "همه ماشین و موتورت اینجا پارک می‌شن",
                () -> {
                    view.teleportVehiclesHome();
                    ui.toast("خودروها جلوی خونه پارک شدن!");
                    ui.closeMenu();
                }));
        m.items.add(new UIManager.MenuItem("ذخیره بازی", "پیشرفتت محفوظ می‌مونه",
                () -> {
                    view.saveGame();
                    ui.toast("بازی ذخیره شد!");
                    ui.closeMenu();
                }));
        ui.openMenu(m);
    }

    // ================= فروشگاه لباس =================

    private void openClothesMenu() {
        UIManager.Menu m = new UIManager.Menu("فروشگاه شیک‌پوش — چه مدلی دوست داری؟");
        for (int i = 0; i < SpriteLib.OUTFITS.length; i++) {
            final int idx = i;
            boolean owned = view.ownedOutfits[idx];
            String sub = owned ? "مال خودته!" : UIManager.faMoney(SpriteLib.OUTFIT_PRICES[i]) + " تومان";
            m.items.add(new UIManager.MenuItem(SpriteLib.OUTFIT_NAMES[i], sub, () -> {
                if (player.outfit == idx) {
                    ui.toast("همین الان اینو پوشیدی!");
                    return;
                }
                if (view.ownedOutfits[idx]) {
                    player.outfit = idx;
                    view.fx("click");
                    ui.toast("پوشیدی: " + SpriteLib.OUTFIT_NAMES[idx]);
                } else if (pay(SpriteLib.OUTFIT_PRICES[idx])) {
                    view.ownedOutfits[idx] = true;
                    player.outfit = idx;
                    ui.toast("خریدی و پوشیدی: " + SpriteLib.OUTFIT_NAMES[idx] + " — چه خوشگل شدی!");
                }
            }));
        }
        ui.openMenu(m);
    }

    // ================= آرایشگاه =================

    private void openBarberMenu() {
        UIManager.Menu m = new UIManager.Menu("آرایشگاه آفتاب — موهات رو عوض کنیم؟");
        for (int i = 0; i < SpriteLib.HAIR_NAMES.length; i++) {
            final int idx = i;
            String sub = (i == 0) ? "مدل پیش‌فرض" : UIManager.faMoney(SpriteLib.HAIR_PRICES[i]) + " تومان";
            m.items.add(new UIManager.MenuItem(SpriteLib.HAIR_NAMES[i], sub, () -> {
                if (player.hair == idx) {
                    ui.toast("موهات همین الان همین مدله!");
                    return;
                }
                if (pay(SpriteLib.HAIR_PRICES[idx])) {
                    player.hair = idx;
                    ui.toast("مدل جدید مو: " + SpriteLib.HAIR_NAMES[idx] + " — عالی شد!");
                }
            }));
        }
        ui.openMenu(m);
    }

    // ================= غذا =================

    private void openFoodMenu(String title, Object[][] foods, String subtitle) {
        UIManager.Menu m = new UIManager.Menu(title + " — " + subtitle);
        for (Object[] f : foods) {
            final String name = (String) f[0];
            final int price = (Integer) f[1];
            final float hunger = (Float) f[2];
            final float energy = (Float) f[3];
            m.items.add(new UIManager.MenuItem(name, UIManager.faMoney(price) + " تومان", () -> {
                if (pay(price)) {
                    player.eat(hunger, energy);
                    view.fx("eat");
                    ui.toast(name + " خوردی! نوش جان!");
                }
            }));
        }
        ui.openMenu(m);
    }

    // ================= سینما =================

    private void openCinemaMenu() {
        UIManager.Menu m = new UIManager.Menu("سینما ستاره — فیلم امروز: «ماشین‌های بامزه»");
        m.items.add(new UIManager.MenuItem("بلیت تماشای فیلم", "۳۰۰ تومان — دو ساعت می‌گذره",
                () -> {
                    if (pay(300)) {
                        player.eat(0f, 18f);
                        world.dayNight.setTime((world.dayNight.getHour() + 2f) * 60f + world.dayNight.getMinute());
                        view.fx("success");
                        ui.toast("چه فیلم باحالی! حال دلت خوب شد.");
                        ui.closeMenu();
                    }
                }));
        ui.openMenu(m);
    }

    // ================= بیمارستان =================

    private void openHospitalMenu() {
        UIManager.Menu m = new UIManager.Menu("بیمارستان مهربانی — پرستارها مهربونن!");
        m.items.add(new UIManager.MenuItem("معاینه و استراحت", "۲۰۰ تومان — انرژی‌ت پر می‌شه",
                () -> {
                    if (player.energy >= 99f) {
                        ui.toast("کاملاً سرحالی! نیازی نیست.");
                        return;
                    }
                    if (pay(200)) {
                        player.energy = 100f;
                        view.fx("success");
                        ui.toast("حالا انگار نو شدی! سرحالی.");
                        ui.closeMenu();
                    }
                }));
        ui.openMenu(m);
    }

    // ================= بانک =================

    private void openBankMenu() {
        UIManager.Menu m = new UIManager.Menu("بانک سپهر — موجودی: " + UIManager.faMoney(player.money) + " تومان");
        if (view.lastAllowanceDay < world.dayNight.dayCount) {
            m.items.add(new UIManager.MenuItem("دریافت پول جیبی امروز", "۳۰۰ تومان — هر روز یکبار",
                    () -> {
                        view.lastAllowanceDay = world.dayNight.dayCount;
                        player.earn(300);
                        view.fx("coin");
                        ui.toast("پول جیبی امروز: +۳۰۰ تومان!");
                        ui.closeMenu();
                    }));
        } else {
            m.items.add(new UIManager.MenuItem("پول جیبی امروز را گرفتی", "فردا دوباره بیا!", null));
        }
        ui.openMenu(m);
    }

    // ================= اسباب‌بازی =================

    private void openToyMenu() {
        UIManager.Menu m = new UIManager.Menu("فروشگاه فرفره — چه اسباب‌بازی باحالی!");
        String[][] toys = {
                {"توپ رنگی", "200"},
                {"عروسک خرگوش", "500"},
                {"ماشین کنترلی", "1200"},
                {"کیت ربات کوچک", "900"},
        };
        for (String[] t : toys) {
            final String name = t[0];
            final int price = Integer.parseInt(t[1]);
            m.items.add(new UIManager.MenuItem(name, UIManager.faMoney(price) + " تومان", () -> {
                if (pay(price)) {
                    player.eat(0f, 6f);
                    view.toysOwned++;
                    view.fx("success");
                    ui.toast(name + " خریدی! حالا " + G.fa(view.toysOwned) + " تا اسباب‌بازی داری!");
                }
            }));
        }
        ui.openMenu(m);
    }

    // ================= نمایشگاه ماشین =================

    private void openCarShop() {
        UIManager.Menu m = new UIManager.Menu("نمایشگاه تندر — ماشین‌های امروزی");
        for (int i = 0; i <= 4; i++) {
            final int model = i;
            Vehicle.Model mod = Vehicle.MODELS[i];
            m.items.add(new UIManager.MenuItem(mod.name + " — " + UIManager.faMoney(mod.price) + " تومان",
                    mod.desc, () -> buyVehicle(model)));
        }
        ui.openMenu(m);
    }

    private void openBikeShop() {
        UIManager.Menu m = new UIManager.Menu("موتور برق‌وباد — با باد برق بزن!");
        for (int i = 5; i <= 7; i++) {
            final int model = i;
            Vehicle.Model mod = Vehicle.MODELS[i];
            m.items.add(new UIManager.MenuItem(mod.name + " — " + UIManager.faMoney(mod.price) + " تومان",
                    mod.desc, () -> buyVehicle(model)));
        }
        ui.openMenu(m);
    }

    private void buyVehicle(int model) {
        // اگر همین مدل را داری
        for (Vehicle v : world.vehicles) {
            if (v.model == model && v.mode != Vehicle.MODE_TRAFFIC) {
                ui.toast("یکی از اینو داری! برو پارکینگ کنارش.");
                return;
            }
        }
        Vehicle.Model mod = Vehicle.MODELS[model];
        if (!pay(mod.price)) return;

        Building shop = world.buildingByType(model >= 5 ? Building.BIKESHOP : Building.CARSHOP);
        float[] spot = findFreeSpotNear(shop.doorX, shop.doorY);
        if (spot == null) spot = new float[]{shop.doorX, shop.doorY + G.TILE};
        Vehicle v = new Vehicle(model, spot[0], spot[1]);
        v.angle = spot.length > 2 ? (float) Math.toRadians(spot[2]) : 0f;
        world.vehicles.add(v);
        ui.toast(mod.name + " خریدی! جلوی نمایشگاه منتظرته.");
        view.fx("success");
    }

    private float[] findFreeSpotNear(float x, float y) {
        ArrayList<float[]> sorted = new ArrayList<>(world.parkingSpots);
        sorted.sort((a, b) -> Float.compare(G.dist(a[0], a[1], x, y), G.dist(b[0], b[1], x, y)));
        for (float[] s : sorted) {
            boolean free = true;
            for (Vehicle v : world.vehicles) {
                if (v.mode != Vehicle.MODE_TRAFFIC && G.dist(v.x, v.y, s[0], s[1]) < 120f) {
                    free = false;
                    break;
                }
            }
            if (free) return s;
        }
        return null;
    }

    // ================= گاراژ تیونینگ =================

    private void openGarageMenu() {
        // آیا ماشین یا موتوری داری؟
        Vehicle mine = view.activeOwnedVehicle();
        if (mine == null) {
            ui.toast("اول از نمایشگاه یه ماشین یا موتور بخر، بعد بیا تیونینگ!");
            return;
        }
        UIManager.Menu m = new UIManager.Menu("گاراژ توربو — تیونینگ " + mine.displayName());

        // رنگ
        for (int i = 0; i < SpriteLib.CAR_COLORS.length; i++) {
            final int colorIdx = i;
            m.items.add(new UIManager.MenuItem("رنگ‌آمیزی " + SpriteLib.CAR_COLOR_NAMES[i], "۵۰۰ تومان",
                    () -> {
                        if (pay(500)) {
                            Vehicle v = view.activeOwnedVehicle();
                            if (v != null) {
                                v.paint = SpriteLib.CAR_COLORS[colorIdx];
                                ui.toast("وای چه رنگ قشنگی!");
                            }
                        }
                    }));
        }

        // موتور اسپرت
        if (mine.engineLevel < 3) {
            final int price = 1500 + mine.engineLevel * 1500;
            m.items.add(new UIManager.MenuItem("موتور اسپرت (سطح " + G.fa(mine.engineLevel + 1) + ")",
                    UIManager.faMoney(price) + " تومان — سرعت بیشتر",
                    () -> {
                        Vehicle v = view.activeOwnedVehicle();
                        if (v != null && v.engineLevel < 3 && pay(price)) {
                            v.engineLevel++;
                            view.fx("success");
                            ui.toast("موتورش خفن شد! سطح " + G.fa(v.engineLevel));
                        }
                    }));
        } else {
            m.items.add(new UIManager.MenuItem("موتور: حداکثر اسپرت", "دیگه جا نداره!", null));
        }

        // بال اسپرت
        if (!mine.spoiler) {
            m.items.add(new UIManager.MenuItem("نصب بال اسپرت", "۲۰۰۰ تومان — ظاهر خفن",
                    () -> {
                        Vehicle v = view.activeOwnedVehicle();
                        if (v != null && !v.spoiler && pay(2000)) {
                            v.spoiler = true;
                            view.fx("success");
                            ui.toast("بال اسپرت وصل شد! خفن شدی.");
                        }
                    }));
        }

        // نئون
        if (mine.neonColor == 0) {
            m.items.add(new UIManager.MenuItem("نصب نور نئون", "۱۲۰۰ تومان — شب می‌درخشه",
                    () -> {
                        Vehicle v = view.activeOwnedVehicle();
                        if (v != null && v.neonColor == 0 && pay(1200)) {
                            v.neonColor = 0xFF00E5FF;
                            view.fx("success");
                            ui.toast("نئون آبی نصب شد! شب ببینش!");
                        }
                    }));
        } else {
            m.items.add(new UIManager.MenuItem("برداشتن نور نئون", "مجانی", () -> {
                Vehicle v = view.activeOwnedVehicle();
                if (v != null) {
                    v.neonColor = 0;
                    ui.toast("نئون برداشته شد.");
                }
            }));
        }

        ui.openMenu(m);
    }

    // ================= اداره مشاغل =================

    private void openJobCenter() {
        UIManager.Menu m = new UIManager.Menu("اداره مشاغل کارینا — امروز چه‌کار کنیم؟");

        if (view.jobs.activeJob == JobSystem.JOB_TAXI) {
            m.items.add(new UIManager.MenuItem("پایان کار تاکسی", "شیفت فعلی تمام می‌شه", () -> {
                view.jobs.stopJob();
                ui.toast("شیفت تاکسی تموم شد. ممنون!");
            }));
        } else {
            m.items.add(new UIManager.MenuItem("شغل تاکسی رانی", "ماشین لازم داری — مسافر ببر، پول بگیر",
                    () -> {
                        if (view.jobs.startTaxi(player)) {
                            ui.toast("حالت تاکسی فعال شد! سوار ماشین شو.");
                            ui.closeMenu();
                        } else {
                            ui.toast("اول از نمایشگاه «تندر» یه ماشین بخر!");
                        }
                    }));
        }

        if (view.jobs.activeJob == JobSystem.JOB_COURIER) {
            m.items.add(new UIManager.MenuItem("پایان کار پیک", "شیفت فعلی تمام می‌شه", () -> {
                view.jobs.stopJob();
                ui.toast("شیفت پیک تموم شد. خسته نباشی!");
            }));
        } else {
            m.items.add(new UIManager.MenuItem("شغل پیک موتوری", "موتور لازم داری — بسته برسون",
                    () -> {
                        if (view.jobs.startCourier(player)) {
                            ui.toast("حالت پیک فعال شد! برو سراغ بسته‌ها.");
                            ui.closeMenu();
                        } else {
                            ui.toast("اول از «موتور برق‌وباد» یه موتور بخر!");
                        }
                    }));
        }

        m.items.add(new UIManager.MenuItem("شغل گارسونی", "برو داخل «رستوران زنجبیل» با مدیر حرف بزن", null));
        m.items.add(new UIManager.MenuItem("شغل فروشندگی", "برو داخل «سوپرمارکت فراوان» با مدیر حرف بزن", null));
        ui.openMenu(m);
    }

    private void openTaxiStand() {
        UIManager.Menu m = new UIManager.Menu("ایستگاه تاکسی");
        if (view.jobs.activeJob == JobSystem.JOB_TAXI) {
            m.items.add(new UIManager.MenuItem("پایان شیفت تاکسی", "استراحت کن!", () -> {
                view.jobs.stopJob();
                ui.toast("شیفت تموم شد!");
            }));
        } else {
            m.items.add(new UIManager.MenuItem("شروع شیفت تاکسی", "ماشین لازم داری",
                    () -> {
                        if (view.jobs.startTaxi(player)) {
                            ui.toast("سوار ماشین شو و منتظر مسافر بمون!");
                            ui.closeMenu();
                        } else {
                            ui.toast("بدون ماشین نمی‌شه تاکسی راند! از نمایشگاه بخر.");
                        }
                    }));
        }
        ui.openMenu(m);
    }

    // ================= منوی مدیران داخل مغازه =================

    public void openManagerMenu(Npc manager) {
        if (manager.role == Npc.ROLE_RESTAURANT_MANAGER) {
            UIManager.Menu m = new UIManager.Menu("مدیر رستوران زنجبیل");
            if (view.jobs.activeJob == JobSystem.JOB_WAITER) {
                m.items.add(new UIManager.MenuItem("پایان شیفت گارسونی", "شیفت تمام می‌شه", () -> {
                    view.jobs.stopJob();
                    ui.toast("ممنون از کارت! شیفت تموم شد.");
                }));
            } else {
                m.items.add(new UIManager.MenuItem("شروع شیفت گارسونی", "۳ مشتری را خدمت بده",
                        () -> {
                            view.jobs.startWaiter();
                            ui.toast("مشتری اول اومد! برو سفارش بگیر.");
                            ui.closeMenu();
                        }));
            }
            m.items.add(new UIManager.MenuItem("سفارش غذا", "یه چیزی بخوریم!",
                    () -> openFoodMenu("رستوران زنجبیل", RESTAURANT_FOODS, "سفارش بده")));
            ui.openMenu(m);
        } else if (manager.role == Npc.ROLE_MARKET_MANAGER) {
            UIManager.Menu m = new UIManager.Menu("مدیر سوپرمارکت فراوان");
            if (view.jobs.activeJob == JobSystem.JOB_SHOPKEEPER) {
                m.items.add(new UIManager.MenuItem("پایان شیفت فروشندگی", "شیفت تمام می‌شه", () -> {
                    view.jobs.stopJob();
                    ui.toast("ممنون! شیفت تموم شد.");
                }));
            } else {
                m.items.add(new UIManager.MenuItem("شروع شیفت فروشندگی", "۴ مشتری را خدمت بده",
                        () -> {
                            view.jobs.startShopkeeper();
                            ui.toast("مشتری اول داره میاد!");
                            ui.closeMenu();
                        }));
            }
            m.items.add(new UIManager.MenuItem("خرید خوراکی", "از قفسه‌های فروشگاه",
                    () -> openFoodMenu("سوپرمارکت فراوان", MARKET_FOODS, "بخر و بخور")));
            ui.openMenu(m);
        }
    }
}
