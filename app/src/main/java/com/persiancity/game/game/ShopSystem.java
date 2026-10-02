package com.persiancity.game.game;

import com.persiancity.game.SoundManager;

/**
 * فروشگاه‌ها و منوهای ساختمان‌ها
 */
public class ShopSystem {
    private static final Object[][] RESTAURANT_FOODS = {
        {"چلوکباب", 250, 45f, 30f},
        {"پیتزا مخصوص", 180, 35f, 25f},
        {"همبرگر", 120, 28f, 20f},
        {"سالاد فصل", 80, 18f, 15f}
    };
    private static final Object[][] CAFE_FOODS = {
        {"شکلات داغ", 60, 10f, 22f},
        {"کیک شکلاتی", 90, 20f, 18f},
        {"بستنی توت‌فرنگی", 70, 15f, 15f}
    };
    private static final Object[][] MARKET_FOODS = {
        {"سیب قرمز", 30, 12f, 8f},
        {"آب‌میوه پرتقال", 40, 10f, 14f},
        {"نان و شکلات", 50, 18f, 10f},
        {"ساندویچ", 80, 26f, 16f}
    };
    private static final Object[][] TOYS = {
        {"🧸 عروسک خرس", 300},
        {"🚗 ماشین کنترلی", 450},
        {"🧩 لگو رنگی", 600}
    };

    private static final int CINEMA_TICKET = 500;
    private static final int ZOO_TICKET = 300;
    private static final int HELI_PRICE = 60000;
    private static final int TRAIN_PRICE = 50000;
    private static final int TRAIN_TICKET = 200;

    private final GameView view;
    private final World world;
    private final Player player;
    private final UIManager ui;

    public ShopSystem(GameView view, World world, Player player, UIManager ui) {
        this.view = view;
        this.world = world;
        this.player = player;
        this.ui = ui;
    }

    public boolean pay(int price) {
        if (player.money < price) {
            ui.toast("پول کافی نداری! برو کار کن 💪");
            return false;
        }
        player.addMoney(-price);
        SoundManager.play("buy");
        return true;
    }

    // ================= منوی اصلی ساختمان‌ها =================

    public void openBuildingMenu(Building b) {
        if (b == null) return;
        switch (b.type) {
            case Building.RESTAURANT:
                world.enterInterior(b, player);
                openRestaurantMenu();
                break;
            case Building.CAFE:
                world.enterInterior(b, player);
                openFoodMenu("کافه شکلات", CAFE_FOODS, "یه چیزی بخوریم!");
                break;
            case Building.MARKET:
                world.enterInterior(b, player);
                openMarketMenu();
                break;
            case Building.CINEMA: openCinemaMenu(); break;
            case Building.ZOO: openZooMenu(); break;
            case Building.HELIPORT: openHeliportMenu(); break;
            case Building.TRAIN_STATION: openTrainMenu(); break;
            case Building.HOME: openHomeMenu(); break;
            case Building.HOSPITAL: openHospitalMenu(); break;
            case Building.BANK: openBankMenu(); break;
            case Building.TOYSTORE: openToyMenu(); break;
            default:
                world.enterInterior(b, player);
                openInfoMenu(Building.nameOf(b.type), b.info());
                break;
        }
    }

    private void openInfoMenu(String title, String text) {
        ui.openInfo(title, text);
    }

    // ================= رستوران =================

    private void openRestaurantMenu() {
        UIManager.Menu m = new UIManager.Menu("رستوران خوشمزه — نوش جان!");
        for (Object[] f : RESTAURANT_FOODS) {
            String name = (String) f[0];
            int price = (int) f[1];
            float hunger = (float) f[2];
            float energy = (float) f[3];
            m.items.add(new UIManager.MenuItem(name,
                    UIManager.faMoney(price) + " تومان",
                    () -> {
                        if (pay(price)) {
                            player.eat(hunger, energy);
                            view.fx("eat");
                            ui.toast("نوش جان! حالا حالتیه!");
                        }
                    }));
        }
        m.items.add(new UIManager.MenuItem("شیفت گارسونی", "مشتری‌ها رو سرو کن و انعام بگیر!",
                () -> {
                    if (view.jobs.startWaiter()) {
                        ui.closeMenu();
                        ui.toast("شیفت شروع شد! نزدیک مشتری وایسا و دکمه رو بزن");
                    }
                }));
        m.items.add(new UIManager.MenuItem("خروج از رستوران", "برگرد خیابون",
                this::exitInteriorNow));
        ui.openMenu(m);
    }

