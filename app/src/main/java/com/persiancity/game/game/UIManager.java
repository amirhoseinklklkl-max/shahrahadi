package com.persiancity.game.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;

import java.util.ArrayList;
import java.util.List;

/**
 * رابط کاربری روی صفحه — HUD، منوها، دیالوگ‌ها، توقف و پیام‌ها
 */
public class UIManager {

    public interface Action {
        void run();
    }

    public static class MenuItem {
        public final String label;
        public final String sub;
        public final Action action;
        public boolean enabled = true;

        public MenuItem(String label, String sub, Action action) {
            this.label = label;
            this.sub = sub;
            this.action = action;
        }
    }

    public static class Menu {
        public final String title;
        public final List<MenuItem> items = new ArrayList<>();

        public Menu(String title) {
            this.title = title;
        }
    }

    // وضعیت‌های نمایش
    public static final int UI_PLAY = 0;
    public static final int UI_DIALOG = 1;
    public static final int UI_MENU = 2;
    public static final int UI_PAUSE = 3;

    public int uiState = UI_PLAY;

    private final GameView view;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private float vw, vh, fs;   // عرض، ارتفاع، ضریب فونت

    // منو
    private Menu menu = null;
    private float menuScroll = 0f;
    private final ArrayList<RectF> itemRects = new ArrayList<>();
    private RectF menuPanelRect;

    // دیالوگ
    private String dlgName = "", dlgText = "";
    private final ArrayList<String> dlgOptions = new ArrayList<>();
    private final ArrayList<RectF> dlgRects = new ArrayList<>();
    private StaticLayout dlgLayout;

    // پیام‌های شناور
    private static class Toast {
        String text;
        float time;
    }
    private final ArrayList<Toast> toasts = new ArrayList<>();

    public UIManager(GameView view) {
        this.view = view;
        textP.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textP.setTextAlign(Paint.Align.CENTER);
        tp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }

    public void layout(int w, int h) {
        vw = w;
        vh = h;
        fs = Math.min(w, h) / 720f;
        if (fs < 0.7f) fs = 0.7f;
    }

    public float fontScale() {
        return fs;
    }

    // ================================================= پیام‌ها

    public void toast(String text) {
        Toast t = new Toast();
        t.text = text;
        t.time = 3.2f;
        if (toasts.size() > 3) toasts.remove(0);
        toasts.add(t);
    }

    // ================================================= منو

    public void openMenu(Menu m) {
        menu = m;
        menuScroll = 0f;
        uiState = UI_MENU;
    }

    public void closeMenu() {
        menu = null;
        uiState = UI_PLAY;
    }

    public boolean isMenuOpen() {
        return menu != null;
    }

    public Menu currentMenu() {
        return menu;
    }

    // ================================================= دیالوگ

    public void openDialogue(String name, String text, List<String> options) {
        dlgName = name;
        dlgText = text;
        dlgOptions.clear();
        dlgOptions.addAll(options);
        uiState = UI_DIALOG;
        buildDialogueLayout();
    }

    public void updateDialogueText(String text) {
        dlgText = text;
        buildDialogueLayout();
    }

