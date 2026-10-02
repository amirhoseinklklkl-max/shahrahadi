package com.persiancity.game.game;

import java.util.Random;

/**
 * سیستم شغل‌ها: تاکسی رانی، پیک موتوری، گارسونی، فروشندگی
 */
public class JobSystem {

    public static final int JOB_NONE = 0;
    public static final int JOB_TAXI = 1;
    public static final int JOB_COURIER = 2;
    public static final int JOB_WAITER = 3;
    public static final int JOB_SHOPKEEPER = 4;

    public int activeJob = JOB_NONE;

    // تاکسی
    private Npc taxiPassenger = null;
    private boolean passengerOnBoard = false;
    private float destX, destY;
    private float taxiRespawn = 0f;
    private float taxiTripDist = 0f;

    // پیک
    private int courierStage = 0;   // ۰=برای بردن بسته ۱=تحویل
    private int courierTargetType = -1;
    private float courierTimer = 0f;
    private float courierTripDist = 0f;

    // گارسونی
    private Npc waiterCustomer = null;
    private int waiterStage = 0;    // ۰=راه رفتن به میز ۱=منتظر سفارش‌گیری ۲=منتظر غذا ۳=تمام
    private int waiterServedCount = 0;
    private float tableX, tableY;
    private int tableIndex = 0;

    // فروشندگی
    private Npc shopCustomer = null;
    private int shopStage = 0;      // ۰=در راه صندوق ۱=منتظر خدمت
    private int shopServedCount = 0;
    private boolean serving = false;
    private float servingTimer = 0f;

    private final World world;
    private final Random rnd;
    private final GameView view;

    public JobSystem(World world, Random rnd, GameView view) {
        this.world = world;
        this.rnd = rnd;
        this.view = view;
    }

    public void stopJob() {
        activeJob = JOB_NONE;
        taxiPassenger = null;
        passengerOnBoard = false;
        waiterCustomer = null;
        shopCustomer = null;
        serving = false;
        courierStage = 0;
    }

    // ---------------- شروع شغل‌ها ----------------

    public boolean startTaxi(Player p) {
        boolean hasCar = false;
        for (Vehicle v : world.vehicles) {
            if (v.mode != Vehicle.MODE_TRAFFIC && !v.isBike()) {
                hasCar = true;
                break;
            }
        }
        if (!hasCar) return false;
        activeJob = JOB_TAXI;
        taxiRespawn = 1f;
        return true;
    }

    public boolean startCourier(Player p) {
        boolean hasBike = false;
        for (Vehicle v : world.vehicles) {
            if (v.mode != Vehicle.MODE_TRAFFIC && v.isBike()) {
                hasBike = true;
                break;
            }
        }
        if (!hasBike) return false;
        activeJob = JOB_COURIER;
        nextCourierTarget();
        return true;
    }

    public void startWaiter() {
        activeJob = JOB_WAITER;
        waiterServedCount = 0;
        spawnWaiterCustomer();
    }

    public void startShopkeeper() {
        activeJob = JOB_SHOPKEEPER;
        shopServedCount = 0;
        spawnShopCustomer();
    }

    // ---------------- تاکسی ----------------

    private void updateTaxi(float dt, Player p) {
        if (p.driving == null) return;   // باید پشت فرمان باشی

        if (taxiPassenger == null) {
            taxiRespawn -= dt;
            if (taxiRespawn <= 0f) {
                // مسافر جدید در نقطه‌ای از پیاده‌رو
                float[] spot = world.randomWalkableNear(p.x, p.y, 30f * G.TILE, false);
                if (spot != null) {
                    taxiPassenger = new Npc(spot[0], spot[1], "مسافر", Npc.ROLE_TAXI_PASSENGER, rnd);
                    taxiPassenger.say(Dialogues.pick(Dialogues.TAXI_PICKUP_LINES, rnd), 4f);
                    passengerOnBoard = false;
                    world.cityNpcs.add(taxiPassenger);
                } else {
                    taxiRespawn = 2f;
                }
            }
            return;
        }

        Vehicle car = p.driving;
        if (!passengerOnBoard) {
            // رسیدن به مسافر
            if (G.dist(car.x, car.y, taxiPassenger.x, taxiPassenger.y) < 110f && Math.abs(car.speed) < 25f) {
                world.cityNpcs.remove(taxiPassenger);
                car.hasPassenger = true;
                passengerOnBoard = true;
                // مقصد: جلوی یک ساختمان تصادفی
                Building b = world.buildings.get(rnd.nextInt(world.buildings.size()));
                destX = b.doorX;
                destY = b.doorY;
                taxiTripDist = G.dist(car.x, car.y, destX, destY);
                view.toast("مسافر سوار شد! برو به «" + b.name + "»");
            }
        } else {
            if (G.dist(car.x, car.y, destX, destY) < 130f && Math.abs(car.speed) < 25f) {
                car.hasPassenger = false;
                int pay = 300 + (int) (taxiTripDist / 8f);
                pay = Math.max(400, Math.min(1500, pay));
                p.earn(pay);
                view.toast(Dialogues.pick(Dialogues.TAXI_DROP_LINES, rnd) + " (+" + G.fa(pay) + " تومان)");
                view.fx("coin");
                taxiPassenger = null;
                passengerOnBoard = false;
                taxiRespawn = 3f + rnd.nextFloat() * 4f;
            }
        }
    }

