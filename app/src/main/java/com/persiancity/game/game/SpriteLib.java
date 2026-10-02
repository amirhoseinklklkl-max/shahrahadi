package com.persiancity.game.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.util.List;

/**
 * کتابخانه رسم کارتونی — همه گرافیک بازی با کد کشیده می‌شود
 */
public class SpriteLib {

    public boolean nightMode = false;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textP = new Paint(Paint.ANTI_ALIAS_FLAG);

    // لباس‌ها: {رنگ بالن، رنگ شلوار، رنگ کفش}
    public static final int[][] OUTFITS = {
            {0xFF42A5F5, 0xFF3949AB, 0xFF37474F},   // ۰ عادی
            {0xFF66BB6A, 0xFF2E7D32, 0xFF3E2723},   // ۱ ورزشی
            {0xFFEF5350, 0xFF880E4F, 0xFF212121},   // ۲ مهمونی
            {0xFFFFCA28, 0xFF5D4037, 0xFF6D4C41},   // ۳ زمستونی
            {0xFF26C6DA, 0xFF00695C, 0xFF37474F},   // ۴ دریایی
            {0xFFF06292, 0xFF6A1B9A, 0xFF4E342E},   // ۵ صورتی بامزه
            {0xFF8D6E63, 0xFF3E2723, 0xFF212121},   // ۶ کارِ درست‌وحسابی
            {0xFF7C4DFF, 0xFF311B92, 0xFF212121},   // ۷ خفن بنفش
    };
    public static final String[] OUTFIT_NAMES = {
            "لباس عادی", "لباس ورزشی", "لباس مهمونی", "لباس زمستونی",
            "لباس دریایی", "لباس صورتی", "لباس کار", "لباس خفن"
    };
    public static final int[] OUTFIT_PRICES = {0, 800, 1200, 1000, 1500, 1800, 900, 2500};

    public static final String[] HAIR_NAMES = {"موی کوتاه", "موی فرفری", "موهای بلند", "با کلاه"};
    public static final int[] HAIR_PRICES = {0, 300, 500, 400};

    // رنگ‌های رنگ‌آمیزی ماشین در گاراژ
    public static final int[] CAR_COLORS = {
            0xFFEF5350, 0xFF42A5F5, 0xFF66BB6A, 0xFFFFCA28,
            0xFFAB47BC, 0xFFFF7043, 0xFF26C6DA, 0xFF212121
    };
    public static final String[] CAR_COLOR_NAMES = {"قرمز", "آبی", "سبز", "زرد", "بنفش", "نارنجی", "فیروزه‌ای", "مشکی"};

    public SpriteLib() {
        textP.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textP.setTextAlign(Paint.Align.CENTER);
    }

    // ================================================= شخصیت

