package dev.abruptsteve.centuryrafflehelper.config;

import com.google.gson.annotations.Expose;

public class HudPosition {
    private static final float DEFAULT_SCALE = 1.0f;
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 4.0f;

    @Expose
    public int x;

    @Expose
    public int y;

    @Expose
    public float scale = DEFAULT_SCALE;

    public HudPosition() {
        this(10, 10);
    }

    public HudPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public HudPosition(int x, int y, float scale) {
        this.x = x;
        this.y = y;
        this.scale = scale;
    }

    public float scale() {
        if (scale <= 0.0f) {
            scale = DEFAULT_SCALE;
        }
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    public void adjustScale(float delta) {
        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale() + delta));
    }

    public void reset(int defaultX, int defaultY) {
        reset(defaultX, defaultY, DEFAULT_SCALE);
    }

    public void reset(int defaultX, int defaultY, float defaultScale) {
        x = defaultX;
        y = defaultY;
        scale = defaultScale;
    }
}
