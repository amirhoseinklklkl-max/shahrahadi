package com.persiancity.game.game;

import com.persiancity.game.SoundManager;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.Locale;

/**
 * رابط کاربری: اهرم، دکمه‌ها، منوها، باکس اطلاعات (نگاه کن)، مینی‌مپ، پیام‌ها
 */
public class UIManager {
    public static final int UI_PLAY = 0;
    public static final int UI_MENU = 1;
    public static final int UI_INFO = 2;
    public static final int UI_PAUSE = 3;

    public int state = UI_PLAY;

    private final GameView view;
    public MiniMap mm;

    // دکمه‌ها
    private final RectF btnAction = new RectF();
    private final RectF btnEnter = new RectF();
    private final RectF btnLook = new RectF();
    private final RectF btnPause = new RectF();
    private final RectF btnExitInt = new RectF();   // ✅ دکمه خروج از ساختمان — وسط پایین صفحه
    private final RectF btnRideHorse = new RectF(); // 🐴 دکمه سوار شدن به اسب
    public final RectF btnSkipMovie = new RectF();
    // ✅ دکمه‌های قبول/رد مأموریت
    private final RectF btnYes = new RectF();
    private final RectF btnNo = new RectF();
    public boolean decisionMode = false;   // باکس اطلاعات با دکمه قبول/رد
    private final ArrayList<RectF> menuItemRects = new ArrayList<>();
    private final RectF menuPanel = new RectF();
    private final RectF infoPanel = new RectF();

    // منو
    public static class MenuItem {
        public final String name;
        public final String desc;
        public final Runnable action;
        public MenuItem(String n, String d, Runnable a) { name = n; desc = d; action = a; }
    }

    public static class Menu {
        public final String title;
        public final ArrayList<MenuItem> items = new ArrayList<>();
        public Menu(String t) { title = t; }
    }

    private Menu menu = null;

    // باکس اطلاعات — با کلیک باز می‌ماند تا بیرون باکس لمس شود
    private String infoTitle = null;
    private String infoText = null;

    // پیام
    private String toast = null;
    private float toastTimer = 0f;

    // وضعیت داده‌شده از GameView
    public Building nearBuilding = null;
    public String actionLabel = null;   // دکمه اقدام: «خدمت بده» / «پیاده شو» ...

