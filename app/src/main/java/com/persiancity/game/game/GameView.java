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

    // استایل‌های لباس خریده‌شده (۰ آبی و ۵ صورتی پیش‌فرض مال خودت است)
    public final boolean[] outfitOwned = new boolean[SpriteLib.OUTFITS.length];

    // سواري شهربازی: ۰=هیچ ۱=چرخ‌وفلک ۲=سرسیر
    public int rideKind = 0;
    private float rideTimer = 0f;
    private float rideTime = 0f;

    // NPC نزدیک برای گفتگو + وسیله قابل سوار شدن
    private Npc nearNpc = null;
    private Vehicle boardable = null;
    // 🐴 آیا اسبی نزدیک است؟ (نمایش دکمهٔ «سوار اسب»)
    public boolean rideHorseNear = false;

    // قطار مسافری + ایستگاه‌ها
    private int activeTrainLoop = 0;
    private int nearStationIdx = -1;
    private int nearBusStopIdx = -1;
    // اتوبوس بین‌شهری
    public static final int BUS_FARE = 150;

    // فیلم سینما (اگر اینترنت نباشد برفک نشان داده می‌شود)
    public boolean moviePlaying = false;
    public boolean movieOffline = false;
    private float movieTimer = 0f;
    private int movieIndex = 0;
    private float movieTime = 0f;

    // ✅ پخش واقعی ویدیو در سینما (تام و جری) — WebView داخل اکتیویتی
    public static final String TOM_JERRY_URL = "https://ifilo.net/embed/ahSyhls";
    public boolean videoOpen = false;

    /** رابط ارتباط با اکتیویتی برای نمایش ویدیوی واقعی سینما */
    public interface CinemaHost {
        void openCinemaVideo(String url);
        void closeCinemaVideo();
    }

    public CinemaHost cinemaHost = null;

    // صدای حیوانات باغ‌وحش + حیوانات روستا و اسب‌ها (نزدیک که می‌رسی صدا می‌دهند)
    private float zooSoundCd = 0f;
    private float farmSoundCd = 0f;
    private static final String[] ZOO_SOUNDS = {
        "lion", "elephant", "monkey", "neigh", "birds",
        "giraffe", "bear", "tiger", "birds", "camel"
    };

    // ذخیره خودکار
    private float autoSaveTimer = 0f;

    // قطار مسافری (برای همگام‌سازی ذخیره)
    private Vehicle trainRideVehicle = null;

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
            for (int i = 0; i < outfitOwned.length; i++) outfitOwned[i] = (i == 0 || i == 5);
            if (save != null) {
                try {
                    player.readJson(save);
                    world.readTime(save);
                    heliOwned = save.optBoolean("heliOwned", false);
                    trainOwned = save.optBoolean("trainOwned", false);
                    missions.setCurrent(save.optInt("mission", 0));
                    // بازیابی لباس‌های خریده‌شده
                    org.json.JSONArray owned = save.optJSONArray("outfits");
                    if (owned != null) {
                        for (int i = 0; i < owned.length() && i < outfitOwned.length; i++) {
                            if (owned.optBoolean(i, false)) outfitOwned[i] = true;
                        }
                    }
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
                player.outfit = player.gender == 1 ? 5 : 0;
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
     * نشانگرهای ثابت مینی‌مپ — همه مکان‌های مهم هر سه شهر
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
        addMarkerFor(mm, Building.CLOTHES, 0xFFAD1457);
        addMarkerFor(mm, Building.AIRPORT, 0xFF0277BD);
        addMarkerFor(mm, Building.HELIPORT, 0xFF00BCD4);
        // همه ایستگاه‌های قطار (۳ ایستگاه)
        for (int i = 0; i < world.buildings.size(); i++) {
            Building b = world.buildings.get(i);
            if (b.type == Building.TRAIN_STATION) {
                mm.addMarker(b.doorX, b.doorY, 0xFF5D4037, 0);
            }
        }
        // ایستگاه‌های اتوبوس
        for (int i = 0; i < world.busStops.size(); i++) {
            float[] bs = world.busStops.get(i);
            mm.addMarker(bs[0], bs[1], 0xFF1E88E5, 0);
        }
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
            if (moviePlaying && !videoOpen) {
                movieTime += dt;
                movieTimer -= dt;
                if (movieTimer <= 0f) {
                    stopMovie();
                    ui.toast("فیلم تمام شد! امیدوارم لذت برده باشی 🍿");
                }
            }

            // 🔊 صدای حیوانات باغ‌وحش (نزدیک قفس)
            if (world.interior != null && world.interior.floorType.equals("zoo")) {
                zooSoundCd -= dt;
                if (zooSoundCd <= 0f) {
                    for (int i = 0; i < world.interior.animalPos.size(); i++) {
                        float[] ap = world.interior.animalPos.get(i);
                        if (G.dist(player.x, player.y, ap[0], ap[1]) < 230f) {
                            int t = world.interior.animalTypes.get(i);
                            t = Math.max(0, Math.min(t, ZOO_SOUNDS.length - 1));
                            SoundManager.play(ZOO_SOUNDS[t]);
                            zooSoundCd = 3.4f + (float) Math.random() * 2.2f;
                            break;
                        }
                    }
                }
            }

            // 🔊 صدای حیوانات روستا و اسب‌ها — بیرون از ساختمان (درخواست کاربر)
            if (world.interior == null && !player.ridingTrain) {
                farmSoundCd -= dt;
                if (farmSoundCd <= 0f) {
                    farmSoundCd = 2.6f + (float) Math.random() * 2.0f;
                    boolean played = false;
                    // اسب‌های در حال چرا
                    for (int i = 0; i < world.vehicles.size() && !played; i++) {
                        Vehicle v = world.vehicles.get(i);
                        if (v.type == Vehicle.CAR_HORSE && v.isActive()
                                && G.dist(player.x, player.y, v.x, v.y) < 260f) {
                            SoundManager.play("neigh");
                            played = true;
                        }
                    }
                    // گاو و گوسفند روستا
                    for (int i = 0; i < world.villageAnimals.size() && !played; i++) {
                        float[] a = world.villageAnimals.get(i);
                        if (G.dist(player.x, player.y, a[0], a[1]) < 240f) {
                            SoundManager.play(((int) a[2]) == 0 ? "cow" : "sheep");
                            played = true;
                        }
                    }
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
                if (G.dist(player.x, player.y, ap[0], ap[1] + 20f) < 230f) {
                    al = "نگاه کن";
                    break;
                }
            }
        }
        // ✅ دکمه پیاده شدن برای همه وسیله‌ها (ماشین، قایق، هلی، اسب، قطار)
        if (al == null && player.driving != null) {
            al = "پیاده شو";
        }
        if (al == null && player.ridingTrain) {
            al = "پیاده شو";
        }
        if (al == null && world.interior == null && !player.ridingTrain
                && player.driving == null && !player.swimming) {
            // 🚂 ایستگاه قطار نزدیک؟
            nearStationIdx = nearestTrainStation(240f);
            if (nearStationIdx >= 0) {
                al = "سوار قطار";
            }
            // 🚌 ایستگاه اتوبوس نزدیک؟
            if (al == null) {
                nearBusStopIdx = nearestBusStop(150f);
                if (nearBusStopIdx >= 0) al = "اتوبوس";
            }
        } else if (player.driving != null || player.ridingTrain || player.swimming) {
            nearStationIdx = -1;
            nearBusStopIdx = -1;
        }
        if (al == null && world.interior == null && !player.ridingTrain) {
            // 🎣 ماهیگیری کنار اسکله
            if (ui.nearBuilding != null && ui.nearBuilding.type == Building.DOCK
                    && player.driving == null) {
                al = "ماهیگیری";
            }
            // 💬 گفتگو با شهروند نزدیک
            nearNpc = findNearNpc(140f);
            // 🚗 سوار شدن به وسیله‌ی خودت (ماشین/موتور/قایق/هلی پارک‌شده یا اسب در حال چرا)
            boardable = findBoardable(130f);
            if (al == null && nearNpc != null) al = "صحبت کن";
            if (al == null && boardable != null) al = "سوار شو";
        } else {
            nearNpc = null;
            boardable = null;
        }
        // 🐴 آیا نزدیک‌ترین وسیلهٔ قابل سوار شدن، اسب است؟ (دکمهٔ مخصوص سوار اسب)
        rideHorseNear = boardable != null && boardable.type == Vehicle.CAR_HORSE;
        ui.actionLabel = al;
    }

    private int nearestTrainStation(float maxDist) {
        int best = -1;
        float bestD = maxDist;
        for (int i = 0; i < world.trainStations.size(); i++) {
            float[] st = world.trainStations.get(i);
            float d = G.dist(player.x, player.y, st[0], st[1]);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    private int nearestBusStop(float maxDist) {
        int best = -1;
        float bestD = maxDist;
        for (int i = 0; i < world.busStops.size(); i++) {
            float[] bs = world.busStops.get(i);
            float d = G.dist(player.x, player.y, bs[0], bs[1]);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
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
            boolean ok = (v.mode == Vehicle.MODE_PARKED ||
                    (v.type == Vehicle.CAR_HORSE && v.mode == Vehicle.MODE_GRAZE))
                    && v.owned && v.isActive();
            if (!ok) continue;
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
            if (v.mode == Vehicle.MODE_PLAYER && world.rail(v.railIndex) != null) {
                v.trackPos += v.speed * dt;
                float[] pos = new float[3];
                world.rail(v.railIndex).posAt(v.trackPos, pos);
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

        if (v.type == Vehicle.CAR_HORSE) {
            // 🐴 سواری اسب — همه‌جا می‌رود (به جز آب و ساختمان)
            float steer = joystick.getDx();
            float gas = -joystick.getDy();
            v.speed = G.clamp(v.speed + gas * 240f * dt - v.speed * 0.9f * dt, 0f, 150f);
            v.angle += steer * 2.0f * dt * (v.speed > 5f ? 1f : 0f);
            // ✅ جهت نگاه اسب فقط از مؤلفهٔ افقی — اسب هیچ‌وقت وارونه نمی‌شود
            float hca = (float) Math.cos(v.angle);
            if (Math.abs(hca) > 0.25f) v.face = hca > 0f ? 1 : -1;
            float nx = v.x + (float) Math.cos(v.angle) * v.speed * dt;
            float ny = v.y + (float) Math.sin(v.angle) * v.speed * dt;
            if (!world.isBlocked(nx, ny, 20f)) {
                v.x = G.clamp(nx, 100f, G.WORLD_W - 100f);
                v.y = G.clamp(ny, 100f, G.WORLD_H - 100f);
            } else {
                v.speed = 0f;
            }
            player.x = v.x;
            player.y = v.y;
            float ca = (float) Math.cos(v.angle), sa = (float) Math.sin(v.angle);
            if (Math.abs(ca) > Math.abs(sa)) player.dir = ca > 0 ? 3 : 1;
            else player.dir = sa > 0 ? 0 : 2;
            v.anim += dt;
            SoundManager.startEngine();
            SoundManager.setEngineChop(false);
            SoundManager.setEngineIntensity(v.speed / 300f);
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
        Vehicle train = findTrain(activeTrainLoop);
        RailPath path = world.rail(activeTrainLoop);
        if (train == null || path == null) {
            player.ridingTrain = false;
            return;
        }
        // بازیکن در واگن اول می‌نشیند
        float[] seat = new float[2];
        train.pointAt(world, 1, seat);
        player.x = seat[0];
        player.y = seat[1];

        float traveled = train.trackPos - player.trainBoardDist;
        while (traveled < 0) traveled += path.perimeter;
        boolean lapDone = traveled >= path.perimeter - 80f;
        boolean nearStation = false;
        for (int i = 0; i < world.trainStations.size(); i++) {
            float[] st = world.trainStations.get(i);
            if ((int) st[2] != activeTrainLoop) continue;
            if (G.dist(train.x, train.y, st[0], st[1]) < 260f) {
                nearStation = true;
                break;
            }
        }

        if (lapDone && nearStation) {
            dismountTrain();
        }
    }

    private void dismountTrain() {
        player.ridingTrain = false;
        // پیاده شدن کنار نزدیک‌ترین ایستگاه همین خط
        float[] best = null;
        float bestD = Float.MAX_VALUE;
        for (int i = 0; i < world.trainStations.size(); i++) {
            float[] st = world.trainStations.get(i);
            if ((int) st[2] != activeTrainLoop) continue;
            float d = G.dist(player.x, player.y, st[0], st[1]);
            if (d < bestD) {
                bestD = d;
                best = st;
            }
        }
        if (best != null) {
            float[] spot = world.findWalkableNear(best[0], best[1]);
            if (spot != null) {
                player.x = spot[0];
                player.y = spot[1];
            } else {
                player.x = best[0];
                player.y = best[1] + 90f;
            }
        }
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
            boolean atStation = false;
            for (int i = 0; i < world.trainStations.size(); i++) {
                float[] st = world.trainStations.get(i);
                if ((int) st[2] != v.railIndex) continue;
                if (G.dist(v.x, v.y, st[0], st[1]) < 300f) {
                    player.x = st[0];
                    player.y = st[1] + 90f;
                    if (world.isBlocked(player.x, player.y, 16f)) {
                        float[] spot = world.findWalkableNear(st[0], st[1]);
                        if (spot != null) {
                            player.x = spot[0];
                            player.y = spot[1];
                        }
                    }
                    ui.toast("به ایستگاه خوش آمدی! 🚂");
                    atStation = true;
                    break;
                }
            }
            if (!atStation) {
                player.x = v.x;
                player.y = v.y + 80f;
                if (world.isBlocked(player.x, player.y, 16f)) {
                    player.y = v.y - 80f;
                }
                ui.toast("پیاده شدی!");
            }
            v.speed = 150f;
            v.mode = Vehicle.MODE_RAIL;   // قطار دوباره خودش دور می‌زند
            return;
        }

        // ماشین/موتور/هلی/قایق/اسب
        if (v.type == Vehicle.CAR_BOAT) {
            // ✅ پیاده شدن از قایق — حتی وسط دریاچه (شنا می‌کنی!)
            float[] spot = world.findWalkableNear(v.x, v.y);
            v.mode = Vehicle.MODE_PARKED;
            v.speed = 0f;
            SoundManager.play("splash");
            if (spot != null) {
                player.x = spot[0];
                player.y = spot[1];
                player.swimming = false;
                ui.toast("از قایق پیاده شدی ⛵");
            } else {
                player.x = v.x;
                player.y = v.y + 40f;
                player.swimming = true;
                ui.toast("🏊 وسط دریاچه‌ای! به ساحل شنا کن — یا دوباره سوار قایق شو");
            }
            return;
        }
        if (v.type == Vehicle.CAR_HORSE) {
            player.x = v.x + 60f;
            player.y = v.y + 10f;
            if (world.isBlocked(player.x, player.y, 16f)) {
                player.x = v.x - 60f;
            }
            v.mode = Vehicle.MODE_GRAZE;   // اسب دوباره می‌چرد
            v.resetGraze();
            ui.toast("از اسب پیاده شدی! 🐴");
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

    private Vehicle findTrain(int loopIndex) {
        for (int i = 0; i < world.vehicles.size(); i++) {
            Vehicle v = world.vehicles.get(i);
            if (v.type == Vehicle.CAR_TRAIN && v.railIndex == loopIndex) return v;
        }
        return null;
    }

    // ================= فیلم سینما =================

    public void startMovie(int index) {
        movieIndex = index;
        movieTime = 0f;
        moviePlaying = true;
        movieOffline = !hasInternet();
        if (movieOffline) {
            movieTimer = 60f;   // برفک هم ۶۰ ثانیه پخش می‌شود
            ui.toast("📡 اینترنت متصل نیست — پخش با برفک!");
        } else if (index == 0 && cinemaHost != null) {
            // ✅ تام و جری — پخش واقعی ویدیو در WebView (درخواست کاربر)
            movieTimer = Float.MAX_VALUE;   // فیلم واقعی پایان زمانی ندارد
            videoOpen = true;
            cinemaHost.openCinemaVideo(TOM_JERRY_URL);
        } else {
            movieTimer = 45f;
            SoundManager.play("success");
            ui.toast("🎬 فیلم شروع شد — از تماشای آن لذت ببر!");
        }
    }

    public void stopMovie() {
        moviePlaying = false;
        movieOffline = false;
        if (videoOpen) {
            videoOpen = false;
            if (cinemaHost != null) cinemaHost.closeCinemaVideo();
        }
    }

    /** اسم فیلم در حال پخش */
    public String movieName(int i) {
        switch (i) {
            case 0: return "تام و جری 🐭";
            case 1: return "ماهی رنگارنگ 🐠";
            case 2: return "موشک فضایی 🚀";
            default: return "کارتون";
        }
    }

    /**
     * آیا دستگاه به اینترنت وصل است؟ (برای سینما)
     */
    public boolean hasInternet() {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                    appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        } catch (Throwable t) {
            return false;
        }
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
                if (G.dist(player.x, player.y, ap[0], ap[1] + 20f) < 230f) {
                    ui.openInfo("درباره این حیوان", Dialogues.animalInfo(world.interior.animalTypes.get(i)));
                    return;
                }
            }
        }

        if (world.interior != null) return;

        // 🚂 سوار قطار مسافری از ایستگاه نزدیک
        if (nearStationIdx >= 0) {
            boardTrainAt(nearStationIdx);
            return;
        }
        // 🚌 سفر با اتوبوس بین‌شهری
        if (nearBusStopIdx >= 0) {
            openBusMenu(nearBusStopIdx);
            return;
        }
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
        // 🚗 سوار شدن به وسیله‌ی خودت (ماشین/قایق/اسب/هلی)
        if (boardable != null) {
            player.driving = boardable;
            boardable.mode = Vehicle.MODE_PLAYER;
            player.x = boardable.x;
            player.y = boardable.y;
            player.swimming = false;
            if (boardable.type == Vehicle.CAR_BOAT) {
                SoundManager.play("splash");
                ui.toast("سوار قایق شدی! ⛵ با اهرم دریانوردی کن");
            } else if (boardable.type == Vehicle.CAR_HORSE) {
                SoundManager.play("neigh");
                ui.toast("سوار اسب شدی! 🐴 تندتر از پیاده راه می‌ری");
            } else {
                SoundManager.play("door");
                ui.toast("سوار شدی! با اهرم بران — دکمه اقدام = پیاده شدن");
            }
        }
    }

    /**
     * سوار شدن به قطار مسافری از روی سکوی ایستگاه (بلیط ۲۰۰ تومان)
     */
    private void boardTrainAt(int stationIdx) {
        float[] st = world.trainStations.get(stationIdx);
        int loop = (int) st[2];
        Vehicle train = findTrain(loop);
        if (train == null) {
            ui.toast("قطار پیدا نشد!");
            return;
        }
        if (G.dist(train.x, train.y, st[0], st[1]) > 650f) {
            ui.toast("قطار باید نزدیک ایستگاه باشد — کمی صبر کن! 🚂");
            return;
        }
        if (!shop.pay(200)) return;
        player.ridingTrain = true;
        activeTrainLoop = loop;
        player.trainBoardDist = train.trackPos;
        player.driving = null;
        player.swimming = false;
        SoundManager.play("success");
        ui.toast("سفر شروع شد! 🚂 از پنجره شهرها را تماشا کن");
    }

    /**
     * 🚌 سفر با اتوبوس بین‌شهری — از ایستگاه نزدیک به ایستگاه مقصد
     */
    private void openBusMenu(int fromIdx) {
        UIManager.Menu m = new UIManager.Menu("🚌 " + world.busStopNames.get(fromIdx));
        for (int i = 0; i < world.busStops.size(); i++) {
            if (i == fromIdx) continue;
            final int to = i;
            m.items.add(new UIManager.MenuItem("به " + world.busStopNames.get(i),
                    "بلیط: " + UIManager.faMoney(BUS_FARE) + " تومان — مستقیم و سریع!", () -> {
                        if (!shop.pay(BUS_FARE)) return;
                        float[] dst = world.busStops.get(to);
                        player.x = dst[0];
                        player.y = dst[1];
                        player.driving = null;
                        player.ridingTrain = false;
                        player.swimming = false;
                        ui.closeMenu();
                        SoundManager.play("whoosh");
                        ui.toast("🚌 سفر با اتوبوس به " + world.busStopNames.get(to) + " — به سلامت رسیدی!");
                        missions.completeByTitle("اتوبوس");
                    }));
        }
        ui.openMenu(m);
    }

    /**
     * 💬 گفتگو با شهروند — سلام و احوال‌پرسی + شاید یک مأموریت محله‌ای!
     * ✅ اگر مأموریت داشته باشد، دو دکمهٔ سبز «قبول» و قرمز «رد» نشان داده می‌شود
     */
    private void talkToNpc(Npc n) {
        String greet = Dialogues.greeting();
        n.say(greet);
        SoundManager.play("click");
        String errand = missions.offerErrand();
        if (errand != null) {
            ui.openDecision("💬 شهروند شهر شادی", greet + "\n\nراستی یک خواهش دارم:\n" + errand
                    + "\n\nانجامش بدهی جایزهٔ خوبی می‌گیری! قبول می‌کنی؟");
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
            // لباس‌های خریده‌شده
            org.json.JSONArray outfits = new org.json.JSONArray();
            for (int i = 0; i < outfitOwned.length; i++) {
                outfits.put(outfitOwned[i]);
            }
            o.put("outfits", outfits);
            // ماشین‌های خریده‌شده بازیکن (موقعیت پارک هم ذخیره می‌شود)
            org.json.JSONArray cars = new org.json.JSONArray();
            for (int i = 0; i < world.vehicles.size(); i++) {
                Vehicle v = world.vehicles.get(i);
                if (v.owned && v.type != Vehicle.CAR_TRAIN && v.type != Vehicle.CAR_BOAT
                        && v.type != Vehicle.CAR_HELICOPTER && v.type != Vehicle.CAR_HORSE) {
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
            case "ridehorse":
                // 🐴 دکمهٔ مخصوص سوار شدن به اسب (درخواست کاربر)
                SoundManager.play("click");
                if (boardable != null && boardable.type == Vehicle.CAR_HORSE) {
                    player.driving = boardable;
                    boardable.mode = Vehicle.MODE_PLAYER;
                    player.x = boardable.x;
                    player.y = boardable.y;
                    player.swimming = false;
                    SoundManager.play("neigh");
                    ui.toast("سوار اسب شدی! 🐴 با اهرم بران — دکمه اقدام = پیاده شدن");
                }
                break;
            case "yes":
                // ✅ قبول مأموریت (دکمهٔ سبز)
                SoundManager.play("click");
                missions.acceptErrand();
                ui.closeInfo();
                break;
            case "no":
                // ✅ رد مأموریت (دکمهٔ قرمز)
                SoundManager.play("click");
                missions.declineErrand();
                ui.closeInfo();
                break;
            case "exitint":
                // ✅ خروج از ساختمان با دکمه — از هر جای محیط داخلی
                SoundManager.play("click");
                shop.exitInteriorNow();
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

                // بازیکن (اگر داخل وسیله‌ای که می‌راند روی وسیله رسم نشود — به جز اسب: سوار بر اسب دیده می‌شود)
                if (player.driving == null || player.driving.type == Vehicle.CAR_HORSE) {
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

                // نشانگر مأموریت فعال (محله‌ای یا اصلی) — سبز پرنده
                float[] em = missions.activeMarker();
                if (em != null && !player.ridingTrain) {
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

                // پرده سینما: فیلم تمام‌صفحه (یا برفک اگر اینترنت نباشد)
                if (world.interior.floorType.equals("cinema") && moviePlaying) {
                    for (int i = 0; i < world.interior.furniture.size(); i++) {
                        int[] st = world.interior.furnitureStyle.get(i);
                        if (st[1] == 9) {
                            float[] f = world.interior.furniture.get(i);
                            movieScreen.set(f[0] + 6f, f[1] + 6f, f[0] + f[2] - 6f, f[1] + f[3] - 6f);
                            if (movieOffline) {
                                sprites.drawMovieStatic(c, movieScreen, movieTime);
                            } else {
                                sprites.drawMovie(c, movieScreen, movieIndex, movieTime);
                            }
                        }
                    }
                }

                c.restore();
            }

            // پرده شب
            world.dayNight.drawOverlay(c, viewW, viewH);

            // عنوان سینما حین فیلم
            if (moviePlaying && world.interior != null && world.interior.floorType.equals("cinema")) {
                tp.setTextAlign(Paint.Align.CENTER);
                if (movieOffline) {
                    tp.setColor(0xFFFF5252);
                    tp.setTextSize(38f);
                    c.drawText("📡 اینترنت متصل نیست", viewW / 2f, 60f, tp);
                    tp.setColor(0xFFFFFFFF);
                    tp.setTextSize(26f);
                    c.drawText("برای تماشای فیلم اینترنت را روشن کن", viewW / 2f, 100f, tp);
                } else {
                    tp.setColor(0xFFFFFFFF);
                    tp.setTextSize(30f);
                    c.drawText("🎬 سینما ستاره در حال پخش: " + movieName(movieIndex), viewW / 2f, 55f, tp);
                }
            }

            // رابط کاربری
            ui.draw(c, viewW, viewH);
        } catch (Throwable t) {
            c.drawColor(Color.BLACK);
        }
    }

    private void drawPlayer(Canvas c) {
        int oi = Math.max(0, Math.min(player.outfit, SpriteLib.OUTFITS.length - 1));
        int[] outfit = SpriteLib.OUTFITS[oi];
        int carrying = -1;
        if (jobs.jobName() != null && jobs.activeJob == JobSystem.JOB_WAITER && jobs.actionLabel() != null) {
            carrying = 1;
        }
        // 🐴 سوار بر اسب — کمی بالاتر رسم می‌شود
        float py = player.y;
        if (player.driving != null && player.driving.type == Vehicle.CAR_HORSE) {
            py = player.y - 26f;
        }
        sprites.drawPerson(c, player.x, py, player.dir, player.anim,
                outfit[0], outfit[1], G.COL_SKIN, 0xFF3E2723,
                player.gender == 1 ? 2 : 0, carrying, true, player.gender);

        // 🏊 حالت شنا — آب روی نیمه پایین بدن
        if (player.swimming && player.driving == null) {
            sprites.p.setColor(0xB364B5F6);
            c.drawOval(player.x - 22f, player.y - 6f, player.x + 22f, player.y + 24f, sprites.p);
            sprites.p.setColor(0x77FFFFFF);
            c.drawOval(player.x - 26f, player.y + 10f, player.x - 12f, player.y + 15f, sprites.p);
            c.drawOval(player.x + 12f, player.y + 6f, player.x + 26f, player.y + 11f, sprites.p);
        }
    }
}
