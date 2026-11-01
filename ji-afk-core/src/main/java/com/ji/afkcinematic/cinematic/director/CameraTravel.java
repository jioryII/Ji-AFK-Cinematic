package com.ji.afkcinematic.cinematic.director;

/** Sweeps the camera itself, never the invisible subject-to-camera tether. */
public final class CameraTravel {
    private CameraVector previous;
    private CameraVector previousSubject;
    private int detour;
    public void reset() { previous = null; previousSubject = null; detour = 0; }
    public CameraVector move(CameraVector desired, double seconds, CompositionDirector.Scene scene) {
        return move(desired, CameraVector.ZERO, seconds, scene);
    }
    /** Subject translation is carried at its actual speed; only the shot's relative motion is capped. */
    public CameraVector move(CameraVector desired, CameraVector subject, double seconds, CompositionDirector.Scene scene) {
        if (previous == null) { previous = desired; previousSubject = subject; return desired; }
        double dt = CompositionPath.clamp(seconds, 0, .1);
        CameraVector carry = subject.subtract(previousSubject);
        previousSubject = subject;
        CameraVector relative = desired.subtract(previous.add(carry));
        double relativeDistance = relative.length();
        double shotStep = Math.min(relativeDistance, .48 * com.ji.afkcinematic.cinematic.ShotMotion.PACE_ADJUSTMENT * dt);
        CameraVector delta = carry.add(relative.normalized().scale(shotStep));
        double distance = delta.length();
        if (distance < 1e-7) return previous;
        CameraVector direction = delta.scale(1 / distance);
        // Look ahead along travel, slow before an obstacle; do not retract towards the player.
        double preview = Math.min(1.6, Math.max(.5, distance + .8));
        double clear = scene.clearDistance(previous, direction, preview);
        double braking = clear >= preview - 1e-6 ? 1 : CompositionPath.clamp((clear - .22) / .8, 0, 1);
        double step = distance * braking;
        CameraVector next = previous.add(direction.scale(step));
        if (braking < .35) {
            CameraVector lateral = new CameraVector(-direction.z(), 0, direction.x()).normalized();
            if (detour == 0) {
                double left = scene.clearDistance(previous, lateral, .65);
                double right = scene.clearDistance(previous, lateral.scale(-1), .65);
                detour = left >= right ? 1 : -1;
            }
            CameraVector tangent = lateral.scale(detour);
            if (scene.clearDistance(previous, tangent, .6) > .45)
                next = next.add(tangent.scale(.18 * dt * (1 - braking)));
        } else if (braking > .9) detour = 0;
        CameraVector safe = CompositionDirector.resolveCollision(previous, next, scene);
        previous = safe;
        return safe;
    }
}
