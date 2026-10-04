package com.persiancity.game.game;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.ArrayList;

/**
 * کتابخانه رسم کارتونی شهر شادی — همه‌چیز با Canvas
 */
public class SpriteLib {

    protected final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();

    // لباس‌ها: [پیراهن، شلوار]
    public static final int[][] OUTFITS = {
        {0xFF42A5F5, 0xFF3949AB},   // ۰ آبی
        {0xFFEF5350, 0xFF37474F},   // ۱ قرمز
        {0xFF66BB6A, 0xFF5D4037},   // ۲ سبز
        {0xFFFFCA28, 0xFF455A64},   // ۳ زرد
        {0xFF26C6DA, 0xFF37474F},   // ۴ فیروزه‌ای
        {0xFFF06292, 0xFFAD1457}    // ۵ صورتی (دخترانه)
    };

    /**
 * مثلث کشیدن (Canvas متد داخلی ندارد)
 */
    private void tri(Canvas c, float[] pts) {
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(pts[0], pts[1]);
        path.lineTo(pts[2], pts[3]);
        path.lineTo(pts[4], pts[5]);
        path.close();
        c.drawPath(path, p);
    }

    // ================= شخصیت =================

    public void drawPerson(Canvas c, float x, float y, int facing, float anim,
                           int shirt, int pants, int skin, int hairColor, int hairStyle,
                           int carrying, boolean isPlayer, int gender) {
        boolean walking = anim > 0.01f;
        boolean girl = gender == 1;
        float swing = walking ? (float) Math.sin(anim * 11f) * 6.5f : 0f;
        // ✅ بالا و پایین پریدن بدن هنگام راه رفتن
        float bob = walking ? Math.abs((float) Math.sin(anim * 11f)) * 2.4f : 0f;
        // جهت راه رفتن افقی است یا عمودی؟ (پاها در راستای حرکت جلو-عقب می‌روند)
        boolean horiz = facing == 1 || facing == 3;

        // سایه
        p.setColor(0x33000000);
        c.drawOval(x - 16, y + 24, x + 16, y + 32, p);

        c.save();
        c.translate(0f, -bob);

        // ✅ پاها — حرکت طبیعی در راستای راه رفتن
        p.setColor(pants);
        if (walking) {
            if (horiz) {
                // قدم در راستای چپ/راست: پاها جلو-عقب
                c.drawRoundRect(x - 10 + swing, y + 2, x - 2 + swing, y + 28, 4, 4, p);
                c.drawRoundRect(x + 2 - swing, y + 2, x + 10 - swing, y + 28, 4, 4, p);
            } else {
                c.drawRoundRect(x - 10, y + 2 + swing, x - 2, y + 28, 4, 4, p);
                c.drawRoundRect(x + 2, y + 2 - swing, x + 10, y + 28, 4, 4, p);
            }
        } else {
            c.drawRoundRect(x - 9, y + 2, x - 1, y + 28, 4, 4, p);
            c.drawRoundRect(x + 1, y + 2, x + 9, y + 28, 4, 4, p);
        }
        // کفش‌ها هم پای پاها
        p.setColor(0xFF4E342E);
        if (walking && horiz) {
            c.drawRoundRect(x - 11 + swing, y + 24, x - 1 + swing, y + 30, 3, 3, p);
            c.drawRoundRect(x + 1 - swing, y + 24, x + 11 - swing, y + 30, 3, 3, p);
        } else {
            c.drawRoundRect(x - 11, y + 24, x - 1, y + 30, 3, 3, p);
            c.drawRoundRect(x + 1, y + 24, x + 11, y + 30, 3, 3, p);
        }

        // بدن — دختر: پیراهن مثلثی
        if (girl) {
            p.setColor(shirt);
            c.drawRoundRect(x - 11, y - 18, x + 11, y + 6, 8, 8, p);
            c.drawRoundRect(x - 15, y - 2, x + 15, y + 8, 6, 6, p);
        } else {
            p.setColor(shirt);
            c.drawRoundRect(x - 11, y - 18, x + 11, y + 6, 8, 8, p);
        }

        // ✅ دست‌ها — تاب در جهت مخالف پاها
        p.setColor(skin);
        if (walking) {
            if (horiz) {
                c.drawRoundRect(x - 16 + swing * 0.7f, y - 16, x - 10 + swing * 0.7f, y - 2, 4, 4, p);
                c.drawRoundRect(x + 10 - swing * 0.7f, y - 16, x + 16 - swing * 0.7f, y - 2, 4, 4, p);
            } else {
                c.drawRoundRect(x - 16, y - 16 - swing, x - 10, y - 2 + swing * 0.5f, 4, 4, p);
                c.drawRoundRect(x + 10, y - 16 + swing, x + 16, y - 2 - swing * 0.5f, 4, 4, p);
            }
        } else {
            c.drawRoundRect(x - 16, y - 16, x - 10, y - 2, 4, 4, p);
            c.drawRoundRect(x + 10, y - 16, x + 16, y - 2, 4, 4, p);
        }

        // وسط دست: سینی یا جعبه
        if (carrying == 1) {
            p.setColor(0xFFD7CCC8);
            c.drawRoundRect(x - 14, y - 20, x + 14, y - 16, 3, 3, p);
            p.setColor(0xFFFF7043);
            c.drawCircle(x - 6, y - 22, 3.5f, p);
            c.drawCircle(x + 4, y - 22, 3.5f, p);
        } else if (carrying == 2) {
            p.setColor(0xFFA1887F);
            c.drawRoundRect(x + 10, y - 24, x + 24, y - 12, 3, 3, p);
        }

        // سر
        p.setColor(skin);
        c.drawCircle(x, y - 28, 13, p);

        // ✅ موها — فقط بالای سر؛ چهره از روبه‌رو کاملاً باز می‌ماند
        // (رفع باگ: صورت از روبه‌رو مثل پشت سر پرِ مو دیده می‌شد)
        boolean back = facing == 2;
        if (girl && hairStyle == 2 && back) {
            // ✅ پشت سر دختر — سر کامل پوشیده از مو + موی بلند روی پشت
            // (رفع باگ درخواست کاربر: پشت سر دخترها کچل دیده می‌شد)
            p.setColor(hairColor);
            c.drawCircle(x, y - 30f, 13.4f, p);
            c.drawRoundRect(x - 13, y - 36, x + 13, y - 8, 6, 6, p);
            // راه‌راه بافت مو
            p.setColor(0x33000000);
            p.setStrokeWidth(1.6f);
            c.drawLine(x - 4f, y - 33f, x - 4f, y - 12f, p);
            c.drawLine(x + 4f, y - 33f, x + 4f, y - 12f, p);
            p.setStrokeWidth(0.1f);
        } else if (hairStyle == 2 && girl) {
            // موی بلند دخترانه — از بغل هم ته سر مو دارد (کلیپ‌شده تا روی چشم نیاید)
            c.save();
            if (facing == 3) {
                c.clipRect(x - 15, y - 44, x - 4.5f, y - 16);
                p.setColor(hairColor);
                c.drawCircle(x - 8f, y - 30f, 10f, p);
            } else if (facing == 1) {
                c.clipRect(x + 4.5f, y - 44, x + 15, y - 16);
                p.setColor(hairColor);
                c.drawCircle(x + 8f, y - 30f, 10f, p);
            }
            c.restore();
            // چتری کوتاه + دو طرف بلند (بدون پوشاندن صورت)
            c.save();
            c.clipRect(x - 15, y - 44, x + 15, y - 31.5f);
            p.setColor(hairColor);
            c.drawCircle(x, y - 30f, 13.4f, p);
            c.restore();
            p.setColor(hairColor);
            c.drawRoundRect(x - 15, y - 34, x - 10, y - 10, 4, 4, p);
            c.drawRoundRect(x + 10, y - 34, x + 15, y - 10, 4, 4, p);
        } else if (back) {
            // از پشت: کل سر مو
            p.setColor(hairColor);
            c.drawCircle(x, y - 30f, 13.2f, p);
            p.setColor(0x33000000);
            c.drawCircle(x, y - 30f, 13.2f, p);
        } else {
            // موی کوتاه: فقط نیمه بالای سر
            c.save();
            c.clipRect(x - 15, y - 44, x + 15, y - 31.5f);
            p.setColor(hairColor);
            c.drawCircle(x, y - 30f, 13.4f, p);
            if (hairStyle == 1) {
                // موی فرفری
                c.drawCircle(x - 7, y - 38, 4, p);
                c.drawCircle(x, y - 40, 4.5f, p);
                c.drawCircle(x + 7, y - 38, 4, p);
            }
            c.restore();
        }
        if (girl) {
            // پاپیون
            p.setColor(0xFFFF5252);
            c.drawCircle(x - 10, y - 38, 3.5f, p);
            c.drawCircle(x - 15, y - 38, 2.5f, p);
            c.drawCircle(x - 12.5f, y - 38, 2f, p);
        }

        // صورت — مناسب هر جهت (✅ درخواست کاربر: چشم و دهان کامل از روبه‌رو)
        p.setColor(0xFF3E2723);
        boolean right = facing == 3, left = facing == 1, up = facing == 2;
        if (up) {
            // از پشت: فقط پشت سر (مو بالا کشیده شده)
        } else {
            float ox = right ? 3.5f : (left ? -3.5f : 0f);
            // چشم سفید + مردمک
            p.setColor(0xFFFFFFFF);
            c.drawCircle(x - 4.5f + ox, y - 29f, 3.1f, p);
            c.drawCircle(x + 4.5f + ox, y - 29f, 3.1f, p);
            p.setColor(0xFF3E2723);
            c.drawCircle(x - 4f + ox * 1.4f, y - 29f, 1.7f, p);
            c.drawCircle(x + 5f + ox * 1.4f, y - 29f, 1.7f, p);
            // لپ‌های صورتی
            p.setColor(0x50EF5350);
            c.drawCircle(x - 8.5f + ox * 0.6f, y - 24.5f, 2.6f, p);
            c.drawCircle(x + 8.5f + ox * 0.6f, y - 24.5f, 2.6f, p);
            // 😊 دهان لبخند — از روبه‌رو دهان کامل، از بغل لبخند کوچک
            p.setColor(0xFF3E2723);
            p.setStrokeWidth(1.8f);
            p.setStyle(Paint.Style.STROKE);
            if (left || right) {
                rf.set(x + ox - 2.5f, y - 26.5f, x + ox + 3.5f, y - 22f);
                c.drawArc(rf, 20f, 140f, false, p);
            } else {
                rf.set(x - 4.5f, y - 27f, x + 4.5f, y - 21f);
                c.drawArc(rf, 15f, 150f, false, p);
            }
            p.setStyle(Paint.Style.FILL);
        }

        // نشان بازیکن: هاله طلایی ملایم
        if (isPlayer) {
            p.setColor(0x22FFC107);
            c.drawCircle(x, y, 26f, p);
        }

        c.restore();
    }

