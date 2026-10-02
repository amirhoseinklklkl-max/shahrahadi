package com.persiancity.game.game;

/**
 * مسیر بسته ریل قطار — یک حلقه مستطیلی دور شهر
 * قطار همیشه روی همین مسیر حرکت می‌کند.
 */
public class RailPath {
    private final float[] xs;   // ۴ گوشه
    private final float[] ys;
    private final float[] segLen = new float[4];
    public final float perimeter;

    // جهت هر پاره‌خط (واحد)
    private final float[] segDx = new float[4];
    private final float[] segDy = new float[4];

    public RailPath(float left, float top, float right, float bottom) {
        xs = new float[]{left, right, right, left};
        ys = new float[]{top, top, bottom, bottom};
        float total = 0f;
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            float dx = xs[j] - xs[i];
            float dy = ys[j] - ys[i];
            segDx[i] = dx;
            segDy[i] = dy;
            segLen[i] = (float) Math.sqrt(dx * dx + dy * dy);
            total += segLen[i];
        }
        perimeter = total;
    }

    /**
     * مکان و زاویه قطار روی مسیر در فاصله d از مبدأ
     * out: [0]=x [1]=y [2]=زاویه (رادیان — جهت حرکت)
     */
    public void posAt(float d, float[] out) {
        float dist = d % perimeter;
        if (dist < 0) dist += perimeter;
        for (int i = 0; i < 4; i++) {
            if (dist <= segLen[i]) {
                float t = dist / segLen[i];
                out[0] = xs[i] + segDx[i] * t;
                out[1] = ys[i] + segDy[i] * t;
                out[2] = (float) Math.atan2(segDy[i], segDx[i]);
                return;
            }
            dist -= segLen[i];
        }
        out[0] = xs[0];
        out[1] = ys[0];
        out[2] = 0f;
    }

    /**
     * نزدیک‌ترین فاصله روی مسیر به یک نقطه (برای ایستگاه)
     */
    public float distOfPoint(float px, float py) {
        float best = 0f;
        float bestD = Float.MAX_VALUE;
        float acc = 0f;
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            float ex = xs[j] - xs[i], ey = ys[j] - ys[i];
            float px2 = px - xs[i], py2 = py - ys[i];
            float len2 = segLen[i] * segLen[i];
            float t = G.clamp((ex * px2 + ey * py2) / len2, 0f, 1f);
            float qx = xs[i] + ex * t, qy = ys[i] + ey * t;
            float d = G.dist(px, py, qx, qy);
            if (d < bestD) {
                bestD = d;
                best = acc + segLen[i] * t;
            }
            acc += segLen[i];
        }
        return best;
    }

    public float left()   { return xs[0]; }
    public float top()    { return ys[0]; }
    public float right()  { return xs[1]; }
    public float bottom() { return ys[2]; }
}
