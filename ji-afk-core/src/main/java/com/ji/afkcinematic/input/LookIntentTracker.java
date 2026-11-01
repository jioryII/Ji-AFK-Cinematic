package com.ji.afkcinematic.input;

/** Tracks net displacement from a stable anchor rather than counting noisy callbacks. */
public final class LookIntentTracker {
    // Initial policy: less than one default-sensitivity cursor pixel. The client
    // baseline records 0.5 px = 0.075 degrees and 1 px = 0.15 degrees.
    public static final double ANGULAR_TOLERANCE_DEGREES = 0.1F;
    public static final double GUI_CURSOR_TOLERANCE_PIXELS = 2.0;
    private boolean rotationReady;
    private boolean cursorReady;
    private double yawAnchor;
    private double pitchAnchor;
    private double cursorXAnchor;
    private double cursorYAnchor;

    public boolean rotation(double yaw, double pitch) {
        if (!Double.isFinite(yaw) || !Double.isFinite(pitch)) {
            rotationReady = false;
            return false;
        }
        if (!rotationReady) {
            rebaseRotation(yaw, pitch);
            return false;
        }
        double dy = wrappedYaw(yaw - yawAnchor);
        double dp = pitch - pitchAnchor;
        if (dy * dy + dp * dp <= ANGULAR_TOLERANCE_DEGREES * ANGULAR_TOLERANCE_DEGREES) {
            return false;
        }
        rebaseRotation(yaw, pitch);
        return true;
    }

    public boolean cursor(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            cursorReady = false;
            return false;
        }
        if (!cursorReady) {
            rebaseCursor(x, y);
            return false;
        }
        double dx = x - cursorXAnchor;
        double dy = y - cursorYAnchor;
        if (dx * dx + dy * dy <= GUI_CURSOR_TOLERANCE_PIXELS * GUI_CURSOR_TOLERANCE_PIXELS) {
            return false;
        }
        rebaseCursor(x, y);
        return true;
    }

    public void rebaseRotation(double yaw, double pitch) {
        rotationReady = Double.isFinite(yaw) && Double.isFinite(pitch);
        yawAnchor = yaw;
        pitchAnchor = pitch;
    }

    public void rebaseCursor(double x, double y) {
        cursorReady = Double.isFinite(x) && Double.isFinite(y);
        cursorXAnchor = x;
        cursorYAnchor = y;
    }

    /** Subtract one known application while retaining accumulated, unrelated slow rotation. */
    public void excludeRotation(double beforeYaw, double beforePitch, double afterYaw, double afterPitch) {
        if (!Double.isFinite(beforeYaw) || !Double.isFinite(beforePitch)
                || !Double.isFinite(afterYaw) || !Double.isFinite(afterPitch)) {
            rotationReady = false;
            return;
        }
        if (!rotationReady) { rebaseRotation(afterYaw, afterPitch); return; }
        yawAnchor += wrappedYaw(afterYaw - beforeYaw);
        pitchAnchor += afterPitch - beforePitch;
    }

    public void reset() {
        rotationReady = false;
        cursorReady = false;
    }

    private static double wrappedYaw(double delta) {
        double wrapped = delta % 360.0;
        if (wrapped >= 180.0) wrapped -= 360.0;
        if (wrapped < -180.0) wrapped += 360.0;
        return wrapped;
    }
}
