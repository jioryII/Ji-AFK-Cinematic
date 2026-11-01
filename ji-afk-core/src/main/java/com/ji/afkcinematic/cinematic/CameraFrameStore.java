package com.ji.afkcinematic.cinematic;

/** Publishes only complete, finite transforms. Consumers retain one snapshot per frame. */
public final class CameraFrameStore {
    private volatile CameraFrame current = CameraFrame.ORIGIN;

    /** Returns null for an invalid candidate, so the caller can preserve the vanilla camera. */
    public CameraFrame publish(CameraFrame candidate) {
        if (candidate == null || !candidate.isFinite()) return null;
        current = candidate;
        return candidate;
    }

    public CameraFrame current() {
        return current;
    }

    public void reset() {
        current = CameraFrame.ORIGIN;
    }
}
