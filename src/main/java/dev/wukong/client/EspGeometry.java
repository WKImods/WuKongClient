package dev.wukong.client;

/** GUI-space line clipping. No Minecraft dependencies; deterministic and testable. */
public final class EspGeometry {
    public record Line(float x0, float y0, float x1, float y1) {}

    /** Liang-Barsky segment clipping to rectangle [0,width] x [0,height]. */
    public static Line clip(float x0, float y0, float x1, float y1, float width, float height) {
        if (!Float.isFinite(x0) || !Float.isFinite(y0) ||
            !Float.isFinite(x1) || !Float.isFinite(y1)) return null;
        float dx = x1 - x0, dy = y1 - y0;
        float[] p = {-dx, dx, -dy, dy};
        float[] q = {x0, width - x0, y0, height - y0};
        float start = 0f, end = 1f;
        for (int i = 0; i < 4; i++) {
            if (Math.abs(p[i]) < 0.000001f) {
                if (q[i] < 0f) return null;
            } else {
                float t = q[i] / p[i];
                if (p[i] < 0f) start = Math.max(start, t);
                else end = Math.min(end, t);
                if (start > end) return null;
            }
        }
        return new Line(x0 + start * dx, y0 + start * dy,
                x0 + end * dx, y0 + end * dy);
    }

    private EspGeometry() {}
}