    // ---------------- پیک موتوری ----------------

    private void nextCourierTarget() {
        int[] types = {Building.RESTAURANT, Building.MARKET, Building.CAFE, Building.TOYSTORE, Building.CLOTHES};
        courierTargetType = types[rnd.nextInt(types.length)];
        courierStage = 0;
        courierTimer = 75f;
    }

    private Building courierTargetBuilding() {
        for (Building b : world.buildings) {
            if (b.type == courierTargetType) return b;
        }
        return null;
    }

    private void updateCourier(float dt, Player p) {
        if (courierStage == 0) courierTimer -= dt;
        Building target = courierTargetBuilding();
        if (target == null) {
            nextCourierTarget();
            return;
        }

        Entity ent = p.driving != null ? p.driving : p;
        float d = G.dist(ent.x, ent.y, target.doorX, target.doorY);
        if (d < 140f && (p.driving == null || Math.abs(p.driving.speed) < 30f)) {
            if (courierStage == 0) {
                courierStage = 1;
                courierTripDist = d;
                p.carrying = 1;
                view.toast("بسته رو برداشتی! حالا ببرش به «" + target.name + "»");
                view.fx("click");
            } else {
                int pay = 500 + (int) (courierTripDist / 12f);
                if (courierTimer > 0f) pay += 200;
                pay = Math.min(1800, pay);
                p.earn(pay);
                p.carrying = -1;
                view.toast("بسته رسید! (+" + G.fa(pay) + " تومان)");
                view.fx("coin");
                nextCourierTarget();
            }
        }
    }

    // ---------------- گارسونی ----------------

    private void spawnWaiterCustomer() {
        Interior in = world.interior;
        if (in == null) return;
        waiterCustomer = new Npc(in.doorX, in.doorY, "مشتری", Npc.ROLE_TABLE_CUSTOMER, rnd);
        tableIndex = waiterServedCount % in.tableSpots.size();
        float[] spot = in.tableSpots.get(tableIndex);
        tableX = spot[0];
        tableY = spot[1];
        waiterCustomer.setTarget(tableX + G.TILE * 0.6f, tableY + G.TILE * 0.7f);
        waiterStage = 0;
        world.interiorNpcs.add(waiterCustomer);
    }

    private void updateWaiter(float dt, Player p) {
        if (waiterCustomer == null) return;
        switch (waiterStage) {
            case 0:
                if (waiterCustomer.atTarget()) {
                    waiterStage = 1;
                    waiterCustomer.say("ممنون! من یه غذای خوشمزه می‌خوام.", 3f);
                }
                break;
            case 2:
                // غذا رسیده؟ چک در doJobAction انجام می‌شود
                break;
        }
    }

    // ---------------- فروشندگی ----------------

    private void spawnShopCustomer() {
        Interior in = world.interior;
        if (in == null) return;
        shopCustomer = new Npc(in.doorX, in.doorY, "مشتری", Npc.ROLE_QUEUE_CUSTOMER, rnd);
        float[] spot = in.standSpots.get(0);
        shopCustomer.setTarget(spot[0], spot[1]);
        shopStage = 0;
        world.interiorNpcs.add(shopCustomer);
    }

    private void updateShopkeeper(float dt, Player p) {
        if (shopCustomer == null) return;
        if (shopStage == 0 && shopCustomer.atTarget()) {
            shopStage = 1;
            shopCustomer.say("سلام! می‌خوام خرید کنم.", 3f);
        }
        if (serving) {
            servingTimer -= dt;
            if (servingTimer <= 0f) {
                serving = false;
                p.earn(150);
                view.toast("خرید انجام شد! (+۱۵۰ تومان)");
                view.fx("coin");
                world.interiorNpcs.remove(shopCustomer);
                shopCustomer = null;
                shopServedCount++;
                if (shopServedCount < 4) {
                    spawnShopCustomer();
                } else {
                    p.earn(300);
                    view.toast("شیفت تموم شد! دستمزد: +۳۰۰ تومان");
                    view.fx("mission");
                    stopJob();
                }
            }
        }
    }

    // ---------------- اکشن شغل‌ها ----------------

    /**
     * متن دکمه اقدام مخصوص شغل (اگر زمینه شغلی باشد)
     */
    public String jobPrompt(Player p) {
        switch (activeJob) {
            case JOB_WAITER:
                if (waiterCustomer != null) {
                    if (waiterStage == 1 && p.carrying < 0
                            && G.dist(p.x, p.y, tableX, tableY) < 90f) {
                        return "سفارش بگیر";
                    }
                    if (waiterStage == 1 && p.carrying < 0 && G.dist(p.x, p.y, world.interior.counterX, world.interior.counterY) < 110f) {
                        return "غذا آماده کن";
                    }
                    if (p.carrying == 0 && G.dist(p.x, p.y, tableX, tableY) < 90f) {
                        return "غذا بده";
                    }
                }
                break;
            case JOB_SHOPKEEPER:
                if (shopCustomer != null && shopStage == 1 && !serving) {
                    if (G.dist(p.x, p.y, world.interior.counterX, world.interior.counterY) < 100f) {
                        return "خدمت بده";
                    }
                }
                break;
        }
        return null;
    }