    // ================= کوله‌پشتی مدرسه 🎒 =================

    /**
     * ✅ کوله‌پشتی دانش‌آموزی — مناسب هر جهت نگاه
     * (از پشت کامل، از بغل نیمه، از روبه‌رو فقط بند‌ها)
     */
    public void drawBackpack(Canvas c, float x, float y, int dir, int color) {
        p.setColor(0x22000000);
        if (dir == 2) {
            // از پشت — کوله کامل روی پشت
            p.setColor(color);
            c.drawRoundRect(x - 11, y - 17, x + 11, y + 4, 6, 6, p);
            // درِ کوله
            p.setColor(0x33000000);
            c.drawRoundRect(x - 8, y - 12, x + 8, y - 5, 4, 4, p);
            // بند‌ها روی شانه
            p.setColor(0x2A000000);
            c.drawRoundRect(x - 10, y - 19, x - 7, y - 13, 2, 2, p);
            c.drawRoundRect(x + 7, y - 19, x + 10, y - 13, 2, 2, p);
        } else if (dir == 1 || dir == 3) {
            // از بغل — نیمهٔ کوله در سمت پشت سر
            p.setColor(color);
            if (dir == 3) c.drawRoundRect(x - 16, y - 15, x - 8, y + 2, 5, 5, p);
            else c.drawRoundRect(x + 8, y - 15, x + 16, y + 2, 5, 5, p);
            // بند روی شانه
            p.setColor(0x2A000000);
            if (dir == 3) c.drawRoundRect(x - 6, y - 18, x - 3, y - 12, 2, 2, p);
            else c.drawRoundRect(x + 3, y - 18, x + 6, y - 12, 2, 2, p);
        } else {
            // از روبه‌رو — فقط بند‌های روی شانه
            p.setColor(0x33000000);
            c.drawRoundRect(x - 9, y - 17, x - 6, y - 6, 2, 2, p);
            c.drawRoundRect(x + 6, y - 17, x + 9, y - 6, 2, 2, p);
        }
    }

    // ================= درخت =================

    public void drawTree(Canvas c, float x, float y, float size) {
        p.setColor(0x33000000);
        c.drawOval(x - 14 * size, y + 12 * size, x + 14 * size, y + 18 * size, p);
        p.setColor(G.COL_TRUNK);
        c.drawRoundRect(x - 4 * size, y - 8 * size, x + 4 * size, y + 14 * size, 3, 3, p);
        p.setColor(G.COL_TREE);
        c.drawCircle(x, y - 18 * size, 16 * size, p);
        c.drawCircle(x - 11 * size, y - 10 * size, 12 * size, p);
        c.drawCircle(x + 11 * size, y - 10 * size, 12 * size, p);
        p.setColor(0x3366BB6A);
        c.drawCircle(x - 4 * size, y - 22 * size, 7 * size, p);
    }

    // ================= فواره =================

    public void drawFountain(Canvas c, float x, float y, float minutes) {
        float t = minutes * 0.6f;
        p.setColor(0xFFB0BEC5);
        c.drawCircle(x, y, 42, p);
        p.setColor(G.COL_WATER);
        c.drawCircle(x, y, 34, p);
        p.setColor(0xFF90CAF9);
        c.drawCircle(x, y, 10, p);
        // قطره‌های پرشی
        p.setColor(0xB3E3F2FD);
        for (int i = 0; i < 8; i++) {
            float a = i * (float) Math.PI / 4f + t;
            float rr = 18f + (float) Math.sin(t * 2f + i) * 6f;
            c.drawCircle(x + (float) Math.cos(a) * rr, y + (float) Math.sin(a) * rr * 0.5f - 14f, 3.5f, p);
        }
        p.setColor(0x33000000);
        c.drawOval(x - 42, y + 34, x + 42, y + 44, p);
    }

    // ================= آبشار پارک — صخره‌ها با رگه‌های متحرک و کف سفید =================

    public void drawWaterfall(Canvas c, float x, float y, float minutes) {
        float t = minutes * 2.2f;

        // صخره‌های بالا
        p.setColor(0xFF78909C);
        c.drawCircle(x - 66, y - 26, 26, p);
        c.drawCircle(x + 66, y - 26, 26, p);
        p.setColor(0xFF90A4AE);
        c.drawCircle(x - 44, y - 34, 20, p);
        c.drawCircle(x + 44, y - 34, 20, p);

        // رگه‌های آب در حال ریختن
        for (int i = 0; i < 5; i++) {
            float off = ((t * 90f + i * 40f) % 100f) / 100f;
            int alpha = 120 + (int) (Math.sin(t * 3f + i) * 50);
            p.setColor((alpha << 24) | 0xBBDEFB);
            float sx = x - 48 + i * 24f;
            c.drawRoundRect(sx, y - 20 + off * 80f, sx + 13, y + 30 + off * 70f, 6, 6, p);
        }
        // ستون اصلی آب
        p.setColor(0x99E3F2FD);
        c.drawRoundRect(x - 52, y - 20, x + 52, y + 34, 10, 10, p);

        // کف و مه
        float foam = (float) Math.sin(t * 6f) * 3f;
        p.setColor(0xCCFFFFFF);
        c.drawOval(x - 58, y + 24 + foam, x + 58, y + 44 + foam, p);
        p.setColor(0x55FFFFFF);
        c.drawOval(x - 70, y + 30 + foam, x - 40, y + 42 + foam, p);
        c.drawOval(x + 40, y + 30 + foam, x + 70, y + 42 + foam, p);
    }

    // ================= ریل قطار =================

