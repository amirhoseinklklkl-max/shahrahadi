package com.persiancity.game.game;

import com.persiancity.game.SoundManager;

import java.util.ArrayList;
import java.util.Random;

/**
 * نظام مشاغل: تاکسی، پیک، گارسون، صندوق‌دار — با دکمه اقدام کار می‌کنند
 */
public class JobSystem {
    public static final int JOB_NONE = 0;
    public static final int JOB_TAXI = 1;
    public static final int JOB_COURIER = 2;
    public static final int JOB_WAITER = 3;
    public static final int JOB_SHOPKEEPER = 4;

    public int activeJob = JOB_NONE;

    private final GameView view;
    private final World world;
    private final Player player;
    private final UIManager ui;
    private final Random rnd = new Random();

    // تاکسی / پیک
    private Npc taxiPassenger = null;
    private float[] taxiDest = null;
    private float taxiBoardX, taxiBoardY;
    private float taxiRespawn = 1.5f;
    private boolean carryingPackage = false;

    // گارسون
    private int waiterServedCount = 0;
    private int waiterStage = 0;
    private Npc waiterCustomer = null;

    // صندوق
    private int shopServedCount = 0;
    private int shopStage = 0;
    private boolean serving = false;

    public JobSystem(GameView view, World world, Player player, UIManager ui) {
        this.view = view;
        this.world = world;
        this.player = player;
        this.ui = ui;
    }

    // ================= شروع شیفت‌ها =================

    public boolean startTaxi() {
        if (activeJob != JOB_NONE) return false;
        if (player.driving == null) {
            ui.toast("اول سوار یک ماشین یا موتور شو!");
            return false;
        }
        activeJob = JOB_TAXI;
        taxiPassenger = null;
        taxiRespawn = 0.5f;
        ui.toast("شیفت تاکسی شروع شد! برو سراغ مسافر 🚕");
        return true;
    }

    public boolean startCourier() {
        if (activeJob != JOB_NONE) return false;
        if (player.driving == null) {
            ui.toast("برای پیک بودن باید موتور یا ماشین داشته باشی!");
            return false;
        }
        activeJob = JOB_COURIER;
        carryingPackage = false;
        taxiRespawn = 0.5f;
        ui.toast("شیفت پیک شروع شد! بسته را بردار 🛵");
        return true;
    }

    public boolean startWaiter() {
        if (activeJob != JOB_NONE) return false;
        activeJob = JOB_WAITER;
        waiterServedCount = 0;
        waiterStage = 0;
        waiterCustomer = null;
        spawnWaiterCustomer();
        return true;
    }

    public boolean startShopkeeper() {
        if (activeJob != JOB_NONE) return false;
        activeJob = JOB_SHOPKEEPER;
        shopServedCount = 0;
        shopStage = 0;
        serving = false;
        spawnShopCustomer();
        return true;
    }

    public void stopJob() {
        if (activeJob == JOB_NONE) return;
        activeJob = JOB_NONE;
        taxiPassenger = null;
        taxiDest = null;
        waiterCustomer = null;
        carryingPackage = false;
    }

    // ================= برچسب دکمه اقدام =================

    public String actionLabel() {
        switch (activeJob) {
            case JOB_TAXI:
                if (taxiPassenger == null && taxiDest == null) return "سوار کن";
                if (taxiDest != null) return "پیاده کن";
                return null;
            case JOB_COURIER:
                return "بردار / تحویل بده";
            case JOB_WAITER:
                return waiterStage == 1 ? "سرو کن" : null;
            case JOB_SHOPKEEPER:
                return shopStage == 1 ? "خدمت بده" : null;
            default:
                return null;
        }
    }

    public boolean doAction() {
        switch (activeJob) {
            case JOB_TAXI: return taxiAction();
            case JOB_COURIER: return courierAction();
            case JOB_WAITER: return waiterAction();
            case JOB_SHOPKEEPER: return shopkeeperAction();
            default: return false;
        }
    }

    // ================= به‌روزرسانی =================

