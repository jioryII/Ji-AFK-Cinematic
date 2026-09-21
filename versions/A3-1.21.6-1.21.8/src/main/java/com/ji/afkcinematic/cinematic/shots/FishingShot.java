package com.ji.afkcinematic.cinematic.shots;

import com.ji.afkcinematic.cinematic.AbstractCameraShot;
import com.ji.afkcinematic.cinematic.CameraCollisionHelper;
import com.ji.afkcinematic.cinematic.FishingApproach;
import net.minecraft.util.math.Vec3d;

/** Stable, direction-aware compositions for vanilla's fish-approach event. */
public final class FishingShot extends AbstractCameraShot {
    public enum Preset {
        FRONT_LEFT_LOW("front_left_low", -4.35f, 1.65f, 1.15f, -0.85f, 0.45f),
        FRONT_LEFT_ELEVATED("front_left_elevated", -3.85f, 3.0f, 1.35f, -0.7f, 0.55f),
        FRONT_CENTER("front_center", 0.0f, 2.15f, 4.35f, 0.65f, -0.55f),
        FRONT_RIGHT_LOW("front_right_low", 4.35f, 1.65f, 1.15f, 0.85f, 0.45f),
        FRONT_RIGHT_ELEVATED("front_right_elevated", 3.85f, 3.0f, 1.35f, 0.7f, 0.55f);

        final String id;
        final float lateral;
        final float height;
        final float front;
        final float lateralDrift;
        final float frontDrift;

        Preset(String id, float lateral, float height, float front,
               float lateralDrift, float frontDrift) {
            this.id = id;
            this.lateral = lateral;
            this.height = height;
            this.front = front;
            this.lateralDrift = lateralDrift;
            this.frontDrift = frontDrift;
        }
    }

    private Preset preset = Preset.FRONT_CENTER;
    private Vec3d playerAnchor = Vec3d.ZERO;
    private Vec3d hookAnchor = Vec3d.ZERO;
    private Vec3d forward = new Vec3d(0, 0, 1);
    private Vec3d side = new Vec3d(-1, 0, 0);
    private Vec3d target = Vec3d.ZERO;

    /** Captures the actual vanilla particle and locks a stable composition. */
    public void start(Vec3d approachParticle) {
        if (client.player == null || client.player.fishHook == null) return;
        playerAnchor = client.player.getLerpedPos(0.0f);
        hookAnchor = client.player.fishHook.getLerpedPos(0.0f);
        Vec3d horizontal = new Vec3d(hookAnchor.x - playerAnchor.x, 0, hookAnchor.z - playerAnchor.z);
        if (horizontal.lengthSquared() < 0.01) {
            double yaw = Math.toRadians(client.player.getYaw());
            horizontal = new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw));
        }
        forward = horizontal.normalize();
        side = new Vec3d(-forward.z, 0, forward.x);
        Vec3d baseTarget = playerAnchor.add(0, 1.15, 0).lerp(hookAnchor, 0.48);
        Vec3d incomingLane = new Vec3d(approachParticle.x - hookAnchor.x, 0,
                approachParticle.z - hookAnchor.z);
        if (incomingLane.lengthSquared() > 0.01) baseTarget = baseTarget.add(incomingLane.normalize().multiply(0.6));
        target = baseTarget;

        int preferredSide = FishingApproach.preferredCameraSide(playerAnchor, hookAnchor, approachParticle);
        Preset low = preferredSide > 0 ? Preset.FRONT_RIGHT_LOW : Preset.FRONT_LEFT_LOW;
        Preset elevated = preferredSide > 0 ? Preset.FRONT_RIGHT_ELEVATED : Preset.FRONT_LEFT_ELEVATED;
        if (preferredSide == 0) {
            preset = Preset.FRONT_CENTER;
        } else {
            preset = clearance(low) >= clearance(elevated) ? low : elevated;
            if (clearance(preset) < 0.55 && clearance(Preset.FRONT_CENTER) > clearance(preset)) {
                preset = Preset.FRONT_CENTER;
            }
        }
    }

    @Override public void start() {
        if (client.player != null && client.player.fishHook != null) {
            start(client.player.fishHook.getLerpedPos(0.0f).subtract(side));
        }
    }

    @Override public Vec3d updatePosition(float seconds, float speed, float tickDelta) {
        // A single constant-rate truck movement keeps the shot alive without the
        // slow-fast-slow acceleration that made the camera feel mechanical.
        float travel = Math.max(0.0f, seconds) * Math.max(0.25f, speed) / 10.0f;
        float lateral = preset.lateral + preset.lateralDrift * travel;
        float front = preset.front + preset.frontDrift * travel;
        float lift = 0.16f * travel;
        return desired(preset, lateral, front, lift);
    }

    @Override public float updatePitch(float seconds, float speed, float tickDelta) {
        Vec3d camera = updatePosition(seconds, speed, tickDelta);
        return (float) -Math.toDegrees(Math.atan2(target.y - camera.y,
                Math.hypot(target.x - camera.x, target.z - camera.z)));
    }

    @Override public float updateYaw(float seconds, float speed, float tickDelta) {
        Vec3d camera = updatePosition(seconds, speed, tickDelta);
        return (float) Math.toDegrees(Math.atan2(camera.x - target.x, target.z - camera.z));
    }

    private Vec3d desired(Preset candidate, float lateral, float front, float lift) {
        Vec3d center = playerAnchor.lerp(hookAnchor, 0.40);
        return center.add(forward.multiply(front)).add(side.multiply(lateral))
                .add(0, candidate.height + lift, 0);
    }

    private double clearance(Preset candidate) {
        Vec3d desired = desired(candidate, candidate.lateral, candidate.front, 0);
        Vec3d eye = playerAnchor.add(0, 1.62, 0);
        double distance = desired.distanceTo(eye);
        return distance < 0.01 ? 0.0
                : CameraCollisionHelper.resolveCollision(eye, desired).distanceTo(eye) / distance;
    }

    @Override public String getId() { return "fishing/" + preset.id; }
}
