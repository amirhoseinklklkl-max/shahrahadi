package com.persiancity.game.game;

import java.util.Random;

/**
 * دیالوگ‌های فارسی بازی — همه حرف‌های NPCها
 */
public final class Dialogues {

    private Dialogues() {}

    public static final String[] GREETINGS = {
            "سلام سلام! خوشحالم که می‌بینمت!",
            "سلام رفیق! روزت چطوره؟",
            "هی! تو همون بچه‌ی معروف شهر هستی، نه؟",
            "سلام عزیزم! خدا رو شکر که سلامتی.",
            "سلام! هوا امروز عالیه، نه؟",
    };

    public static final String[] NEWS = {
            "شنیدم فروشگاه لباس یه مدل جدید آورده!",
            "گاراژ توربو یه ماشین رو آنقدر اسپرت کرده که شب می‌درخشه!",
            "رستوران زنجبیل غذاش عالی‌ه؛ حتماً امتحان کن.",
            "پارک وسط شهر بهترین جا برای بازی‌کردنه!",
            "نمایشگاه تندر یه ماشین اسپرت نارنجی آورده؛ چشم‌ها رو می‌زنه!",
            "تو سینما ستاره یه فیلم بامزه اکران شده.",
            "اگه گشنته، سوپرمارکت فراوان نزدیکه!",
            "آرایشگاه آفتاب موهاش رو عالی می‌زنه.",
            "برای کار و درآمد، اداره مشاغل کارینا بهترین جاست!",
            "شب‌ها شهر چراغ‌های قشنگی داره، یه پیاده‌روی شب برو!",
    };

    public static final String[] AMBIENT_BUBBLES = {
            "چه روز خوبی!",
            "آخیش، هوای تازه!",
            "وای، دلم می‌خواد بستنی بخورم!",
            "دارم می‌رم پارک.",
            "شهر ما بهترین شهر دنیاست!",
            "امروز خیلی سرحالم!",
            "یادم باشه به مامانم هدیه بخرم.",
            "کتابخونه چه حالی می‌ده...",
    };

    public static final String[] MISSION_OFFERS = {
            "می‌تونی یه کاری برام بکنی؟",
            "راستش یه خواهشی دارم ازت...",
            "ببین، یه ماموریت کوچولو دارم!",
            "تو معتبرترین آدم این شهر هستی؛ کمکم می‌کنی؟",
    };

    public static final String[] MISSION_DONE_LINES = {
            "ممنونم! بهترینی!",
            "وای، عجله کردی! دمت گرم!",
            "آفرین! همیشه روش من باش.",
            "خدا رو شکر! خیلی آقایی.",
    };

    public static final String[] FAREWELL = {
            "خداحافظ رفیق!",
            "بدرود! مواظب خودت باش.",
            "فعلاً! بزن بریم!",
            "یاعلی!",
    };

    public static final String[] TAXI_PICKUP_LINES = {
            "سلام! ممنون که اومدی. برو زود، عجله دارم!",
            "سلام راننده جان! می‌خوام برم شهر رو بچرخم.",
            "سلام! چه تاکسی قشنگی داری!",
    };

    public static final String[] TAXI_DROP_LINES = {
            "چه راننده خوبی هستی! بفرما، اجرتت.",
            "خیلی سریع رسوندی! اینم کرایه.",
            "ممنون! سفر خوبی بود.",
    };

    public static final String[] MANAGER_RESTAURANT = {
            "به رستوران زنجبیل خوش اومدی!",
            "اگه می‌خوای گارسون شیفت بذاری، بگو تا مشتری‌ها رو بیارم!",
            "مشتری‌ها منتظر غذا هستن، سریع باش!",
    };

    public static final String[] MANAGER_MARKET = {
            "به سوپرمارکت فراوان خوش اومدی!",
            "برای کار کردن پشت صندوق، فقط بگو «شیفت»!",
            "مشتری صبور نیستا، سریع خدمت بده!",
    };

    public static String pick(String[] arr, Random rnd) {
        return arr[rnd.nextInt(arr.length)];
    }

    public static String randomGreeting(Random rnd) {
        return pick(GREETINGS, rnd);
    }

    public static String randomNews(Random rnd) {
        return pick(NEWS, rnd);
    }

    public static String randomAmbient(Random rnd) {
        return pick(AMBIENT_BUBBLES, rnd);
    }

    public static String randomFarewell(Random rnd) {
        return pick(FAREWELL, rnd);
    }

    /**
     * متن کامل گپ با NPC
     */
    public static String chatLine(Random rnd) {
        return randomGreeting(rnd) + " " + randomNews(rnd);
    }
}