    public void update(float dt, Player p) {
        switch (activeJob) {
            case JOB_TAXI: updateTaxi(dt, p); break;
            case JOB_COURIER: updateCourier(dt, p); break;
            case JOB_WAITER: updateWaiter(dt, p); break;
            case JOB_SHOPKEEPER: updateShopkeeper(dt, p); break;
        }
        // خروج از وسیله = پایان شیفت رانندگی
        if ((activeJob == JOB_TAXI || activeJob == JOB_COURIER) && p.driving == null) {
            stopJob();
        }
        // خروج از مغازه = پایان شیفت داخل
        if ((activeJob == JOB_WAITER || activeJob == JOB_SHOPKEEPER) && world.interior == null) {
            stopJob();
        }
    }

    private void updateTaxi(float dt, Player p) {
        if (p.driving == null) return;
        if (taxiPassenger == null && taxiDest == null) {
            taxiRespawn -= dt;
            if (taxiRespawn <= 0f) {
                float[] spot = world.randomWalkableNear(p.x, p.y, 26f * G.TILE, false);
                if (spot != null) {
                    Npc cust = new Npc(spot[0], spot[1], rnd.nextInt(2));
                    cust.say("تاکسی! 🚕");
                    taxiPassenger = cust;
                    world.cityNpcs.add(cust);
                    taxiBoardX = spot[0];
                    taxiBoardY = spot[1];
                    ui.toast("یک مسافر منتظر است! نشان تو را می‌بینی.");
                } else {
                    taxiRespawn = 2f;
                }
            }
        }
    }

    private boolean taxiAction() {
        Player p = player;
        if (taxiPassenger != null && p.driving != null
                && G.dist(p.x, p.y, taxiBoardX, taxiBoardY) < 110f) {
            taxiDest = world.randomWalkableNear(p.x + (rnd.nextBoolean() ? 1 : -1) * 30f * G.TILE,
                    p.y + (rnd.nextBoolean() ? 1 : -1) * 24f * G.TILE, 6f * G.TILE, false);
            if (taxiDest == null) taxiDest = new float[]{G.WORLD_W / 2, G.WORLD_H / 2};
            world.cityNpcs.remove(taxiPassenger);
            taxiPassenger = null;
            SoundManager.play("door");
            ui.toast("مسافر سوار شد! برو سمت مقصد و دکمه «پیاده کن» را بزن");
            return true;
        }
        if (taxiDest != null && p.driving != null && G.dist(p.x, p.y, taxiDest[0], taxiDest[1]) < 120f) {
            float d = G.dist(p.x, p.y, taxiBoardX, taxiBoardY);
            int fare = 120 + (int) (d / G.TILE) * 8;
            p.addMoney(fare);
            SoundManager.play("coin");
            ui.toast(" کرایه: " + UIManager.faMoney(fare) + " تومان! 🎉");
            taxiDest = null;
            taxiRespawn = 2f;
            return true;
        }
        return false;
    }

    private void updateCourier(float dt, Player p) {
        if (p.driving == null || carryingPackage) return;
        if (packageSpot == null) {
            taxiRespawn -= dt;
            if (taxiRespawn <= 0f) {
                float[] spot = world.randomWalkableNear(p.x, p.y, 24f * G.TILE, false);
                if (spot != null) {
                    packageSpot = spot;
                    ui.toast("یک بسته منتظر است — برو بردارش!");
                } else {
                    taxiRespawn = 2f;
                }
            }
        }
    }

    private float[] packageSpot = null;

    private boolean courierAction() {
        Player p = player;
        if (!carryingPackage && packageSpot != null && G.dist(p.x, p.y, packageSpot[0], packageSpot[1]) < 110f) {
            carryingPackage = true;
            float[] dest = world.randomWalkableNear(p.x + (rnd.nextBoolean() ? 1 : -1) * 28f * G.TILE,
                    p.y + (rnd.nextBoolean() ? 1 : -1) * 22f * G.TILE, 6f * G.TILE, false);
            packageSpot = dest != null ? dest : new float[]{G.WORLD_W / 2, G.WORLD_H / 2};
            SoundManager.play("click");
            ui.toast("بسته برداشتی! ببرش به مقصد");
            return true;
        }
        if (carryingPackage && packageSpot != null && G.dist(p.x, p.y, packageSpot[0], packageSpot[1]) < 120f) {
            int pay = 90 + rnd.nextInt(80);
            p.addMoney(pay);
            carryingPackage = false;
            packageSpot = null;
            taxiRespawn = 2f;
            SoundManager.play("coin");
            ui.toast("تحویل داده شد! دستمزد: " + UIManager.faMoney(pay) + " تومان 🎉");
            return true;
        }
        return false;
    }