    private float vw, vh;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);

    public UIManager(GameView view) {
        this.view = view;
    }

    // ================= متن و قیمت =================

    public static String faMoney(int n) {
        return G.fa(String.format(Locale.US, "%,d", n));
    }

    public void toast(String t) {
        toast = t;
        toastTimer = 2.8f;
    }

    public void openMenu(Menu m) {
        menu = m;
        state = UI_MENU;
        view.joystick.reset();
    }

    public void closeMenu() {
        menu = null;
        state = UI_PLAY;
    }

    public boolean isMenuOpen() {
        return state == UI_MENU;
    }

    /**
     * باکس اطلاعات — باز می‌ماند تا لمس بیرون باکس
     */
    public void openInfo(String title, String text) {
        infoTitle = title;
        infoText = text;
        decisionMode = false;
        state = UI_INFO;
        view.joystick.reset();
    }

    /**
     * ✅ باکس اطلاعات با دو دکمه: سبز «قبول» و قرمز «رد» (پیشنهاد مأموریت NPC)
     */
    public void openDecision(String title, String text) {
        infoTitle = title;
        infoText = text;
        decisionMode = true;
        state = UI_INFO;
        view.joystick.reset();
    }

    public void closeInfo() {
        infoTitle = null;
        infoText = null;
        decisionMode = false;
        state = UI_PLAY;
    }

    // ================= چیدمان =================

    public void layout(float w, float h) {
        vw = w;
        vh = h;
        // ✅ دکمه‌های بزرگ‌تر (درخواست کاربر — قبلاً خیلی کوچک بودند)
        float bs = Math.min(260f, Math.max(180f, h * 0.31f));
        float margin = 26f;
        btnAction.set(w - margin - bs, h - margin - bs, w - margin, h - margin);
        btnEnter.set(w - margin - bs * 2.6f, h - margin - bs * 0.55f, w - margin - bs * 1.3f, h - margin + bs * 0.55f);
        btnLook.set(w - margin - bs * 2.6f, h - margin - bs * 1.25f, w - margin - bs * 1.3f, h - margin - bs * 0.15f);
        // ✅ دکمه منو (مکث) — وسط بالای صفحه (درخواست کاربر)
        btnPause.set(w / 2f - 78f, 14f, w / 2f + 78f, 86f);
        // ✅ دکمه خروج — وسط پایین صفحه (درخواست کاربر)
        btnExitInt.set(w / 2f - 122f, h - margin - 88f, w / 2f + 122f, h - margin);
        btnSkipMovie.set(w / 2f - 130f, h - margin - 178f, w / 2f + 130f, h - margin - 110f);
        // 🐴 دکمه سوار اسب — بالای دکمه اقدام
        btnRideHorse.set(w - margin - bs, h - margin - bs * 2.25f, w - margin, h - margin - bs * 1.35f);
    }

    /**
     * آیا نقطه‌ی لمس روی یکی از دکمه‌های حالت بازی است؟
     * (تا اهرم زیر دکمه‌ها شروع نشود و دکمه‌ها درست کار کنند)
     */
    public boolean isPlayButtonAt(float x, float y) {
        return btnAction.contains(x, y) || btnEnter.contains(x, y)
                || btnLook.contains(x, y) || btnPause.contains(x, y)
                || btnExitInt.contains(x, y)
                || (horseButtonVisible() && btnRideHorse.contains(x, y));
    }

    /** آیا دکمه سوار اسب این لحظه نمایش داده می‌شود؟ */
    public boolean horseButtonVisible() {
        return view.rideHorseNear && view.world.interior == null
                && playerSafeForHorse();
    }

    private boolean playerSafeForHorse() {
        return view.player.driving == null && !view.player.ridingTrain && !view.player.swimming;
    }

    // ================= لمس =================

    /**
     * @return شناسه دکمه فشرده‌شده یا null
     */
    public String handleTouch(int action, float x, float y) {
        if (action == android.view.MotionEvent.ACTION_UP || action == android.view.MotionEvent.ACTION_POINTER_UP) {
            switch (state) {
                case UI_PLAY:
                    if (btnAction.contains(x, y)) return "action";
                    // 🐴 دکمه سوار اسب — فقط وقتی اسبی نزدیک است و دکمه دیده می‌شود
                    if (actionLabel == null && horseButtonVisible()
                            && btnRideHorse.contains(x, y)) return "ridehorse";
                    // ✅ دکمه خروج از ساختمان — وسط پایین، همیشه داخل محیط داخلی در دسترس است
                    if (view.world.interior != null && btnExitInt.contains(x, y)) return "exitint";
                    if (nearBuilding != null && btnEnter.contains(x, y)) return "enter";
                    if (nearBuilding != null && btnLook.contains(x, y)) return "look";
                    if (btnPause.contains(x, y)) return "pause";
                    break;

                case UI_MENU: {
                    if (btnPause.contains(x, y)) { closeMenu(); return null; }
                    for (int i = 0; i < menuItemRects.size() && menu != null; i++) {
                        if (i < menu.items.size() && menuItemRects.get(i).contains(x, y)) {
                            SoundManager.play("click");
                            Runnable r = menu.items.get(i).action;
                            if (r != null) r.run();
                            return "item:" + i;
                        }
                    }
                    // لمس بیرون پنل = بستن منو
                    if (!menuPanel.contains(x, y)) {
                        closeMenu();
                    }
                    break;
                }

                case UI_INFO:
                    // ✅ دکمه‌های قبول/رد مأموریت
                    if (decisionMode) {
                        if (btnYes.contains(x, y)) return "yes";
                        if (btnNo.contains(x, y)) return "no";
                    }
                    // باکس باز می‌ماند؛ فقط لمس بیرون باکس می‌بندد
                    if (!infoPanel.contains(x, y)) {
                        closeInfo();
                        SoundManager.play("click");
                    }
                    break;

                case UI_PAUSE: {
                    float pw = Math.min(560f, vw * 0.55f);
                    float px = vw / 2f - pw / 2f;
                    float py = vh / 2f - 270f;
                    if (x > px && x < px + pw) {
                        for (int i = 0; i < 4; i++) {
                            float by = py + 96f + i * 106f;
                            if (y > by && y < by + 92f) {
                                return new String[]{"resume", "sound", "save", "exitmenu"}[i];
                            }
                        }
                    }
                    break;
                }
            }
        }
        return null;
    }

    public boolean outsideTapConsumed(float x, float y) {
        return state == UI_INFO && !infoPanel.contains(x, y);
    }

    // ================= به‌روزرسانی و رسم =================

    public void update(float dt) {
        if (toastTimer > 0f) toastTimer -= dt;
    }

    public void draw(Canvas c, float viewW, float viewH) {
        vw = viewW;
        vh = viewH;
        p.setTextAlign(Paint.Align.CENTER);

        // نوار بالا: پول + ساعت
        Player pl = view.player;
        float bw = 310f;
        p.setColor(0xE6FFFFFF);
        c.drawRoundRect(20f, 14f, 20f + bw, 78f, 16, 16, p);
        p.setColor(0xFFE65100);
        p.setTextSize(34f);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText("💰 " + faMoney(pl.money), 20f + bw / 2f, 58f, p);

        String clock = view.world.dayNight.clockText() + (view.world.dayNight.isNight() ? " 🌙" : " ☀");
        p.setColor(0xE6FFFFFF);
        c.drawRoundRect(20f + bw + 12f, 14f, 20f + bw + 172f, 78f, 16, 16, p);
        p.setColor(0xFF37474F);
        p.setTextSize(32f);
        c.drawText(clock, 20f + bw + 97f, 58f, p);

        // نوارهای گرسنگی و انرژی
        drawBar(c, 20f + bw + 184f, 14f, 155f, pl.hunger, 0xFFEF5350, "🍽");
        drawBar(c, 20f + bw + 184f, 48f, 155f, pl.energy, 0xFF66BB6A, "⚡");

        // مینی‌مپ با نشانگرهای پویا (هر دو قطار + ماشین‌های کاربر)
        float mmW = Math.min(320f, vw * 0.22f);
        mm.clearDynamic();
        for (int i = 0; i < view.world.vehicles.size(); i++) {
            Vehicle v = view.world.vehicles.get(i);
            if (v.type == Vehicle.CAR_TRAIN && v.isActive()) {
                mm.addDynamic(v.x, v.y, v.railIndex == 0 ? 0xFFD32F2F : 0xFFFB8C00);
            }
            if (v.owned && (v.mode == Vehicle.MODE_PARKED || v.mode == Vehicle.MODE_GRAZE)
                    && v.type != Vehicle.CAR_TRAIN && v.type != Vehicle.CAR_HORSE) {
                mm.addDynamic(v.x, v.y, 0xFF00ACC1);
            }
        }
        float mmH = mm.draw(c, vw - mmW - 20f, 90f, mmW, pl.x, pl.y);

        // ✅ علامت کلاکت + «جایزه» درخشان — گوشه راست پایین مینی‌مپ
        // (برای جایزه تبلیغ — فعلاً فقط تزئینی است)
        drawClapperReward(c, vw - mmW - 20f, 90f, mmW, mmH, view.world.dayNight.minutes);

        // دکمه منو (مکث) — وسط بالای صفحه
        p.setColor(0xE6FFFFFF);
        c.drawRoundRect(btnPause, 16, 16, p);
        p.setColor(0xFFE65100);
        p.setTextSize(34f);
        c.drawText("⏸ منو", btnPause.centerX(), btnPause.centerY() + 12f, p);

        // دکمه اقدام
        String al = actionLabel;
        if (pl.ridingTrain) al = "پیاده شو";
        if (al != null) {
            drawButton(c, btnAction, "✋ " + al, 0xFFFF9800, 32f);
        }
        // 🐴 دکمه سوار اسب — وقتی اسبی نزدیک است (درخواست کاربر)
        if (horseButtonVisible() && al == null) {
            drawButton(c, btnRideHorse, "🐴 سوار اسب", 0xFF6D4C41, 30f);
        }
        if (nearBuilding != null) {
            drawButton(c, btnEnter, "🚪 وارد شو", 0xFF43A047, 30f);
            drawButton(c, btnLook, "👀 نگاه کن", 0xFF1E88E5, 30f);
        }

        // ✅ دکمه خروج از ساختمان — وسط پایین صفحه (درخواست کاربر)
        if (view.world.interior != null && state == UI_PLAY) {
            drawButton(c, btnExitInt, "🚪 خروج", 0xFFE53935, 34f);
        }

        // فیلم سینما: دکمه رد کردن
        if (view.moviePlaying) {
            drawButton(c, btnSkipMovie, "رد کردن فیلم ⏭", 0xFF6A1B9A, 26f);
        }

        // ✅ فلش جهت مأموریت — مثلثی به سمت محل انجام مأموریت (درخواست کاربر)
        drawMissionArrow(c);

        // پیام (بالا آمده تا زیر دکمه‌های خروج و رد کردن فیلم نیاید)
        if (toast != null && toastTimer > 0f) {
            p.setTextSize(36f);
            float tw = p.measureText(toast) + 76f;
            p.setColor(0xE637474F);
            c.drawRoundRect(vw / 2f - tw / 2f, vh - 296f, vw / 2f + tw / 2f, vh - 218f, 18, 18, p);
            p.setColor(0xFFFFFFFF);
            c.drawText(toast, vw / 2f, vh - 244f, p);
        }

        // مأموریت — زیر دکمه منو (وسط بالا)
        String mission = view.missions.hudText();
        if (mission != null && state == UI_PLAY) {
            p.setTextSize(28f);
            float tw = p.measureText(mission) + 52f;
            p.setColor(0xD9FFF3E0);
            c.drawRoundRect(vw / 2f - tw / 2f, 100f, vw / 2f + tw / 2f, 152f, 16, 16, p);
            p.setColor(0xFFE65100);
            c.drawText("🎯 " + mission, vw / 2f, 134f, p);
        }

        // پنل منو
        if (state == UI_MENU && menu != null) {
            drawMenu(c);
        }

        // باکس اطلاعات
        if (state == UI_INFO && infoTitle != null) {
            drawInfo(c);
        }

        // منوی مکث
        if (state == UI_PAUSE) {
            drawPause(c);
        }

        // اهرم
        drawJoystick(c);
    }

    private void drawBar(Canvas c, float x, float y, float w, float v, int color, String icon) {
        p.setColor(0xE6FFFFFF);
        c.drawRoundRect(x, y, x + w, y + 26f, 12, 12, p);
        p.setColor(color);
        c.drawRoundRect(x + 3f, y + 3f, x + 3f + (w - 6f) * G.clamp(v / 100f, 0f, 1f), y + 23f, 9, 9, p);
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(20f);
        c.drawText(icon, x + 8f, y + 20f, p);
        p.setTextAlign(Paint.Align.CENTER);
    }

    private void drawButton(Canvas c, RectF r, String label, int color, float size) {
        p.setColor(color);
        c.drawRoundRect(r, 22, 22, p);
        p.setColor(0x33000000);
        c.drawRoundRect(r.left, r.bottom - 10f, r.right, r.bottom, 22, 22, p);
        p.setColor(color);
        c.drawRoundRect(r.left, r.top, r.right, r.bottom - 6f, 22, 22, p);
        p.setColor(0xFFFFFFFF);
        p.setTextSize(size);
        c.drawText(label, r.centerX(), r.centerY() + size * 0.35f, p);
    }

    private void drawMenu(Canvas c) {
        float pw = Math.min(760f, vw * 0.82f);
        int n = menu.items.size();
        float rowH = 100f;
        float ph = 140f + n * rowH;
        float px = vw / 2f - pw / 2f;
        float py = Math.max(40f, vh / 2f - ph / 2f);
        menuPanel.set(px, py, px + pw, py + ph);

        p.setColor(0xF7FFF8E1);
        c.drawRoundRect(menuPanel, 24, 24, p);
        p.setColor(0xFFFF9800);
        c.drawRoundRect(px, py, px + pw, py + 78f, 24, 24, p);
        p.setColor(0xFFFF9800);
        c.drawRect(px, py + 40f, px + pw, py + 78f, p);
        p.setColor(0xFFFFFFFF);
        p.setTextSize(35f);
        c.drawText(menu.title, vw / 2f, py + 52f, p);

        menuItemRects.clear();
        for (int i = 0; i < n; i++) {
            MenuItem it = menu.items.get(i);
            float ry = py + 94f + i * rowH;
            RectF rr = new RectF(px + 20f, ry, px + pw - 20f, ry + rowH - 12f);
            menuItemRects.add(rr);
            p.setColor(0xFFFFFFFF);
            c.drawRoundRect(rr, 14, 14, p);
            p.setColor(0xFF4E342E);
            p.setTextAlign(Paint.Align.RIGHT);
            p.setTextSize(31f);
            c.drawText(it.name, rr.right - 20f, rr.top + 40f, p);
            if (it.desc != null && !it.desc.isEmpty()) {
                p.setColor(0xFF8D6E63);
                p.setTextSize(24f);
                c.drawText(it.desc, rr.right - 20f, rr.top + 72f, p);
            }
            p.setTextAlign(Paint.Align.CENTER);
        }
    }

    private void drawInfo(Canvas c) {
        float pw = Math.min(860f, vw * 0.9f);
        float ph = Math.min(640f, vh * 0.88f);
        float px = vw / 2f - pw / 2f;
        float py = vh / 2f - ph / 2f;
        infoPanel.set(px, py, px + pw, py + ph);

        p.setColor(0x66000000);
        c.drawRect(0, 0, vw, vh, p);
        p.setColor(0xFFFFFFFF);
        c.drawRoundRect(infoPanel, 26, 26, p);
        p.setColor(0xFFFF9800);
        c.drawRoundRect(px, py, px + pw, py + 84f, 26, 26, p);
        p.setColor(0xFFFF9800);
        c.drawRect(px, py + 44f, px + pw, py + 84f, p);
        p.setColor(0xFFFFFFFF);
        p.setTextSize(37f);
        c.drawText(infoTitle, vw / 2f, py + 57f, p);

        p.setColor(0xFF4E342E);
        p.setTextSize(31f);   // ✅ متن باکس اطلاعات بزرگ‌تر شد (درخواست کاربر)
        p.setTextAlign(Paint.Align.RIGHT);
        // چند خطی
        String[] lines = infoText.split("\n");
        float ly = py + 136f;
        for (String line : lines) {
            if (line.length() > 34) {
                // شکستن خط طولانی
                int cut = 34;
                for (int i = 28; i < Math.min(line.length(), 34); i++) {
                    if (line.charAt(i) == ' ') { cut = i; break; }
                }
                c.drawText(line.substring(0, cut), px + pw - 32f, ly, p);
                ly += 46f;
                c.drawText(line.substring(cut), px + pw - 32f, ly, p);
                ly += 48f;
            } else {
                c.drawText(line, px + pw - 32f, ly, p);
                ly += 48f;
            }
        }
        p.setTextAlign(Paint.Align.CENTER);

        if (decisionMode) {
            // ✅ دو دکمه پایین باکس: سبز «قبول» و قرمز «رد» (درخواست کاربر)
            float byw = pw * 0.42f;
            float by = py + ph - 118f;
            btnYes.set(px + pw - 24f - byw, by, px + pw - 24f, by + 88f);
            btnNo.set(px + 24f, by, px + 24f + byw, by + 88f);
            drawButton(c, btnYes, "✔ قبول", 0xFF43A047, 34f);
            drawButton(c, btnNo, "✖ رد", 0xFFE53935, 34f);
        } else {
            p.setColor(0xFF9E9E9E);
            p.setTextSize(23f);
            c.drawText("برای بستن، بیرون باکس را لمس کن", vw / 2f, py + ph - 24f, p);
        }
    }

    private void drawPause(Canvas c) {
        p.setColor(0x88000000);
        c.drawRect(0, 0, vw, vh, p);
        float pw = Math.min(560f, vw * 0.55f);
        float px = vw / 2f - pw / 2f;
        float py = vh / 2f - 270f;

        p.setColor(0xFFFFFFFF);
        c.drawRoundRect(px, py, px + pw, py + 540f, 26, 26, p);
        p.setColor(0xFFE65100);
        p.setTextSize(34f);
        c.drawText("مکث", vw / 2f, py + 64f, p);

        drawPauseBtn(c, px + 30f, py + 96f, pw - 60f, "▶ ادامه بازی", 0xFF43A047);
        drawPauseBtn(c, px + 30f, py + 202f, pw - 60f,
                SoundManager.isMuted() ? "🔇 صدا: خاموش" : "🔊 صدا: روشن", 0xFF1E88E5);
        drawPauseBtn(c, px + 30f, py + 308f, pw - 60f, "💾 ذخیره خودکار فعال ✔", 0xFFF9A825);
        drawPauseBtn(c, px + 30f, py + 414f, pw - 60f, "🚪 خروج به منو", 0xFFE53935);
    }

    private void drawPauseBtn(Canvas c, float x, float y, float w, String label, int color) {
        RectF r = new RectF(x, y, x + w, y + 92f);
        p.setColor(color);
        c.drawRoundRect(r, 18, 18, p);
        p.setColor(0xFFFFFFFF);
        p.setTextSize(33f);
        c.drawText(label, r.centerX(), r.centerY() + 11f, p);
    }

    private void drawJoystick(Canvas c) {
        if (state != UI_PLAY || view.moviePlaying) return;
        Joystick j = view.joystick;
        float bx = j.baseX, by = j.baseY;
        if (!j.active) {
            bx = 150f;
            by = vh - 160f;
        }
        p.setColor(0x88FFFFFF);
        c.drawCircle(bx, by, j.getRadius(), p);
        p.setColor(0x6690A4AE);
        c.drawCircle(bx, by, j.getRadius() * 0.72f, p);
        p.setColor(0xF2FF7043);
        float kx = j.active ? j.knobX : bx;
        float ky = j.active ? j.knobY : by;
        c.drawCircle(kx, ky, j.getRadius() * 0.4f, p);
        p.setColor(0xFFFFFFFF);
        c.drawCircle(kx - 6f, ky - 8f, j.getRadius() * 0.16f, p);
    }

    // ================= کلاکت جایزه 🎬 =================

    /**
     * ✅ علامت کلاکت در گوشهٔ راست پایین مینی‌مپ + کلمهٔ «جایزه» با افکت درخشش
     * (جای دکمهٔ جایزهٔ تبلیغی — فعلاً تزئینی، بعداً جایزه داده می‌شود)
     */
    private void drawClapperReward(Canvas c, float mmLeft, float mmTop, float mmW, float mmH, float t) {
        float bw = 96f, bh = 78f;
        float bx = mmLeft + mmW - bw + 10f;      // کمی بیرون‌زده از گوشهٔ راست پایین نقشه
        float by = mmTop + mmH - 30f;
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 2.6f);

        // بدنهٔ کلاکت
        p.setColor(0xF7263238);
        c.drawRoundRect(bx, by, bx + bw, by + bh, 14, 14, p);
        p.setColor(0xFFFFFFFF);
        c.drawRoundRect(bx + 5f, by + 5f, bx + bw - 5f, by + bh - 5f, 10, 10, p);

        // کلاکت: بدنهٔ مشکی با خطوط مورب + بالهٔ بالا
        float ix = bx + 12f, iy = by + 34f;
        p.setColor(0xFF263238);
        c.drawRoundRect(ix, iy, ix + 72f, iy + 30f, 4, 4, p);
        p.setColor(0xFF263238);
        c.save();
        c.rotate(-12f, ix + 36f, iy - 2f);
        c.drawRoundRect(ix, iy - 12f, ix + 72f, iy + 2f, 3, 3, p);
        p.setColor(0xFFFFFFFF);
        for (int i = 0; i < 4; i++) {
            c.save();
            c.rotate(-12f, ix + 36f, iy - 2f);
            c.drawRoundRect(ix + 6f + i * 18f, iy - 12f, ix + 15f + i * 18f, iy + 2f, 2, 2, p);
            c.restore();
        }
        c.restore();

        // حلقه‌های لولا
        p.setColor(0xFF90A4AE);
        c.drawCircle(ix + 10f, iy - 4f, 3f, p);
        c.drawCircle(ix + 62f, iy - 4f, 3f, p);

        // «جایزه» با افکت درخشش (هالهٔ نارنجی تپنده)
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(26f);
        float tx = bx + bw / 2f;
        float ty = by + bh + 26f;
        int glow = (int) (90f + pulse * 130f);
        p.setColor((glow << 24) | 0xFFFF9800);
        c.drawText("جایزه", tx - 2f, ty, p);
        c.drawText("جایزه", tx + 2f, ty, p);
        c.drawText("جایزه", tx, ty - 2f, p);
        c.drawText("جایزه", tx, ty + 2f, p);
        p.setColor((int) (200f + pulse * 55f) << 24 | 0xFFFFD54F);
        c.drawText("جایزه", tx, ty, p);
        p.setTextAlign(Paint.Align.CENTER);
    }

    // ================= فلش جهت مأموریت ➤ =================

    /**
     * ✅ وقتی مأموریتی فعال است، یک مثلث در لبهٔ صفحه به سمت محل انجام مأموریت می‌چرخد
     * (مثلاً اگر هدف سمت چپ باشد، مثلث سمت چپ صفحه رو به چپ است)
     */
    private void drawMissionArrow(Canvas c) {
        if (state != UI_PLAY || view.moviePlaying || view.videoOpen) return;
        if (view.world.interior != null) return;
        float[] mt = view.missions.activeMarker();
        if (mt == null) return;

        // تبدیل مختصات دنیا به صفحه
        float sx = (mt[0] - view.camera.x) * view.camera.zoom + vw / 2f;
        float sy = (mt[1] - view.camera.y) * view.camera.zoom + vh / 2f;

        // اگر هدف روی صفحه است، نشانگر پرنده بالای ساختمان کافی است
        if (sx > 60f && sx < vw - 60f && sy > 110f && sy < vh - 100f) return;

        float cx = vw / 2f, cy = vh / 2f;
        float ang = (float) Math.atan2(sy - cy, sx - cx);
        float dx = (float) Math.cos(ang), dy = (float) Math.sin(ang);

        // نقطهٔ برخورد جهت با مستطیل مرزی
        float mX = vw / 2f - 90f, mY = vh / 2f - 80f;
        float tX = Math.abs(dx) > 0.001f ? mX / Math.abs(dx) : Float.MAX_VALUE;
        float tY = Math.abs(dy) > 0.001f ? mY / Math.abs(dy) : Float.MAX_VALUE;
        float tt = Math.min(tX, tY);
        float ax = cx + dx * tt;
        float ay = cy + dy * tt;
        float bounce = (float) Math.sin(view.world.dayNight.minutes * 3.4f) * 8f;
        ax += dx * bounce;
        ay += dy * bounce;

        float deg = (float) Math.toDegrees(ang);
        c.save();
        c.translate(ax, ay);
        c.rotate(deg);
        // مثلث نارنجی با حاشیهٔ سفید
        p.setColor(0xFFFFFFFF);
        c.drawCircle(0f, 0f, 34f, p);
        p.setColor(0xFFFF9800);
        c.drawCircle(0f, 0f, 29f, p);
        p.setColor(0xFFFFFFFF);
        c.drawRoundRect(2f, -7f, 20f, 7f, 3, 3, p);
        p.setColor(0xFFFFFFFF);
        c.drawCircle(-14f, 0f, 7f, p);
        c.restore();

        p.setTextAlign(Paint.Align.CENTER);
        p.setColor(0xB3FFFFFF);
        p.setTextSize(22f);
        c.drawText("مأموریت", ax, ay + 56f, p);
        p.setTextAlign(Paint.Align.CENTER);
    }
}
