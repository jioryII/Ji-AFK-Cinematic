package com.ji.afkcinematic.cinematic.director;

import com.ji.afkcinematic.cinematic.CameraFrame;

/** Constant-rate, pose-aware shot execution. Variants stay fixed for one take. */
public record CompositionPath(CompositionPreset preset, Variation variation, double anchorYaw, double horizon) {
    public CompositionPath(CompositionPreset preset, Variation variation, double anchorYaw) {
        this(preset, variation, anchorYaw, Double.NaN);
    }
    public record Subject(CameraVector position, double eyeHeight, double height, double verticalFov, double aspect) {
        public Subject {
            eyeHeight = clamp(eyeHeight, .2, 3);
            height = clamp(height, .3, 4);
            verticalFov = clamp(verticalFov, 30, 110);
            aspect = clamp(aspect, .5, 4);
        }
        public CameraVector eye() { return position.add(new CameraVector(0, eyeHeight, 0)); }
        public double horizontalFov() { return Math.toDegrees(2 * Math.atan(Math.tan(Math.toRadians(verticalFov / 2)) * aspect)); }
    }
    public record Variation(double angle, double scale, double heightScale, double direction, double framingSide, double motionScale) {
        public Variation(double angle, double scale, double heightScale, double direction, double framingSide) {
            this(angle, scale, heightScale, direction, framingSide, 1);
        }
        public static final Variation DEFAULT = new Variation(0, 1, 1, 1, 0);
    }
    public record Pose(CameraVector position, CameraVector target, double yawBias, double pitchBias) {
        public CameraFrame at(CameraVector resolved) {
            CameraVector delta = target.subtract(resolved);
            return new CameraFrame(resolved.x(), resolved.y(), resolved.z(),
                    (float) (-Math.toDegrees(Math.atan2(delta.y(), Math.hypot(delta.x(), delta.z()))) + pitchBias),
                    (float) (Math.toDegrees(Math.atan2(-delta.x(), delta.z())) + yawBias), 0);
        }
    }

