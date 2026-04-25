package com.ji.afkcinematic.cinematic;

import net.minecraft.util.math.Vec3d;

public interface CameraShot {
    /** Called when this shot begins — pre-calculate angles, directions, etc. */
    void start();

    /**
     * Updates and returns the camera position for the current progress.
     * @param progress 0.0 to 1.0 representing time through the shot
     * @param speedMultiplier camera speed multiplier from config (1.0 = normal)
     * @param tickDelta sub-tick interpolation for smooth tracking of moving entities
     */
    Vec3d updatePosition(float progress, float speedMultiplier, float tickDelta);

    /**
     * Returns the camera pitch (up/down) in degrees.
     * @param progress 0.0 to 1.0
     * @param speedMultiplier camera speed multiplier
     * @param tickDelta sub-tick interpolation
     */
    float updatePitch(float progress, float speedMultiplier, float tickDelta);

    /**
     * Returns the camera yaw (left/right) in degrees.
     * @param progress 0.0 to 1.0
     * @param speedMultiplier camera speed multiplier
     * @param tickDelta sub-tick interpolation
     */
    float updateYaw(float progress, float speedMultiplier, float tickDelta);
}

