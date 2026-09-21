package com.ji.afkcinematic.cinematic.shots;

import com.ji.afkcinematic.cinematic.AbstractCameraShot;
import com.ji.afkcinematic.cinematic.CameraCollisionHelper;
import com.ji.afkcinematic.cinematic.FishingApproach;
import net.minecraft.world.phys.Vec3;

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
    private Vec3 playerAnchor = Vec3.ZERO;
    private Vec3 hookAnchor = Vec3.ZERO;
    private Vec3 forward = new Vec3(0, 0, 1);
    private Vec3 side = new Vec3(-1, 0, 0);
    private Vec3 target = Vec3.ZERO;

    /** Captures the actual vanilla particle and locks a stable composition. */
    public void start(Vec3 approachParticle) {
        if (client.player == null || client.player.fishing == null) return;
        playerAnchor = client.player.position();
        hookAnchor = client.player.fishing.position();
        Vec3 horizontal = new Vec3(hookAnchor.x - playerAnchor.x, 0, hookAnchor.z - playerAnchor.z);
        if (horizontal.lengthSqr() < 0.01) {
            double yaw = Math.toRadians(client.player.getYRot());
            horizontal = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        }
        forward = horizontal.normalize();
        side = new Vec3(-forward.z, 0, forward.x);
        Vec3 baseTarget = playerAnchor.add(0, 1.15, 0).lerp(hookAnchor, 0.48);
        Vec3 incomingLane = new Vec3(approachParticle.x - hookAnchor.x, 0,
                approachParticle.z - hookAnchor.z);
        if (incomingLane.lengthSqr() > 0.01) baseTarget = baseTarget.add(incomingLane.normalize().scale(0.6));
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
        if (client.player != null && client.player.fishing != null) {
            start(client.player.fishing.position().subtract(side));
        }
    }

    @Override public Vec3 updatePosition(float seconds, float speed, float tickDelta) {
        // A single constant-rate truck movement keeps the shot alive without the
        // slow-fast-slow acceleration that made the camera feel mechanical.
        float travel = Math.max(0.0f, seconds) * Math.max(0.25f, speed) / 10.0f;
        float lateral = preset.lateral + preset.lateralDrift * travel;
        float front = preset.front + preset.frontDrift * travel;
        float lift = 0.16f * travel;
        return desired(preset, lateral, front, lift);
    }

    @Override public float updatePitch(float seconds, float speed, float tickDelta) {
        Vec3 camera = updatePosition(seconds, speed, tickDelta);
        return (float) -Math.toDegrees(Math.atan2(target.y - camera.y,
                Math.hypot(target.x - camera.x, target.z - camera.z)));
    }

    @Override public float updateYaw(float seconds, float speed, float tickDelta) {
        Vec3 camera = updatePosition(seconds, speed, tickDelta);
        return (float) Math.toDegrees(Math.atan2(camera.x - target.x, target.z - camera.z));
    }

    private Vec3 desired(Preset candidate, float lateral, float front, float lift) {
        Vec3 center = playerAnchor.lerp(hookAnchor, 0.40);
        return center.add(forward.scale(front)).add(side.scale(lateral))
                .add(0, candidate.height + lift, 0);
    }

    private double clearance(Preset candidate) {
        Vec3 desired = desired(candidate, candidate.lateral, candidate.front, 0);
        Vec3 eye = playerAnchor.add(0, 1.62, 0);
        double distance = desired.distanceTo(eye);
        return distance < 0.01 ? 0.0
                : CameraCollisionHelper.resolveCollision(eye, desired).distanceTo(eye) / distance;
    }

    @Override public String getId() { return "fishing/" + preset.id; }
}
