package com.ji.afkcinematic.cinematic.shots;

import com.ji.afkcinematic.cinematic.AbstractCameraShot;
import com.ji.afkcinematic.cinematic.ShotMotion;
import net.minecraft.world.phys.Vec3;

/** Fifteen intentional character compositions evaluated at a duration-independent speed. */
public final class ComposedCharacterShot extends AbstractCameraShot {
    private static final float NOMINAL_TRAVEL_SECONDS = 12.0f;

    public enum Preset {
        EYE_LEVEL_ARC("eye_level_arc", 4.2f, 4.2f, 1.7f, 1.7f, 28, 35, 1),
        LOW_HERO_ARC("low_hero_arc", 4.8f, 4.8f, 0.45f, 0.65f, 24, 145, -1),
        HIGH_PORTRAIT("high_portrait", 4.5f, 4.2f, 3.1f, 2.8f, 18, 215, 1),
        LEFT_SHOULDER("left_shoulder", 2.6f, 2.8f, 1.75f, 1.75f, 9, 120, 1),
        RIGHT_SHOULDER("right_shoulder", 2.6f, 2.8f, 1.75f, 1.75f, 9, 240, -1),
        SLOW_PUSH_IN("slow_push_in", 7.5f, 3.8f, 1.8f, 1.8f, 4, 20, 1),
        SLOW_PULL_BACK("slow_pull_back", 3.5f, 8.5f, 1.6f, 2.2f, 5, 200, -1),
        CRANE_RISE("crane_rise", 5.5f, 7.5f, 1.0f, 6.5f, 12, 310, 1),
        CRANE_DESCENT("crane_descent", 7.5f, 5.2f, 6.0f, 1.4f, 10, 70, -1),
        AERIAL_THREE_QUARTER("aerial_three_quarter", 7.0f, 7.0f, 7.5f, 7.0f, 22, 155, 1),
        DISTANT_SILHOUETTE("distant_silhouette", 13.0f, 14.0f, 3.0f, 3.4f, 12, 265, -1),
        GROUND_TO_PORTRAIT("ground_to_portrait", 4.0f, 4.0f, 0.2f, 1.9f, 8, 330, 1),
        PROFILE_TRUCK("profile_truck", 5.5f, 5.5f, 1.65f, 1.65f, 34, 90, -1),
        TIGHT_PORTRAIT("tight_portrait", 3.0f, 3.2f, 1.8f, 1.8f, 12, 225, 1),
        HEROIC_THREE_QUARTER("heroic_three_quarter", 4.6f, 5.2f, 1.8f, 2.4f, 9, 45, 1);

        final String id;
        final float startDistance, endDistance, startHeight, endHeight, sweep, angleOffset, direction;

        Preset(String id, float startDistance, float endDistance, float startHeight,
               float endHeight, float sweep, float angleOffset, float direction) {
            this.id = id;
            this.startDistance = startDistance;
            this.endDistance = endDistance;
            this.startHeight = startHeight;
            this.endHeight = endHeight;
            this.sweep = sweep;
            this.angleOffset = angleOffset;
            this.direction = direction;
        }
    }

    private final Preset preset;
    private float startAngle;

    public ComposedCharacterShot(Preset preset) { this.preset = preset; }

    @Override
    public void start() {
        float playerYaw = client.player != null ? client.player.getYRot() : 0.0f;
        startAngle = playerYaw + preset.angleOffset;
    }

    private float travel(float elapsedSeconds, float speedMultiplier) {
        return ShotMotion.travel(elapsedSeconds, speedMultiplier, NOMINAL_TRAVEL_SECONDS);
    }

    private float angle(float phase) { return startAngle + preset.sweep * preset.direction * phase; }

    @Override
    public Vec3 updatePosition(float elapsedSeconds, float speedMultiplier, float tickDelta) {
        if (!isPlayerAvailable()) return Vec3.ZERO;
        float travel = travel(elapsedSeconds, speedMultiplier);
        float p = Math.min(1.0f, travel);
        Vec3 offset = getCircularOffset(angle(travel), lerp(p, preset.startDistance, preset.endDistance));
        return getPlayerPos(tickDelta).add(offset.x, lerp(p, preset.startHeight, preset.endHeight), offset.z);
    }

    @Override
    public float updatePitch(float elapsedSeconds, float speedMultiplier, float tickDelta) {
        float p = Math.min(1.0f, travel(elapsedSeconds, speedMultiplier));
        float distance = lerp(p, preset.startDistance, preset.endDistance);
        float height = lerp(p, preset.startHeight, preset.endHeight);
        return (float) Math.toDegrees(Math.atan2(height - 1.35f, Math.max(0.1f, distance)));
    }

    @Override
    public float updateYaw(float elapsedSeconds, float speedMultiplier, float tickDelta) {
        return angle(travel(elapsedSeconds, speedMultiplier)) - 90.0f;
    }

    @Override
    public String getId() { return "character/" + preset.id; }
}
