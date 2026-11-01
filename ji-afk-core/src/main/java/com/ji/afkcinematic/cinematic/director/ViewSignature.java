package com.ji.afkcinematic.cinematic.director;

import com.ji.afkcinematic.cinematic.CameraFrame;

/** Perceptual geometry, independent of preset identity and without any world queries. */
public record ViewSignature(double bearing, double elevation, double yaw, double pitch,
                            double size, double screenX, double screenY, double motion) {
    public static ViewSignature of(CameraFrame frame, CompositionPath.Subject subject, double motion) {
        CameraVector offset = new CameraVector(frame.x(), frame.y(), frame.z()).subtract(subject.eye());
        double distance = Math.max(.1, offset.length());
        CameraVector delta = subject.eye().subtract(new CameraVector(frame.x(), frame.y(), frame.z()));
        CameraVector forward = CameraVector.direction(frame.yaw(), frame.pitch());
        CameraVector right = CameraVector.direction(frame.yaw() + 90, 0);
        CameraVector up = right.cross(forward).normalized();
        double depth = Math.max(.1, delta.dot(forward));
        double lens = Math.tan(Math.toRadians(subject.verticalFov() / 2));
        return new ViewSignature(Math.toDegrees(Math.atan2(offset.z(), offset.x())),
                Math.toDegrees(Math.asin(offset.y() / distance)), frame.yaw(), frame.pitch(),
                Math.log(subject.height() / (distance * lens)), delta.dot(right) / (depth * lens * subject.aspect()),
                delta.dot(up) / (depth * lens), motion);
    }
    public static ViewSignature start(CompositionPath path, CompositionPath.Subject subject, double duration, double speed) {
        var start = path.sample(subject, 0, speed);
        var end = path.sample(subject, duration, speed);
        CameraVector movement = end.position().subtract(start.position());
        double motion = movement.length() < .01 ? 0 : Math.toDegrees(Math.atan2(movement.z(), movement.x()));
        return of(start.at(start.position()), subject, motion);
    }
    /** 1 means a visibly different view. A new coordinate alone is not enough. */
    public double difference(ViewSignature other) {
        double perspective = Math.max(angle(bearing, other.bearing) / 50, Math.abs(elevation - other.elevation) / 28);
        double framing = Math.max(Math.abs(screenX - other.screenX) / .45, Math.abs(screenY - other.screenY) / .4);
        double view = Math.max(angle(yaw, other.yaw) / 50, Math.abs(pitch - other.pitch) / 30);
        double scale = Math.abs(size - other.size) / .5;
        return Math.min(1, Math.max(Math.max(perspective, view), Math.max(scale, framing)) + .06 * angle(motion, other.motion) / 180);
    }
    private static double angle(double a, double b) { return Math.abs(Math.IEEEremainder(a - b, 360)); }
}
