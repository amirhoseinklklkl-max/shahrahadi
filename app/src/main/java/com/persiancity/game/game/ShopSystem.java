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
    private static final int WHEEL_PRICE = 300;
    private static final int CAROUSEL_PRICE = 250;

    // 👕 استایل‌های فروشگاه لباس: {اسم، اندیس، قیمت}
    private static final Object[][] OUTFITS_FOR_SALE = {
        {"👕 استایل آبی", 0, 0},
        {"🎽 استایل قرمز", 1, 300},
        {"🌿 استایل سبز", 2, 300},
        {"⚡ استایل زرد", 3, 350},
        {"🌊 استایل فیروزه‌ای", 4, 350},
        {"🌸 استایل صورتی", 5, 300}
    };

    private static final Object[][] CARS_FOR_SALE = {
        {"🚗 سدان شادی", Vehicle.CAR_SEDAN, 8000},
        {"🏎 اسپرت جت", Vehicle.CAR_SPORT, 15000},
        {"🛵 موتور تندر", Vehicle.MOTOR, 4000},
        {"🚙 وانت بار", Vehicle.CAR_PICKUP, 11000}
    };

    private static final Object[][] BAKERY_FOODS = {
        {"نان بربری تازه", 40, 20f, 12f},
        {"کیک محلی", 80, 18f, 14f},
        {"چای کمرگیلد", 30, 6f, 18f}
    };

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
            case Building.CARSHOP: openCarshopMenu(); break;
            case Building.DOCK: openDockMenu(); break;
            case Building.AMUSEMENT: openAmusementMenu(); break;
            case Building.CLOTHES:
                world.enterInterior(b, player);
                openClothesMenu();
                break;
            case Building.AIRPORT:
                world.enterInterior(b, player);
                openInfoMenu("فرودگاه شهر شادی ✈", b.info());
                break;
            case Building.BAKERY:
                world.enterInterior(b, player);
                openFoodMenu("نانوایی روستا", BAKERY_FOODS, "بوی نان تازه!");
                break;
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

    // ================= فروشگاه لباس 👕 =================

    private void openClothesMenu() {
        UIManager.Menu m = new UIManager.Menu("فروشگاه لباس شادی — خوش‌تیپ شو!");
        for (Object[] o : OUTFITS_FOR_SALE) {
            final int idx = (int) o[1];
            final int price = (int) o[2];
            boolean owned = view.outfitOwned[idx];
            String desc = owned ? "مال توست — همین حالا بپوشش!"
                    : UIManager.faMoney(price) + " تومان";
            m.items.add(new UIManager.MenuItem((String) o[0], desc, () -> {
                if (view.outfitOwned[idx]) {
                    player.outfit = idx;
                    SoundManager.play("click");
                    ui.closeMenu();
                    ui.toast("استایل عوض شد! حالا خوش‌تیپ شدی!");
                } else if (pay(price)) {
                    view.outfitOwned[idx] = true;
                    player.outfit = idx;
                    ui.closeMenu();
                    ui.toast("🎉 لباس نو خریدی و پوشیدی! چه شیکی!");
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
        UIManager.Menu m = new UIManager.Menu("باغ‌وحش شادی — ۱۰ حیوان دوست‌داشتنی!");
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
                            // هلی پارک‌شده هلی‌پورت مال بازیکن می‌شود (هلی تکراری ساخته نمی‌شود)
                            Vehicle heli = findParked(Vehicle.CAR_HELICOPTER);
                            if (heli == null) {
                                Building hp = world.buildingByType(Building.HELIPORT);
                                heli = new Vehicle(Vehicle.CAR_HELICOPTER, hp.doorX, hp.y + hp.h * 0.42f);
                                heli.mode = Vehicle.MODE_PARKED;
                                world.vehicles.add(heli);
                            }
                            heli.owned = true;
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

    // ================= فروشگاه ماشین 🚗 =================

    private void openCarshopMenu() {
        UIManager.Menu m = new UIManager.Menu("فروشگاه ماشین شادی — تکون خوردن توپ!");
        for (Object[] car : CARS_FOR_SALE) {
            String name = (String) car[0];
            final int vType = (int) car[1];
            int price = (int) car[2];
            m.items.add(new UIManager.MenuItem(name, UIManager.faMoney(price) + " تومان — همین حالا تحویل!",
                    () -> buyVehicle(vType, price, name)));
        }
        m.items.add(new UIManager.MenuItem("ℹ راهنما", "نزدیک ماشینت که وایسی دکمه اقدام (✋) سوارت می‌کند",
                () -> ui.openInfo("چطور سوار شوم؟", "بعد از خرید، ماشین جلوی فروشگاه پارک می‌شود.\n\nنزدیکش برو؛ دکمه نارنجی اقدام (✋ سوار شو) ظاهر می‌شود.\nبا اهرم بران!\nبرای پیاده شدن دوباره دکمه اقدام را بزن.")));
        ui.openMenu(m);
    }

    private void buyVehicle(int vType, int price, String name) {
        if (!pay(price)) return;
        Building shop = world.buildingByType(Building.CARSHOP);
        Vehicle v = new Vehicle(vType, 0, 0);
        v.owned = true;
        v.mode = Vehicle.MODE_PARKED;
        // جای پارک جلوی فروشگاه — چند ردیف
        if (shop != null) {
            int n = 0;
            for (Vehicle o : world.vehicles) if (o.owned && o != v) n++;
            v.x = shop.x + shop.w / 2f + ((n % 3) - 1) * 110f;
            v.y = shop.doorY + 70f + (n / 3) * 90f;
            if (world.isBlocked(v.x, v.y, 20f)) {
                v.x = shop.x + shop.w / 2f;
                v.y = shop.doorY + 80f;
            }
        } else {
            v.x = player.x + 80f;
            v.y = player.y + 40f;
        }
        v.color = Vehicle.CAR_COLORS[(int) (Math.random() * Vehicle.CAR_COLORS.length)];
        world.vehicles.add(v);
        ui.closeMenu();
        ui.toast("🎉 تبریک! " + name + " مال تو شد — جلوی فروشگاه پارک شد!");
    }

    // ================= اسکله و قایق ⚓ =================

    private void openDockMenu() {
        UIManager.Menu m = new UIManager.Menu("اسکله دریاچه — قایق منتظر است!");
        m.items.add(new UIManager.MenuItem("⚓ سوار قایق شو", "قایق کنار اسکله مستقر است — رایگان!",
                () -> {
                    Vehicle boat = findParked(Vehicle.CAR_BOAT);
                    if (boat == null) {
                        ui.toast("قایق پیدا نشد!");
                        return;
                    }
                    if (G.dist(player.x, player.y, boat.x, boat.y) > 500f) {
                        ui.toast("قایق باید نزدیک باشد — به کنار اسکله برو!");
                        return;
                    }
                    player.driving = boat;
                    boat.mode = Vehicle.MODE_PLAYER;
                    player.x = boat.x;
                    player.y = boat.y;
                    ui.closeMenu();
                    SoundManager.play("splash");
                    ui.toast("سوار قایق شدی! ⛵ با اهرم روی دریاچه بگرد — دکمه اقدام = پیاده شدن");
                }));
        m.items.add(new UIManager.MenuItem("🎣 راهنمای ماهیگیری", "چطور ماهی بگیرم؟",
                () -> ui.openInfo("ماهیگیری", "کنار اسکله وایسا؛ دکمه اقدام (✋ ماهیگیری) را بزن!\n\nهر ماهی قیمت خودش را دارد:\n🐟 ماهی نقره‌ای ۶۰ تومان\n🐠 ماهی رنگارنگ ۱۰۰ تومان\n🦈 ماهی خوششانسی ۱۶۰ تومان!")));
        ui.openMenu(m);
    }

    // ================= شهربازی 🎡 =================

    private void openAmusementMenu() {
        UIManager.Menu m = new UIManager.Menu("شهربازی شادی — صدای خنده!");
        m.items.add(new UIManager.MenuItem("🎡 چرخ‌وفلک بزرگ", UIManager.faMoney(WHEEL_PRICE) + " تومان — یک چرخ کامل بالا!",
                () -> {
                    if (pay(WHEEL_PRICE)) {
                        ui.closeMenu();
                        view.startWheelRide();
                    }
                }));
        m.items.add(new UIManager.MenuItem("🎠 سرسیر کاروسل", UIManager.faMoney(CAROUSEL_PRICE) + " تومان — اسب چوبی تو!",
                () -> {
                    if (pay(CAROUSEL_PRICE)) {
                        ui.closeMenu();
                        view.startCarouselRide();
                    }
                }));
        m.items.add(new UIManager.MenuItem("🍿 پاپ‌کورن", "۸۰ تومان — ترد و خوشمزه!",
                () -> {
                    if (pay(80)) {
                        player.eat(12f, 8f);
                        view.fx("eat");
                        ui.toast("پاپ‌کورن خوردی! 🍿");
                    }
                }));
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