    public void drawPerson(Canvas c, float x, float y, int facing, float anim,
                           int shirt, int pants, int skin, int hairColor, int hairStyle,
                           int carrying, boolean isPlayer) {
        float walk = (float) Math.sin(anim * 11f) * 5f;
        boolean walking = anim > 0.01f;

        // سایه
        p.setColor(0x33000000);
        c.drawOval(x - 16, y + 24, x + 16, y + 32, p);

        // پاها
        p.setColor(pants);
        float legSwing = walking ? walk : 0f;
        c.drawRoundRect(x - 10, y + 4 - Math.max(0, legSwing), x - 2, y + 28, 4, 4, p);
        c.drawRoundRect(x + 2, y + 4 - Math.max(0, -legSwing), x + 10, y + 28, 4, 4, p);
        // کفش
        p.setColor(0xFF37474F);
        c.drawRoundRect(x - 11, y + 24, x - 1, y + 29, 3, 3, p);
        c.drawRoundRect(x + 1, y + 24, x + 11, y + 29, 3, 3, p);

        // بدن
        p.setColor(shirt);
        c.drawRoundRect(x - 13, y - 12, x + 13, y + 8, 8, 8, p);
        // دست‌ها
        p.setColor(skin);
        float armSwing = walking ? -walk * 0.8f : 0f;
        c.drawRoundRect(x - 18, y - 10 + armSwing, x - 12, y + 4 + armSwing, 4, 4, p);
        c.drawRoundRect(x + 12, y - 10 - armSwing, x + 18, y + 4 - armSwing, 4, 4, p);

        // سر
        p.setColor(skin);
        c.drawCircle(x, y - 24, 14, p);

        // موها
        p.setColor(hairColor);
        if (hairStyle == 0) {          // کوتاه
            c.drawArc(x - 14, y - 38, x + 14, y - 18, 180, 180, true, p);
        } else if (hairStyle == 1) {   // فرفری
            c.drawCircle(x - 8, y - 30, 7, p);
            c.drawCircle(x, y - 34, 8, p);
            c.drawCircle(x + 8, y - 30, 7, p);
        } else if (hairStyle == 2) {   // بلند
            c.drawArc(x - 14, y - 40, x + 14, y - 16, 160, 220, true, p);
            c.drawRoundRect(x - 14, y - 30, x - 9, y - 14, 4, 4, p);
            c.drawRoundRect(x + 9, y - 30, x + 14, y - 14, 4, 4, p);
        } else {                        // کلاه
            c.drawArc(x - 14, y - 38, x + 14, y - 20, 180, 180, true, p);
            p.setColor(isPlayer ? 0xFFE53935 : 0xFF455A64);
            c.drawRoundRect(x - 15, y - 34, x + 15, y - 30, 3, 3, p);
            c.drawArc(x - 14, y - 40, x + 14, y - 26, 180, 180, true, p);
        }

        // صورت
        if (facing != 1) { // نه از پشت
            p.setColor(0xFF212121);
            if (facing == 0) {
                c.drawCircle(x - 5, y - 24, 2f, p);
                c.drawCircle(x + 5, y - 24, 2f, p);
                // لبخند
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2f);
                c.drawArc(x - 6, y - 22, x + 6, y - 14, 20, 140, false, p);
                p.setStyle(Paint.Style.FILL);
            } else if (facing == 2) {
                c.drawCircle(x - 8, y - 24, 2f, p);
            } else {
                c.drawCircle(x + 8, y - 24, 2f, p);
            }
        }

