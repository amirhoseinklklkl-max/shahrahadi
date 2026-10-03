package com.persiancity.game.game;

/**
 * ساختمان‌های شهر — نوع، مکان، در و رنگ
 */
public class Building {
    public static final int HOME = 0;
    public static final int BANK = 1;
    public static final int HOSPITAL = 2;
    public static final int SCHOOL = 3;
    public static final int LIBRARY = 4;
    public static final int RESTAURANT = 5;
    public static final int CAFE = 6;
    public static final int MARKET = 7;
    public static final int TOYSTORE = 8;
    public static final int CINEMA = 9;
    public static final int ZOO = 10;
    public static final int HELIPORT = 11;
    public static final int POLICE = 12;
    public static final int FIRE = 13;
    public static final int TRAIN_STATION = 14;
    public static final int CARSHOP = 15;
    public static final int DOCK = 16;
    public static final int AMUSEMENT = 17;
    public static final int VILLAGE_HOME = 18;
    public static final int BAKERY = 19;
    public static final int FARM = 20;

    public final int type;
    public float x, y;          // گوشه بالا-چپ
    public float w, h;          // اندازه
    public float doorX, doorY;  // مرکز در (پایین ساختمان)
    public int wallColor;
    public int roofColor;
    public String label = null; // اسم سفارشی (مثلاً «خانه تو»)

