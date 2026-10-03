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

    // سواري شهربازی: ۰=هیچ ۱=چرخ‌وفلک ۲=سرسیر
    public int rideKind = 0;
    private float rideTimer = 0f;
    private float rideTime = 0f;

    // NPC نزدیک برای گفتگو + وسیله قابل سوار شدن
    private Npc nearNpc = null;
    private Vehicle boardable = null;

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

            // نشانگرهای ثابت مینی‌مپ (✅ سینما، رستوران و همه مکان‌های مهم)
            addMinimapMarkers();

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
                    // بازیابی ماشین‌های خریده‌شده
                    org.json.JSONArray cars = save.optJSONArray("cars");
                    if (cars != null) {
                        for (int i = 0; i < cars.length(); i++) {
                            JSONObject cv = cars.optJSONObject(i);
                            if (cv == null) continue;
                            Vehicle v = new Vehicle(cv.optInt("t", 0),
                                    (float) cv.optDouble("x", world.spawnX),
                                    (float) cv.optDouble("y", world.spawnY));
                            v.owned = true;
                            v.mode = Vehicle.MODE_PARKED;
                            v.color = cv.optInt("c", v.color);
                            world.vehicles.add(v);
                        }
                    }
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

    /**
     * نشانگرهای ثابت مینی‌مپ — همه مکان‌های مهم شهر
     */
    private void addMinimapMarkers() {
        MiniMap mm = ui.mm;
        addMarkerFor(mm, Building.HOME, 0xFFFF9800);
        addMarkerFor(mm, Building.RESTAURANT, 0xFFE65100);
        addMarkerFor(mm, Building.CINEMA, 0xFF6A1B9A);
        addMarkerFor(mm, Building.HOSPITAL, 0xFFE53935);
        addMarkerFor(mm, Building.BANK, 0xFF2E7D32);
        addMarkerFor(mm, Building.ZOO, 0xFF33691E);
        addMarkerFor(mm, Building.MARKET, 0xFF43A047);
        addMarkerFor(mm, Building.SCHOOL, 0xFF1565C0);
        addMarkerFor(mm, Building.LIBRARY, 0xFF8E24AA);
        addMarkerFor(mm, Building.POLICE, 0xFF37474F);
        addMarkerFor(mm, Building.CARSHOP, 0xFF00897B);
        addMarkerFor(mm, Building.AMUSEMENT, 0xFFF06292);
        addMarkerFor(mm, Building.DOCK, 0xFF0277BD);
        addMarkerFor(mm, Building.BAKERY, 0xFFC7A008);
        addMarkerFor(mm, Building.TRAIN_STATION, 0xFF5D4037);
        addMarkerFor(mm, Building.HELIPORT, 0xFF00BCD4);
        // پارک و چرخ‌وفلک
        if (world.fountainX > 0f) mm.addMarker(world.fountainX, world.fountainY, 0xFF1E88E5, 0);
        if (world.wheelX > 0f) mm.addMarker(world.wheelX, world.wheelY, 0xFFF06292, 0);
    }

    private void addMarkerFor(MiniMap mm, int type, int color) {
        Building b = world.buildingByType(type);
        if (b != null) mm.addMarker(b.doorX, b.doorY, color, 0);
    }

    // ================= به‌روزرسانی =================

    public void update(float dt) {
        try {
            if (ui.state == UIManager.UI_PAUSE) return;

            world.playerRef = player;   // برای ترمز ماشین‌ها پشت بازیکن
            world.update(dt, player);
            ui.update(dt);
            missions.update(dt);

            // سواری شهربازی (چرخ‌وفلک / سرسیر)
            if (rideKind > 0) {
                updateRide(dt);
                ui.actionLabel = null;   // حین سواری دکمه اقدام معنا ندارد
                camera.follow(player.x, player.y, dt);
                jobs.update(dt, player);
                return;
            }

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

            // ✅ زوم دوربین: پیاده نزدیک، با وسیله کمی دورتر (دوربین نزدیک‌تر از قبل)
            camera.zoomTarget = player.driving != null
                    ? (player.driving.type == Vehicle.CAR_HELICOPTER ? 1.15f : 1.3f)
                    : (player.ridingTrain ? 1.2f : 1.55f);
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
        if (al == null && world.interior == null && !player.ridingTrain) {
            // 🎣 ماهیگیری کنار اسکله
            if (ui.nearBuilding != null && ui.nearBuilding.type == Building.DOCK
                    && player.driving == null) {
                al = "ماهیگیری";
            }
            // 💬 گفتگو با شهروند نزدیک
            nearNpc = findNearNpc(140f);
            // 🚗 سوار شدن به وسیله‌ی خودت (ماشین/موتور/قایق/هلی پارک‌شده)
            boardable = findBoardable(130f);
            if (al == null && nearNpc != null) al = "صحبت کن";
            if (al == null && boardable != null) al = "سوار شو";
        } else {
            nearNpc = null;
            boardable = null;
        }
        ui.actionLabel = al;
    }

    private Npc findNearNpc(float maxDist) {
        Npc best = null;
        float bestD = maxDist;
        for (int i = 0; i < world.cityNpcs.size(); i++) {
            Npc n = world.cityNpcs.get(i);
            float d = G.dist(player.x, player.y, n.x, n.y);
            if (d < bestD) {
                bestD = d;
                best = n;
            }
        }
        return best;
    }

    private Vehicle findBoardable(float maxDist) {
        Vehicle best = null;
        float bestD = maxDist;
        for (int i = 0; i < world.vehicles.size(); i++) {
            Vehicle v = world.vehicles.get(i);
            if (v.mode != Vehicle.MODE_PARKED || !v.owned || !v.isActive()) continue;
            if (v.type == Vehicle.CAR_TRAIN) continue;
            float d = G.dist(player.x, player.y, v.x, v.y);
            if (d < bestD) {
                bestD = d;
                best = v;
            }
        }
        return best;
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

        if (v.type == Vehicle.CAR_BOAT) {
            // ⛵ قایق — فقط روی آب!
            float steer = joystick.getDx();
            float gas = -joystick.getDy();
            v.speed = G.clamp(v.speed + gas * 260f * dt - v.speed * 0.8f * dt, 0f, 190f);
            v.angle += steer * 2.2f * dt * (v.speed > 8f ? 1f : 0f);
            float nx = v.x + (float) Math.cos(v.angle) * v.speed * dt;
            float ny = v.y + (float) Math.sin(v.angle) * v.speed * dt;
            if (world.isWaterAt(nx, ny)) {
                v.x = nx;
                v.y = ny;
            } else {
                v.speed = 0f;   // به ساحل خوردی
            }
            player.x = v.x;
            player.y = v.y;
            SoundManager.startEngine();
            SoundManager.setEngineChop(false);
            SoundManager.setEngineIntensity(0.3f);
            return;
        }

        // ماشین و موتور — روی جاده سریع، بیرون جاده خیلی کند
        float steer = joystick.getDx();
        float gas = -joystick.getDy();
        v.speed = G.clamp(v.speed + gas * 300f * dt - v.speed * 0.6f * dt, 0f,
                v.type == Vehicle.MOTOR ? 260f : 220f);
        v.angle += steer * 2.4f * dt * (v.speed > 5f ? 1f : 0f);
        float nx = v.x + (float) Math.cos(v.angle) * v.speed * dt;
        float ny = v.y + (float) Math.sin(v.angle) * v.speed * dt;
        if (!world.isBlocked(nx, ny, 24f)) {
            // 🛣 ماشین‌ها فقط روی جاده تند می‌روند (خارج جاده کند می‌شوند)
            int t = world.tileAt(nx, ny);
            boolean onRoad = t == World.T_ROAD || t == World.T_SIDEWALK
                    || t == World.T_PATH || t == World.T_DIRT;
            if (!onRoad && v.speed > 70f) {
                v.speed = 70f;
            }
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

        // ماشین/موتور/هلی/قایق
        if (v.type == Vehicle.CAR_BOAT) {
            // پیاده شدن از قایق — باید نزدیک ساحل باشی
            float[] spot = world.findWalkableNear(v.x, v.y);
            if (spot == null) {
                ui.toast("باید نزدیک ساحل پیاده شوی!");
                return;
            }
            player.x = spot[0];
            player.y = spot[1];
            v.mode = Vehicle.MODE_PARKED;
            v.speed = 0f;
            SoundManager.play("splash");
            ui.toast("از قایق پیاده شدی ⛵");
            return;
        }
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
        // حین سواری شهربازی دکمه اقدام کاری نمی‌کند
        if (rideKind > 0) return;
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

        if (world.interior != null) return;

        // 🎣 ماهیگیری کنار اسکله
        if (ui.nearBuilding != null && ui.nearBuilding.type == Building.DOCK) {
            doFishing();
            return;
        }
        // 💬 گفتگو با شهروند
        if (nearNpc != null) {
            talkToNpc(nearNpc);
            return;
        }
        // 🚗 سوار شدن به وسیله‌ی خودت
        if (boardable != null) {
            player.driving = boardable;
            boardable.mode = Vehicle.MODE_PLAYER;
            player.x = boardable.x;
            player.y = boardable.y;
            SoundManager.play(boardable.type == Vehicle.CAR_BOAT ? "splash" : "door");
            if (boardable.type == Vehicle.CAR_BOAT) {
                ui.toast("سوار قایق شدی! ⛵ با اهرم دریانوردی کن");
            } else {
                ui.toast("سوار شدی! با اهرم بران — دکمه اقدام = پیاده شدن");
            }
        }
    }

    /**
     * 💬 گفتگو با شهروند — سلام و احوال‌پرسی + شاید یک مأموریت محله‌ای!
     */
    private void talkToNpc(Npc n) {
        String greet = Dialogues.greeting();
        n.say(greet);
        SoundManager.play("click");
        String errand = missions.offerErrand();
        if (errand != null) {
            ui.openInfo("💬 شهروند شهر شادی", greet + "\n\nراستی یک خواهش دارم:\n" + errand
                    + "\n\nانجامش بدهی جایزه خوبی می‌گیری!");
        } else {
            ui.openInfo("💬 شهروند شهر شادی", greet + "\n\n" + Dialogues.jobTalk());
        }
    }

    /**
     * 🎣 ماهیگیری — کنار اسکله ماهی بگیر و بفروش
     */
    private void doFishing() {
        String[] fishNames = {"🐟 ماهی نقره‌ای", "🐠 ماهی رنگارنگ", "🦈 ماهی خوششانسی"};
        int[] fishPrices = {60, 100, 160};
        int idx = Math.random() < 0.15 ? 2 : (Math.random() < 0.45 ? 1 : 0);
        player.addMoney(fishPrices[idx]);
        SoundManager.play("fish");
        ui.toast(fishNames[idx] + " گرفتی! فروختی: " + UIManager.faMoney(fishPrices[idx]) + " تومان");
    }

    // ================= سواری شهربازی =================

    public void startWheelRide() {
        if (world.wheelX <= 0f) return;
        player.driving = null;
        player.ridingTrain = false;
        rideKind = 1;
        rideTime = 0f;
        rideTimer = 18f;
        joystick.reset();
        SoundManager.play("success");
        ui.toast("سوار چرخ‌وفلک شدی! 🎡 بالا می‌رویم...");
    }

    public void startCarouselRide() {
        if (world.carouselX <= 0f) return;
        player.driving = null;
        player.ridingTrain = false;
        rideKind = 2;
        rideTime = 0f;
        rideTimer = 14f;
        joystick.reset();
        SoundManager.play("success");
        ui.toast("سوار سرسیر شدی! 🎠 اسبت را محکم بگیر!");
    }

    private void updateRide(float dt) {
        rideTime += dt;
        rideTimer -= dt;
        if (rideKind == 1) {
            // صندلی چرخ‌وفلک: دور یک دایره بزرگ
            float ang = rideTime * 0.55f;
            float R = 118f;
            player.x = world.wheelX + (float) Math.cos(ang) * R;
            player.y = world.wheelY + (float) Math.sin(ang) * R + 10f;
        } else {
            // اسب کاروسل: دور مرکز با بالا پایین
            float ang = rideTime * 1.6f;
            player.x = world.carouselX + (float) Math.cos(ang) * 44f;
            player.y = world.carouselY - 22f + (float) Math.sin(ang) * 18f
                    - (float) Math.abs(Math.sin(rideTime * 3f)) * 5f;
        }
        player.dir = (int) ((rideTime * 2f) % 4f);
        player.anim += dt * 1.2f;
        if (rideTimer <= 0f) {
            // پیاده شدن کنار وسیله
            float ex = rideKind == 1 ? world.wheelX : world.carouselX;
            float ey = (rideKind == 1 ? world.wheelY : world.carouselY) + 120f;
            if (world.isBlocked(ex, ey, 16f)) {
                ex = ex + 130f;
            }
            player.x = ex;
            player.y = ey;
            rideKind = 0;
            player.anim = 0f;
            player.energy = Math.min(100f, player.energy + 15f);
            SoundManager.play("mission");
            ui.toast("چه سواری باحالی! 🎉 انرژی: +۱۵");
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
            // ماشین‌های خریده‌شده بازیکن (موقعیت پارک هم ذخیره می‌شود)
            org.json.JSONArray cars = new org.json.JSONArray();
            for (int i = 0; i < world.vehicles.size(); i++) {
                Vehicle v = world.vehicles.get(i);
                if (v.owned && v.type != Vehicle.CAR_TRAIN && v.type != Vehicle.CAR_BOAT
                        && v.type != Vehicle.CAR_HELICOPTER) {
                    JSONObject cv = new JSONObject();
                    cv.put("t", v.type);
                    cv.put("x", v.x);
                    cv.put("y", v.y);
                    cv.put("c", v.color);
                    cars.put(cv);
                }
            }
            o.put("cars", cars);
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
                // اهرم فقط وقتی شروع شود که روی دکمه‌ها نباشیم (رفع باگ دکمه نگاه کن)
                if (x < viewW * 0.45f && !ui.isPlayButtonAt(x, y)) {
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
                // ⚠ رفع باگ اصلی: مقدار بازگشتی روی ACTION_UP قبلاً دور ریخته می‌شد
                // و به همین دلیل دکمه‌های نگاه کن / وارد شو / اقدام / مکث هرگز کار نمی‌کردند!
                String pressed = ui.handleTouch(action, x, y);
                if (pressed != null) {
                    handleUiPress(pressed);
                }
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
                // رسم با احتساب زوم دوربین (فقط بخش مرئی رسم می‌شود)
                world.draw(c, sprites, camera.x, camera.y,
                        viewW / camera.zoom, viewH / camera.zoom);

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

                // نشانگر مأموریت محله‌ای (از شهروندها) — سبز پرنده
                float[] em = missions.errandMarker();
                if (em != null && !player.ridingTrain && player.driving == null) {
                    sprites.p.setColor(0xFF43A047);
                    float bounce = (float) Math.abs(Math.sin(world.dayNight.minutes * 3.4f)) * 16f;
                    c.drawCircle(em[0], em[1] - 70f - bounce, 12f, sprites.p);
                    sprites.p.setColor(0xFFFFFFFF);
                    sprites.p.setTextSize(18f);
                    sprites.p.setTextAlign(Paint.Align.CENTER);
                    c.drawText("!", em[0], em[1] - 62f - bounce, sprites.p);
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

    /**
     * قطار شهر (برای مینی‌مپ متحرک)
     */
    public Vehicle trainVehicle() {
        for (int i = 0; i < world.vehicles.size(); i++) {
            Vehicle v = world.vehicles.get(i);
            if (v.type == Vehicle.CAR_TRAIN) return v;
        }
        return null;
    }
}
