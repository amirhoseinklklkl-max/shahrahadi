package com.persiancity.game.game;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

/**
 * حلقه اصلی بازی — ۶۰ فریم هدف
 */
public class GameThread extends Thread {
    private final SurfaceHolder holder;
    private final GameView view;
    private boolean running = false;
    private boolean paused = false;

    public GameThread(SurfaceHolder holder, GameView view) {
        this.holder = holder;
        this.view = view;
    }

    public void setRunning(boolean r) {
        running = r;
    }

    public void setPaused(boolean p) {
        paused = p;
    }

    public boolean isPaused() {
        return paused;
    }

    @Override
    public void run() {
        long last = System.nanoTime();
        while (running) {
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    synchronized (holder) {
                        long now = System.nanoTime();
                        float dt = Math.min((now - last) / 1_000_000_000f, 0.05f);
                        last = now;
                        if (!paused) {
                            view.update(dt);
                        }
                        view.doDraw(c);
                    }
                }
            } catch (Exception e) {
                // هرگز حلقه را نکش
            } finally {
                if (c != null) {
                    try {
                        holder.unlockCanvasAndPost(c);
                    } catch (Exception e) {
                    }
                }
            }
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
