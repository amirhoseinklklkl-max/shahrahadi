package com.persiancity.game.game;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;

/**
 * ✅ فونت فارسی «وزیر» (Vazirmatn) برای همهٔ متن‌های بازی
 * فایل در assets/fonts/ قرار دارد؛ اگر به هر دلیلی لود نشود،
 * بازی با فونت پیش‌فرض سیستم ادامه می‌یابد (هیچ‌وقت کرش نمی‌کند).
 */
public class Fonts {
    private static Typeface regular = null;
    private static Typeface bold = null;
    private static boolean tried = false;

    public static void init(Context ctx) {
        if (tried) return;
        tried = true;
        try {
            regular = Typeface.createFromAsset(ctx.getAssets(), "fonts/Vazirmatn-Regular.ttf");
        } catch (Throwable t) {
            regular = null;
        }
        try {
            bold = Typeface.createFromAsset(ctx.getAssets(), "fonts/Vazirmatn-Bold.ttf");
        } catch (Throwable t) {
            bold = regular;   // اگر بولد نبود، از همان معمولی استفاده کن
        }
    }

    /** متن معمولی با فونت وزیر */
    public static void apply(Paint p) {
        if (regular != null) p.setTypeface(regular);
    }

    /** متن بولد با فونت وزیر (اسم ساختمان‌ها و تیترها) */
    public static void applyBold(Paint p) {
        if (bold != null) p.setTypeface(bold);
    }
}
