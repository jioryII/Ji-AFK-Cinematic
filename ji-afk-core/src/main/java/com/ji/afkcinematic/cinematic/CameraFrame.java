package com.ji.afkcinematic.cinematic;

/** A complete camera transform, independent of the game and renderer. */
public record CameraFrame(double x, double y, double z, float pitch, float yaw, float roll) {
    public static final CameraFrame ORIGIN = new CameraFrame(0, 0, 0, 0, 0, 0);

    public boolean isFinite() {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && Float.isFinite(pitch) && Float.isFinite(yaw) && Float.isFinite(roll);
    }
}