    // ================= سوپرمارکت =================

    private void openMarketMenu() {
        UIManager.Menu m = new UIManager.Menu("سوپرمارکت فراوان — تازه و خوشمزه!");
        for (Object[] f : MARKET_FOODS) {
            String name = (String) f[0];
            int price = (int) f[1];
            float hunger = (float) f[2];
            float energy = (float) f[3];
            m.items.add(new UIManager.MenuItem(name,
                    UIManager.faMoney(price) + " تومان",
                    () -> {
                        if (pay(price)) {
                            player.eat(hunger, energy);
                            view.fx("eat");
                            ui.toast("خوشمزه بود!");
                        }
                    }));
        }
        m.items.add(new UIManager.MenuItem("شیفت صندوق", "پشت صندوق وایسا و مشتری‌ها رو برون!",
                () -> {
                    if (view.jobs.startShopkeeper()) {
                        ui.closeMenu();
                        ui.toast("پشت صندوق وایسا و دکمه «خدمت بده» رو بزن!");
                    }
                }));
        m.items.add(new UIManager.MenuItem("خروج از سوپرمارکت", "برگرد خیابون",
                this::exitInteriorNow));
        ui.openMenu(m);
    }

    private void openFoodMenu(String title, Object[][] foods, String msg) {
        UIManager.Menu m = new UIManager.Menu(title + " — " + msg);
        for (Object[] f : foods) {
            String name = (String) f[0];
            int price = (int) f[1];
            float hunger = (float) f[2];
            float energy = (float) f[3];
            m.items.add(new UIManager.MenuItem(name,
                    UIManager.faMoney(price) + " تومان",
                    () -> {
                        if (pay(price)) {
                            player.eat(hunger, energy);
                            view.fx("eat");
                            ui.toast("آخ جان! خوشمزه بود 😋");
                        }
                    }));
        }
        m.items.add(new UIManager.MenuItem("خروج", "برگرد بیرون",
                this::exitInteriorNow));
        ui.openMenu(m);
    }

    // ================= سینما =================

    private void openCinemaMenu() {
        UIManager.Menu m = new UIManager.Menu("سینما ستاره — امروز: کارتون‌های خفن!");
        m.items.add(new UIManager.MenuItem("🏎 ماشین‌های مسابقه", "بلیط: " + UIManager.faMoney(CINEMA_TICKET) + " تومان",
                () -> buyTicket(0)));
        m.items.add(new UIManager.MenuItem("🐠 ماهی رنگارنگ", "بلیط: " + UIManager.faMoney(CINEMA_TICKET) + " تومان",
                () -> buyTicket(1)));
        m.items.add(new UIManager.MenuItem("🚀 موشک فضایی", "بلیط: " + UIManager.faMoney(CINEMA_TICKET) + " تومان",
                () -> buyTicket(2)));
        ui.openMenu(m);
    }

    private void buyTicket(int movie) {
        if (pay(CINEMA_TICKET)) {
            Building cinema = world.buildingByType(Building.CINEMA);
            world.enterInterior(cinema, player);
            // ردیف وسط صندلی
            int seatIndex = Math.min(8, world.interior.seatSpots.size() / 2 + 1);
            float[] seat = world.interior.seatSpots.get(seatIndex);
            player.x = seat[0];
            player.y = seat[1] + 8f;
            ui.closeMenu();
            view.startMovie(movie);
        }
    }

    // ================= باغ‌وحش =================