    private void buildDialogueLayout() {
        float panelW = Math.min(vw * 0.86f, 980f);
        tp.setTextSize(20f * fs);
        int width = (int) (panelW - 60 * fs);
        dlgLayout = StaticLayout.Builder
                .obtain(dlgText, 0, dlgText.length(), tp, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
                .setLineSpacing(0f, 1.1f)
                .setIncludePad(false)
                .build();
    }

    public void closeDialogue() {
        uiState = UI_PLAY;
    }

    // ================================================= لمس

    /**
     * لمس در حالت‌های UI (نه بازی). اگر مصرف شود true برمی‌گرداند
     */
    public boolean onTouchUp(float x, float y) {
        switch (uiState) {
            case UI_MENU:
                return handleMenuTap(x, y);
            case UI_DIALOG:
                return handleDialogueTap(x, y);
            case UI_PAUSE:
                return handlePauseTap(x, y);
        }
        return false;
    }

    public boolean onTouchScroll(float x, float y, float dy) {
        if (uiState == UI_MENU && menuPanelRect != null) {
            menuScroll = G.clamp(menuScroll + dy, 0f, Math.max(0, menuScrollMax()));
            return true;
        }
        return false;
    }

    private float menuScrollMax() {
        if (menu == null) return 0f;
        float rowH = 62f * fs;
        float contentH = menu.items.size() * (rowH + 12f * fs);
        float panelH = menuPanelRect != null ? menuPanelRect.height() : vh * 0.7f;
        return Math.max(0f, contentH - (panelH - 120f * fs));
    }

    private boolean handleMenuTap(float x, float y) {
        for (int i = 0; i < itemRects.size(); i++) {
            RectF r = itemRects.get(i);
            if (r.contains(x, y) && menu != null && i < menu.items.size()) {
                MenuItem it = menu.items.get(i);
                if (it.enabled && it.action != null) {
                    view.fx("click");
                    it.action.run();
                    return true;
                }
            }
        }
        // بستن با لمس بیرون
        if (menuPanelRect != null && !menuPanelRect.contains(x, y)) {
            closeMenu();
        }
        return true;
    }

    private boolean handleDialogueTap(float x, float y) {
        for (int i = 0; i < dlgRects.size(); i++) {
            RectF r = dlgRects.get(i);
            if (r.contains(x, y) && i < dlgOptions.size()) {
                view.onDialogueOption(i);
                return true;
            }
        }
        return true;
    }

    private boolean handlePauseTap(float x, float y) {
        // دکمه‌های توقف در draw ساخته می‌شوند؛ اینجا همان مستطیل‌ها چک می‌شوند
        for (int i = 0; i < itemRects.size(); i++) {
            RectF r = itemRects.get(i);
            if (r.contains(x, y)) {
                view.fx("click");
                view.onPauseOption(i);
                return true;
            }
        }
        return true;
    }

    // ================================================= ترسیم

    public void update(float dt) {
        for (int i = toasts.size() - 1; i >= 0; i--) {
            toasts.get(i).time -= dt;
            if (toasts.get(i).time <= 0f) toasts.remove(i);
        }
    }

    public void drawHud(Canvas c, Player player, World world, JobSystem jobs, MissionSystem missions,
                        float joyX, float joyY, Joystick joystick, String actionLabel) {
        itemRects.clear();

        // ---------- پنل پول و ساعت (بالا-چپ) ----------
        float panelX = 16f * fs, panelY = 14f * fs;
        float panelW = 250f * fs, panelH = 92f * fs;
        p.setColor(0xD9FFFFFF);
        c.drawRoundRect(panelX, panelY, panelX + panelW, panelY + panelH, 16, 16, p);
        p.setColor(0xFFFFC107);
        c.drawCircle(panelX + 34f * fs, panelY + 30f * fs, 13f * fs, p);
        p.setColor(0xFFF57F17);
        textP.setTextSize(15f * fs);
        c.drawText("ت", panelX + 34f * fs, panelY + 35f * fs, textP);

        textP.setTextAlign(Paint.Align.RIGHT);
        textP.setTextSize(19f * fs);
        textP.setColor(0xFF33691E);
        c.drawText(faMoney(player.money) + " تومان", panelX + panelW - 14f * fs, panelY + 36f * fs, textP);

        textP.setTextSize(16f * fs);
        textP.setColor(0xFF37474F);
        c.drawText("روز " + G.fa(world.dayNight.dayCount) + " — " + world.dayNight.timeText(),
                panelX + panelW - 14f * fs, panelY + 66f * fs, textP);
        textP.setTextAlign(Paint.Align.CENTER);

        // ---------- نوارهای سیری و انرژی (زیر پنل پول) ----------
        drawBar(c, panelX, panelY + panelH + 10f * fs, 150f * fs, "انرژی", player.energy / 100f, 0xFF66BB6A);
        drawBar(c, panelX, panelY + panelH + 42f * fs, 150f * fs, "سیری", player.hunger / 100f, 0xFFFF7043);

        // ---------- وضعیت شغل ----------
        String jobText = jobs.statusText(player);
        if (jobText != null) {
            p.setColor(0xD9FFF3E0);
            textP.setTextSize(17f * fs);
            float jw = textP.measureText(jobText) + 40f * fs;
            c.drawRoundRect(vw / 2f - jw / 2f, panelY, vw / 2f + jw / 2f, panelY + 44f * fs, 14, 14, p);
            textP.setColor(0xFFE65100);
            c.drawText(jobText, vw / 2f, panelY + 29f * fs, textP);
        }

        // ---------- مینی‌مپ (بالا-راست) ----------
        float mmW = Math.min(vw * 0.17f, 230f);
        missionsMarkers(world, jobs, missions, player);
        MiniMap mm = view.miniMap;
        mm.draw(c, player.x, player.y, vw - mmW - 16f * fs, panelY + 6f * fs, mmW);

        // ---------- فلش راهنما ----------
        if (world.interior == null) {
            float[] target = guideTarget(world, jobs, missions);
            if (target != null) {
                drawGuideArrow(c, target, world);
            }
        }

        // ---------- دکمه مکث ----------
        float pw = 54f * fs;
        RectF pauseRect = new RectF(panelX + panelW + 24f * fs, panelY, panelX + panelW + 24f * fs + pw, panelY + pw);
        p.setColor(0xD9FFFFFF);
        c.drawRoundRect(pauseRect, 14, 14, p);
        p.setColor(0xFF455A64);
        c.drawRoundRect(pauseRect.centerX() - 10f * fs, pauseRect.top + 14f * fs,
                pauseRect.centerX() - 4f * fs, pauseRect.bottom - 14f * fs, 3, 3, p);
        c.drawRoundRect(pauseRect.centerX() + 4f * fs, pauseRect.top + 14f * fs,
                pauseRect.centerX() + 10f * fs, pauseRect.bottom - 14f * fs, 3, 3, p);
        itemRects.add(pauseRect);
        view.pauseBtnRect = pauseRect;

        // ---------- جوی‌استیک ----------
        if (joystick.active) {
            p.setColor(0x55FFFFFF);
            c.drawCircle(joystick.baseX, joystick.baseY, joystick.getRadius(), p);
            p.setColor(0x88FFFFFF);
            c.drawCircle(joystick.baseX, joystick.baseY, joystick.getRadius() * 0.55f, p);
            p.setColor(0xCC43A047);
            c.drawCircle(joystick.knobX, joystick.knobY, joystick.getRadius() * 0.42f, p);
        } else {
            // راهنمای محو
            float jx = 120f * fs, jy = vh - 120f * fs;
            p.setColor(0x33FFFFFF);
            c.drawCircle(jx, jy, 80f * fs, p);
            p.setColor(0x44FFFFFF);
            c.drawCircle(jx + joyX * 30f * fs, jy + joyY * 30f * fs, 34f * fs, p);
        }

        // ---------- دکمه اقدام ----------
        float ar = 64f * fs;
        float ax = vw - ar - 40f * fs;
        float ay = vh - ar - 40f * fs;
        p.setColor(0xE6FF9800);
        c.drawCircle(ax, ay, ar, p);
        p.setColor(0xFFE65100);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4f * fs);
        c.drawCircle(ax, ay, ar, p);
        p.setStyle(Paint.Style.FILL);
        textP.setColor(0xFFFFFFFF);
        textP.setTextSize((actionLabel.length() > 6 ? 17f : 21f) * fs);
        c.drawText(actionLabel, ax, ay + 7f * fs, textP);
        view.actionBtnRect = new RectF(ax - ar, ay - ar, ax + ar, ay + ar);

        // ---------- دکمه بوق (فقط رانندگی) ----------
        if (player.driving != null) {
            float hr = 40f * fs;
            float hx = ax - ar - 30f * fs - hr;
            float hy = ay - 10f * fs;
            p.setColor(0xE642A5F5);
            c.drawCircle(hx, hy, hr, p);
            textP.setTextSize(16f * fs);
            c.drawText("بوق", hx, hy + 6f * fs, textP);
            view.hornBtnRect = new RectF(hx - hr, hy - hr, hx + hr, hy + hr);
        } else {
            view.hornBtnRect = null;
        }

        // ---------- پیام‌های شناور ----------
        float ty = vh * 0.42f;
        for (Toast t : toasts) {
            float alpha = Math.min(1f, t.time / 0.5f);
            textP.setTextSize(19f * fs);
            float tw = textP.measureText(t.text) + 44f * fs;
            p.setColor(Color.argb((int) (alpha * 225), 33, 33, 33));
            c.drawRoundRect(vw / 2f - tw / 2f, ty, vw / 2f + tw / 2f, ty + 46f * fs, 20, 20, p);
            textP.setColor(Color.argb((int) (alpha * 255), 255, 255, 178));
            c.drawText(t.text, vw / 2f, ty + 31f * fs, textP);
            ty += 56f * fs;
        }
    }

