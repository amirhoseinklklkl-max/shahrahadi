package com.persiancity.game.game;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

/**
 * حلقه اصلی بازی — با نرخ ثابت ۶۰ فریم بر ثانیه
 */
public class GameThread extends Thread {

    private final SurfaceHolder holder;
    private final GameView view;
    private volatile boolean running = false;
    private static final long FRAME_TIME_NS = 1_000_000_000L / 60L;

    public GameThread(SurfaceHolder holder, GameView view) {
        this.holder = holder;
        this.view = view;
    }

    public void setRunning(boolean r) {
        running = r;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        long last = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = (now - last) / 1_000_000_000f;
            last = now;
            if (dt > 0.1f) dt = 0.1f;

            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    synchronized (holder) {
                        view.update(dt);
                        view.render(c);
                    }
                }
            } catch (Throwable ignored) {
                // هیچ خطایی نباید بازی را ببندد؛ فریم بعدی ادامه می‌دهد
            } finally {
                if (c != null) {
                    try {
                        holder.unlockCanvasAndPost(c);
                    } catch (Throwable ignored) {
                    }
                }
            }

            // استراحت برای رسیدن به ۶۰ فریم
            long elapsed = System.nanoTime() - now;
            long sleep = FRAME_TIME_NS - elapsed;
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep / 1_000_000L, (int) (sleep % 1_000_000L));
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