    public void drawRails(Canvas c, RailPath path) {
        if (path == null) return;
        float[] xs = {path.left(), path.right(), path.right(), path.left()};
        float[] ys = {path.top(), path.top(), path.bottom(), path.bottom()};
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            float x1 = xs[i], y1 = ys[i], x2 = xs[j], y2 = ys[j];
            boolean horizontal = Math.abs(y2 - y1) < 1f;

            // بالاست (شن)
            p.setColor(0xFFBCAAA4);
            p.setStrokeWidth(46f);
            c.drawLine(x1, y1, x2, y2, p);
            p.setColor(0xFFA1887F);
            p.setStrokeWidth(38f);
            c.drawLine(x1, y1, x2, y2, p);

            // تخت‌ها (چوب‌های ریل)
            p.setColor(0xFF6D4C41);
            p.setStrokeWidth(10f);
            float len = (float) Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
            int sleepers = (int) (len / 34f);
            for (int k = 1; k < sleepers; k++) {
                float t = k * 34f / len;
                float sx = x1 + (x2 - x1) * t, sy = y1 + (y2 - y1) * t;
                if (horizontal) {
                    c.drawLine(sx, sy - 20f, sx, sy + 20f, p);
                } else {
                    c.drawLine(sx - 20f, sy, sx + 20f, sy, p);
                }
            }

            // دو ریل فولادی
            p.setColor(0xFFCFD8DC);
            p.setStrokeWidth(5f);
            if (horizontal) {
                c.drawLine(x1, y1 - 11f, x2, y2 - 11f, p);
                c.drawLine(x1, y1 + 11f, x2, y2 + 11f, p);
            } else {
                c.drawLine(x1 - 11f, y1, x2 - 11f, y2, p);
                c.drawLine(x1 + 11f, y1, x2 + 11f, y2, p);
            }
        }
    }

    // ================= قطار با واگن‌ها =================

    public void drawTrain(Canvas c, Vehicle v, RailPath path, float minutes) {
        if (path == null) return;
        // واگن‌ها از عقب به جلو رسم شوند
        for (int i = 3; i >= 1; i--) {
            drawWagon(c, v, path, i);
        }
        drawLocomotive(c, v, minutes);
    }

    private void drawWagon(Canvas c, Vehicle v, RailPath path, int i) {
        float[] pos = new float[3];
        path.posAt(v.trackPos - i * 95f, pos);
        c.save();
        c.translate(pos[0], pos[1]);
        c.rotate((float) Math.toDegrees(pos[2]));

        int[] colors = {0, 0xFF1976D2, 0xFF43A047, 0xFFFB8C00};
        p.setColor(0x33000000);
        c.drawRoundRect(-46f, 6f, 46f, 30f, 8, 8, p);
        p.setColor(colors[i]);
        c.drawRoundRect(-44f, -24f, 44f, 22f, 9, 9, p);
        p.setColor(0xFFFFFFFF);
        p.setStrokeWidth(3f);
        for (int w = -30; w <= 30; w += 15) {
            c.drawRoundRect(w - 5f, -16f, w + 5f, -4f, 3, 3, p);
        }
        // چرخ‌ها
        p.setColor(0xFF37474F);
        c.drawCircle(-30f, 24f, 8f, p);
        c.drawCircle(0f, 24f, 8f, p);
        c.drawCircle(30f, 24f, 8f, p);
        p.setColor(0xFF90A4AE);
        c.drawCircle(-30f, 24f, 3.5f, p);
        c.drawCircle(0f, 24f, 3.5f, p);
        c.drawCircle(30f, 24f, 3.5f, p);
        c.restore();
    }

    private void drawLocomotive(Canvas c, Vehicle v, float minutes) {
        c.save();
        c.translate(v.x, v.y);
        c.rotate((float) Math.toDegrees(v.angle));

        // سایه
        p.setColor(0x33000000);
        c.drawRoundRect(-60f, 10f, 62f, 34f, 10, 10, p);

        // بدنه قرمز
        p.setColor(0xFFD32F2F);
        c.drawRoundRect(-58f, -28f, 60f, 26f, 10, 10, p);
        // اتاق راننده
        p.setColor(0xFFB71C1C);
        c.drawRoundRect(-58f, -46f, 6f, -20f, 8, 8, p);
        p.setColor(0xFF81D4FA);
        c.drawRoundRect(-52f, -42f, -12f, -26f, 5, 5, p);
        // نوار طلایی
        p.setColor(0xFFFFD54F);
        c.drawRoundRect(-58f, -8f, 60f, 0f, 4, 4, p);
        // دودکش
        p.setColor(0xFF424242);
        c.drawRoundRect(28f, -52f, 44f, -26f, 5, 5, p);
        p.setColor(0xFF616161);
        c.drawRoundRect(24f, -58f, 48f, -50f, 5, 5, p);
        // جلوکوب
        p.setColor(0xFF6D4C41);
        c.drawRoundRect(56f, -16f, 70f, 26f, 6, 6, p);
        // چراغ جلو
        p.setColor(0xFFFFEE58);
        c.drawCircle(58f, -18f, 6f, p);

        // چرخ‌ها
        p.setColor(0xFF37474F);
        c.drawCircle(-38f, 26f, 11f, p);
        c.drawCircle(-8f, 26f, 11f, p);
        c.drawCircle(24f, 26f, 11f, p);
        p.setColor(0xFF90A4AE);
        c.drawCircle(-38f, 26f, 5f, p);
        c.drawCircle(-8f, 26f, 5f, p);
        c.drawCircle(24f, 26f, 5f, p);

        c.restore();

        // برچسب شادی
        Fonts.applyBold(p);
        p.setColor(0xFFFFFFFF);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(18f);
        c.drawText("شادی", v.x, v.y - 34f, p);
    }

    // ================= خودروها =================

    public void drawVehicle(Canvas c, Vehicle v, float minutes) {
        if (v.type == Vehicle.CAR_HELICOPTER) {
            drawHelicopter(c, v, minutes);
            return;
        }
        if (v.type == Vehicle.CAR_BOAT) {
            drawBoat(c, v, minutes);
            return;
        }
        if (v.type == Vehicle.CAR_HORSE) {
            drawHorse(c, v.x, v.y, v.face, v.anim, v.speed > 5f);
            return;
        }
        c.save();
        c.translate(v.x, v.y);
        c.rotate((float) Math.toDegrees(v.angle));

        boolean motor = v.type == Vehicle.MOTOR;
        boolean bus = v.type == Vehicle.CAR_BUS;
        boolean pickup = v.type == Vehicle.CAR_PICKUP;
        boolean sport = v.type == Vehicle.CAR_SPORT;
        boolean police = v.type == Vehicle.CAR_POLICE;
        // ✅ ماشین‌ها بزرگ‌تر شدند
        float L = motor ? 33f : (bus ? 72f : 58f);
        float W = motor ? 16f : (bus ? 30f : 25f);

        // سایه
        p.setColor(0x33000000);
        c.drawRoundRect(-L, W - 4f, L, W + 7f, 9, 9, p);

        // ✅ بدون چرخ و لاستیک — طبق درخواست کاربر (بدون چرخ قشنگ‌تر است)
        // بدنه
        p.setColor(v.color);
        c.drawRoundRect(-L, -W, L, W, 10, 10, p);
        p.setColor(0x26000000);
        c.drawRoundRect(-L, W * 0.35f, L, W, 9, 9, p);   // سایه زیر بدنه
        // سپر جلو و عقب
        p.setColor(0xFF37474F);
        c.drawRoundRect(L - 7f, -W + 3f, L + 4f, W - 3f, 4, 4, p);
        c.drawRoundRect(-L - 4f, -W + 3f, -L + 5f, W - 3f, 4, 4, p);

        if (motor) {
            // موتور: بدن راننده + غربیلک
            p.setColor(0xFF5D4037);
            c.drawRoundRect(-L * 0.5f, -W * 0.7f, -L * 0.1f, W * 0.7f, 5, 5, p);
            p.setColor(0xFF263238);
            c.drawCircle(L * 0.45f, 0f, 6f, p);
            // چراغ جلو
            p.setColor(0xFFFFEE58);
            c.drawCircle(L + 1f, -W * 0.4f, 3.5f, p);
            c.drawCircle(L + 1f, W * 0.4f, 3.5f, p);
        } else if (police) {
            // 🚓 پلیس: بدنه سفید + نوار تیره + چراغ‌گردان
            p.setColor(0xFF37474F);
            c.drawRoundRect(-L * 0.9f, -W + 3f, -L * 0.1f, W - 3f, 5, 5, p);
            p.setColor(0xFFB3E5FC);
            c.drawRoundRect(L * 0.1f, -W + 4f, L * 0.55f, W - 4f, 5, 5, p);
            // چراغ‌گردان قرمز/آبی چشمک‌زن
            boolean flash = ((int) (minutes * 4f) % 2) == 0;
            p.setColor(flash ? 0xFFFF1744 : 0xFF2979FF);
            c.drawRoundRect(-6f, -W - 7f, -1f, -W - 1f, 2, 2, p);
            p.setColor(flash ? 0xFF2979FF : 0xFFFF1744);
            c.drawRoundRect(1f, -W - 7f, 6f, -W - 1f, 2, 2, p);
        } else if (bus) {
            // اتوبوس: ردیف پنجره + در
            p.setColor(0xFFB3E5FC);
            for (int i = 0; i < 4; i++) {
                float wx2 = -L * 0.85f + i * (L * 0.42f);
                c.drawRoundRect(wx2, -W + 5f, wx2 + L * 0.3f, -W * 0.1f, 3, 3, p);
            }
            p.setColor(0xFFFFC107);
            c.drawRoundRect(L * 0.55f, -W * 0.6f, L * 0.8f, W * 0.6f, 3, 3, p);
            // خط رنگی
            p.setColor(0xFFFFFFFF);
            c.drawRoundRect(-L, W * 0.15f, L, W * 0.35f, 2, 2, p);
        } else if (pickup) {
            // وانت: کابین جلو + باربند عقب
            p.setColor(0xFFB3E5FC);
            c.drawRoundRect(L * 0.05f, -W + 4f, L * 0.5f, W - 4f, 5, 5, p);
            p.setColor(0xFF6D4C41);
            c.drawRoundRect(-L * 0.9f, -W + 3f, -L * 0.05f, W - 3f, 3, 3, p);
            p.setColor(0x55000000);
            c.drawRoundRect(-L * 0.82f, -W + 5f, -L * 0.13f, W - 5f, 2, 2, p);
        } else {
            // سدان/اسپرت: کابین + شیشه جلو و عقب
            p.setColor(0xFF3E2723);
            float cabL = sport ? L * 0.62f : L * 0.55f;
            c.drawRoundRect(-cabL, -W + 3f, cabL * 0.75f, W - 3f, 7, 7, p);
            p.setColor(0xFFB3E5FC);
            c.drawRoundRect(-cabL + 4f, -W + 6f, -cabL * 0.15f, W - 6f, 4, 4, p);
            c.drawRoundRect(cabL * 0.15f, -W + 6f, cabL * 0.65f, W - 6f, 4, 4, p);
            if (sport) {
                // باله عقب اسپرت
                p.setColor(0xFF263238);
                c.drawRoundRect(-L - 2f, -W - 8f, -L * 0.55f, -W - 3f, 3, 3, p);
            }
        }
        // چراغ‌ها: زرد جلو، قرمز عقب
        if (!motor) {
            p.setColor(0xFFFFEE58);
            c.drawCircle(L - 3f, -W * 0.55f, 4f, p);
            c.drawCircle(L - 3f, W * 0.55f, 4f, p);
            p.setColor(0xFFE53935);
            c.drawCircle(-L + 3f, -W * 0.55f, 3.5f, p);
            c.drawCircle(-L + 3f, W * 0.55f, 3.5f, p);
        }
        // تاکسی: تابلو
        Fonts.apply(p);
        if (v.type == Vehicle.CAR_TAXI) {
            p.setColor(0xFFFFEB3B);
            c.drawRoundRect(-8f, -W - 9f, 8f, -W - 2f, 3, 3, p);
            p.setColor(0xFF37474F);
            p.setTextSize(8f);
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText("تاکسی", 0f, -W - 3.5f, p);
        }
        // اتوبوس: تابلو
        if (bus) {
            p.setColor(0xFF37474F);
            p.setTextSize(11f);
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText("اتوبوس", 0f, -W - 5f, p);
        }
        c.restore();
    }

    // ================= اسب 🐴 =================

    /**
     * ✅ اسب همیشه ایستاده رسم می‌شود (رفع باگ: حرکت به پایین اسب را وارونه می‌کرد)
     * face = ۱ → رو به راست | face = ۱- → رو به چپ (آینه افقی، بدون هیچ چرخشی)
     */
    public void drawHorse(Canvas c, float x, float y, int face, float t, boolean moving) {
        c.save();
        c.translate(x, y);
        c.scale(face, 1f);   // فقط آینه افقی — پاها همیشه پایین، سر همیشه بالا
        float swing = moving ? (float) Math.sin(t * 10f) * 5f : 0f;

        // سایه
        p.setColor(0x33000000);
        c.drawOval(-32f, 14f, 38f, 26f, p);

        // پاها (دو جفت، تاب طبیعی)
        p.setColor(0xFF6D4C41);
        c.drawRoundRect(-24f + swing, 0f, -17f, 18f, 3, 3, p);
        c.drawRoundRect(14f - swing, 0f, 21f, 18f, 3, 3, p);
        c.drawRoundRect(-24f - swing, -4f, -17f, 14f, 3, 3, p);
        c.drawRoundRect(14f + swing, -4f, 21f, 14f, 3, 3, p);
        // سم‌ها
        p.setColor(0xFF3E2723);
        c.drawRoundRect(-24f + swing, 16f, -17f + swing, 20f, 2, 2, p);
        c.drawRoundRect(14f - swing, 16f, 21f - swing, 20f, 2, 2, p);

        // بدن
        p.setColor(0xFF8D5524);
        c.drawRoundRect(-30f, -14f, 26f, 10f, 13, 13, p);
        // دم
        p.setColor(0xFF4E342E);
        p.setStrokeWidth(5f);
        c.drawLine(-30f, -6f, -40f, 8f, p);
        p.setStrokeWidth(0.1f);
        // یال
        c.drawRoundRect(6f, -24f, 14f, -10f, 5, 5, p);
        // گردن و سر
        c.drawRoundRect(16f, -28f, 30f, -6f, 9, 9, p);
        c.drawCircle(33f, -25f, 8.5f, p);
        // گوش
        tri(c, new float[]{28f, -32f, 32f, -32f, 30f, -38f});
        // چشم و پوزه
        p.setColor(0xFF3E2723);
        c.drawCircle(36f, -26f, 1.6f, p);
        p.setColor(0xFF5D4037);
        c.drawOval(36f, -22f, 43f, -17f, p);
        // زین قرمز
        p.setColor(0xFFB71C1C);
        c.drawRoundRect(-10f, -19f, 8f, -9f, 4, 4, p);
        p.setColor(0xFFFFD54F);
        c.drawCircle(-1f, -14f, 2.5f, p);

        c.restore();
    }

    // ================= هواپیمای فرودگاه ✈ =================

    public void drawPlane(Canvas c, float x, float y, float angle, float alt, float minutes) {
        // سایه روی زمین (با ارتفاع دور می‌شود)
        if (alt > 6f) {
            p.setColor(0x33000000);
            c.save();
            c.translate(x + alt * 0.5f, y + alt * 0.9f);
            c.rotate((float) Math.toDegrees(angle));
            c.drawOval(-42f, -13f, 46f, 13f, p);
            c.restore();
        }

        c.save();
        c.translate(x, y);
        c.rotate((float) Math.toDegrees(angle));
        float s = 1f + alt / 700f;
        c.scale(s, s);

        // بال‌ها
        p.setColor(0xFFB0BEC5);
        c.drawRoundRect(-12f, -48f, 6f, 48f, 10, 10, p);
        // بدنه
        p.setColor(0xFFECEFF1);
        c.drawRoundRect(-46f, -11f, 48f, 11f, 13, 13, p);
        p.setColor(0xFFE53935);
        c.drawRoundRect(-46f, -4f, 48f, 4f, 4, 4, p);
        // دم
        tri(c, new float[]{-42f, -2f, -26f, -2f, -42f, -24f});
        tri(c, new float[]{-42f, 2f, -26f, 2f, -42f, 24f});
        // کاکپیت
        p.setColor(0xFF81D4FA);
        c.drawRoundRect(34f, -7f, 45f, 7f, 4, 4, p);
        // پنجره‌ها
        p.setColor(0xFF90CAF9);
        for (int i = 0; i < 6; i++) {
            c.drawCircle(-26f + i * 10f, -5f, 2.2f, p);
        }
        // ملخ چرخان
        float spin = Math.abs((float) Math.sin(minutes * 70f));
        p.setColor(0xFF37474F);
        p.setStrokeWidth(4.5f);
        c.drawLine(48f, -17f * spin, 48f, 17f * spin, p);
        p.setStrokeWidth(0.1f);

        c.restore();
    }

    // ================= ایستگاه اتوبوس 🚌 =================

    public void drawBusStop(Canvas c, float x, float y, String name, float minutes) {
        // سایه
        p.setColor(0x33000000);
        c.drawOval(x - 30f, y + 18f, x + 30f, y + 30f, p);
        // نیمکت
        p.setColor(0xFF8D6E63);
        c.drawRoundRect(x - 34f, y + 4f, x + 6f, y + 14f, 4, 4, p);
        c.drawRoundRect(x - 32f, y + 14f, x - 28f, y + 24f, 2, 2, p);
        c.drawRoundRect(x - 16f, y + 14f, x - 12f, y + 24f, 2, 2, p);
        // پایه تابلو
        p.setColor(0xFF546E7A);
        c.drawRoundRect(x + 16f, y - 32f, x + 22f, y + 20f, 3, 3, p);
        // تابلوی آبی اتوبوس
        p.setColor(0xFF1E88E5);
        c.drawRoundRect(x - 4f, y - 60f, x + 46f, y - 30f, 6, 6, p);
        p.setColor(0xFFFFFFFF);
        c.drawRoundRect(x + 0f, y - 56f, x + 42f, y - 46f, 3, 3, p);
        c.drawRoundRect(x + 4f, y - 54f, x + 38f, y - 48f, 2, 2, p);
        p.setColor(0xFF0D47A1);
        c.drawCircle(x + 10f, y - 45f, 3f, p);
        c.drawCircle(x + 32f, y - 45f, 3f, p);
        // اسم ایستگاه — با فونت وزیر (درخواست کاربر)
        Fonts.applyBold(p);
        p.setColor(0xFF37474F);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(16f);
        c.drawText(name, x + 6f, y - 68f, p);
    }

    /**
     * قایق بادبانی کوچک — فقط روی آب
     */
    private void drawBoat(Canvas c, Vehicle v, float minutes) {
        c.save();
        c.translate(v.x, v.y);
        c.rotate((float) Math.toDegrees(v.angle));
        float rock = (float) Math.sin(minutes * 2f) * 2f;
        c.translate(0f, rock);

        // موج زیر قایق
        p.setColor(0x88FFFFFF);
        c.drawOval(-40f, 12f, 40f, 22f, p);
        // بدنه چوبی
        p.setColor(0xFF8D6E63);
        android.graphics.Path hull = new android.graphics.Path();
        hull.moveTo(-38f, -14f);
        hull.lineTo(38f, -14f);
        hull.lineTo(26f, 16f);
        hull.lineTo(-26f, 16f);
        hull.close();
        c.drawPath(hull, p);
        p.setColor(0xFFA1887F);
        c.drawRoundRect(-34f, -12f, 34f, -2f, 4, 4, p);
        // نیم‌کت و سکان
        p.setColor(0xFF5D4037);
        c.drawRoundRect(-8f, -12f, 6f, 6f, 3, 3, p);
        // بادبان
        p.setColor(0xFFFFFFFF);
        c.drawLine(0f, -12f, 0f, -54f, p);
        p.setStrokeWidth(3f);
        c.drawLine(0f, -12f, 0f, -54f, p);
        p.setStrokeWidth(0.1f);
        p.setColor(0xFFFF7043);
        android.graphics.Path sail = new android.graphics.Path();
        sail.moveTo(3f, -52f);
        sail.quadTo(30f, -34f, 4f, -14f);
        sail.close();
        c.drawPath(sail, p);
        p.setColor(0xFFE53935);
        android.graphics.Path sail2 = new android.graphics.Path();
        sail2.moveTo(-3f, -50f);
        sail2.quadTo(-26f, -32f, -4f, -14f);
        sail2.close();
        c.drawPath(sail2, p);
        // پرچم
        p.setColor(0xFFFFD54F);
        c.drawCircle(0f, -56f, 3.5f, p);
        c.restore();
    }

    private void drawHelicopter(Canvas c, Vehicle v, float minutes) {
        c.save();
        c.translate(v.x, v.y);

        float fly = v.mode == Vehicle.MODE_PLAYER ? -14f : 0f;
        c.translate(0, fly);

        // سایه روی زمین
        p.setColor(0x22000000);
        c.drawOval(-30f, 22f - fly, 30f, 34f - fly, p);

        // بدنه
        p.setColor(0xFFE53935);
        c.drawOval(-30f, -20f, 26f, 18f, p);
        p.setColor(0xFFB3E5FC);
        c.drawOval(-16f, -13f, 4f, 2f, p);
        // دم
        p.setColor(0xFFC62828);
        c.drawRoundRect(22f, -6f, 54f, 0f, 4, 4, p);
        c.drawCircle(54f, -3f, 8f, p);
        // اسکی‌ها
        p.setColor(0xFF455A64);
        p.setStrokeWidth(4f);
        c.drawLine(-22f, 18f, 18f, 18f, p);
        c.drawLine(-14f, 12f, -14f, 18f, p);
        c.drawLine(10f, 12f, 10f, 18f, p);
        // ملخ چرخان
        float spin = minutes * 60f;
        p.setColor(0xFF37474F);
        p.setStrokeWidth(5f);
        c.drawLine(-34f, -22f, 34f, -22f, p);
        c.save();
        c.rotate((spin % 360f), 0f, -24f);
        p.setStrokeWidth(6f);
        c.drawLine(-44f, -24f, 44f, -24f, p);
        c.restore();
        c.restore();
    }

    // ================= حیوانات باغ‌وحش =================

    public void drawAnimal(Canvas c, float x, float y, int type, float minutes) {
        float bob = (float) Math.sin(minutes * 2f + x) * 3f;
        p.setColor(0x33000000);
        c.drawOval(x - 26f, y + 16f, x + 26f, y + 24f, p);

        switch (type) {
            case 0:   // شیر
                p.setColor(0xFFFF8F00);
                c.drawCircle(x, y - 4f + bob, 22f, p);
                p.setColor(0xFFFFB74D);
                c.drawCircle(x, y - 2f + bob, 15f, p);
                p.setColor(0xFF3E2723);
                c.drawCircle(x - 5f, y - 4f + bob, 2f, p);
                c.drawCircle(x + 5f, y - 4f + bob, 2f, p);
                c.drawCircle(x, y + 2f + bob, 2.5f, p);
                break;
            case 1:   // فیل
                p.setColor(0xFF90A4AE);
                c.drawRoundRect(x - 26f, y - 8f + bob, x + 22f, y + 14f, 14, 14, p);
                c.drawCircle(x + 22f, y - 10f + bob, 13f, p);
                p.setStrokeWidth(6f);
                c.drawLine(x + 30f, y - 6f + bob, x + 34f, y + 12f, p);
                c.drawCircle(x - 18f, y + 16f, 6f, p);
                c.drawCircle(x + 12f, y + 16f, 6f, p);
                p.setColor(0xFFFFFFFF);
                c.drawCircle(x + 26f, y - 13f + bob, 2f, p);
                break;
            case 2:   // میمون
                p.setColor(0xFF8D6E63);
                c.drawCircle(x, y + bob, 14f, p);
                c.drawCircle(x, y - 14f + bob, 9f, p);
                p.setColor(0xFFD7CCC8);
                c.drawCircle(x, y - 12f + bob, 5.5f, p);
                p.setColor(0xFF3E2723);
                c.drawCircle(x - 3f, y - 15f + bob, 1.5f, p);
                c.drawCircle(x + 3f, y - 15f + bob, 1.5f, p);
                p.setStrokeWidth(4f);
                p.setColor(0xFF8D6E63);
                c.drawLine(x + 12f, y + bob, x + 22f, y - 10f + bob, p);
                break;
            case 3:   // گورخر
                p.setColor(0xFFFFFFFF);
                c.drawRoundRect(x - 24f, y - 6f + bob, x + 20f, y + 14f, 12, 12, p);
                p.setColor(0xFF37474F);
                for (int i = 0; i < 5; i++) {
                    float sx = x - 18f + i * 9f;
                    c.drawLine(sx, y - 6f + bob, sx, y + 14f, p);
                }
                p.setStrokeWidth(0.1f);
                p.setColor(0xFF37474F);
                c.drawCircle(x + 22f, y - 12f + bob, 8f, p);
                break;
            case 4:   // پنگوئن
                p.setColor(0xFF263238);
                c.drawRoundRect(x - 12f, y - 18f + bob, x + 12f, y + 16f, 12, 12, p);
                p.setColor(0xFFFFFFFF);
                c.drawOval(x - 7f, y - 10f + bob, x + 7f, y + 12f, p);
                p.setColor(0xFFFFB300);
                tri(c, new float[]{x - 6f, y - 14f + bob, x + 6f, y - 14f + bob, x, y - 9f + bob});
                p.setColor(0xFF263238);
                c.drawCircle(x - 3.5f, y - 13f + bob, 1.5f, p);
                c.drawCircle(x + 3.5f, y - 13f + bob, 1.5f, p);
                break;
            default:  // زرافه
                p.setColor(0xFFFFCA28);
                c.drawRoundRect(x - 18f, y - 2f + bob, x + 16f, y + 16f, 10, 10, p);
                c.drawRoundRect(x + 8f, y - 34f + bob, x + 15f, y - 2f, 7, 7, p);
                c.drawCircle(x + 13f, y - 38f + bob, 7f, p);
                p.setColor(0xFF8D6E63);
                c.drawCircle(x - 8f, y + 4f + bob, 2.5f, p);
                c.drawCircle(x + 2f, y + 8f + bob, 2.5f, p);
                c.drawCircle(x + 11f, y - 20f + bob, 2f, p);
                p.setColor(0xFF3E2723);
                c.drawCircle(x + 15f, y - 39f + bob, 1.5f, p);
                break;
            case 6:   // خرس
                p.setColor(0xFF795548);
                c.drawRoundRect(x - 22f, y - 8f + bob, x + 16f, y + 14f, 12, 12, p);
                c.drawCircle(x + 18f, y - 8f + bob, 11f, p);
                c.drawCircle(x + 11f, y - 17f + bob, 4f, p);
                c.drawCircle(x + 25f, y - 17f + bob, 4f, p);
                p.setColor(0xFFD7CCC8);
                c.drawOval(x + 14f, y - 6f + bob, x + 23f, y + 0f + bob, p);
                p.setColor(0xFF3E2723);
                c.drawCircle(x + 15f, y - 11f + bob, 1.8f, p);
                c.drawCircle(x + 22f, y - 11f + bob, 1.8f, p);
                break;
            case 7:   // ببر
                p.setColor(0xFFFF8F00);
                c.drawRoundRect(x - 24f, y - 6f + bob, x + 18f, y + 14f, 12, 12, p);
                c.drawCircle(x + 20f, y - 10f + bob, 10f, p);
                p.setColor(0xFF3E2723);
                for (int i = 0; i < 4; i++) {
                    float sx3 = x - 17f + i * 10f;
                    c.drawLine(sx3, y - 6f + bob, sx3 - 2f, y + 6f + bob, p);
                }
                p.setColor(0xFFFFFFFF);
                c.drawOval(x + 16f, y - 6f + bob, x + 25f, y - 1f + bob, p);
                p.setColor(0xFF3E2723);
                c.drawCircle(x + 18f, y - 13f + bob, 1.7f, p);
                c.drawCircle(x + 24f, y - 13f + bob, 1.7f, p);
                break;
            case 8:   // پاندا
                p.setColor(0xFFFFFFFF);
                c.drawRoundRect(x - 20f, y - 8f + bob, x + 14f, y + 14f, 12, 12, p);
                c.drawCircle(x + 15f, y - 10f + bob, 10f, p);
                p.setColor(0xFF37474F);
                c.drawCircle(x + 8f, y - 14f + bob, 3.5f, p);
                c.drawCircle(x + 22f, y - 14f + bob, 3.5f, p);
                c.drawOval(x + 11f, y - 11f + bob, x + 19f, y - 4f + bob, p);
                c.drawCircle(x - 6f, y + 2f + bob, 5f, p);
                c.drawRoundRect(x - 20f, y - 8f + bob, x - 13f, y + 8f, 5, 5, p);
                p.setColor(0xFF3E2723);
                c.drawCircle(x + 15f, y - 8f + bob, 1.6f, p);
                break;
            case 9:   // شتر
                p.setColor(0xFFD7A86E);
                c.drawRoundRect(x - 26f, y - 4f + bob, x + 14f, y + 14f, 12, 12, p);
                c.drawCircle(x - 8f, y - 12f + bob, 8f, p);
                c.drawCircle(x + 4f, y - 12f + bob, 8f, p);
                c.drawRoundRect(x + 14f, y - 26f + bob, x + 22f, y - 2f, 6, 6, p);
                c.drawCircle(x + 19f, y - 28f + bob, 6.5f, p);
                p.setColor(0xFF3E2723);
                c.drawCircle(x + 17f, y - 30f + bob, 1.4f, p);
                p.setStrokeWidth(3f);
                c.drawLine(x + 22f, y - 24f + bob, x + 26f, y - 18f + bob, p);
                p.setStrokeWidth(0.1f);
                break;
        }
    }

    // ================= ساختمان‌ها =================

    public void drawBuilding(Canvas c, Building b, float minutes) {
        boolean night = minutes > 18.5f * 60f || minutes < 6f * 60f;

        // سایه
        p.setColor(0x44000000);
        c.drawRoundRect(b.x + 8f, b.y + 10f, b.x + b.w + 8f, b.y + b.h + 10f, 14, 14, p);

        if (b.type == Building.HELIPORT) {
            drawHeliport(c, b, minutes);
            return;
        }

        // ✅ ساختمان‌ها کوتاه‌تر شدند (قبلاً خیلی کشیده بودند)
        float bodyTop = b.y + b.h * 0.22f;
        float ground = b.y + b.h;

        // بدنه
        p.setColor(b.wallColor);
        c.drawRoundRect(b.x, bodyTop, b.x + b.w, ground, 10, 10, p);
        // سقف
        p.setColor(b.roofColor);
        c.drawRoundRect(b.x - 6f, bodyTop - 16f, b.x + b.w + 6f, bodyTop + 18f, 10, 10, p);
        p.setColor(0x22000000);
        c.drawRoundRect(b.x - 6f, bodyTop + 10f, b.x + b.w + 6f, bodyTop + 18f, 6, 6, p);

        // پنجره‌ها (شب روشن)
        float winTop = bodyTop + 32f;
        float winBot = ground - 86f;
        if (winBot > winTop + 20f) {
            int winCols = Math.max(2, (int) (b.w / 150f));
            float wgap = b.w / (winCols + 1f);
            for (int i = 0; i < winCols; i++) {
                float wx = b.x + wgap * (i + 1) - 24f;
                p.setColor(night ? 0xFFFFF59D : 0xFFB3E5FC);
                c.drawRoundRect(wx, winTop, wx + 46f, winBot, 6, 6, p);
                p.setColor(0x66000000);
                p.setStrokeWidth(3f);
                c.drawLine(wx + 23f, winTop, wx + 23f, winBot, p);
            }
        }

        // در
        p.setColor(0xFF6D4C41);
        c.drawRoundRect(b.doorX - 24f, b.doorY - 42f, b.doorX + 24f, b.doorY, 8, 8, p);
        p.setColor(0xFFFFD54F);
        c.drawCircle(b.doorX + 14f, b.doorY - 20f, 3f, p);

        // ✅ اسم ساختمان بالای در — مثلاً «کلانتری ۱۰» (بدون استیکر، متن ساده!)
        float labelHalf = drawNameLabel(c, b);

        // تابلوی ویژه هر ساختمان (کنار اسم، بالای در)
        drawSign(c, b, night, labelHalf);
    }

    /**
     * ✅ اسم ساختمان — متن ساده روی بدنه (بدون تابلو/استیکر پشتش)
     * ✅ حالا با فونت فارسی «وزیر» + سایهٔ نرم‌تر — خواناتر و زیباتر (درخواست کاربر)
     * عرض نصف متن را برمی‌گرداند.
     */
    private float drawNameLabel(Canvas c, Building b) {
        String label = b.name();
        if (label == null || label.isEmpty()) return 0f;
        Fonts.applyBold(p);
        float ts = Math.min(28f, 580f / Math.max(8f, label.length()));
        if (ts < 16f) ts = 16f;
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(ts);
        float lcx = b.doorX;
        float lcy = b.doorY - 58f;
        float halfW = p.measureText(label) / 2f;
        // سایهٔ نرم یک‌تکه زیر متن (زیباتر از حاشیهٔ ضخیم چهارطرفه)
        p.setColor(0x8A102028);
        c.drawText(label, lcx, lcy + ts * 0.35f + 2.5f, p);
        p.setColor(0x66102028);
        c.drawText(label, lcx - 1.2f, lcy + ts * 0.35f + 1.2f, p);
        c.drawText(label, lcx + 1.2f, lcy + ts * 0.35f + 1.2f, p);
        p.setColor(0xFFFFFFFF);
        c.drawText(label, lcx, lcy + ts * 0.35f, p);
        return halfW;
    }

    private void drawSign(Canvas c, Building b, boolean night, float labelHalf) {
        float sx = b.doorX - labelHalf - 26f, sy = b.doorY - 58f;   // کنار اسم، بالای در
        if (sx < b.x + 20f) sx = b.x + 20f;   // نرود بیرون ساختمان
        switch (b.type) {
            case Building.BANK:
                p.setColor(0xFF2E7D32);
                c.drawCircle(sx, sy, 15f, p);
                p.setColor(0xFFFFFFFF);
                p.setTextAlign(Paint.Align.CENTER);
                p.setTextSize(20f);
                c.drawText("$", sx, sy + 7f, p);
                break;
            case Building.HOSPITAL:
                p.setColor(0xFFE53935);
                c.drawCircle(sx, sy, 15f, p);
                p.setColor(0xFFFFFFFF);
                c.drawRoundRect(sx - 11f, sy - 4f, sx + 11f, sy + 4f, 2, 2, p);
                c.drawRoundRect(sx - 4f, sy - 11f, sx + 4f, sy + 11f, 2, 2, p);
                break;
            case Building.CINEMA:
                p.setColor(0xFF6A1B9A);
                c.drawRoundRect(sx - 24f, sy - 13f, sx + 24f, sy + 13f, 8, 8, p);
                for (int i = -2; i <= 2; i++) {
                    p.setColor(night ? 0xFFFFEB3B : 0xFFCE93D8);
                    c.drawCircle(sx + i * 10f, sy - 13f, 3f, p);
                    c.drawCircle(sx + i * 10f, sy + 13f, 3f, p);
                }
                p.setColor(0xFFFFFFFF);
                p.setTextAlign(Paint.Align.CENTER);
                p.setTextSize(14f);
                c.drawText("★", sx, sy + 5f, p);
                break;
            case Building.ZOO:
                p.setColor(0xFF33691E);
                c.drawCircle(sx, sy, 15f, p);
                p.setColor(0xFFFFFFFF);
                p.setTextAlign(Paint.Align.CENTER);
                p.setTextSize(16f);
                c.drawText("🦁", sx, sy + 6f, p);
                break;
            case Building.TRAIN_STATION:
                p.setColor(0xFF5D4037);
                c.drawCircle(sx, sy, 15f, p);
                p.setColor(0xFFFFD54F);
                p.setTextAlign(Paint.Align.CENTER);
                p.setTextSize(15f);
                c.drawText("🚂", sx, sy + 5f, p);
                break;
            case Building.RESTAURANT:
                p.setColor(0xFFE65100);
                c.drawCircle(sx, sy, 15f, p);
                p.setColor(0xFFFFFFFF);
                p.setTextAlign(Paint.Align.CENTER);
                p.setTextSize(15f);
                c.drawText("🍽", sx, sy + 6f, p);
                break;
            case Building.CLOTHES:
                p.setColor(0xFFAD1457);
                c.drawCircle(sx, sy, 15f, p);
                p.setColor(0xFFFFFFFF);
                p.setTextAlign(Paint.Align.CENTER);
                p.setTextSize(15f);
                c.drawText("👕", sx, sy + 6f, p);
                break;
            case Building.AIRPORT:
                p.setColor(0xFF0277BD);
                c.drawCircle(sx, sy, 15f, p);
                p.setColor(0xFFFFFFFF);
                p.setTextAlign(Paint.Align.CENTER);
                p.setTextSize(15f);
                c.drawText("✈", sx, sy + 6f, p);
                break;
            default:
                break;
        }
    }

    private void drawHeliport(Canvas c, Building b, float minutes) {
        // سکوی بتنی
        p.setColor(0xFF78909C);
        c.drawRoundRect(b.x, b.y, b.x + b.w, b.y + b.h, 16, 16, p);
        p.setColor(0xFFCFD8DC);
        c.drawRoundRect(b.x + 14f, b.y + 14f, b.x + b.w - 14f, b.y + b.h - 14f, 12, 12, p);
        // H بزرگ
        p.setColor(0xFF37474F);
        p.setStrokeWidth(14f);
        float cx = b.doorX, cy = b.y + b.h * 0.42f;
        c.drawLine(cx - 26f, cy - 30f, cx - 26f, cy + 30f, p);
        c.drawLine(cx + 26f, cy - 30f, cx + 26f, cy + 30f, p);
        c.drawLine(cx - 26f, cy, cx + 26f, cy, p);
        // بادبان جهت‌نما
        p.setColor(0xFFFF7043);
        tri(c, new float[]{b.x + 20f, b.y - 40f, b.x + 20f, b.y - 16f, b.x + 60f, b.y - 28f});
        p.setColor(0xFF616161);
        p.setStrokeWidth(4f);
        c.drawLine(b.x + 20f, b.y - 44f, b.x + 20f, b.y, p);

        // اسم هلی‌پورت هم مثل بقیه ساختمان‌ها
        drawNameLabel(c, b);
    }

    // ================= مبلمان داخلی =================

    public void drawFurniture(Canvas c, float x, float y, float w, float h, int color, int type, float minutes) {
        switch (type) {
            case Interior.F_TABLE:
                p.setColor(0x33000000);
                c.drawRoundRect(x + 4f, y + h, x + w + 4f, y + h + 8f, 4, 4, p);
                p.setColor(color);
                c.drawRoundRect(x, y, x + w, y + h, 8, 8, p);
                p.setColor(0x33000000);
                c.drawCircle(x + w * 0.3f, y + h * 0.4f, 5f, p);
                break;
            case Interior.F_COUNTER:
                p.setColor(color);
                c.drawRoundRect(x, y, x + w, y + h, 8, 8, p);
                p.setColor(0xFFEEEEEE);
                c.drawRoundRect(x, y, x + w, y + h * 0.4f, 6, 6, p);
                break;
            case Interior.F_SHELF:
                p.setColor(0xFF5D4037);
                c.drawRoundRect(x, y, x + w, y + h, 5, 5, p);
                for (int i = 0; i < 6; i++) {
                    p.setColor(i % 2 == 0 ? 0xFFEF5350 : 0xFF42A5F5);
                    c.drawCircle(x + 16f + i * (w - 32f) / 5f, y + h * 0.4f, 6f, p);
                }
                break;
            case Interior.F_PLANT:
                p.setColor(0xFF8D6E63);
                c.drawRoundRect(x + w * 0.3f, y + h * 0.6f, x + w * 0.7f, y + h, 4, 4, p);
                p.setColor(0xFF2E7D32);
                c.drawCircle(x + w * 0.5f, y + h * 0.35f, w * 0.32f, p);
                p.setColor(0xFF43A047);
                c.drawCircle(x + w * 0.35f, y + h * 0.5f, w * 0.2f, p);
                c.drawCircle(x + w * 0.65f, y + h * 0.5f, w * 0.2f, p);
                break;
            case Interior.F_BED:
                p.setColor(0xFFB0BEC5);
                c.drawRoundRect(x, y, x + w, y + h, 6, 6, p);
                p.setColor(color);
                c.drawRoundRect(x + 4f, y + h * 0.3f, x + w - 4f, y + h - 4f, 6, 6, p);
                p.setColor(0xFFFFFFFF);
                c.drawRoundRect(x + 6f, y + 4f, x + w * 0.4f, y + h * 0.35f, 5, 5, p);
                break;
            case Interior.F_TV:
                p.setColor(0xFF37474F);
                c.drawRoundRect(x, y, x + w, y + h, 5, 5, p);
                int hue = (int) ((minutes * 40f) % 360f);
                p.setColor(0xFF000000 | (android.graphics.Color.HSVToColor(new float[]{hue, 0.5f, 1f}) & 0xFFFFFF));
                c.drawRoundRect(x + 5f, y + 5f, x + w - 5f, y + h - 5f, 3, 3, p);
                break;
            case Interior.F_RUG:
                p.setColor(color);
                c.drawOval(x, y, x + w, y + h, p);
                p.setColor(0x44FFFFFF);
                c.drawOval(x + 14f, y + 10f, x + w - 14f, y + h - 10f, p);
                break;
            case Interior.F_FENCE:
                p.setColor(0xFF8D6E63);
                p.setStrokeWidth(6f);
                c.drawLine(x, y, x + w, y, p);
                c.drawLine(x, y + h, x + w, y + h, p);
                c.drawLine(x, y, x, y + h, p);
                c.drawLine(x + w, y, x + w, y + h, p);
                c.drawLine(x, y + h / 2f, x + w, y + h / 2f, p);
                for (float px = x; px <= x + w; px += 48f) {
                    c.drawLine(px, y, px, y + h, p);
                }
                p.setColor(0x2266BB6A);
                c.drawRoundRect(x + 4f, y + 4f, x + w - 4f, y + h - 4f, 6, 6, p);
                break;
            case Interior.F_BOOKCASE:
                p.setColor(0xFF5D4037);
                c.drawRoundRect(x, y, x + w, y + h, 5, 5, p);
                for (int i = 0; i < 8; i++) {
                    p.setColor(new int[]{0xFFEF5350, 0xFF42A5F5, 0xFFFFCA28, 0xFF66BB6A}[i % 4]);
                    c.drawRoundRect(x + 8f + i * (w - 16f) / 8f, y + 6f, x + 8f + (i + 1) * (w - 16f) / 8f - 3f, y + h - 6f, 2, 2, p);
                }
                break;
            case Interior.F_DESK:
                p.setColor(color);
                c.drawRoundRect(x, y, x + w, y + h, 6, 6, p);
                p.setColor(0x44000000);
                c.drawRoundRect(x + 6f, y + 4f, x + w - 6f, y + h * 0.5f, 4, 4, p);
                break;
            case Interior.F_NUMBOARD: {
                // ✅ تخته آموزش اعداد (مدرسه)
                p.setColor(0xFF6D4C41);
                c.drawRoundRect(x - 6f, y - 6f, x + w + 6f, y + h + 6f, 8, 8, p);
                p.setColor(0xFF2E7D32);
                c.drawRoundRect(x, y, x + w, y + h, 5, 5, p);
                Fonts.applyBold(p);
                p.setTextAlign(Paint.Align.CENTER);
                String[] digs = {"۱", "۲", "۳", "۴", "۵"};
                int[] dcol = {0xFFFF7043, 0xFF42A5F5, 0xFFFFCA28, 0xFF66BB6A, 0xFFF06292};
                for (int i = 0; i < 5; i++) {
                    p.setColor(dcol[i]);
                    p.setTextSize(h * 0.55f);
                    c.drawText(digs[i], x + w * (0.14f + i * 0.18f), y + h * 0.72f, p);
                }
                break;
            }
            case Interior.F_ALPHABOARD: {
                // ✅ تخته آموزش الفبا (مدرسه)
                p.setColor(0xFF6D4C41);
                c.drawRoundRect(x - 6f, y - 6f, x + w + 6f, y + h + 6f, 8, 8, p);
                p.setColor(0xFF1565C0);
                c.drawRoundRect(x, y, x + w, y + h, 5, 5, p);
                Fonts.applyBold(p);
                p.setTextAlign(Paint.Align.CENTER);
                String[] lets = {"ا", "ب", "پ", "ت"};
                int[] lcol = {0xFFFFFFFF, 0xFFFFEB3B, 0xFF80CBC4, 0xFFFFAB91};
                for (int i = 0; i < 4; i++) {
                    p.setColor(lcol[i]);
                    p.setTextSize(h * 0.6f);
                    c.drawText(lets[i], x + w * (0.15f + i * 0.23f), y + h * 0.74f, p);
                }
                break;
            }
            default:
                p.setColor(color);
                c.drawRoundRect(x, y, x + w, y + h, 6, 6, p);
                break;
        }
    }

    public void drawCinemaSeat(Canvas c, float x, float y) {
        p.setColor(0xFFB71C1C);
        c.drawRoundRect(x - 22f, y - 18f, x + 22f, y + 14f, 8, 8, p);
        p.setColor(0xFFD32F2F);
        c.drawRoundRect(x - 22f, y - 30f, x + 22f, y - 14f, 8, 8, p);
        p.setColor(0xFFE53935);
        c.drawRoundRect(x - 17f, y - 26f, x + 17f, y - 18f, 4, 4, p);
    }

    // ================= شهربازی: چرخ‌وفلک و سرسیر =================

    public void drawFerrisWheel(Canvas c, float x, float y, float minutes) {
        float spin = minutes * 0.55f;
        float R = 118f;

        // پایه A
        p.setColor(0xFF78909C);
        p.setStrokeWidth(12f);
        c.drawLine(x - 60f, y + 70f, x, y, p);
        c.drawLine(x + 60f, y + 70f, x, y, p);
        p.setStrokeWidth(0.1f);

        // حلقه بیرونی
        p.setColor(0xFFE53935);
        p.setStrokeWidth(9f);
        p.setStyle(Paint.Style.STROKE);
        c.drawCircle(x, y, R, p);
        p.setColor(0xFFFFB300);
        p.setStrokeWidth(5f);
        c.drawCircle(x, y, R - 10f, p);

        // پره‌ها + کابین‌های رنگی
        int[] cabinColors = {0xFF1E88E5, 0xFF43A047, 0xFFFDD835, 0xFFE53935,
                             0xFF8E24AA, 0xFFFF7043, 0xFF00ACC1, 0xFFF06292};
        for (int i = 0; i < 8; i++) {
            float a = spin + i * (float) (Math.PI / 4.0);
            float sx2 = x + (float) Math.cos(a) * R;
            float sy2 = y + (float) Math.sin(a) * R;
            p.setColor(0xFF90A4AE);
            p.setStrokeWidth(4f);
            c.drawLine(x, y, sx2, sy2, p);
            // کابین آویزان (همیشه رو به پایین)
            p.setColor(cabinColors[i]);
            c.drawRoundRect(sx2 - 12f, sy2, sx2 + 12f, sy2 + 22f, 6, 6, p);
            p.setColor(0xFFB3E5FC);
            c.drawRoundRect(sx2 - 8f, sy2 + 3f, sx2 + 8f, sy2 + 12f, 3, 3, p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setStrokeWidth(0.1f);

        // محور مرکزی
        p.setColor(0xFFFFD54F);
        c.drawCircle(x, y, 14f, p);
        p.setColor(0xFFE53935);
        c.drawCircle(x, y, 7f, p);

        // تابلو
        Fonts.apply(p);
        p.setColor(0xFF37474F);
        p.setTextSize(20f);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText("🎡 چرخ‌وفلک", x, y + 96f, p);
    }

    public void drawCarousel(Canvas c, float x, float y, float minutes) {
        float spin = minutes * 1.6f;

        // سکو
        p.setColor(0x33000000);
        c.drawOval(x - 62f, y + 22f, x + 62f, y + 40f, p);
        p.setColor(0xFFFF8FAE);
        c.drawOval(x - 58f, y + 14f, x + 58f, y + 34f, p);
        p.setColor(0xFFE91E63);
        c.drawOval(x - 58f, y + 10f, x + 58f, y + 22f, p);

        // ستون مرکزی
        p.setColor(0xFFFFD54F);
        c.drawRoundRect(x - 5f, y - 64f, x + 5f, y + 20f, 4, 4, p);

        // اسب‌های چوبی در حال چرخش
        int[] horseColors = {0xFF42A5F5, 0xFF66BB6A, 0xFFFFCA28, 0xFFAB47BC, 0xFFFF7043, 0xFF4DD0E1};
        for (int i = 0; i < 6; i++) {
            float a = spin + i * (float) (Math.PI / 3.0);
            float hx = x + (float) Math.cos(a) * 44f;
            float hy = y + (float) Math.sin(a) * 18f - 22f
                    - (float) Math.sin(minutes * 3f + i) * 5f;   // بالا پایین پریدن
            float scale = 0.7f + 0.3f * ((float) Math.sin(a) + 1f) * 0.5f;   // دور = کوچکتر
            // میله
            p.setColor(0xFFB0BEC5);
            p.setStrokeWidth(3f);
            c.drawLine(hx, hy - 26f, hx, hy + 8f, p);
            p.setStrokeWidth(0.1f);
            // اسب
            p.setColor(horseColors[i]);
            c.drawRoundRect(hx - 13f * scale, hy - 6f * scale, hx + 13f * scale, hy + 7f * scale, 6, 6, p);
            c.drawCircle(hx + 12f * scale, hy - 7f * scale, 5.5f * scale, p);
            p.setColor(0xFF3E2723);
            c.drawCircle(hx + 14f * scale, hy - 8f * scale, 1.2f, p);
            p.setColor(horseColors[i]);
            c.drawCircle(hx - 13f * scale, hy - 10f * scale, 3f * scale, p);
        }

        // سقف مخروطی راه‌راه
        for (int i = 0; i < 8; i++) {
            float a1 = i * (float) (Math.PI / 4.0);
            float a2 = a1 + (float) (Math.PI / 4.0);
            p.setColor(i % 2 == 0 ? 0xFFE91E63 : 0xFFFFD54F);
            tri(c, new float[]{
                    x, y - 96f,
                    x + (float) Math.cos(a1) * 64f, y - 60f,
                    x + (float) Math.cos(a2) * 64f, y - 60f
            });
        }
        p.setColor(0xFFFFD54F);
        c.drawCircle(x, y - 98f, 7f, p);

        Fonts.apply(p);
        p.setColor(0xFF37474F);
        p.setTextSize(20f);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText("🎠 سرسیر", x, y + 62f, p);
    }

    // ================= دریاچه و روستا =================

    /**
     * ماهی‌های شناور دریاچه — روی تایل آب
     */
    public void drawFish(Canvas c, float x, float y, int type, float t) {
        int body;
        if (type == 0) body = 0xFFB0BEC5;          // نقره‌ای
        else if (type == 1) body = 0xFFFF7043;     // نارنجی رنگارنگ
        else body = 0xFF26C6DA;                    // فیروزه‌ای
        float wig = (float) Math.sin(t * 2.2f) * 8f;
        float flip = (float) Math.cos(t * 0.9f) > 0f ? 1f : -1f;

        p.setColor(0x66FFFFFF);
        c.drawOval(x - 14f + wig * 0.3f, y + 4f, x + 14f + wig * 0.3f, y + 9f, p);
        p.setColor(body);
        c.drawOval(x - 13f, y - 6f + wig * 0.4f, x + 13f, y + 6f + wig * 0.4f, p);
        tri(c, new float[]{
                x - 13f * flip, y + wig * 0.4f,
                x - 20f * flip, y - 7f + wig,
                x - 20f * flip, y + 7f + wig
        });
        p.setColor(0xFF212121);
        c.drawCircle(x + 7f * flip, y - 2f + wig * 0.3f, 1.7f, p);
    }

    /**
     * حیوانات روستا — ۰=گاو ۱=گوسفند
     */
    public void drawFarmAnimal(Canvas c, float x, float y, int type, float t) {
        float bob = (float) Math.sin(t * 1.6f) * 1.5f;
        p.setColor(0x33000000);
        c.drawOval(x - 22f, y + 12f, x + 22f, y + 19f, p);

        if (type == 0) {
            // 🐄 گاو
            p.setColor(0xFFF5F5F5);
            c.drawRoundRect(x - 22f, y - 10f + bob, x + 14f, y + 12f + bob, 10, 10, p);
            p.setColor(0xFF37474F);
            c.drawCircle(x - 8f, y - 2f + bob, 4.5f, p);
            c.drawCircle(x + 2f, y + 4f + bob, 5.5f, p);
            c.drawCircle(x + 8f, y - 4f + bob, 3.5f, p);
            // سر
            p.setColor(0xFFF5F5F5);
            c.drawCircle(x + 19f, y - 6f + bob, 9f, p);
            p.setColor(0xFFFFB300);
            c.drawOval(x + 14f, y - 4f + bob, x + 24f, y + 2f + bob, p);
            p.setColor(0xFF3E2723);
            c.drawCircle(x + 17f, y - 9f + bob, 1.6f, p);
            c.drawCircle(x + 22f, y - 9f + bob, 1.6f, p);
            // شاخ
            p.setColor(0xFFBCAAA4);
            c.drawCircle(x + 14f, y - 14f + bob, 3f, p);
            c.drawCircle(x + 24f, y - 14f + bob, 3f, p);
            // سم‌ها
            p.setColor(0xFF37474F);
            c.drawRoundRect(x - 18f, y + 10f, x - 13f, y + 16f, 2, 2, p);
            c.drawRoundRect(x + 8f, y + 10f, x + 13f, y + 16f, 2, 2, p);
        } else {
            // 🐑 گوسفند
            p.setColor(0xFFEEEEEE);
            c.drawCircle(x - 10f, y - 2f + bob, 12f, p);
            c.drawCircle(x, y - 6f + bob, 13f, p);
            c.drawCircle(x + 10f, y - 1f + bob, 11f, p);
            c.drawCircle(x + 2f, y + 3f + bob, 12f, p);
            // سر تیره
            p.setColor(0xFF6D4C41);
            c.drawCircle(x + 18f, y - 6f + bob, 7.5f, p);
            p.setColor(0xFF3E2723);
            c.drawCircle(x + 20f, y - 8f + bob, 1.4f, p);
            // پاها
            p.setColor(0xFF6D4C41);
            c.drawRoundRect(x - 12f, y + 8f, x - 8f, y + 15f, 2, 2, p);
            c.drawRoundRect(x + 6f, y + 8f, x + 10f, y + 15f, 2, 2, p);
        }
    }

    // ================= حباب گفتگو =================

    public void drawBubbles(Canvas c, ArrayList<Npc> npcs) {
        for (int i = 0; i < npcs.size(); i++) {
            Npc n = npcs.get(i);
            if (n.bubble == null || n.bubbleTimer <= 0f) continue;
            Fonts.apply(p);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(17f);
            float tw = p.measureText(n.bubble) + 26f;
            float bx = n.x, by = n.y - 80f;
            p.setColor(0xF0FFFFFF);
            c.drawRoundRect(bx - tw / 2f, by - 24f, bx + tw / 2f, by + 4f, 10, 10, p);
            p.setColor(0xFF6D4C41);
            tri(c, new float[]{bx - 7f, by + 3f, bx + 7f, by + 3f, bx, by + 12f});
            p.setColor(0xFF3E2723);
            c.drawText(n.bubble, bx, by - 4f, p);
        }
    }

    // ================= برفک تلویزیون (عدم اتصال اینترنت) =================

    public void drawMovieStatic(Canvas c, RectF screen, float time) {
        p.setColor(0xFF0D0D0D);
        c.drawRect(screen, p);
        java.util.Random r = new java.util.Random((long) (time * 900f));
        for (int i = 0; i < 240; i++) {
            int g = 35 + r.nextInt(200);
            p.setColor(0xFF000000 | (g << 16) | (g << 8) | g);
            float bw = 4f + r.nextInt(24);
            float bh = 3f + r.nextInt(10);
            float bx = screen.left + r.nextFloat() * Math.max(1f, screen.width() - bw);
            float by = screen.top + r.nextFloat() * Math.max(1f, screen.height() - bh);
            c.drawRect(bx, by, bx + bw, by + bh, p);
        }
        // خط اسکن متحرک
        p.setColor(0x55FFFFFF);
        float ly = screen.top + (time * 240f) % screen.height();
        c.drawRect(screen.left, ly, screen.right, ly + 7f, p);
    }
}
