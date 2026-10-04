package com.persiancity.game.game;

/**
 * ✅ بخش آموزش مدرسه — آموزش اعداد و حروف الفبای فارسی
 *
 * کارت‌ها روی پنل بزرگ نشان داده می‌شوند؛ با دکمهٔ «بعدی/قبلی» ورق می‌خورند.
 *
 * 🔊 صداها: وقتی کاربر فایل‌های صدا را فرستاد، کافی است آن‌ها را با این اسم‌ها
 * در مسیر assets/school/ بگذاریم (خودکار پخش می‌شوند — بدون تغییر کد):
 *   • اعداد:    assets/school/numbers/1.mp3  تا  11.mp3   (صفر تا ده — به ترتیب کارت)
 *   • حروف:    assets/school/letters/1.mp3  تا  32.mp3   (ا تا ی — به ترتیب کارت)
 * اگر فایلی نباشد، بازی بی‌صدا رد می‌شود و هیچ خطایی نمی‌دهد.
 */
public class LearnSystem {

    public int mode = 0;     // ۰ = اعداد | ۱ = الفبا
    public int index = 0;

    // اعداد ۰ تا ۱۰
    public static final String[] NUM_GLYPHS = {"۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹", "۱۰"};
    public static final String[] NUM_WORDS = {
        "صفر", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه", "ده"
    };
    public static final String[] NUM_EXAMPLES = {
        "هیچ سیبی ندارم!", "یک خورشید", "دو چشم", "سه رنگ چراغ", "چهار فصل سال",
        "پنج انگشت", "شش ضلعی", "هفت روز هفته", "هشت ستاره", "نه ماه مدرسه", "ده انگشت دست"
    };

    // ۳۲ حرف الفبای فارسی
    public static final String[] LETTER_GLYPHS = {
        "ا", "ب", "پ", "ت", "ث", "ج", "چ", "ح", "خ", "د", "ذ", "ر", "ز", "ژ",
        "س", "ش", "ص", "ض", "ط", "ظ", "ع", "غ", "ف", "ق", "ک", "گ", "ل", "م",
        "ن", "و", "ه", "ی"
    };
    public static final String[] LETTER_WORDS = {
        "آب", "باران", "پرنده", "توپ", "ثانیه", "جام", "چتر", "حباب", "خرس", "در",
        "ذرت", "رنگین‌کمان", "زرافه", "ژاله", "سیب", "شیر", "صدف", "ضرب", "طوطی", "ظرف",
        "عروسک", "غار", "فیل", "قایق", "کتاب", "گل", "لیوان", "ماه", "نان", "ورزش",
        "هوا", "یخ"
    };

    public void open(int m) {
        mode = m;
        index = 0;
    }

    public void next() {
        index = (index + 1) % count();
    }

    public void prev() {
        index = (index - 1 + count()) % count();
    }

    public int count() {
        return mode == 0 ? NUM_GLYPHS.length : LETTER_GLYPHS.length;
    }

    public String glyph() {
        return mode == 0 ? NUM_GLYPHS[index] : LETTER_GLYPHS[index];
    }

    public String word() {
        return mode == 0 ? NUM_WORDS[index] : LETTER_WORDS[index];
    }

    public String example() {
        return mode == 0 ? NUM_EXAMPLES[index] : "مثل: " + LETTER_WORDS[index];
    }

    /**
     * مسیر فایل صدای این کارت (اگر کاربر فرستاده باشد در assets هست)
     */
    public String soundPath() {
        return mode == 0
                ? "school/numbers/" + (index + 1) + ".mp3"
                : "school/letters/" + (index + 1) + ".mp3";
    }
}