    private void openZooMenu() {
        UIManager.Menu m = new UIManager.Menu("باغ‌وحش شادی — ۶ حیوان دوست‌داشتنی!");
        m.items.add(new UIManager.MenuItem("بلیط ورود بخر", UIManager.faMoney(ZOO_TICKET) + " تومان — برو پیش حیوان‌ها!",
                () -> {
                    if (pay(ZOO_TICKET)) {
                        Building zoo = world.buildingByType(Building.ZOO);
                        world.enterInterior(zoo, player);
                        ui.closeMenu();
                        ui.toast("جلو هر حیوان وایسا و «نگاه کن» رو بزن تا درباره‌اش بخونی!");
                    }
                }));
        ui.openMenu(m);
    }

    // ================= هلی‌پورت =================

    private void openHeliportMenu() {
        UIManager.Menu m = new UIManager.Menu("هلی‌پورت شهر — پرواز فوق‌العاده!");
        if (!view.heliOwned) {
            m.items.add(new UIManager.MenuItem("خرید هلیکوپتر شادی", UIManager.faMoney(HELI_PRICE) + " تومان — خفن‌ترین وسیله شهر!",
                    () -> {
                        if (pay(HELI_PRICE)) {
                            view.heliOwned = true;
                            Building hp = world.buildingByType(Building.HELIPORT);
                            Vehicle heli = new Vehicle(Vehicle.CAR_HELICOPTER, hp.doorX, hp.y + hp.h * 0.42f);
                            heli.mode = Vehicle.MODE_PARKED;
                            world.vehicles.add(heli);
                            ui.closeMenu();
                            ui.toast("هلیکوپتر تو خریده شد! 🚁 سوار شو!");
                        }
                    }));
        } else {
            m.items.add(new UIManager.MenuItem("سوار هلیکوپتر شو", "پرواز کن و شهر رو از بالا ببین!",
                    () -> {
                        Vehicle heli = findParked(Vehicle.CAR_HELICOPTER);
                        if (heli != null && G.dist(player.x, player.y, heli.x, heli.y) < 400f) {
                            player.driving = heli;
                            heli.mode = Vehicle.MODE_PLAYER;
                            player.x = heli.x;
                            player.y = heli.y;
                            ui.closeMenu();
                            ui.toast("پرواز کن! 🚁 اهرم = حرکت | دکمه اقدام = فرود");
                        } else {
                            ui.toast("هلیکوپتر نزدیک نیست!");
                        }
                    }));
        }
        ui.openMenu(m);
    }

    // ================= ایستگاه قطار 🚂 =================

    private void openTrainMenu() {
        UIManager.Menu m = new UIManager.Menu("ایستگاه قطار شهری — سفر دور شهر!");
        m.items.add(new UIManager.MenuItem("🎫 بلیط سفر با قطار", UIManager.faMoney(TRAIN_TICKET) + " تومان — یک دور کامل!",
                () -> {
                    Vehicle train = findTrain();
                    if (train == null) {
                        ui.toast("قطار الان اینجا نیست — کمی صبر کن!");
                        return;
                    }
                    if (G.dist(player.x, player.y, train.x, train.y) > 600f) {
                        ui.toast("قطار باید نزدیک ایستگاه باشد!");
                        return;
                    }
                    if (pay(TRAIN_TICKET)) {
                        player.ridingTrain = true;
                        player.trainBoardDist = train.trackPos;
                        player.driving = null;
                        ui.closeMenu();
                        SoundManager.play("success");
                        ui.toast("سفر شروع شد! 🚂 از پنجره شهر را تماشا کن — آخر ایستگاه پیاده می‌شی!");
                    }
                }));
        if (!view.trainOwned) {
            m.items.add(new UIManager.MenuItem("🚂 خرید قطار شادی", UIManager.faMoney(TRAIN_PRICE) + " تومان — خودت راننده باش!",
                    () -> {
                        if (pay(TRAIN_PRICE)) {
                            view.trainOwned = true;
                            ui.closeMenu();
                            ui.toast("قطار خریده شد! دکمه اقدام = سوار شدن");
                        }
                    }));
        } else {
            m.items.add(new UIManager.MenuItem("🚂 راننده قطار باش", "اهرم بالا/پایین = گاز و ترمز",
                    () -> {
                        Vehicle train = findTrain();
                        if (train == null) {
                            ui.toast("قطار پیدا نشد!");
                            return;
                        }
                        if (G.dist(player.x, player.y, train.x, train.y) > 700f) {
                            ui.toast("قطار باید نزدیک باشد!");
                            return;
                        }
                        player.ridingTrain = false;
                        player.driving = train;
                        player.x = train.x;
                        player.y = train.y;
                        ui.closeMenu();
                        ui.toast("تو راننده قطار شدی! 🚂 دکمه اقدام = پیاده شدن");
                    }));
        }
        ui.openMenu(m);
    }

