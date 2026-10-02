package com.persiancity.game.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.persiancity.game.GameActivity;
import com.persiancity.game.SaveManager;
import com.persiancity.game.SoundManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;

/**
 * نمای اصلی بازی — همه سیستم‌ها اینجا به هم وصل می‌شوند
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    // سیستم‌های بازی (برای دسترسی سایر کلاس‌ها public هستند)
    public World world;
    public Player player;
    public Camera camera;
    public SpriteLib sprites;
    public UIManager ui;
    public JobSystem jobs;
    public MissionSystem missions;
    public ShopSystem shop;
    public MiniMap miniMap;

    public Random rng = new Random();

    // دکمه‌های صفحه
    public RectF actionBtnRect = null;
    public RectF hornBtnRect = null;
    public RectF pauseBtnRect = null;

    // وضعیت اضافی
    public boolean[] ownedOutfits = new boolean[SpriteLib.OUTFITS.length];
    public int toysOwned = 0;
    public int lastAllowanceDay = 0;

    private final Joystick joystick;
    private GameThread thread;
    private final Context appContext;

    // لایه تاریکی شب
    private Bitmap darkness;
    private Canvas darknessCanvas;
    private final Paint darkPaint = new Paint();
    private final Paint lightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // دیالوگ
    private Npc dlgNpc = null;
    private int dlgMode = 0;   // ۰=اصلی ۱=پیشنهاد ماموریت

    // هشدارها
    private boolean warnedHunger = false, warnedEnergy = false;
    private float hintTimer = 0f;

    public GameView(Context context) {
        super(context);
        appContext = context;
        getHolder().addCallback(this);
        setFocusable(true);

        world = new World();
        player = new Player(world.buildings.get(0).doorX, world.buildings.get(0).doorY);
        camera = new Camera();
        sprites = new SpriteLib();
        ui = new UIManager(this);
        miniMap = new MiniMap();
        miniMap.buildFromWorld(world.tileType);
        jobs = new JobSystem(world, rng, this);
        missions = new MissionSystem(world, rng);
        shop = new ShopSystem(this);
        ownedOutfits[0] = true;

        joystick = new Joystick(90f);

        darkPaint.setStyle(Paint.Style.FILL);
        lightPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        loadOrStart();
    }

    private void loadOrStart() {
        JSONObject save = SaveManager.load(appContext);
        if (save != null && applySave(save)) {
            ui.toast("خوش برگشتی! بازی قبلیت برگردونده شد.");
        } else {
            ui.toast("به شهر شادی خوش اومدی!");
            ui.toast("با دکمه نارنجی به مغازه‌ها برو و با NPCها حرف بزن!");
        }
        camera.snapTo(player.x, player.y);
    }

    // ================================================= چرخه حیات

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        resumeGame();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        ui.layout(width, height);
        darkness = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        darknessCanvas = new Canvas(darkness);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pauseGame();
    }

    public void resumeGame() {
        if (getWidth() > 0 && getHeight() > 0) {
            ui.layout(getWidth(), getHeight());
            if (darkness == null || darkness.getWidth() != getWidth()
                    || darkness.getHeight() != getHeight()) {
                darkness = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
                darknessCanvas = new Canvas(darkness);
            }
        }
        if (thread != null && thread.isRunning()) return;
        thread = new GameThread(getHolder(), this);
        thread.setRunning(true);
        thread.start();
    }

    public void pauseGame() {
        if (thread != null) {
            thread.setRunning(false);
            try {
                thread.join(500);
            } catch (InterruptedException ignored) {
            }
            thread = null;
        }
        SoundManager.stopEngine();
    }

    public void openPauseMenu() {
        switch (ui.uiState) {
            case UIManager.UI_PLAY:
                ui.uiState = UIManager.UI_PAUSE;
                break;
            case UIManager.UI_PAUSE:
                ui.uiState = UIManager.UI_PLAY;
                break;
            case UIManager.UI_MENU:
                ui.closeMenu();
                break;
            case UIManager.UI_DIALOG:
                ui.closeDialogue();
                break;
        }
    }

    /**
     * انتخاب یکی از گزینه‌های منوی مکث
     */
    public void onPauseOption(int i) {
        boolean jobActive = jobs.activeJob != JobSystem.JOB_NONE;
        // ایندکس‌ها: ۰=ادامه ۱=ذخیره [۲=پایان شیفت] بعد صدا و خروج
        int soundIndex = jobActive ? 3 : 2;
        int exitIndex = jobActive ? 4 : 3;
        if (i == 0) {
            ui.uiState = UIManager.UI_PLAY;
        } else if (i == 1) {
            saveGame();
            fx("success");
            ui.uiState = UIManager.UI_PLAY;
            toast("بازی ذخیره شد!");
        } else if (i == 2 && jobActive) {
            jobs.stopJob();
            ui.uiState = UIManager.UI_PLAY;
            toast("شیفت تمام شد!");
        } else if (i == soundIndex) {
            SoundManager.setMuted(appContext, !SoundManager.isMuted());
            if (SoundManager.isMuted()) SoundManager.stopEngine();
        } else if (i == exitIndex) {
            saveGame();
            SoundManager.stopEngine();
            if (appContext instanceof GameActivity) {
                ((GameActivity) appContext).finish();
            }
        }
    }

    // ================================================= به‌روزرسانی

    public void update(float dt) {
        ui.update(dt);

        if (ui.uiState != UIManager.UI_PLAY) {
            joystick.reset();   // جلوگیری از حرکت ناخواسته بعد از بستن منو
            return;   // وقتی منو باز است بازی می‌ایستد
        }

        // جوی‌استیک
        float jx = joystick.outX;
        float jy = joystick.outY;

        if (player.driving != null) {
            player.driving.drive(dt, jx, jy, world);
            player.x = player.driving.x;
            player.y = player.driving.y;
            SoundManager.setEngineIntensity(player.driving.speedNorm());
        } else {
            player.update(dt, jx, jy, world);
        }

        world.update(dt, player);
        jobs.update(dt, player);
        camera.follow(player.x, player.y, dt, player.driving != null);
        sprites.nightMode = world.dayNight.isNight();

        // خروج خودکار از محیط داخلی (درِ خروج)
        if (world.interior != null) {
            if (G.dist(player.x, player.y, world.interior.doorX, world.interior.doorY) < 42f) {
                if (jobs.activeJob == JobSystem.JOB_WAITER || jobs.activeJob == JobSystem.JOB_SHOPKEEPER) {
                    jobs.stopJob();
                    toast("شیفت نیمه‌کاره رها شد!");
                }
                world.exitInterior(player);
                camera.snapTo(player.x, player.y);
                fx("door");
            }
        }

        // هشدارهای گرسنگی و خستگی
        if (player.hunger < 25f && !warnedHunger) {
            warnedHunger = true;
            toast("گشنه شدی! یه چیزی بخور (رستوران، کافه یا سوپرمارکت)");
        }
        if (player.hunger > 50f) warnedHunger = false;
        if (player.energy < 20f && !warnedEnergy) {
            warnedEnergy = true;
            toast("خسته‌ای! براق خونه بخواب یا تو بیمارستان استراحت کن");
        }
        if (player.energy > 50f) warnedEnergy = false;

        // راهنمای گاه‌به‌گاه
        hintTimer -= dt;
        if (hintTimer <= 0f && world.interior == null) {
            hintTimer = 45f;
            if (player.money < 2000 && jobs.activeJob == JobSystem.JOB_NONE && missions.getActive().isEmpty()) {
                toast("برای پول: اداره مشاغل، ایستگاه تاکسی یا حرف زدن با آدم‌های شهر!");
            }
        }
    }

    // ================================================= ورودی لمسی

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (ui.uiState == UIManager.UI_PLAY) {
                    if (pauseBtnRect != null && pauseBtnRect.contains(x, y)) {
                        fx("click");
                        openPauseMenu();
                        return true;
                    }
                    if (actionBtnRect != null && actionBtnRect.contains(x, y)) {
                        doContextAction();
                        return true;
                    }
                    if (hornBtnRect != null && hornBtnRect.contains(x, y)) {
                        SoundManager.play("horn");
                        return true;
                    }
                    joystick.onTouchDown(x, y, getWidth(), getHeight());
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (ui.uiState == UIManager.UI_PLAY) {
                    joystick.onTouchMove(x, y);
                } else if (ui.uiState == UIManager.UI_MENU) {
                    // اسکرول منو
                    float dy = y - (lastTouchY >= 0 ? lastTouchY : y);
                    ui.onTouchScroll(x, y, dy);
                }
                lastTouchY = y;
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (ui.uiState == UIManager.UI_PLAY) {
                    joystick.onTouchUp();
                } else {
                    ui.onTouchUp(x, y);
                }
                lastTouchY = -1;
                return true;
        }
        return true;
    }

    private float lastTouchY = -1f;

    // ================================================= اقدام زمینه‌ای

    /**
     * متن دکمه اقدام بر اساس موقعیت
     */
    public String contextActionLabel() {
        if (player.driving != null) {
            return Math.abs(player.driving.speed) > 200f ? "ترمز!" : "پیاده شو";
        }
        if (world.interior != null) {
            String jp = jobs.jobPrompt(player);
            if (jp != null) return jp;
            Npc manager = findManagerNear();
            if (manager != null) return "با مدیر حرف بزن";
            return "کاری نکنم؟";
        }
        // نزدیک درِ ساختمان
        Building b = world.buildingNearDoor(player.x, player.y);
        if (b != null) {
            if (b.isEnterable()) {
                // اگر هدف ماموریت همین‌جاست
                if (missions.missionNearTarget(player) != null) return "تحویل بده";
                return "برو تو";
            }
            return "نگاه کن";
        }
        // نزدیک NPC
        Npc n = nearestNpc();
        if (n != null) return "حرف بزن";
        // نزدیک خودروی خودم
        Vehicle v = nearestOwnedVehicle();
        if (v != null) return "سوار شو";
        String jp = jobs.jobPrompt(player);
        if (jp != null) return jp;
        return "کاری نکنم؟";
    }

    private void doContextAction() {
        if (ui.uiState != UIManager.UI_PLAY) return;

        // رانندگی: پیاده شدن
        if (player.driving != null) {
            exitVehicle();
            return;
        }

        // داخل محیط
        if (world.interior != null) {
            String jp = jobs.jobPrompt(player);
            if (jp != null && jobs.doJobAction(player)) return;
            Npc manager = findManagerNear();
            if (manager != null) {
                shop.openManagerMenu(manager);
                return;
            }
            return;
        }

        // تحویل ماموریت در جلوی ساختمان
        Building b = world.buildingNearDoor(player.x, player.y);
        if (b != null) {
            if (b.isEnterable()) {
                String msg = missions.tryComplete(player, null);
                if (msg != null) {
                    fx("mission");
                    toast(msg);
                    return;
                }
                fx("door");
                shop.openBuildingMenu(b);
                if (world.interior != null) {
                    camera.snapTo(player.x, player.y);
                }
                return;
            } else {
                toast(b.flavorText());
                return;
            }
        }

        // حرف زدن با NPC
        Npc n = nearestNpc();
        if (n != null) {
            talkTo(n);
            return;
        }

        // سوار شدن به خودرو
        Vehicle v = nearestOwnedVehicle();
        if (v != null) {
            enterVehicle(v);
            return;
        }
    }

    // ================================================= گفتگو

    private void talkTo(Npc n) {
        if (n.role == Npc.ROLE_RESTAURANT_MANAGER || n.role == Npc.ROLE_MARKET_MANAGER) {
            shop.openManagerMenu(n);
            return;
        }
        if (n.role == Npc.ROLE_CITIZEN) {
            // اول چک تحویل ماموریت
            String done = missions.tryComplete(player, n);
            if (done != null) {
                fx("mission");
                ui.openDialogue(n.name, done, new ArrayList<String>() {{
                    add("خواهش می‌کنم!");
                    add("خداحافظ");
                }});
                dlgMode = 2;   // حالت تشکر
                dlgNpc = n;
                return;
            }

            dlgNpc = n;
            dlgMode = 0;
            String text = Dialogues.randomGreeting(rng) + " من " + n.name + " هستم.";
            ArrayList<String> opts = new ArrayList<>();
            opts.add("سلام! چه خبرهای شهر؟");
            opts.add("ماموریت داری؟");
            opts.add("خداحافظ");
            ui.openDialogue(n.name, text, opts);
            return;
        }
        // مسافر تاکسی یا مشتری‌ها
        if (n.bubble == null) n.say(Dialogues.randomAmbient(rng));
    }

    public void onDialogueOption(int i) {
        if (dlgMode == 2) {
            ui.closeDialogue();
            return;
        }
        if (dlgMode == 1 && dlgNpc != null) {
            // پاسخ به پیشنهاد ماموریت
            if (i == 0) {
                MissionSystem.Mission m = missions.createMission(dlgNpc);
                fx("click");
                ui.openDialogue(dlgNpc.name, "ممنون! " + m.title + " جایزه‌ش " + UIManager.faMoney(m.reward) + " تومانه!",
                        new ArrayList<String>() {{
                            add("قبوله، می‌رسم!");
                            add("خداحافظ");
                        }});
                dlgMode = 2;
            } else {
                ui.openDialogue(dlgNpc.name, Dialogues.randomFarewell(rng),
                        new ArrayList<String>() {{ add("خداحافظ"); }});
                dlgMode = 2;
            }
            return;
        }
        if (dlgNpc == null) {
            ui.closeDialogue();
            return;
        }

        switch (i) {
            case 0: // خبرها
                ui.updateDialogueText(Dialogues.chatLine(rng));
                fx("click");
                break;
            case 1: // ماموریت
                if (missions.hasFreeSlots() && missions.canOffer(dlgNpc)) {
                    dlgMode = 1;
                    ArrayList<String> opts = new ArrayList<>();
                    opts.add("قبوله، انجامش می‌دم!");
                    opts.add("نه، الان وقت ندارم");
                    ui.openDialogue(dlgNpc.name, Dialogues.pick(Dialogues.MISSION_OFFERS, rng), opts);
                } else {
                    ui.updateDialogueText("فعلاً کاری ندارم ولی اگه گشتی بگرد شهر، حتماً به کسی کمک کن!");
                }
                break;
            default:
                ui.openDialogue(dlgNpc.name, Dialogues.randomFarewell(rng),
                        new ArrayList<String>() {{ add("خداحافظ"); }});
                dlgMode = 2;
                break;
        }
    }

    // ================================================= خودرو

    public void enterVehicle(Vehicle v) {
        player.driving = v;
        v.mode = Vehicle.MODE_PLAYER;
        v.speed = 0f;
        SoundManager.startEngine();
        fx("door");
        toast("سوار " + v.displayName() + " شدی!");
    }

    public void exitVehicle() {
        Vehicle v = player.driving;
        if (v == null) return;
        v.speed = 0f;
        v.mode = Vehicle.MODE_PARKED;
        player.driving = null;
        SoundManager.stopEngine();
        // بازیکن کنار خودرو پیاده می‌شود
        float px = v.x - (float) Math.cos(v.angle) * (v.h / 2f + 40f);
        float py = v.y - (float) Math.sin(v.angle) * (v.h / 2f + 40f);
        if (world.collides(px, py, 16f)) {
            px = v.x;
            py = v.y + v.w + 30f;
        }
        player.x = px;
        player.y = py;
        fx("door");
    }

    public Vehicle nearestOwnedVehicle() {
        Vehicle best = null;
        float bestD = 95f;
        for (Vehicle v : world.vehicles) {
            if (v.mode == Vehicle.MODE_TRAFFIC) continue;
            float d = G.dist(player.x, player.y, v.x, v.y);
            if (d < bestD) {
                bestD = d;
                best = v;
            }
        }
        return best;
    }

    public Vehicle activeOwnedVehicle() {
        // نزدیک‌ترین خودروی خودِ بازیکن (برای گاراژ)
        Vehicle best = null;
        float bestD = 500f;
        Building garage = world.buildingByType(Building.GARAGE);
        float refX = garage != null ? garage.doorX : player.x;
        float refY = garage != null ? garage.doorY : player.y;
        for (Vehicle v : world.vehicles) {
            if (v.mode == Vehicle.MODE_TRAFFIC) continue;
            float d = G.dist(refX, refY, v.x, v.y);
            if (d < bestD) {
                bestD = d;
                best = v;
            }
        }
        return best;
    }

    public void teleportVehiclesHome() {
        Building home = world.buildingByType(Building.HOME);
        if (home == null) return;
        int i = 0;
        for (Vehicle v : world.vehicles) {
            if (v.mode == Vehicle.MODE_TRAFFIC) continue;
            float[] spot = world.nearestParking(home.doorX + (i - 1) * G.TILE, home.doorY);
            if (spot != null) {
                v.x = spot[0];
                v.y = spot[1];
                v.angle = (float) Math.toRadians(spot[2]);
                v.speed = 0f;
            }
            i++;
        }
    }

    // ================================================= کمکی‌ها

    private Npc nearestNpc() {
        ArrayList<Npc> list = world.interior != null ? world.interiorNpcs : world.cityNpcs;
        Npc best = null;
        float bestD = 85f;
        for (Npc n : list) {
            if (n.role == Npc.ROLE_TAXI_PASSENGER) continue;
            float d = G.dist(player.x, player.y, n.x, n.y);
            if (d < bestD) {
                bestD = d;
                best = n;
            }
        }
        return best;
    }

    private Npc findManagerNear() {
        for (Npc n : world.interiorNpcs) {
            if (n.role == Npc.ROLE_RESTAURANT_MANAGER || n.role == Npc.ROLE_MARKET_MANAGER) {
                if (G.dist(player.x, player.y, n.x, n.y) < 100f) return n;
            }
        }
        return null;
    }

    public void toast(String text) {
        ui.toast(text);
    }

    public void fx(String name) {
        SoundManager.play(name);
    }

    public void sleepUntilMorning() {
        if (world.dayNight.minutes >= 7f * 60f) {
            world.dayNight.dayCount++;
        }
        world.dayNight.setTime(7f * 60f);
        player.sleep();
        saveGame();
        fx("success");
        toast("صبح بخیر! روز " + G.fa(world.dayNight.dayCount) + " شروع شد.");
    }

    // ================================================= ترسیم

    public void render(Canvas c) {
        int vw = getWidth(), vh = getHeight();
        float s = camera.scale;

        // آسمان
        c.drawColor(world.dayNight.skyColor());

        // دنیا
        c.save();
        c.translate(vw / 2f, vh / 2f);
        c.scale(s, s);
        c.translate(-camera.x, -camera.y);

        world.draw(c, sprites, camera, vw, vh);

        // بازیکن
        if (player.driving == null) {
            int[] oc = SpriteLib.OUTFITS[player.outfit];
            sprites.drawPerson(c, player.x, player.y, player.facing, player.animTime,
                    oc[0], oc[1], G.COL_SKIN, 0xFF3E2723, player.hair,
                    player.carrying, true);
        }

        c.restore();

        // تاریکی شب (فقط در شهر)
        if (world.interior == null && world.dayNight.darkness() > 0.03f && darkness != null) {
            drawDarkness(c, vw, vh, s);
        }

        // رابط کاربری
        switch (ui.uiState) {
            case UIManager.UI_PLAY:
                ui.drawHud(c, player, world, jobs, missions, joystick.outX, joystick.outY,
                        joystick, contextActionLabel());
                drawServingBar(c);
                break;
            case UIManager.UI_MENU:
                ui.drawHud(c, player, world, jobs, missions, joystick.outX, joystick.outY,
                        joystick, contextActionLabel());
                ui.drawMenu(c);
                break;
            case UIManager.UI_DIALOG:
                ui.drawHud(c, player, world, jobs, missions, joystick.outX, joystick.outY,
                        joystick, "...");
                ui.drawDialogue(c);
                break;
            case UIManager.UI_PAUSE:
                ui.drawPause(c, jobs.activeJob != JobSystem.JOB_NONE, SoundManager.isMuted());
                break;
        }
    }

    private void drawDarkness(Canvas c, int vw, int vh, float s) {
        int alpha = (int) (world.dayNight.darkness() * 235f);
        darknessCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        darknessCanvas.drawColor(Color.argb(alpha, 6, 12, 40));

        // نور چراغ‌های خیابان (فقط دوربین‌های نزدیک)
        float halfW = vw / (2f * s), halfH = vh / (2f * s);
        for (float[] l : world.streetlights) {
            if (Math.abs(l[0] - camera.x) > halfW + 100f || Math.abs(l[1] - camera.y) > halfH + 100f)
                continue;
            float sx = (l[0] - camera.x) * s + vw / 2f;
            float sy = (l[1] - camera.y) * s + vh / 2f;
            lightPaint.setColor(0xFFFFFFFF);
            darknessCanvas.drawCircle(sx, sy - 44f * s, 70f * s, lightPaint);
        }

        // نور دور بازیکن
        float px = (player.x - camera.x) * s + vw / 2f;
        float py = (player.y - camera.y) * s + vh / 2f;
        lightPaint.setColor(0xFFFFFFFF);
        darknessCanvas.drawCircle(px, py, 150f * s, lightPaint);

        // نور جلوی خودروی بازیکن
        if (player.driving != null) {
            Vehicle v = player.driving;
            float vx = (v.x - camera.x) * s + vw / 2f;
            float vy = (v.y - camera.y) * s + vh / 2f;
            float dirX = (float) Math.cos(v.angle), dirY = (float) Math.sin(v.angle);
            darknessCanvas.drawCircle(vx + dirX * 120f * s, vy + dirY * 120f * s, 95f * s, lightPaint);
            darknessCanvas.drawCircle(vx + dirX * 220f * s, vy + dirY * 220f * s, 70f * s, lightPaint);
        }

        c.drawBitmap(darkness, 0, 0, null);
    }

    private void drawServingBar(Canvas c) {
        if (!jobs.isServing()) return;
        int vw = getWidth(), vh = getHeight();
        Paint barP = new Paint(Paint.ANTI_ALIAS_FLAG);
        float w = 300f, h = 26f;
        float x = vw / 2f - w / 2f, y = vh * 0.72f;
        barP.setColor(0xD9000000);
        c.drawRoundRect(x - 4, y - 4, x + w + 4, y + h + 4, 14, 14, barP);
        barP.setColor(0xFFE0E0E0);
        c.drawRoundRect(x, y, x + w, y + h, 10, 10, barP);
        barP.setColor(0xFF66BB6A);
        c.drawRoundRect(x, y, x + w * jobs.servingProgress(), y + h, 10, 10, barP);
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setColor(0xFFFFFFFF);
        tp.setTextSize(30f);
        c.drawText("در حال خدمت به مشتری...", vw / 2f, y - 14f, tp);
    }

    // ================================================= ذخیره و بارگذاری

    public void saveGame() {
        try {
            JSONObject o = new JSONObject();
            o.put("money", player.money);
            o.put("hunger", player.hunger);
            o.put("energy", player.energy);
            o.put("outfit", player.outfit);
            o.put("hair", player.hair);
            o.put("toys", toysOwned);
            o.put("allowDay", lastAllowanceDay);
            o.put("x", player.x);
            o.put("y", player.y);
            o.put("time", world.dayNight.minutes);
            o.put("day", world.dayNight.dayCount);

            JSONArray outfits = new JSONArray();
            for (boolean b : ownedOutfits) outfits.put(b);
            o.put("outfits", outfits);

            JSONArray vs = new JSONArray();
            for (Vehicle v : world.vehicles) {
                if (v.mode == Vehicle.MODE_TRAFFIC) continue;
                JSONObject vo = new JSONObject();
                vo.put("m", v.model);
                vo.put("c", v.paint);
                vo.put("e", v.engineLevel);
                vo.put("sp", v.spoiler);
                vo.put("n", v.neonColor);
                vo.put("x", v.x);
                vo.put("y", v.y);
                vs.put(vo);
            }
            o.put("vehicles", vs);
            o.put("missions", missions.toJson());

            SaveManager.save(appContext, o);
        } catch (Exception ignored) {
        }
    }

    private boolean applySave(JSONObject o) {
        try {
            player.money = o.optInt("money", G.START_MONEY);
            player.hunger = (float) o.optDouble("hunger", 100.0);
            player.energy = (float) o.optDouble("energy", 100.0);
            player.outfit = o.optInt("outfit", 0);
            player.hair = o.optInt("hair", 0);
            player.x = (float) o.optDouble("x", player.x);
            player.y = (float) o.optDouble("y", player.y);
            world.dayNight.minutes = (float) o.optDouble("time", 8f * 60f);
            world.dayNight.dayCount = o.optInt("day", 1);
            toysOwned = o.optInt("toys", 0);
            lastAllowanceDay = o.optInt("allowDay", 0);

            JSONArray outfits = o.optJSONArray("outfits");
            if (outfits != null) {
                for (int i = 0; i < outfits.length() && i < ownedOutfits.length; i++) {
                    ownedOutfits[i] = outfits.optBoolean(i, i == 0);
                }
            }

            // حذف خودروهای قبلیِ بازیکن (ترافیک می‌ماند)
            ArrayList<Vehicle> keep = new ArrayList<>();
            for (Vehicle v : world.vehicles) {
                if (v.mode == Vehicle.MODE_TRAFFIC) keep.add(v);
            }
            world.vehicles.clear();
            world.vehicles.addAll(keep);

            JSONArray vs = o.optJSONArray("vehicles");
            if (vs != null) {
                for (int i = 0; i < vs.length(); i++) {
                    JSONObject vo = vs.getJSONObject(i);
                    Vehicle v = new Vehicle(vo.optInt("m", 0),
                            (float) vo.optDouble("x", player.x),
                            (float) vo.optDouble("y", player.y));
                    v.paint = vo.optInt("c", Vehicle.MODELS[v.model].baseColor);
                    v.engineLevel = vo.optInt("e", 0);
                    v.spoiler = vo.optBoolean("sp", false);
                    v.neonColor = vo.optInt("n", 0);
                    world.vehicles.add(v);
                }
            }

            missions.fromJson(o.optJSONArray("missions"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