    public Pose sample(Subject subject, double elapsed, double speed) {
        // At the established defaults (10 s, speed .5), a complete move is visible.
        // Velocity still depends on speed, not on the configured shot duration.
        double sweepDistance = Math.toRadians(preset.sweep) * Math.max(preset.startDistance, preset.endDistance);
        double translation = Math.hypot(preset.endDistance - preset.startDistance, preset.endHeight - preset.startHeight);
        double turn = Math.max(preset.sweep, Math.abs(preset.endYawOffset - preset.startYawOffset));
        // Constant-rate motion, with a common gentle translation/angular budget.
        // The speed slider remains useful; even its upper end stays cinematic.
        double seconds = Math.max(14, Math.max(Math.max(translation, sweepDistance) / .32, turn / 2.2));
        double travel = Math.max(0, elapsed) * clamp(speed, 0, 1) * com.ji.afkcinematic.cinematic.ShotMotion.PACE_ADJUSTMENT / seconds;
        double p = Math.min(1, travel);
        if (Double.isFinite(horizon)) {
            double distance = switch (preset.range()) { case CLOSE -> 3.2; case MEDIUM -> 8; case FAR -> 24; };
            double lift = switch (preset.range()) { case CLOSE -> 1.8; case MEDIUM -> 3.2; case FAR -> 11; };
            CameraVector forward = CameraVector.direction(horizon, 0);
            CameraVector side = CameraVector.direction(horizon + 90, 0);
            CameraVector position = subject.position.add(forward.scale(-distance * variation.scale))
                    .add(new CameraVector(0, lift * variation.heightScale, 0))
                    .add(side.scale(variation.framingSide * distance * .12 + (p - .5) * .8 * variation.direction));
            CameraVector target = preset.character ? subject.eye().add(forward.scale(distance * .08))
                    : position.add(CameraVector.direction(horizon + variation.angle * .04, -3).scale(40));
            return new Pose(position, target, 0, preset.character ? -3 : 0);
        }
        if (!preset.character && variation.motionScale < .5 && variation.scale < .5) {
            // A contained environment view follows the probed open heading. Scaling a
            // wide preset alone retains its large orientation change and faces a side
            // wall by the final sample in narrow corridors.
            double heading = anchorYaw + preset.angleOffset + variation.angle - 90;
            CameraVector forward = CameraVector.direction(heading, 0);
            CameraVector lateral = CameraVector.direction(heading + 90, 0);
            double drift = (p - .5) * .16 * variation.direction;
            CameraVector position = subject.eye().add(forward.scale(.45)).add(lateral.scale(drift));
            double pan = preset.motion == CompositionPreset.Motion.TRUCK ? 0 : (p - .5) * 4 * variation.direction;
            return new Pose(position, position.add(CameraVector.direction(heading + pan, 0).scale(16)), 0, 0);
        }
        double scale = variation.scale;
        double heightScale = variation.heightScale;
        double angle = anchorYaw + preset.angleOffset + variation.angle;
        double sweep = preset.sweep * preset.direction * variation.direction * variation.motionScale;
        if (preset == CompositionPreset.PROFILE_TRUCK) angle = anchorYaw + variation.angle;
        if (preset.motion == CompositionPreset.Motion.PORTRAIT)
            angle = anchorYaw + 90 + clamp(variation.angle + sweep * p, -60, 60);
        if (preset.motion == CompositionPreset.Motion.SHOULDER) angle = anchorYaw - 90 + preset.angleOffset + variation.angle * .25;
        if (preset.motion != CompositionPreset.Motion.TRUCK && preset.motion != CompositionPreset.Motion.SHOULDER
                && preset.motion != CompositionPreset.Motion.PORTRAIT) angle += sweep * p;
        double distance = lerp(p, preset.startDistance, preset.endDistance) * scale;
        boolean compact = preset.character && variation.motionScale < .5 && variation.scale < .5;
        if (compact && preset.motion != CompositionPreset.Motion.PORTRAIT) {
            distance *= clamp(Math.tan(Math.toRadians(35)) / Math.tan(Math.toRadians(subject.verticalFov / 2)), .7, 1.4);
        }
        double radians = Math.toRadians(angle);
        CameraVector offset = new CameraVector(Math.cos(radians) * distance,
                lerp(p, preset.startHeight, preset.endHeight) * heightScale, Math.sin(radians) * distance);
        if (preset.motion == CompositionPreset.Motion.TRUCK) {
            // A real lateral truck, distinct from another arc around the player.
            double lateral = (p - .5) * Math.toRadians(sweep) * distance;
            offset = offset.add(new CameraVector(-Math.sin(radians) * lateral, 0, Math.cos(radians) * lateral));
        }
        if (preset.character) {
            // Pose changes, including bed/crouch/swim, change the reference height.
            double poseScale = clamp(subject.eyeHeight / 1.62, .3, 1.5);
            offset = new CameraVector(offset.x(), offset.y() * poseScale, offset.z());
        }
        CameraVector position = subject.position.add(offset);
        if (!preset.character) {
            double yaw = angle + lerp(p, preset.startYawOffset, preset.endYawOffset);
            double pitch = lerp(p, preset.startPitch, preset.endPitch);
            return new Pose(position, position.add(CameraVector.direction(yaw, pitch).scale(16)), 0, 0);
        }
        double targetHeight = subject.eyeHeight * (preset.motion == CompositionPreset.Motion.PORTRAIT || compact ? .94 : .78);
        if (preset == CompositionPreset.GROUND_TO_PORTRAIT) targetHeight = subject.eyeHeight * lerp(p, .4, .96);
        CameraVector target = subject.position.add(new CameraVector(0, targetHeight, 0));
        if (preset.motion == CompositionPreset.Motion.PORTRAIT) {
            // Compensate the complete eye-to-camera vector. Horizontal-only scaling
            // changes the high portrait's angle and leaves the face small at wide FOV.
            // The user's lens stays untouched; perspective determines the distance.
            CameraVector toCamera = position.subtract(target);
            double length = toCamera.length();
            CameraVector axis = toCamera.normalized();
            if (subject.verticalFov != 70) {
                double desiredSize = projectedHeadHeight(subject, target, axis, length, 70);
                double low = .3, high = Math.max(16, length * 4);
                // Bounded arithmetic only, no scene queries. A finite head volume
                // needs perspective projection; point-distance ratios overfill the
                // image at wide FOV. Keep the original elevation while solving size.
                for (int i = 0; i < 16; i++) {
                    double middle = (low + high) * .5;
                    if (projectedHeadHeight(subject, target, axis, middle, subject.verticalFov) > desiredSize) low = middle;
                    else high = middle;
                }
                position = target.add(axis.scale((low + high) * .5));
            }
        }
        if (preset.motion == CompositionPreset.Motion.SHOULDER) {
            target = target.add(CameraVector.direction(anchorYaw, 0).scale(2.5));
        }
        // Modest off-centre framing; the selector checks subject visibility in the actual FOV.
        double yawBias = preset.motion == CompositionPreset.Motion.SHOULDER ? 0 : variation.framingSide * subject.horizontalFov() * .055;
        return new Pose(position, target, yawBias, 0);
    }

    public static double clamp(double value, double min, double max) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
    }
    private static double projectedHeadHeight(Subject subject, CameraVector target, CameraVector axis, double distance, double fov) {
        // Vertical camera basis for forward=-axis, with zero roll.
        double horizontal = Math.hypot(axis.x(), axis.z());
        double upX = -axis.x() * axis.y() / horizontal;
        double upY = horizontal;
        double upZ = -axis.z() * axis.y() / horizontal;
        double radius = Math.min(.25, subject.height / 3);
        double bottom = Math.max(.05, subject.eyeHeight - .38);
        double top = Math.min(subject.height, subject.eyeHeight + .18);
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        double tan = Math.tan(Math.toRadians(fov / 2));
        for (int corner = 0; corner < 8; corner++) {
            double x = subject.position.x() + ((corner & 1) == 0 ? -radius : radius) - target.x();
            double y = subject.position.y() + ((corner & 2) == 0 ? bottom : top) - target.y();
            double z = subject.position.z() + ((corner & 4) == 0 ? -radius : radius) - target.z();
            double depth = distance - (x * axis.x() + y * axis.y() + z * axis.z());
            if (depth <= .01) return Double.POSITIVE_INFINITY;
            double screenY = (x * upX + y * upY + z * upZ) / (2 * depth * tan);
            min = Math.min(min, screenY); max = Math.max(max, screenY);
        }
        return max - min;
    }
    private static double lerp(double p, double start, double end) { return start + (end - start) * p; }
}