    private void drawBar(Canvas c, float x, float y, float w, String label, float v, int color) {
        p.setColor(0xD9FFFFFF);
        c.drawRoundRect(x, y, x + w + 90f * fs, y + 26f * fs, 13, 13, p);
        p.setColor(0xFFE0E0E0);
        c.drawRoundRect(x + 66f * fs, y + 5f * fs, x + w + 82f * fs, y + 21f * fs, 8, 8, p);
        p.setColor(color);
        if (v > 0.02f) {
            c.drawRoundRect(x + 66f * fs, y + 5f * fs, x + 66f * fs + (w + 16f * fs) * G.clamp(v, 0f, 1f),
                    y + 21f * fs, 8, 8, p);
        }
        textP.setTextAlign(Paint.Align.RIGHT);
        textP.setTextSize(14f * fs);
        textP.setColor(0xFF37474F);
        c.drawText(label, x + w + 78f * fs, y + 19f * fs, textP);
        textP.setTextAlign(Paint.Align.CENTER);
    }

    private void missionsMarkers(World world, JobSystem jobs, MissionSystem missions, Player player) {
        MiniMap mm = view.miniMap;
        mm.clearMarkers();
        // ساختمان‌های مهم
        for (Building b : world.buildings) {
            if (b.isEnterable() && b.type != Building.HOME) {
                mm.addMarker(b.doorX, b.doorY, 0xFFEF9A9A);
            }
        }
        // هدف ماموریت
        float[] mt = missions.firstTarget();
        if (mt != null) mm.addMarker(mt[0], mt[1], 0xFFAB47BC);
        // هدف شغل
        float[] jt = jobs.currentMarker(player);
        if (jt != null) mm.addMarker(jt[0], jt[1], 0xFFFFEB3B);
    }

