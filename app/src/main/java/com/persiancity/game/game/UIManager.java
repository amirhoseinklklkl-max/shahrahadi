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
    private final RectF btnExitInt = new RectF();   // ✅ دکمه خروج از ساختمان
    public final RectF btnSkipMovie = new RectF();
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
        state = UI_INFO;
        view.joystick.reset();
    }

    public void closeInfo() {
        infoTitle = null;
        infoText = null;
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
        btnPause.set(margin, h - margin - 68f, margin + 146f, h - margin);
        btnExitInt.set(margin, h - margin - 178f, margin + 224f, h - margin - 84f);
        btnSkipMovie.set(w / 2f - 130f, h - 90f, w / 2f + 130f, h - 20f);
    }

    /**
     * آیا نقطه‌ی لمس روی یکی از دکمه‌های حالت بازی است؟
     * (تا اهرم زیر دکمه‌ها شروع نشود و دکمه‌ها درست کار کنند)
     */
    public boolean isPlayButtonAt(float x, float y) {
        return btnAction.contains(x, y) || btnEnter.contains(x, y)
                || btnLook.contains(x, y) || btnPause.contains(x, y)
                || btnExitInt.contains(x, y);
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
                    // ✅ دکمه خروج از ساختمان — همیشه داخل محیط داخلی در دسترس است
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
        mm.draw(c, vw - mmW - 20f, 90f, mmW, pl.x, pl.y);

        // دکمه مکث
        p.setColor(0xE6FFFFFF);
        c.drawRoundRect(btnPause, 14, 14, p);
        p.setColor(0xFF37474F);
        p.setTextSize(42f);
        c.drawText("⏸", btnPause.centerX(), btnPause.centerY() + 15f, p);

        // دکمه اقدام
        String al = actionLabel;
        if (pl.ridingTrain) al = "پیاده شو";
        if (al != null) {
            drawButton(c, btnAction, "✋ " + al, 0xFFFF9800, 32f);
        }
        if (nearBuilding != null) {
            drawButton(c, btnEnter, "🚪 وارد شو", 0xFF43A047, 30f);
            drawButton(c, btnLook, "👀 نگاه کن", 0xFF1E88E5, 30f);
        }

        // ✅ دکمه خروج از ساختمان — داخل محیط داخلی همیشه دیده می‌شود
        if (view.world.interior != null && state == UI_PLAY) {
            drawButton(c, btnExitInt, "🚪 خروج", 0xFFE53935, 30f);
        }

        // فیلم سینما: دکمه رد کردن
        if (view.moviePlaying) {
            drawButton(c, btnSkipMovie, "رد کردن فیلم ⏭", 0xFF6A1B9A, 24f);
        }

        // پیام
        if (toast != null && toastTimer > 0f) {
            p.setTextSize(33f);
            float tw = p.measureText(toast) + 70f;
            p.setColor(0xE637474F);
            c.drawRoundRect(vw / 2f - tw / 2f, vh - 160f, vw / 2f + tw / 2f, vh - 88f, 18, 18, p);
            p.setColor(0xFFFFFFFF);
            c.drawText(toast, vw / 2f, vh - 113f, p);
        }

        // مأموریت
        String mission = view.missions.hudText();
        if (mission != null && state == UI_PLAY) {
            p.setTextSize(27f);
            float tw = p.measureText(mission) + 52f;
            p.setColor(0xD9FFF3E0);
            c.drawRoundRect(vw / 2f - tw / 2f, 14f, vw / 2f + tw / 2f, 68f, 16, 16, p);
            p.setColor(0xFFE65100);
            c.drawText("🎯 " + mission, vw / 2f, 50f, p);
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
        float pw = Math.min(700f, vw * 0.78f);
        int n = menu.items.size();
        float rowH = 88f;
        float ph = 128f + n * rowH;
        float px = vw / 2f - pw / 2f;
        float py = Math.max(40f, vh / 2f - ph / 2f);
        menuPanel.set(px, py, px + pw, py + ph);

        p.setColor(0xF7FFF8E1);
        c.drawRoundRect(menuPanel, 24, 24, p);
        p.setColor(0xFFFF9800);
        c.drawRoundRect(px, py, px + pw, py + 70f, 24, 24, p);
        p.setColor(0xFFFF9800);
        c.drawRect(px, py + 36f, px + pw, py + 70f, p);
        p.setColor(0xFFFFFFFF);
        p.setTextSize(31f);
        c.drawText(menu.title, vw / 2f, py + 46f, p);

        menuItemRects.clear();
        for (int i = 0; i < n; i++) {
            MenuItem it = menu.items.get(i);
            float ry = py + 84f + i * rowH;
            RectF rr = new RectF(px + 20f, ry, px + pw - 20f, ry + rowH - 12f);
            menuItemRects.add(rr);
            p.setColor(0xFFFFFFFF);
            c.drawRoundRect(rr, 14, 14, p);
            p.setColor(0xFF4E342E);
            p.setTextAlign(Paint.Align.RIGHT);
            p.setTextSize(28f);
            c.drawText(it.name, rr.right - 20f, rr.top + 34f, p);
            if (it.desc != null && !it.desc.isEmpty()) {
                p.setColor(0xFF8D6E63);
                p.setTextSize(21f);
                c.drawText(it.desc, rr.right - 20f, rr.top + 62f, p);
            }
            p.setTextAlign(Paint.Align.CENTER);
        }
    }

    private void drawInfo(Canvas c) {
        float pw = Math.min(780f, vw * 0.86f);
        float ph = Math.min(560f, vh * 0.8f);
        float px = vw / 2f - pw / 2f;
        float py = vh / 2f - ph / 2f;
        infoPanel.set(px, py, px + pw, py + ph);

        p.setColor(0x66000000);
        c.drawRect(0, 0, vw, vh, p);
        p.setColor(0xFFFFFFFF);
        c.drawRoundRect(infoPanel, 26, 26, p);
        p.setColor(0xFFFF9800);
        c.drawRoundRect(px, py, px + pw, py + 76f, 26, 26, p);
        p.setColor(0xFFFF9800);
        c.drawRect(px, py + 40f, px + pw, py + 76f, p);
        p.setColor(0xFFFFFFFF);
        p.setTextSize(31f);
        c.drawText(infoTitle, vw / 2f, py + 50f, p);

        p.setColor(0xFF4E342E);
        p.setTextSize(25f);
        p.setTextAlign(Paint.Align.RIGHT);
        // چند خطی
        String[] lines = infoText.split("\n");
        float ly = py + 122f;
        for (String line : lines) {
            if (line.length() > 40) {
                // شکستن خط طولانی
                int cut = 40;
                for (int i = 34; i < Math.min(line.length(), 40); i++) {
                    if (line.charAt(i) == ' ') { cut = i; break; }
                }
                c.drawText(line.substring(0, cut), px + pw - 30f, ly, p);
                ly += 38f;
                c.drawText(line.substring(cut), px + pw - 30f, ly, p);
                ly += 40f;
            } else {
                c.drawText(line, px + pw - 30f, ly, p);
                ly += 40f;
            }
        }
        p.setTextAlign(Paint.Align.CENTER);
        p.setColor(0xFF9E9E9E);
        p.setTextSize(19f);
        c.drawText("برای بستن، بیرون باکس را لمس کن", vw / 2f, py + ph - 22f, p);
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
        p.setTextSize(29f);
        c.drawText(label, r.centerX(), r.centerY() + 10f, p);
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
}