    public boolean doJobAction(Player p) {
        switch (activeJob) {
            case JOB_WAITER:
                if (waiterCustomer != null && waiterStage == 1
                        && p.carrying < 0
                        && G.dist(p.x, p.y, tableX, tableY) < 90f) {
                    waiterCustomer.say("لطفاً سریع باشه!", 2.5f);
                    view.toast("سفارش گرفتی! برو به آشپزخانه.");
                    view.fx("click");
                    return true;
                }
                if (waiterCustomer != null && waiterStage == 1 && p.carrying < 0
                        && G.dist(p.x, p.y, world.interior.counterX, world.interior.counterY) < 110f) {
                    p.carrying = 0;
                    view.toast("غذا آماده‌ست! ببرش برای مشتری.");
                    view.fx("eat");
                    return true;
                }
                if (waiterCustomer != null && p.carrying == 0
                        && G.dist(p.x, p.y, tableX, tableY) < 90f) {
                    p.carrying = -1;
                    int tip = 120 + rnd.nextInt(7) * 10;
                    p.earn(tip);
                    waiterCustomer.say("وای چه خوشمزه! ممنون!", 3f);
                    view.toast("انعام گرفتی! (+" + G.fa(tip) + " تومان)");
                    view.fx("coin");
                    world.interiorNpcs.remove(waiterCustomer);
                    waiterCustomer = null;
                    waiterServedCount++;
                    if (waiterServedCount < 3) {
                        spawnWaiterCustomer();
                    } else {
                        p.earn(300);
                        view.toast("شیفت گارسونی تموم شد! دستمزد: +۳۰۰ تومان");
                        view.fx("mission");
                        stopJob();
                    }
                    return true;
                }
                break;

            case JOB_SHOPKEEPER:
                if (shopCustomer != null && shopStage == 1 && !serving
                        && G.dist(p.x, p.y, world.interior.counterX, world.interior.counterY) < 100f) {
                    serving = true;
                    servingTimer = 1.4f;
                    view.fx("click");
                    return true;
                }
                break;
        }
        return false;
    }

    // ---------------- به‌روزرسانی کل ----------------

    public void update(float dt, Player p) {
        switch (activeJob) {
            case JOB_TAXI:
                updateTaxi(dt, p);
                break;
            case JOB_COURIER:
                updateCourier(dt, p);
                break;
            case JOB_WAITER:
                updateWaiter(dt, p);
                break;
            case JOB_SHOPKEEPER:
                updateShopkeeper(dt, p);
                break;
        }
    }

    /**
     * هدف فعلی برای فلش راهنما و مینی‌مپ
     */
    public float[] currentMarker(Player p) {
        switch (activeJob) {
            case JOB_TAXI:
                if (p.driving != null) {
                    if (taxiPassenger != null && !passengerOnBoard) {
                        return new float[]{taxiPassenger.x, taxiPassenger.y};
                    }
                    if (passengerOnBoard) {
                        return new float[]{destX, destY};
                    }
                }
                break;
            case JOB_COURIER:
                Building b = courierTargetBuilding();
                if (b != null) return new float[]{b.doorX, b.doorY};
                break;
        }
        return null;
    }

    /**
     * متن وضعیت شغل برای نمایش بالای صفحه
     */
    public String statusText(Player p) {
        switch (activeJob) {
            case JOB_TAXI:
                if (p.driving == null) return "تاکسی: سوار ماشین شو!";
                if (taxiPassenger == null) return "تاکسی: منتظر مسافر...";
                if (!passengerOnBoard) return "تاکسی: برو سراغ مسافر";
                return "تاکسی: مسافر رو برسون";
            case JOB_COURIER:
                if (courierStage == 0) return "پیک: بسته رو بردار (" + (int) Math.max(0, courierTimer) + " ثانیه برای جایزه)";
                return "پیک: بسته رو برسون";
            case JOB_WAITER:
                return "گارسونی: مشتری " + G.fa(waiterServedCount + 1) + " از ۳";
            case JOB_SHOPKEEPER:
                if (serving) return "فروشندگی: در حال خدمت...";
                return "فروشندگی: مشتری " + G.fa(shopServedCount + 1) + " از ۴";
        }
        return null;
    }

    public int getServedCount(int job) {
        if (job == JOB_WAITER) return waiterServedCount;
        if (job == JOB_SHOPKEEPER) return shopServedCount;
        return 0;
    }

    public boolean isServing() {
        return serving;
    }

    public float servingProgress() {
        return serving ? G.clamp(1f - servingTimer / 1.4f, 0f, 1f) : 0f;
    }
}