        // حمل بار
        if (carrying >= 0) {
            drawCarryItem(c, x, y - 44, carrying);
        }
    }

    private void drawCarryItem(Canvas c, float x, float y, int item) {
        if (item == 0) { // بشقاب غذا
            p.setColor(0xFFFFFFFF);
            c.drawOval(x - 14, y - 6, x + 14, y + 8, p);
            p.setColor(0xFFFF8A65);
            c.drawCircle(x, y + 1, 7, p);
        } else if (item == 1) { // بسته پیک
            p.setColor(0xFFD7A86E);
            c.drawRoundRect(x - 12, y - 10, x + 12, y + 8, 4, 4, p);
            p.setColor(0xFF8D6E63);
            p.setStrokeWidth(3f);
            c.drawLine(x, y - 10, x, y + 8, p);
            c.drawLine(x - 12, y - 1, x + 12, y - 1, p);
        } else { // نامه
            p.setColor(0xFFFFFFFF);
            c.drawRoundRect(x - 11, y - 8, x + 11, y + 6, 3, 3, p);
            p.setColor(0xFFE53935);
            p.setStrokeWidth(2.5f);
            c.drawLine(x - 11, y - 8, x, y, p);
            c.drawLine(x + 11, y - 8, x, y, p);
        }
    }

    // ================================================= خودرو

    public void drawCar(Canvas c, Vehicle v) {
        c.save();
        c.translate(v.x, v.y);
        c.rotate((float) Math.toDegrees(v.angle));

        float L = v.h;  // طول (در جهت x محلی)
        float W = v.w;  // عرض

        // نور نئون زیر ماشین
        if (v.neonColor != 0) {
            p.setColor(v.neonColor);
            p.setAlpha(nightMode ? 160 : 90);
            c.drawRoundRect(-L / 2f - 12, -W / 2f - 12, L / 2f + 12, W / 2f + 12, 26, 26, p);
            p.setAlpha(255);
        }

        // سایه
        p.setColor(0x33000000);
        c.drawRoundRect(-L / 2f + 4, -W / 2f + 5, L / 2f + 4, W / 2f + 5, 16, 16, p);

        // چرخ‌ها
        p.setColor(0xFF212121);
        float wx = L * 0.30f, wy = W * 0.52f;
        c.drawRoundRect(-wx - 12, -wy - 4, -wx + 12, -wy + 10, 5, 5, p);
        c.drawRoundRect(-wx - 12, wy - 10, -wx + 12, wy + 4, 5, 5, p);
        c.drawRoundRect(wx - 12, -wy - 4, wx + 12, -wy + 10, 5, 5, p);
        c.drawRoundRect(wx - 12, wy - 10, wx + 12, wy + 4, 5, 5, p);

        // بدنه
        p.setColor(v.paint);
        c.drawRoundRect(-L / 2f, -W / 2f, L / 2f, W / 2f, 18, 18, p);
        p.setColor(darker(v.paint));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3f);
        c.drawRoundRect(-L / 2f, -W / 2f, L / 2f, W / 2f, 18, 18, p);
        p.setStyle(Paint.Style.FILL);

        // شیشه جلو و عقب
        p.setColor(0xFFB3E5FC);
        c.drawRoundRect(L * 0.08f, -W / 2f + 9f, L * 0.22f, W / 2f - 9f, 6, 6, p);
        c.drawRoundRect(-L * 0.24f, -W / 2f + 9f, -L * 0.12f, W / 2f - 9f, 6, 6, p);
        // سقف
        p.setColor(lighter(v.paint, 1.15f));
        c.drawRoundRect(-L * 0.12f, -W / 2f + 8f, L * 0.08f, W / 2f - 8f, 5, 5, p);

        // چراغ جلو و عقب
        p.setColor(nightMode ? 0xFFFFF176 : 0xFFFFEE58);
        c.drawCircle(L / 2f - 5f, -W / 2f + 10f, 5f, p);
        c.drawCircle(L / 2f - 5f, W / 2f - 10f, 5f, p);
        p.setColor(0xFFEF5350);
        c.drawCircle(-L / 2f + 5f, -W / 2f + 10f, 4f, p);
        c.drawCircle(-L / 2f + 5f, W / 2f - 10f, 4f, p);

        // بال اسپرت
        if (v.spoiler) {
            p.setColor(0xFF37474F);
            c.drawRoundRect(-L / 2f - 6f, -W / 2f + 6f, -L / 2f + 2f, W / 2f - 6f, 4, 4, p);
        }

        // علامت تاکسی
        if (v.hasPassenger) {
            p.setColor(0xFFFFEB3B);
            c.drawRoundRect(-10, -12, 10, 12, 4, 4, p);
            textP.setTextSize(13f);
            textP.setColor(0xFF212121);
            c.drawText("تاکسی", 0, 5, textP);
        }

        c.restore();
    }

    public void drawMotorcycle(Canvas c, Vehicle v) {
        c.save();
        c.translate(v.x, v.y);
        c.rotate((float) Math.toDegrees(v.angle));

        float L = v.h, W = v.w;

        // سایه
        p.setColor(0x33000000);
        c.drawRoundRect(-L / 2f + 3, -W / 2f + 4, L / 2f + 3, W / 2f + 4, 12, 12, p);

        // نور نئون
        if (v.neonColor != 0) {
            p.setColor(v.neonColor);
            p.setAlpha(nightMode ? 150 : 80);
            c.drawRoundRect(-L / 2f - 10, -W / 2f - 10, L / 2f + 10, W / 2f + 10, 20, 20, p);
            p.setAlpha(255);
        }

        // چرخ‌ها
        p.setColor(0xFF212121);
        c.drawCircle(L * 0.34f, 0, 11, p);
        c.drawCircle(-L * 0.34f, 0, 11, p);

        // بدنه موتور
        p.setColor(v.paint);
        c.drawRoundRect(-L * 0.28f, -W * 0.42f, L * 0.3f, W * 0.42f, 10, 10, p);
        p.setColor(darker(v.paint));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2.5f);
        c.drawRoundRect(-L * 0.28f, -W * 0.42f, L * 0.3f, W * 0.42f, 10, 10, p);
        p.setStyle(Paint.Style.FILL);

        // فرمان
        p.setColor(0xFF37474F);
        p.setStrokeWidth(5f);
        c.drawLine(L * 0.28f, -W * 0.7f, L * 0.28f, W * 0.7f, p);
        p.setStrokeWidth(0f);

        // چراغ جلو
        p.setColor(nightMode ? 0xFFFFF176 : 0xFFFFEE58);
        c.drawCircle(L * 0.4f, 0, 4.5f, p);

        // راکب (کلاه‌خود بامزه)
        if (v.mode == Vehicle.MODE_PLAYER) {
            p.setColor(0xFFE53935);
            c.drawCircle(-L * 0.05f, 0, 12, p);
            p.setColor(0xFFB3E5FC);
            c.drawArc(-L * 0.05f - 12, -12, -L * 0.05f + 12, 12, -50, 100, true, p);
        }

        c.restore();
    }

    // ================================================= ساختمان

    public void drawBuilding(Canvas c, Building b) {
        float x = b.px, y = b.py, w = b.pw, h = b.ph;

        // سایه
        p.setColor(0x28000000);
        c.drawRoundRect(x + 5, y + 7, x + w + 5, y + h + 7, 12, 12, p);

        // بدنه
        p.setColor(b.wallColor);
        c.drawRoundRect(x, y, x + w, y + h, 10, 10, p);
        p.setColor(darker(b.wallColor));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3.5f);
        c.drawRoundRect(x, y, x + w, y + h, 10, 10, p);
        p.setStyle(Paint.Style.FILL);

        // پنجره‌ها (شب روشن می‌شوند)
        int winLight = nightMode ? 0xFFFFF59D : 0xFFB3E5FC;
        p.setColor(winLight);
        int cols = Math.max(2, (int) (b.tileW / 2.2f));
        int rows = Math.max(1, b.tileH / 3);
        float winW = w / (cols * 2.4f);
        float winH = h * 0.14f;
        float gapX = (w - cols * winW) / (cols + 1f);
        float startY = y + h * (b.doorUp ? 0.24f : 0.16f);
        for (int r = 0; r < rows; r++) {
            for (int cI = 0; cI < cols; cI++) {
                float wx = x + gapX + cI * (winW + gapX);
                float wy = startY + r * (winH + h * 0.1f);
                if (wy + winH > y + h - 30f) break;
                c.drawRoundRect(wx, wy, wx + winW, wy + winH, 5, 5, p);
            }
        }

        // در
        p.setColor(0xFF6D4C41);
        float doorW = 34f, doorH = 44f;
        float dx = x + w / 2f - doorW / 2f;
        float dy = b.doorUp ? y : y + h - doorH;
        c.drawRoundRect(dx, dy, dx + doorW, dy + doorH, 6, 6, p);
        p.setColor(0xFFFFD54F);
        c.drawCircle(b.doorUp ? dx + doorW - 8 : dx + doorW - 8, dy + doorH / 2f, 3f, p);

        // سایه‌بان رنگی بالای در
        p.setColor(b.accentColor);
        float ay = b.doorUp ? y - 4f : y + h - doorH - 14f;
        c.drawRoundRect(dx - 8, ay, dx + doorW + 8, ay + 12, 5, 5, p);

        // تابلو
        float signY = b.doorUp ? y + 6f : y + h - doorH - 46f;
        p.setColor(0xFFFFFFFF);
        c.drawRoundRect(x + 8, signY, x + w - 8, signY + 30, 8, 8, p);
        p.setColor(b.roofColor);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3f);
        c.drawRoundRect(x + 8, signY, x + w - 8, signY + 30, 8, 8, p);
        p.setStyle(Paint.Style.FILL);

        String label = buildingLabel(b);
        textP.setColor(0xFF212121);
        float ts = Math.min(19f, (w - 24f) / Math.max(4f, label.length() * 0.62f));
        textP.setTextSize(ts);
        c.drawText(label, x + w / 2f, signY + 21f, textP);

        // تزئینات مخصوص هر ساختمان
        drawBuildingExtras(c, b);
    }

    private String buildingLabel(Building b) {
        // نام تابلو (کوتاه‌تر برای خوانایی)
        String n = b.name;
        if (n.length() > 16 && !b.isEnterable()) return n;
        return n;
    }

    private void drawBuildingExtras(Canvas c, Building b) {
        float cx = b.px + b.pw / 2f;
        switch (b.type) {
            case Building.HOSPITAL:
                // صلیب قرمز
                p.setColor(0xFFE53935);
                c.drawRoundRect(cx - 8, b.py + b.ph * 0.32f, cx + 8, b.py + b.ph * 0.32f + 32, 3, 3, p);
                c.drawRoundRect(cx - 16, b.py + b.ph * 0.32f + 8, cx + 16, b.py + b.ph * 0.32f + 24, 3, 3, p);
                break;
            case Building.BARBER:
                // میله آرایشگاه
                float py = b.py + b.ph - 4f;
                p.setColor(0xFFE53935);
                c.drawRoundRect(b.px + 8, b.py + 10, b.px + 16, py - 10, 4, 4, p);
                p.setColor(0xFFFFFFFF);
                c.drawRoundRect(b.px + 8, b.py + 10, b.px + 16, b.py + 22, 4, 4, p);
                c.drawRoundRect(b.px + 8, b.py + 34, b.px + 16, b.py + 46, 4, 4, p);
                c.drawRoundRect(b.px + 8, b.py + 58, b.px + 16, b.py + 70, 4, 4, p);
                break;
            case Building.MOSQUE:
                // گنبد و مناره
                p.setColor(0xFF00897B);
                c.drawCircle(cx, b.py + 6f, b.pw * 0.22f, p);
                p.setColor(0xFFFFD54F);
                c.drawCircle(cx, b.py - 12f, 6f, p);
                p.setColor(0xFFE0F2F1);
                c.drawRoundRect(b.px + 6, b.py - 26f, b.px + 18, b.py + 20f, 6, 6, p);
                c.drawRoundRect(b.px + b.pw - 18, b.py - 26f, b.px + b.pw - 6, b.py + 20f, 6, 6, p);
                break;
            case Building.CINEMA:
                // چراغ‌های تابلو سینما
                p.setColor(0xFFFFD54F);
                for (int i = 0; i < 6; i++) {
                    c.drawCircle(b.px + 14 + i * (b.pw - 28) / 5f, b.py + 8f, 4f, p);
                }
                break;
            case Building.TAXISTAND:
                // ستون‌های سرپناه
                p.setColor(0xFF757575);
                c.drawRoundRect(b.px + 10, b.py - 8, b.px + 18, b.py + 10, 3, 3, p);
                c.drawRoundRect(b.px + b.pw - 18, b.py - 8, b.px + b.pw - 10, b.py + 10, 3, 3, p);
                break;
            case Building.GAS:
                // جایگاه سوخت‌گیری
                p.setColor(0xFFB0BEC5);
                c.drawRoundRect(b.px + 10, b.py + 14, b.px + 30, b.py + b.ph - 14, 5, 5, p);
                c.drawRoundRect(b.px + b.pw - 30, b.py + 14, b.px + b.pw - 10, b.py + b.ph - 14, 5, 5, p);
                break;
            case Building.STADIUM:
                // خطوط چمن ورزشگاه
                p.setColor(0xFF4CAF50);
                c.drawRoundRect(b.px + 14, b.py + 14, b.px + b.pw - 14, b.py + b.ph - 14, 30, 30, p);
                p.setColor(0xFFFFFFFF);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(3f);
                c.drawCircle(cx, b.py + b.ph / 2f, Math.min(b.pw, b.ph) * 0.18f, p);
                c.drawLine(cx, b.py + 16, cx, b.py + b.ph - 16, p);
                p.setStyle(Paint.Style.FILL);
                break;
        }
    }

    // ================================================= دکور

    public void drawTree(Canvas c, float x, float y, float size) {
        // سایه
        p.setColor(0x30000000);
        c.drawOval(x - 14 * size, y + 12 * size, x + 14 * size, y + 20 * size, p);
        // تنه
        p.setColor(G.COL_TRUNK);
        c.drawRoundRect(x - 5 * size, y - 4 * size, x + 5 * size, y + 16 * size, 3, 3, p);
        // برگ‌ها
        int leaf = nightMode ? 0xFF2E7D32 : G.COL_TREE;
        p.setColor(leaf);
        c.drawCircle(x, y - 14 * size, 15 * size, p);
        p.setColor(lighter(leaf, 1.18f));
        c.drawCircle(x - 8 * size, y - 18 * size, 10 * size, p);
        c.drawCircle(x + 9 * size, y - 16 * size, 9 * size, p);
    }

    public void drawStreetlight(Canvas c, float x, float y) {
        p.setColor(0xFF546E7A);
        c.drawRoundRect(x - 3, y - 40, x + 3, y + 6, 2, 2, p);
        p.setColor(nightMode ? 0xFFFFF176 : 0xFFCFD8DC);
        c.drawCircle(x, y - 44, 6, p);
        if (nightMode) {
            p.setColor(0x33FFF176);
            c.drawCircle(x, y - 44, 16, p);
        }
    }

    public void drawFountain(Canvas c, float x, float y, float minutes) {
        // حوض
        p.setColor(0xFF90A4AE);
        c.drawCircle(x, y, 92, p);
        p.setColor(G.COL_WATER);
        c.drawCircle(x, y, 78, p);
        p.setColor(lighter(G.COL_WATER, 1.25f));
        float wave = (float) Math.sin(minutes * 0.05f) * 3f;
        c.drawCircle(x, y - wave, 52, p);
        // فواره
        p.setColor(0xCCFFFFFF);
        c.drawCircle(x, y - 10 + wave, 10, p);
        p.setColor(0x88FFFFFF);
        c.drawCircle(x - 12, y - 2 + wave, 5, p);
        c.drawCircle(x + 12, y - 2 - wave, 5, p);
    }

    /**
     * حباب حرف NPCها
     */
    public void drawBubbles(Canvas c, List<Npc> npcs) {
        for (Npc n : npcs) {
            if (n.bubble == null) continue;
            float tw = textP.measureText(n.bubble) + 24f;
            float bx = n.x - tw / 2f;
            float by = n.y - 74f;
            p.setColor(0xF5FFFFFF);
            c.drawRoundRect(bx, by, bx + tw, by + 30, 12, 12, p);
            p.setColor(0xFF90A4AE);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2f);
            c.drawRoundRect(bx, by, bx + tw, by + 30, 12, 12, p);
            p.setStyle(Paint.Style.FILL);
            textP.setColor(0xFF37474F);
            textP.setTextSize(15f);
            c.drawText(n.bubble, n.x, by + 21f, textP);
        }
    }

    // ================================================= ابزار رنگ

    public static int darker(int color) {
        return shift(color, 0.72f);
    }

    public static int lighter(int color, float f) {
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * f));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * f));
        int b = Math.min(255, (int) ((color & 0xFF) * f));
        return Color.rgb(r, g, b);
    }

    private static int shift(int color, float f) {
        int r = (int) (((color >> 16) & 0xFF) * f);
        int g = (int) (((color >> 8) & 0xFF) * f);
        int b = (int) ((color & 0xFF) * f);
        return Color.rgb(r, g, b);
    }
}
