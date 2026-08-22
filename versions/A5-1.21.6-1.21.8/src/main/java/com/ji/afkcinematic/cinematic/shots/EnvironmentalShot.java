package com.ji.afkcinematic.cinematic.shots;

import com.ji.afkcinematic.cinematic.AbstractCameraShot;
import com.ji.afkcinematic.cinematic.ShotMotion;
import net.minecraft.util.math.Vec3d;

/** Fifteen intentional environmental compositions evaluated at a duration-independent speed. */
public final class EnvironmentalShot extends AbstractCameraShot {
    private static final float NOMINAL_TRAVEL_SECONDS = 12.0f;

    public enum Preset {
        HORIZON_REVEAL("horizon_reveal", 8, 14, 2, 7, 8, 15, 1, -90, -84, -4, -10),
        GROUND_SKIM("ground_skim", 5, 10, 0.35f, 0.8f, 12, 105, -1, -85, -70, 2, -2),
        CANOPY_GLIDE("canopy_glide", 9, 13, 11, 14, 18, 195, 1, -100, -75, 18, 8),
        DISTANT_VISTA("distant_vista", 12, 12, 5, 5, 4, 285, -1, -82, -74, 6, 6),
        PARALLAX_TRUCK("parallax_truck", 7, 9, 3, 4, 28, 45, 1, -100, -62, 4, 8),
        SKYWARD_TILT("skyward_tilt", 6, 8, 2, 6, 5, 135, -1, -90, -88, -8, -32),
        VALLEY_CRANE("valley_crane", 6, 15, 3, 16, 10, 225, 1, -92, -78, 8, 24),
        WIDE_ESTABLISHING("wide_establishing", 16, 18, 10, 12, 14, 315, -1, -100, -80, 12, 9),
        HORIZON_ARC("horizon_arc", 11, 13, 6, 8, 35, 75, 1, -90, -55, 6, 10),
        GRAND_LANDSCAPE("grand_landscape", 10, 12, 4, 5.5f, 9, 165, 1, -90, -76, 5, 2),
        RIVERLINE_DRIFT("riverline_drift", 7, 11, 1.2f, 2.0f, 22, 255, -1, -96, -72, 1, 4),
        RIDGELINE_SWEEP("ridgeline_sweep", 15, 17, 9, 11, 11, 345, 1, -88, -70, 10, 7),
        CAVE_MOUTH_REVEAL("cave_mouth_reveal", 5, 9, 1.0f, 2.5f, 16, 30, -1, -108, -76, 3, -6),
        CLOUDLINE_ASCENT("cloudline_ascent", 10, 13, 8, 17, 9, 120, 1, -92, -84, -4, -22),
        FOREGROUND_REVEAL("foreground_reveal", 6, 8, 2.0f, 3.6f, 13, 210, 1, -84, -66, 2, 6);

        final String id;
        final float startDistance, endDistance, startHeight, endHeight, sweep;
        final float angleOffset, direction, startYawOffset, endYawOffset, startPitch, endPitch;

        Preset(String id, float startDistance, float endDistance, float startHeight,
               float endHeight, float sweep, float angleOffset, float direction,
               float startYawOffset, float endYawOffset, float startPitch, float endPitch) {
            this.id = id;
            this.startDistance = startDistance;
            this.endDistance = endDistance;
            this.startHeight = startHeight;
            this.endHeight = endHeight;
            this.sweep = sweep;
            this.angleOffset = angleOffset;
            this.direction = direction;
            this.startYawOffset = startYawOffset;
            this.endYawOffset = endYawOffset;
            this.startPitch = startPitch;
            this.endPitch = endPitch;
        }
    }

    private final Preset preset;
    private float startAngle;

    public EnvironmentalShot(Preset preset) {
        this.preset = preset;
    }

    @Override
    public void start() {
        float playerYaw = client.player != null ? client.player.getYaw() : 0.0f;
        startAngle = playerYaw + preset.angleOffset;
    }

    private float travel(float elapsedSeconds, float speedMultiplier) {
        return ShotMotion.travel(elapsedSeconds, speedMultiplier, NOMINAL_TRAVEL_SECONDS);
    }

    private float angle(float phase) {
        return startAngle + phase * preset.sweep * preset.direction;
    }

    @Override
    public Vec3d updatePosition(float elapsedSeconds, float speedMultiplier, float tickDelta) {
        if (!isPlayerAvailable()) return Vec3d.ZERO;
        float travel = travel(elapsedSeconds, speedMultiplier);
        float p = Math.min(1.0f, travel);
        Vec3d offset = getCircularOffset(angle(travel),
                lerp(p, preset.startDistance, preset.endDistance));
        return getPlayerPos(tickDelta).add(offset.x,
                lerp(p, preset.startHeight, preset.endHeight), offset.z);
    }

    @Override
    public float updatePitch(float elapsedSeconds, float speedMultiplier, float tickDelta) {
        float p = Math.min(1.0f, travel(elapsedSeconds, speedMultiplier));
        return lerp(p, preset.startPitch, preset.endPitch);
    }

    @Override
    public float updateYaw(float elapsedSeconds, float speedMultiplier, float tickDelta) {
        float travel = travel(elapsedSeconds, speedMultiplier);
        float p = Math.min(1.0f, travel);
        return angle(travel) + lerp(p, preset.startYawOffset, preset.endYawOffset) * preset.direction;
    }

    @Override
    public String getId() {
        return "environment/" + preset.id;
    }
}