    private void spawnWaiterCustomer() {
        if (world.interior == null) return;
        Npc cust = new Npc(200f + rnd.nextFloat() * (world.interior.roomW - 400f),
                160f + rnd.nextFloat() * (world.interior.roomH - 260f), rnd.nextInt(2));
        cust.say("لطفاً سفارش بده!");
        waiterCustomer = cust;
        world.interiorNpcs.add(cust);
    }

    private void updateWaiter(float dt, Player p) {
        if (world.interior == null || !world.interior.floorType.equals("restaurant")) return;
        if (waiterCustomer != null) {
            waiterCustomer.update(dt, world);
            if (G.dist(p.x, p.y, waiterCustomer.x, waiterCustomer.y) < 120f) {
                waiterStage = 1;
            } else {
                waiterStage = 0;
            }
        }
    }

    private boolean waiterAction() {
        if (waiterStage == 1 && waiterCustomer != null) {
            int tip = 40 + rnd.nextInt(50);
            player.addMoney(tip);
            waiterServedCount++;
            world.interiorNpcs.remove(waiterCustomer);
            waiterCustomer = null;
            waiterStage = 0;
            SoundManager.play("coin");
            ui.toast("سرو شد! انعام: " + UIManager.faMoney(tip) + " تومان 🍽");
            spawnWaiterCustomer();
            return true;
        }
        return false;
    }

    private void spawnShopCustomer() {
        if (world.interior == null) return;
        Npc cust = new Npc(world.interior.roomW - 120f, world.interior.roomH - 120f, rnd.nextInt(2));
        cust.say("سلام! حساب کن 🛒");
        waiterCustomer = cust;
        world.interiorNpcs.add(cust);
    }

    private void updateShopkeeper(float dt, Player p) {
        if (world.interior == null) return;
        if (waiterCustomer != null) {
            waiterCustomer.update(dt, world);
            if (G.dist(p.x, p.y, waiterCustomer.x, waiterCustomer.y) < 130f) {
                shopStage = 1;
            } else {
                shopStage = 0;
            }
        }
    }

    private boolean shopkeeperAction() {
        if (shopStage == 1 && waiterCustomer != null) {
            int pay = 45 + rnd.nextInt(45);
            player.addMoney(pay);
            shopServedCount++;
            world.interiorNpcs.remove(waiterCustomer);
            waiterCustomer = null;
            shopStage = 0;
            SoundManager.play("coin");
            ui.toast("خرید انجام شد! دستمزد: " + UIManager.faMoney(pay) + " تومان 🛒");
            spawnShopCustomer();
            return true;
        }
        return false;
    }

    /**
     * نشانگر مقصد فعلی برای رسم روی دنیا
     */
    public float[] currentMarker() {
        if (activeJob == JOB_TAXI) {
            if (taxiPassenger != null) return new float[]{taxiBoardX, taxiBoardY};
            if (taxiDest != null) return taxiDest;
        }
        if (activeJob == JOB_COURIER && packageSpot != null) return packageSpot;
        return null;
    }

    public int markerColor() {
        return activeJob == JOB_TAXI ? 0xFFFFEB3B : 0xFFFF7043;
    }

    public String jobName() {
        switch (activeJob) {
            case JOB_TAXI: return "تاکسی";
            case JOB_COURIER: return "پیک";
            case JOB_WAITER: return "گارسون";
            case JOB_SHOPKEEPER: return "صندوق‌دار";
            default: return null;
        }
    }

    public ArrayList<Npc> emptyListForCompile() {
        return new ArrayList<>();
    }
}