    private float[] guideTarget(World world, JobSystem jobs, MissionSystem missions) {
        float[] t = missions.firstTarget();
        if (t == null) t = jobs.currentMarker(view.player);
        return t;
    }

    private void drawGuideArrow(Canvas c, float[] targetWorld, World world) {
        Camera cam = view.camera;
        float sx = (targetWorld[0] - cam.x) * cam.scale + vw / 2f;
        float sy = (targetWorld[1] - cam.y) * cam.scale + vh / 2f;

        boolean onScreen = sx > 40 && sx < vw - 40 && sy > 40 && sy < vh - 40;
        float bob = (float) Math.sin(System.currentTimeMillis() / 180.0) * 6f;

        p.setColor(0xFFAB47BC);
        if (onScreen) {
            // فلش بالای هدف
            c.drawCircle(sx, sy - 66f * fs + bob, 14f * fs, p);
            p.setColor(0xFFFFFFFF);
            c.save();
            c.translate(sx, sy - 66f * fs + bob);
            c.rotate(-90f);
            c.drawCircle(0, 6f * fs, 5f * fs, p);
            c.restore();
        } else {
            // فلش لبه صفحه
            float cx = vw / 2f, cy = vh / 2f;
            float dx = sx - cx, dy = sy - cy;
            float ang = (float) Math.atan2(dy, dx);
            float edgeDist = Math.min(
                    Math.abs((vw / 2f - 60f * fs) / (float) Math.cos(ang)),
                    Math.abs((vh / 2f - 60f * fs) / (float) Math.sin(ang)));
            float ax = cx + (float) Math.cos(ang) * edgeDist;
            float ay = cy + (float) Math.sin(ang) * edgeDist;
            c.save();
            c.translate(ax, ay);
            c.rotate((float) Math.toDegrees(ang));
            c.drawCircle(0, 0, 15f * fs, p);
            p.setColor(0xFFFFFFFF);
            c.drawCircle(4f * fs, 0, 5f * fs, p);
            c.restore();
        }
    }