    private Vehicle findTrain() {
        for (Vehicle v : world.vehicles) {
            if (v.type == Vehicle.CAR_TRAIN) return v;
        }
        return null;
    }

    private Vehicle findParked(int type) {
        for (Vehicle v : world.vehicles) {
            if (v.type == type && v.mode == Vehicle.MODE_PARKED) return v;
        }
        return null;
    }

    // ================= خانه و بقیه =================

    private void openHomeMenu() {
        UIManager.Menu m = new UIManager.Menu("خانه شادیت 🏠");
        m.items.add(new UIManager.MenuItem("🛌 تا صبح بخواب", "انرژی پر می‌شود — ساعت ۷ صبح!",
                () -> {
                    player.energy = 100f;
                    player.hunger = Math.max(20f, player.hunger - 15f);
                    world.dayNight.minutes = 7f * 60f;
                    SoundManager.play("success");
                    ui.closeMenu();
                    ui.toast("صبح بخیر قهرمان! ☀ حالا پر انرژی‌ای!");
                }));
        m.items.add(new UIManager.MenuItem("🛋 استراحت کوتاه", "+۳۰ انرژی",
                () -> {
                    player.energy = Math.min(100f, player.energy + 30f);
                    SoundManager.play("success");
                    ui.toast("حالا کمی سرحال‌تری!");
                }));
        m.items.add(new UIManager.MenuItem("خروج از خانه", "برو بیرون بازی کن!",
                this::exitInteriorNow));
        ui.openMenu(m);
    }

    private void openHospitalMenu() {
        UIManager.Menu m = new UIManager.Menu("بیمارستان مهربانی 🏥");
        m.items.add(new UIManager.MenuItem("معاینه کامل", "۱۰۰ تومان — کاملاً سالم می‌شوی!",
                () -> {
                    if (pay(100)) {
                        player.hunger = 100f;
                        player.energy = 100f;
                        SoundManager.play("success");
                        ui.toast("دکتر می‌گوید: «تو سالم‌ترین بچه شهر هستی!»");
                    }
                }));
        m.items.add(new UIManager.MenuItem("خروج", "", this::exitInteriorNow));
        ui.openMenu(m);
    }

    private void openBankMenu() {
        UIManager.Menu m = new UIManager.Menu("بانک شادی 🏦");
        m.items.add(new UIManager.MenuItem("پس‌انداز روز", "۵۰ تومان جایزه پس‌انداز!",
                () -> {
                    player.addMoney(50);
                    SoundManager.play("coin");
                    ui.toast("کارمند بانک: «آفرین! پس‌انداز یادت نره!»");
                }));
        m.items.add(new UIManager.MenuItem("خروج", "", this::exitInteriorNow));
        ui.openMenu(m);
    }

    private void openToyMenu() {
        UIManager.Menu m = new UIManager.Menu("فروشگاه اسباب‌بازی 🧸");
        for (Object[] t : TOYS) {
            String name = (String) t[0];
            int price = (int) t[1];
            m.items.add(new UIManager.MenuItem(name, UIManager.faMoney(price) + " تومان",
                    () -> {
                        if (pay(price)) {
                            SoundManager.play("success");
                            ui.toast("ول کردی! 🎉 " + name + " مال تو شد!");
                        }
                    }));
        }
        m.items.add(new UIManager.MenuItem("خروج", "", this::exitInteriorNow));
        ui.openMenu(m);
    }

    public void exitInteriorNow() {
        view.jobs.stopJob();
        view.stopMovie();
        world.exitInterior(player);
        ui.closeMenu();
    }
}