    public Building(int type, float x, float y, float w, float h, int wallColor, int roofColor) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.wallColor = wallColor;
        this.roofColor = roofColor;
        this.doorX = x + w / 2f;
        this.doorY = y + h;
    }

    public static String nameOf(int type) {
        switch (type) {
            case HOME: return "خانه";
            case BANK: return "بانک شهر شادی";
            case HOSPITAL: return "بیمارستان مهربانی";
            case SCHOOL: return "مدرسه دانش";
            case LIBRARY: return "کتابخانه نور";
            case RESTAURANT: return "رستوران خوشمزه";
            case CAFE: return "کافه شکلات";
            case MARKET: return "سوپرمارکت فراوان";
            case TOYSTORE: return "فروشگاه اسباب‌بازی";
            case CINEMA: return "سینما ستاره";
            case ZOO: return "باغ‌وحش شادی";
            case HELIPORT: return "هلی‌پورت شهر";
            case POLICE: return "کلانتری ۱۰";
            case FIRE: return "آتش‌نشانی";
            case TRAIN_STATION: return "ایستگاه قطار";
            case CARSHOP: return "فروشگاه ماشین شادی";
            case DOCK: return "اسکله دریاچه";
            case AMUSEMENT: return "شهربازی شادی";
            case VILLAGE_HOME: return "خانه روستایی";
            case BAKERY: return "نانوایی روستا";
            case FARM: return "مزرعه روستا";
            default: return "ساختمان";
        }
    }

    public String name() {
        return label != null ? label : nameOf(type);
    }

    /**
     * متن اطلاعاتی هر ساختمان برای دکمه «نگاه کن»
     */
    public String info() {
        switch (type) {
            case BANK:
                return "🏦 بانک شادی\nاینجا پول‌هایت را نگه می‌دارند. هر روز که کار کنی سکه‌های بیشتری داری!\nکارمند بانک: «پول‌هایت را دور نریز، پس‌انداز کن!»";
            case HOSPITAL:
                return "🏥 بیمارستان مهربانی\nاگر گرسنه یا خسته شوی، دکترهای مهربان تو را درمان می‌کنند.\nدکتر: «میوه بخور و زود بخواب تا هیچ‌وقت مریض نشوی!»";
            case SCHOOL:
                return "🏫 مدرسه دانش\nجای یادگیری چیزهای تازه! ریاضی، خواندن و نقاشی.\nمعلم: «هر روز به مدرسه بیا تا باهوش‌تر شوی!»";
            case LIBRARY:
                return "📚 کتابخانه نور\nهزاران کتاب قصه و علمی. آرام باش و کتاب بخوان!\nکتابدار: «کتاب خواندن مثل پرواز کردن است!»";
            case RESTAURANT:
                return "🍽 رستوران خوشمزه\nغذاهای خوشمزه بخر و بخور یا به‌عنوان گارسون کار کن و انعام بگیر!";
            case CAFE:
                return "🍫 کافه شکلات\nداغ‌ترین شکلات داغ شهر با کیک شکلاتی!\nکافه‌دار: «شکلات = خوشحالی!»";
            case MARKET:
                return "🛒 سوپرمارکت فراوان\nمیوه و خوراکی بخر یا پشت صندوق کار کن.";
            case TOYSTORE:
                return "🧸 فروشگاه اسباب‌بازی\nبهترین اسباب‌بازی‌های شهر! عروسک، ماشین، لگو...";
            case CINEMA:
                return "🎬 سینما ستاره\nبلیط بخر، روی صندلی قرمز بنشین و کارتون تماشا کن!\nامروز: ماشین‌های مسابقه، ماهی رنگارنگ، موشک فضایی";
            case ZOO:
                return "🦁 باغ‌وحش شادی\nشیر، فیل، میمون، گورخر، پنگوئن و زرافه اینجا زندگی می‌کنند!\nجلوی هر حیوان وایسا و دکمه نگاه کن را بزن تا درباره‌اش بخوانی.";
            case HELIPORT:
                return "🚁 هلی‌پورت شهر\nمی‌توانی هلیکوپتر شخصی بخری و از بالا شهر را ببینی!";
            case POLICE:
                return "👮 کلانتری ۱۰\nنگهبانان امنیت شهر. اگر گم شدی اینجا کمک می‌گیرند.";
            case FIRE:
                return "🚒 آتش‌نشانی\nماشین‌های قرمز قهرمان! همیشه آماده کمک‌رسانی.";
            case TRAIN_STATION:
                return "🚂 ایستگاه قطار شهری\nقطار شادی همیشه دور شهر می‌چرخد!\n• با بلیط ۲۰۰ تومانی یک دور کامل سفر کن\n• یا کل قطار را بخر و خودت راننده باش!";
            case CARSHOP:
                return "🚗 فروشگاه ماشین شادی\nبهترین ماشین‌های شهر اینجا فروش می‌رود!\n• سدان شادی ۸ هزار تومان\n• اسپرت جت ۱۵ هزار تومان\n• موتور تندر ۴ هزار تومان\nماشینت را بخر و دور شهر بگرد!";
            case DOCK:
                return "⚓ اسکله دریاچه\nاینجا قایق شادی منتظر توست — سوار شو و دور دریاچه بگرد!\nکنار اسکله هم می‌توانی ماهیگیری کنی و ماهی بفروشی!";
            case AMUSEMENT:
                return "🎡 شهربازی شادی\nچرخ‌وفلک بزرگ و سرسیر کاروسل اینجاست!\nسوار شو و صدای خنده بشنو!";
            case VILLAGE_HOME:
                return "🏡 خانه روستایی\nروستایی‌های مهربان اینجا زندگی می‌کنند.\nهوای روستا خیلی پاکیزه است!";
            case BAKERY:
                return "🍞 نانوایی روستا\nنان تازه از تنور، همراه با چای و کیک محلی!\nبوی نان تا وسط روستا می‌آید!";
            case FARM:
                return "🌾 مزرعه روستا\nگندم، جو و سبزیجات اینجا می‌رویند.\nگاو و گوسفندها همیشه در حال چرا هستند!";
            case HOME:
                return "🏠 خانه تو!\nاینجا می‌توانی استراحت کنی و انرژیت را پر کنی.\nتخت خوابت منتظر توست!";
            default:
                return "یک ساختمان زیبا در شهر شادی.";
        }
    }
}