    // ================================================= پنل منو

    public void drawMenu(Canvas c) {
        if (menu == null) return;
        itemRects.clear();

        float panelW = Math.min(vw * 0.74f, 900f);
        float rowH = 62f * fs;
        float contentH = menu.items.size() * (rowH + 12f * fs);
        float panelH = Math.min(vh * 0.8f, 140f * fs + contentH);
        float px = vw / 2f - panelW / 2f;
        float py = vh / 2f - panelH / 2f;
        menuPanelRect = new RectF(px, py, px + panelW, py + panelH);

        // پس‌زمینه تیره
        p.setColor(0x66000000);
        c.drawRect(0, 0, vw, vh, p);

        p.setColor(0xFAFFFFFF);
        c.drawRoundRect(menuPanelRect, 24, 24, p);
        p.setColor(0xFF43A047);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4f * fs);
        c.drawRoundRect(menuPanelRect, 24, 24, p);
        p.setStyle(Paint.Style.FILL);

        // عنوان
        textP.setTextSize(23f * fs);
        textP.setColor(0xFF2E7D32);
        c.drawText(menu.title, vw / 2f, py + 44f * fs, textP);
        p.setColor(0xFFA5D6A7);
        c.drawRoundRect(px + 30f * fs, py + 56f * fs, px + panelW - 30f * fs, py + 60f * fs, 2, 2, p);

        // آیتم‌ها
        float clipTop = py + 72f * fs;
        float clipBottom = py + panelH - 16f * fs;
        c.save();
        c.clipRect(px + 8f, clipTop, px + panelW - 8f, clipBottom);

        float iy = clipTop - menuScroll;
        for (int i = 0; i < menu.items.size(); i++) {
            MenuItem it = menu.items.get(i);
            float ry = iy + i * (rowH + 12f * fs);
            if (ry + rowH < clipTop || ry > clipBottom) {
                itemRects.add(new RectF(-1, -1, -1, -1)); // خارج از دید
                continue;
            }
            RectF r = new RectF(px + 24f * fs, ry, px + panelW - 24f * fs, ry + rowH);
            p.setColor(it.enabled ? 0xFFE8F5E9 : 0xFFECEFF1);
            c.drawRoundRect(r, 14, 14, p);
            p.setColor(it.enabled ? 0xFF81C784 : 0xFFB0BEC5);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2.5f * fs);
            c.drawRoundRect(r, 14, 14, p);
            p.setStyle(Paint.Style.FILL);

