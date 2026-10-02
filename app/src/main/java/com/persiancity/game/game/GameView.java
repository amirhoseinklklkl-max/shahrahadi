package com.persiancity.game.game;

import com.persiancity.game.SaveManager;
import com.persiancity.game.SoundManager;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.json.JSONObject;

/**
 * نمای اصلی بازی — حلقه به‌روزرسانی، رسم، لمس، ذخیره خودکار
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    public final World world = new World();
    public final Player player = new Player();
    public final Camera camera = new Camera();
    public final Joystick joystick = new Joystick();
    public final UIManager ui = new UIManager(this);
    public final JobSystem jobs;
    public final ShopSystem shop;
    public final MissionSystem missions;
    private final SpriteLib sprites = new SpriteLib();
    private GameThread thread;
    private final Context appContext;

    public float viewW = 1280f, viewH = 720f;

    // مالکیت وسایل
    public boolean heliOwned = false;
    public boolean trainOwned = false;

    // فیلم سینما
    public boolean moviePlaying = false;
    private float movieTimer = 0f;
    private int movieIndex = 0;
    private float movieTime = 0f;

    // ذخیره خودکار
    private float autoSaveTimer = 0f;

    // قطار مسافری
    private Vehicle trainRideVehicle = null;
    private float trainRideStart = 0f;

    private final Paint bgPaint = new Paint();
    private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF movieScreen = new RectF();

    public GameView(Context context) {
        super(context);
        this.appContext = context;
        getHolder().addCallback(this);
        setFocusable(true);

        try {
            SoundManager.init(context);

            jobs = new JobSystem(this, world, player, ui);
            shop = new ShopSystem(this, world, player, ui);
            missions = new MissionSystem(this, world, player);

            CityBuilder.build(world);
            ui.mm = new MiniMap(world);

            // ساخت شخصیت: اول از ذخیره، بعد از تنظیمات
            JSONObject save = SaveManager.load(appContext);
            boolean loaded = false;
            if (save != null) {
                try {
                    player.readJson(save);
                    world.readTime(save);
                    heliOwned = save.optBoolean("heliOwned", false);
                    trainOwned = save.optBoolean("trainOwned", false);
                    missions.setCurrent(save.optInt("mission", 0));
                    loaded = true;
                } catch (Throwable t) {
                    loaded = false;
                }
            }
            if (!loaded) {
                android.content.SharedPreferences prefs =
                        context.getSharedPreferences("shahrshadi_prefs", Context.MODE_PRIVATE);
                player.name = prefs.getString("charName", "قهرمان");
                player.gender = prefs.getInt("charGender", 0);
                player.x = world.spawnX;
                player.y = world.spawnY;
            }

            camera.snap(player.x, player.y);
            bgPaint.setColor(0xFF81D4FA);
        } catch (RuntimeException t) {
            // خطای شروع: گزارش و پرتاب — GameActivity پیام شفاف نشان می‌دهد
            throw t;
        }
    }

    // ================= چرخه حیات Surface =================

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (thread == null || !thread.isAlive()) {
            thread = new GameThread(getHolder(), this);
            thread.setRunning(true);
            thread.start();
        }
        SoundManager.startMusic();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        viewW = width;
        viewH = height;
        camera.setViewport(width, height);
        ui.layout(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (thread != null) {
            thread.setRunning(false);
        }
        saveNow();
        SoundManager.stopEngine();
        SoundManager.stopMusic();
    }

    public void pauseGame() {
        if (thread != null) thread.setPaused(true);
        saveNow();
    }

    public void resumeGame() {
        if (thread != null) thread.setPaused(false);
    }

    public void openPauseMenu() {
        ui.state = UIManager.UI_PAUSE;
        joystick.reset();
        saveNow();
    }

    public void fx(String name) {
        SoundManager.play(name);
    }

    // ================= به‌روزرسانی =================

    public void update(float dt) {
        try {
            if (ui.state == UIManager.UI_PAUSE) return;

            world.update(dt, player);
            ui.update(dt);
            missions.update(dt);

            // حرکت بازیکن
            if (player.driving != null) {
                driveVehicle(dt, player.driving);
            } else if (player.ridingTrain) {
                rideTrain(dt);
            } else if (world.interior != null) {
                player.updateInterior(dt, world, joystick.getDx(), joystick.getDy(), world.interior);
            } else {
                player.update(dt, world, joystick.getDx(), joystick.getDy());
            }

            camera.follow(player.x, player.y, dt);
            jobs.update(dt, player);

            // فیلم سینما
            if (moviePlaying) {
                movieTime += dt;
                movieTimer -= dt;
                if (movieTimer <= 0f) {
                    stopMovie();
                    ui.toast("فیلم تمام شد! امیدوارم لذت برده باشی 🍿");
                }
            }

            // ذخیره خودکار هر ۲۰ ثانیه
            autoSaveTimer += dt;
            if (autoSaveTimer > 20f) {
                autoSaveTimer = 0f;
                saveNow();
            }

            // برچسب دکمه اقدام و ساختمان نزدیک
            updateContext();
        } catch (Throwable t) {
            // حلقه هرگز نباید بکشد
        }
    }

    private void updateContext() {
        if (world.interior == null) {
            ui.nearBuilding = world.buildingNearDoor(player.x, player.y, 150f);
        } else {
            ui.nearBuilding = null;
        }
        String al = jobs.actionLabel();
        if (al == null && world.interior != null && world.interior.floorType.equals("zoo")) {
            // حیوان نزدیک؟
            for (int i = 0; i < world.interior.animalPos.size(); i++) {
                float[] ap = world.interior.animalPos.get(i);
                if (G.dist(player.x, player.y, ap[0], ap[1] + 20f) < 150f) {
                    al = "نگاه کن";
                    break;
                }
            }
        }
        if (al == null && !player.ridingTrain && player.driving != null
                && player.driving.type == Vehicle.CAR_TRAIN) {
            al = "پیاده شو";
        }
        ui.actionLabel = al;
    }

    // ================= رانندگی =================

    private void driveVehicle(float dt, Vehicle v) {
        if (v.type == Vehicle.CAR_TRAIN) {
            // اهرم بالا = گاز، پایین = ترمز/عقب
            float throttle = -joystick.getDy();
            v.speed = G.clamp(v.speed + throttle * 220f * dt, -60f, 170f);
            if (v.mode == Vehicle.MODE_PLAYER && world.railPath != null) {
                v.trackPos += v.speed * dt;
                float[] pos = new float[3];
                world.railPath.posAt(v.trackPos, pos);
                v.x = pos[0];
                v.y = pos[1];
                v.angle = pos[2];
            }
            player.x = v.x;
            player.y = v.y;
            SoundManager.startEngine();
            SoundManager.setEngineChop(false);
            SoundManager.setEngineIntensity(0.25f + Math.abs(v.speed) / 400f);
            return;
        }

        if (v.type == Vehicle.CAR_HELICOPTER) {
            // پرواز آزاد
            float vx = joystick.getDx() * 340f;
            float vy = joystick.getDy() * 340f;
            v.x = G.clamp(v.x + vx * dt, 100f, G.WORLD_W - 100f);
            v.y = G.clamp(v.y + vy * dt, 100f, G.WORLD_H - 100f);
            if (Math.abs(vx) > 10f) v.angle = (float) Math.atan2(vy, vx);
            player.x = v.x;
            player.y = v.y;
            SoundManager.startEngine();
            SoundManager.setEngineChop(true);
            SoundManager.setEngineIntensity(0.5f);
            return;
        }

        // ماشین و موتور — روی زمین با برخورد
        float steer = joystick.getDx();
        float gas = -joystick.getDy();
        v.speed = G.clamp(v.speed + gas * 300f * dt - v.speed * 0.6f * dt, 0f,
                v.type == Vehicle.MOTOR ? 260f : 220f);
        v.angle += steer * 2.4f * dt * (v.speed > 5f ? 1f : 0f);
        float nx = v.x + (float) Math.cos(v.angle) * v.speed * dt;
        float ny = v.y + (float) Math.sin(v.angle) * v.speed * dt;
        if (!world.isBlocked(nx, ny, 24f)) {
            v.x = G.clamp(nx, 100f, G.WORLD_W - 100f);
            v.y = G.clamp(ny, 100f, G.WORLD_H - 100f);
        } else {
            v.speed = 0f;
        }
        player.x = v.x;
        player.y = v.y;
        SoundManager.startEngine();
        SoundManager.setEngineChop(false);
        SoundManager.setEngineIntensity(v.speed / 260f);
    }

    /**
     * سفر مسافری با قطار — یک دور کامل، پیاده شدن در ایستگاه
     */
    private void rideTrain(float dt) {
        Vehicle train = findTrain();
        if (train == null || world.railPath == null) {
            player.ridingTrain = false;
            return;
        }
        trainRideVehicle = train;
        // بازیکن در واگن اول می‌نشیند
        float[] seat = new float[2];
        train.pointAt(world, 1, seat);
        player.x = seat[0];
        player.y = seat[1];

        float traveled = train.trackPos - player.trainBoardDist;
        while (traveled < 0) traveled += world.railPath.perimeter;
        boolean lapDone = traveled >= world.railPath.perimeter - 80f;
        boolean nearStation = G.dist(train.x, train.y, world.trainStationX, world.trainStationY) < 220f;

        if (lapDone && nearStation) {
            dismountTrain();
        }
    }

    private void dismountTrain() {
        player.ridingTrain = false;
        trainRideVehicle = null;
        player.x = world.trainStationX;
        player.y = world.trainStationY + 90f;
        missions.completeByTitle("قطار");
        SoundManager.play("success");
        ui.toast("به ایستگاه رسیدی! سفر خوبی بود 🚂");
    }

    private void exitVehicle() {
        Vehicle v = player.driving;
        if (v == null) return;
        player.driving = null;
        SoundManager.stopEngine();

        if (v.type == Vehicle.CAR_TRAIN) {
            // پیاده شدن کنار ریل (ترجیحاً ایستگاه)
            if (G.dist(v.x, v.y, world.trainStationX, world.trainStationY) < 300f) {
                player.x = world.trainStationX;
                player.y = world.trainStationY + 90f;
                ui.toast("به ایستگاه خوش آمدی! 🚂");
            } else {
                player.x = v.x;
                player.y = v.y + 80f;
                if (world.isBlocked(player.x, player.y, 16f)) {
                    player.y = v.y - 80f;
                }
                ui.toast("پیاده شدی!");
            }
            v.speed = 95f;
            v.mode = Vehicle.MODE_RAIL;   // قطار دوباره خودش دور می‌زند
            return;
        }

        // ماشین/موتور/هلی
        player.x = v.x + 60f;
        player.y = v.y + 20f;
        if (world.isBlocked(player.x, player.y, 16f)) {
            player.x = v.x - 60f;
        }
        if (v.type == Vehicle.CAR_HELICOPTER) {
            v.mode = Vehicle.MODE_PARKED;
            v.x = player.x - 40f;
            v.y = player.y - 20f;
            ui.toast("فرود آمدی! 🚁");
        } else {
            v.mode = Vehicle.MODE_PARKED;
            ui.toast("پیاده شدی!");
        }
    }

    private Vehicle findTrain() {
        for (int i = 0; i < world.vehicles.size(); i++) {
            Vehicle v = world.vehicles.get(i);
            if (v.type == Vehicle.CAR_TRAIN) return v;
        }
        return null;
    }

    // ================= فیلم سینما =================

    public void startMovie(int index) {
        movieIndex = index;
        movieTime = 0f;
        movieTimer = 45f;
        moviePlaying = true;
        SoundManager.play("success");
        ui.toast("🎬 فیلم شروع شد — از تماشای آن لذت ببر!");
    }

    public void stopMovie() {
        moviePlaying = false;
    }

    // ================= دکمه اقدام =================

    private void onAction() {
        // پیاده شدن از قطار مسافری
        if (player.ridingTrain) {
            dismountTrain();
            return;
        }
        // پیاده شدن از وسیله‌ای که می‌رانی
        if (player.driving != null) {
            exitVehicle();
            return;
        }
        // کار شغل‌ها
        if (jobs.doAction()) return;

        // اطلاعات حیوان باغ‌وحش
        if (world.interior != null && world.interior.floorType.equals("zoo")) {
            for (int i = 0; i < world.interior.animalPos.size(); i++) {
                float[] ap = world.interior.animalPos.get(i);
                if (G.dist(player.x, player.y, ap[0], ap[1] + 20f) < 150f) {
                    ui.openInfo("درباره این حیوان", Dialogues.animalInfo(world.interior.animalTypes.get(i)));
                    return;
                }
            }
        }
    }

    // ================= ذخیره =================

    public void saveNow() {
        try {
            JSONObject o = new JSONObject();
            player.writeJson(o);
            world.writeTime(o);
            o.put("heliOwned", heliOwned);
            o.put("trainOwned", trainOwned);
            o.put("mission", missions.currentIndex());
            SaveManager.save(appContext, o);
        } catch (Throwable t) {
        }
    }

    // ================= لمس =================

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            handleTouch(event.getActionMasked(), event.getX(), event.getY());
            return true;
        } catch (Throwable t) {
            joystick.reset();
            return true;
        }
    }

    private void handleTouch(int action, float x, float y) {
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                // اول UI
                String pressed = ui.handleTouch(action, x, y);
                if (pressed != null) {
                    handleUiPress(pressed);
                    return;
                }
                if (ui.state == UIManager.UI_INFO || ui.state == UIManager.UI_MENU
                        || ui.state == UIManager.UI_PAUSE) return;
                if (moviePlaying) {
                    if (ui.btnSkipMovie.contains(x, y)) {
                        stopMovie();
                        ui.toast("فیلم رد شد — دفعه بعد کامل ببین!");
                        return;
                    }
                    return;   // حین فیلم لمس دنیا کار نمی‌کند
                }
                if (x < viewW * 0.45f) {
                    joystick.start(x, y);
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (joystick.active) joystick.move(x, y);
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                ui.handleTouch(action, x, y);
                joystick.release();
                break;
            }
        }
    }

    private void handleUiPress(String id) {
        switch (id) {
            case "action":
                SoundManager.play("click");
                onAction();
                break;
            case "enter":
                if (ui.nearBuilding != null) {
                    SoundManager.play("click");
                    shop.openBuildingMenu(ui.nearBuilding);
                }
                break;
            case "look":
                if (ui.nearBuilding != null) {
                    SoundManager.play("click");
                    ui.openInfo(ui.nearBuilding.name(), ui.nearBuilding.info());
                }
                break;
            case "pause":
                openPauseMenu();
                break;
            case "resume":
                ui.state = UIManager.UI_PLAY;
                break;
            case "sound":
                SoundManager.setMuted(appContext, !SoundManager.isMuted());
                break;
            case "save":
                saveNow();
                ui.toast("بازی ذخیره شد ✔");
                break;
            case "exitmenu":
                saveNow();
                ((android.app.Activity) getContext()).finish();
                break;
            default:
                if (id.startsWith("item:") && ui.isMenuOpen()) {
                    int idx = Integer.parseInt(id.substring(5));
                    // اجرای آیتم از طریق UIManager.handleTouch انجام شده است
                }
                break;
        }
    }

    // ================= رسم =================

    public void doDraw(Canvas c) {
        try {
            if (world.interior == null) {
                // آسمان دور شهر
                c.drawColor(Color.parseColor(world.dayNight.isNight() ? "#0D1B3E" : "#81D4FA"));
                c.save();
                camera.apply(c);
                world.draw(c, sprites, camera.x, camera.y, viewW, viewH);

                // بازیکن (اگر داخل وسیله‌ای که می‌راند روی وسیله رسم نشود)
                if (player.driving == null) {
                    drawPlayer(c);
                }

                // نشانگر مأموریت شغل‌ها
                float[] marker = jobs.currentMarker();
                if (marker != null && !player.ridingTrain) {
                    sprites.p.setColor(jobs.markerColor());
                    float bounce = (float) Math.abs(Math.sin(world.dayNight.minutes * 3f)) * 14f;
                    c.drawCircle(marker[0], marker[1] - 60f - bounce, 10f, sprites.p);
                    c.drawCircle(marker[0], marker[1] - 60f - bounce, 5f, bgPaint);
                }

                c.restore();
            } else {
                // داخل ساختمان: جا دادن اتاق در صفحه
                c.drawColor(Color.parseColor("#3E2723"));
                c.save();
                float scale = Math.min(viewW / (world.interior.roomW + 100f),
                        viewH / (world.interior.roomH + 100f));
                c.translate(viewW / 2f, viewH / 2f);
                c.scale(scale, scale);
                c.translate(-world.interior.roomW / 2f, -world.interior.roomH / 2f);
                world.drawInterior(c, sprites);
                drawPlayer(c);

                // پرده سینما: فیلم تمام‌صفحه
                if (world.interior.floorType.equals("cinema") && moviePlaying) {
                    for (int i = 0; i < world.interior.furniture.size(); i++) {
                        int[] st = world.interior.furnitureStyle.get(i);
                        if (st[1] == 9) {
                            float[] f = world.interior.furniture.get(i);
                            movieScreen.set(f[0] + 6f, f[1] + 6f, f[0] + f[2] - 6f, f[1] + f[3] - 6f);
                            sprites.drawMovie(c, movieScreen, movieIndex, movieTime);
                        }
                    }
                }

                c.restore();
            }

            // پرده شب
            world.dayNight.drawOverlay(c, viewW, viewH);

            // عنوان سینما حین فیلم
            if (moviePlaying && world.interior != null && world.interior.floorType.equals("cinema")) {
                tp.setColor(0xFFFFFFFF);
                tp.setTextAlign(Paint.Align.CENTER);
                tp.setTextSize(30f);
                c.drawText("سینما ستاره در حال پخش...", viewW / 2f, 50f, tp);
            }

            // رابط کاربری
            ui.draw(c, viewW, viewH);
        } catch (Throwable t) {
            c.drawColor(Color.BLACK);
        }
    }

    private void drawPlayer(Canvas c) {
        int[] outfit = player.gender == 1 ? SpriteLib.OUTFITS[5] : SpriteLib.OUTFITS[0];
        int carrying = -1;
        if (jobs.jobName() != null && jobs.activeJob == JobSystem.JOB_WAITER && jobs.actionLabel() != null) {
            carrying = 1;
        }
        sprites.drawPerson(c, player.x, player.y, player.dir, player.anim,
                outfit[0], outfit[1], G.COL_SKIN, 0xFF3E2723,
                player.gender == 1 ? 2 : 0, carrying, true, player.gender);
    }

    // نشانه‌گذاری برای ذخیره در قطار مسافری
    public float trainBoardDist() {
        return player.trainBoardDist;
    }
}
