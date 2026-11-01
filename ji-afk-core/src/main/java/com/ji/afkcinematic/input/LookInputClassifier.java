package com.ji.afkcinematic.input;

/** Separates accepted pre-inertia mouse input from vanilla's applied rotation.
 * Unknown/provider rotation remains observable, including slow accumulated turns.
 * Native adapters call excludeVanillaTurn only around vanilla's own player.turn invocation.
 */
public final class LookInputClassifier {
    private final LookIntentTracker other = new LookIntentTracker();
    private double mouseYaw, mousePitch;

    public boolean acceptedMouse(double yawDegrees, double pitchDegrees) {
        if (!Double.isFinite(yawDegrees) || !Double.isFinite(pitchDegrees)) return false;
        mouseYaw += yawDegrees;
        mousePitch += pitchDegrees;
        // Accepted deltas are displacements. Wrapping them as orientations would hide
        // a legitimate whole-turn input, while unknown player orientation remains wrapped.
        if (Math.hypot(mouseYaw, mousePitch) <= LookIntentTracker.ANGULAR_TOLERANCE_DEGREES) return false;
        mouseYaw = mousePitch = 0;
        return true;
    }

    public void excludeVanillaTurn(double beforeYaw, double beforePitch, double afterYaw, double afterPitch) {
        other.excludeRotation(beforeYaw, beforePitch, afterYaw, afterPitch);
    }

    public boolean rotation(double yaw, double pitch) { return other.rotation(yaw, pitch); }
    public boolean cursor(double x, double y) { return other.cursor(x, y); }
    public void rebaseCursor(double x, double y) { other.rebaseCursor(x, y); }
    public void rebaseRotation(double yaw, double pitch) {
        other.rebaseRotation(yaw, pitch);
        mouseYaw = mousePitch = 0;
    }
    public void reset() {
        other.reset(); mouseYaw = mousePitch = 0;
    }
}
