package com.persiancity.game.game;

import com.persiancity.game.SaveManager;
import com.persiancity.game.SoundManager;

import java.util.ArrayList;
import java.util.Random;

/**
 * مأموریت‌های شاد شهر — کارهای کوچک با جایزه
 */
public class MissionSystem {
    private static class Mission {
        final String title;
        final String desc;
        final int buildingType;   // -۱ = بدون مکان
        final int reward;
        Mission(String t, String d, int bt, int r) {
            title = t; desc = d; buildingType = bt; reward = r;
        }
    }

    private final ArrayList<Mission> missions = new ArrayList<>();
    private int current = 0;
    private int completedCount = 0;

    private final GameView view;
    private final World world;
    private final Player player;
    private final Random rnd = new Random();
    private float checkTimer = 0f;

    public MissionSystem(GameView view, World world, Player player) {
        this.view = view;
        this.world = world;
        this.player = player;
        missions.add(new Mission("به بانک سر بزن", "پول‌هایت را در بانک شادی پس‌انداز کن", Building.BANK, 150));
        missions.add(new Mission("در رستوران غذا بخور", "یک غذای خوشمزه سفارش بده", Building.RESTAURANT, 100));
        missions.add(new Mission("بلیط سینما بخر", "در سینما ستاره یک کارتون تماشا کن", Building.CINEMA, 200));
        missions.add(new Mission("به باغ‌وحش برو", "بلیط بخر و حیوان‌ها را ببین", Building.ZOO, 200));
        missions.add(new Mission("سوار قطار شو", "از ایستگاه، یک دور کامل با قطار بگرد", Building.TRAIN_STATION, 250));
        missions.add(new Mission("در مدرسه درس بخوان", "معلم‌ها منتظر تو هستند", Building.SCHOOL, 150));
        missions.add(new Mission("در سوپرمارکت کار کن", "پشت صندوق وایسا و مشتری برون", Building.MARKET, 300));
        missions.add(new Mission("کتاب بخوان", "در کتابخانه نور یک کتاب قرض بگیر", Building.LIBRARY, 120));
        missions.add(new Mission("شکارچی گنج", "برو حوالی پارک بزرگ — یک سکه پنهان است!", Building.HOME, 180));
    }

    /**
     * متن روی نوار بالای صفحه
     */
    public String hudText() {
        if (current >= missions.size()) return null;
        Mission m = missions.get(current);
        return m.title + " (" + G.fa(completedCount + 1) + "/" + G.fa(missions.size()) + ")";
    }

    public void update(float dt) {
        if (current >= missions.size()) return;
        checkTimer -= dt;
        if (checkTimer > 0f) return;
        checkTimer = 0.5f;

        Mission m = missions.get(current);
        if (m.buildingType < 0) return;
        Building b = world.buildingByType(m.buildingType);
        if (b == null) return;

        // نزدیک در ساختمان هدف باشی (و یا داخلش)
        float dx = player.x, dy = player.y;
        float d = G.dist(dx, dy, b.doorX, b.doorY);
        boolean insideTarget = world.interior != null && world.interior.building == b;
        if (d < 140f || insideTarget) {
            complete(m);
        }
    }

    /**
     * تکمیل دستی (مثلاً سوار شدن بر قطار)
     */
    public void completeByTitle(String contains) {
        if (current >= missions.size()) return;
        Mission m = missions.get(current);
        if (m.title.contains(contains)) {
            complete(m);
        }
    }

    private void complete(Mission m) {
        player.addMoney(m.reward);
        completedCount++;
        current++;
        SoundManager.play("mission");
        view.ui.toast("🎯 مأموریت انجام شد! جایزه: " + UIManager.faMoney(m.reward) + " تومان");
        if (current < missions.size()) {
            view.ui.toast("مأموریت جدید: " + missions.get(current).title);
        }
    }

    public int currentIndex() {
        return current;
    }

    public void setCurrent(int i) {
        current = Math.max(0, Math.min(i, missions.size()));
    }

    public int count() {
        return missions.size();
    }

    public String randomCheer() {
        String[] c = {"آفرین!", "عالی بود!", "تو قهرمان شهر شادی هستی!", "چه بازیکن باهوشی!"};
        return c[rnd.nextInt(c.length)];
    }
}