            textP.setTextAlign(Paint.Align.RIGHT);
            textP.setTextSize(19f * fs);
            textP.setColor(it.enabled ? 0xFF1B5E20 : 0xFF90A4AE);
            c.drawText(it.label, r.right - 20f * fs, ry + rowH / 2f + (it.sub != null ? -2f * fs : 7f * fs), textP);
            if (it.sub != null && !it.sub.isEmpty()) {
                textP.setTextSize(14f * fs);
                textP.setColor(0xFF607D8B);
                c.drawText(it.sub, r.right - 20f * fs, ry + rowH / 2f + 18f * fs, textP);
            }
            textP.setTextAlign(Paint.Align.CENTER);
            itemRects.add(r);
        }
        c.restore();
    }

    // ================================================= دیالوگ

    public void drawDialogue(Canvas c) {
        p.setColor(0x55000000);
        c.drawRect(0, 0, vw, vh, p);

        float panelW = Math.min(vw * 0.86f, 980f);
        float panelH = vh * 0.42f;
        float px = vw / 2f - panelW / 2f;
        float py = vh - panelH - 18f * fs;
        RectF pr = new RectF(px, py, px + panelW, py + panelH);

        p.setColor(0xFAFFFFFF);
        c.drawRoundRect(pr, 24, 24, p);
        p.setColor(0xFF42A5F5);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4f * fs);
        c.drawRoundRect(pr, 24, 24, p);
        p.setStyle(Paint.Style.FILL);

        // نام گوینده
        p.setColor(0xFF42A5F5);
        float nameW = textP.measureText(dlgName) + 40f * fs;
        textP.setTextSize(18f * fs);
        c.drawRoundRect(px + panelW - nameW - 20f * fs, py - 18f * fs, px + panelW - 20f * fs, py + 20f * fs, 14, 14, p);
        textP.setColor(0xFFFFFFFF);
        c.drawText(dlgName, px + panelW - nameW / 2f - 20f * fs, py + 8f * fs, textP);

        // متن
        if (dlgLayout != null) {
            c.save();
            c.translate(px + 30f * fs, py + 36f * fs);
            dlgLayout.draw(c);
            c.restore();
        }

        // گزینه‌ها
        dlgRects.clear();
        float rowH = 52f * fs;
        float oy = py + panelH - dlgOptions.size() * (rowH + 8f * fs) - 14f * fs;
        for (int i = 0; i < dlgOptions.size(); i++) {
            RectF r = new RectF(px + 24f * fs, oy + i * (rowH + 8f * fs),
                    px + panelW - 24f * fs, oy + i * (rowH + 8f * fs) + rowH);
            p.setColor(0xFFE3F2FD);
            c.drawRoundRect(r, 12, 12, p);
            p.setColor(0xFF64B5F6);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2.5f * fs);
            c.drawRoundRect(r, 12, 12, p);
            p.setStyle(Paint.Style.FILL);

            textP.setTextAlign(Paint.Align.RIGHT);
            textP.setTextSize(17f * fs);
            textP.setColor(0xFF0D47A1);
            c.drawText(dlgOptions.get(i), r.right - 18f * fs, r.centerY() + 6f * fs, textP);
            textP.setTextAlign(Paint.Align.CENTER);
            dlgRects.add(r);
        }
    }

    // ================================================= مکث

    public void drawPause(Canvas c, boolean jobActive, boolean muted) {
        p.setColor(0x88000000);
        c.drawRect(0, 0, vw, vh, p);

        itemRects.clear();
        float panelW = Math.min(vw * 0.5f, 480f);
        int n = jobActive ? 5 : 4;
        float rowH = 64f * fs;
        float panelH = 110f * fs + n * (rowH + 14f * fs);
        float px = vw / 2f - panelW / 2f;
        float py = vh / 2f - panelH / 2f;

        p.setColor(0xFAFFFFFF);
        c.drawRoundRect(px, py, px + panelW, py + panelH, 24, 24, p);
        p.setColor(0xFFFF9800);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4f * fs);
        c.drawRoundRect(px, py, px + panelW, py + panelH, 24, 24, p);
        p.setStyle(Paint.Style.FILL);

        textP.setTextSize(24f * fs);
        textP.setColor(0xFFE65100);
        c.drawText("بازی متوقف شد", vw / 2f, py + 52f * fs, textP);

        String[] labels = new String[n];
        labels[0] = "ادامه بازی";
        labels[1] = "ذخیره بازی";
        if (jobActive) {
            labels[2] = "پایان شیفت";
            labels[3] = muted ? "صدا: خاموش" : "صدا: روشن";
            labels[4] = "خروج به منوی اصلی";
        } else {
            labels[2] = muted ? "صدا: خاموش" : "صدا: روشن";
            labels[3] = "خروج به منوی اصلی";
        }

        float iy = py + 80f * fs;
        for (int i = 0; i < n; i++) {
            RectF r = new RectF(px + 30f * fs, iy, px + panelW - 30f * fs, iy + rowH);
            p.setColor(i == 0 ? 0xFF66BB6A : 0xFFFFF3E0);
            c.drawRoundRect(r, 14, 14, p);
            p.setColor(i == 0 ? 0xFF2E7D32 : 0xFFFFB74D);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3f * fs);
            c.drawRoundRect(r, 14, 14, p);
            p.setStyle(Paint.Style.FILL);

            textP.setTextAlign(Paint.Align.CENTER);
            textP.setTextSize(19f * fs);
            textP.setColor(0xFF4E342E);
            c.drawText(labels[i], r.centerX(), r.centerY() + 7f * fs, textP);
            itemRects.add(r);
            iy += rowH + 14f * fs;
        }
    }

    // ================================================= ابزار

    public static String faMoney(int n) {
        String s = String.format(java.util.Locale.US, "%,d", n).replace(',', '٬');
        return G.fa(s);
    }
}
