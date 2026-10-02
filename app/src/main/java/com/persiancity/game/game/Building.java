package com.persiancity.game.game;

/**
 * ساختمان‌های شهر — هر ساختمان یک نوع دارد و بعضی‌ها قابل ورودند
 */
public class Building {

    // انواع ساختمان
    public static final int HOME = 0;          // خانه بازیکن
    public static final int HOUSE = 1;         // خانه همسایه‌ها (فقط تزئینی)
    public static final int CLOTHES = 2;       // فروشگاه لباس
    public static final int BARBER = 3;        // آرایشگاه
    public static final int RESTAURANT = 4;    // رستوران (محیط داخلی دارد)
    public static final int MARKET = 5;        // سوپرمارکت (محیط داخلی دارد)
    public static final int CARSHOP = 6;       // نمایشگاه ماشین
    public static final int BIKESHOP = 7;      // نمایشگاه موتور
    public static final int GARAGE = 8;        // گاراژ تیونینگ
    public static final int JOBCENTER = 9;     // اداره مشاغل
    public static final int CAFE = 10;         // کافه
    public static final int CINEMA = 11;       // سینما
    public static final int HOSPITAL = 12;     // بیمارستان
    public static final int BANK = 13;         // بانک
    public static final int TOYSTORE = 14;     // اسباب‌بازی‌فروشی
    public static final int TAXISTAND = 15;    // ایستگاه تاکسی
    public static final int SCHOOL = 16;       // مدرسه (تزئینی)
    public static final int POLICE = 17;       // پلیس‌خانه (تزئینی)
    public static final int LIBRARY = 18;      // کتابخانه
    public static final int MOSQUE = 19;       // مسجد (تزئینی)
    public static final int STADIUM = 20;      // ورزشگاه (تزئینی)
    public static final int GAS = 21;          // پمپ بنزین (تزئینی)
    public static final int KIOSK = 22;        // دکه روزنامه

    public final int tileX, tileY, tileW, tileH;
    public final int type;
    public final String name;      // تابلوی فارسی
    public final int wallColor, roofColor, accentColor;

    // محاسبه‌شده در پیکسل
    public final float px, py, pw, ph;
    public float doorX, doorY;     // محل جلوی در (پیاده‌رو جلوی ساختمان)
    public boolean doorUp;         // در رو به بالا است؟

    public Building(int tileX, int tileY, int tileW, int tileH, int type, String name,
                    int wallColor, int roofColor, int accentColor, boolean doorUp) {
        this.tileX = tileX;
        this.tileY = tileY;
        this.tileW = tileW;
        this.tileH = tileH;
        this.type = type;
        this.name = name;
        this.wallColor = wallColor;
        this.roofColor = roofColor;
        this.accentColor = accentColor;
        this.doorUp = doorUp;
        this.px = tileX * G.TILE;
        this.py = tileY * G.TILE;
        this.pw = tileW * G.TILE;
        this.ph = tileH * G.TILE;
        float cx = px + pw / 2f;
        if (doorUp) {
            doorX = cx;
            doorY = py - G.TILE * 0.7f;
        } else {
            doorX = cx;
            doorY = py + ph + G.TILE * 0.7f;
        }
    }

    public boolean isEnterable() {
        return type != HOUSE && type != SCHOOL && type != POLICE && type != MOSQUE
                && type != STADIUM && type != GAS && type != KIOSK;
    }

    /**
     * متن تابلوی راهنمای جلوی در
     */
    public String flavorText() {
        switch (type) {
            case HOUSE:
                return "اینجا خونه‌ی همسایه‌هاست. زنگ نزن تا بیدار نشن!";
            case SCHOOL:
                return "مدرسه «دانش» — زنگ خورده، همه خوشحالن!";
            case POLICE:
                return "پلیس‌خانه «امنیت» — نگهبان شهر بهت لبخند می‌زنه.";
            case MOSQUE:
                return "مسجد «نور» — جای آرامش و مهربانی.";
            case STADIUM:
                return "ورزشگاه «تلاش» — امروز مسابقه‌ای نیست.";
            case GAS:
                return "پمپ بنزین «برق» — ماشین‌ها اینجا سوخت می‌گیرن.";
            case KIOSK:
                return "دکه روزنامه — خبرهای خوب شهر!";
            case LIBRARY:
                return "کتابخانه «کتاب» — ساکت باش! بچه‌ها درس می‌خونن.";
        }
        return "";
    }
}
