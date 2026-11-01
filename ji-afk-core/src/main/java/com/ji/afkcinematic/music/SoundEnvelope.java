package com.ji.afkcinematic.music;

/** Runtime amplitude only. Muting is independent from the request to retire a sound. */
public final class SoundEnvelope {
    private final float speed;
    private float volume;
    private float target;
    private boolean stopping;

    public SoundEnvelope(float target, float speed) {
        if (!Float.isFinite(speed) || speed <= 0 || speed > 1) throw new IllegalArgumentException("Invalid fade speed");
        this.speed = speed;
        this.target = bounded(target);
        this.volume = Math.min(0.01f, this.target);
    }
    public void target(float target) { if (!stopping) this.target = bounded(target); }
    public void stop() { stopping = true; target = 0; }
    public void forceStop() { stop(); volume = 0; }
    public float tick() {
        if (volume < target) volume = Math.min(target, volume + speed);
        else if (volume > target) volume = Math.max(target, volume - speed);
        return volume;
    }
    public float volume() { return volume; }
    public boolean finished() { return stopping && volume == 0; }
    private static float bounded(float value) { return Float.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0; }
}
