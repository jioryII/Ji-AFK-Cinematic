package com.ji.afkcinematic.cinematic.director;

/** No background work: at most two horizon shots per cycle, three cycles per event. */
public final class HorizonAwards {
    private long event = Long.MIN_VALUE;
    private int lastCycle = -1, cycles, remaining;
    public void reset() { event = Long.MIN_VALUE; lastCycle = -1; cycles = remaining = 0; }
    public double heading(long dayTime, int cycle, long seed, CompositionPreset.Range range) {
        var random = new java.util.SplittableRandom(com.ji.afkcinematic.random.RandomStreams.mix(seed));
        long day = Math.floorDiv(dayTime, 24000);
        long time = Math.floorMod(dayTime, 24000);
        boolean dawn = time >= 22500 || time <= 500;
        boolean dusk = time >= 11500 && time <= 13500;
        if (!dawn && !dusk) return Double.NaN;
        long key = dawn ? (time <= 500 ? day - 1 : day) * 2 : day * 2 + 1;
        if (key != event) { event = key; cycles = 0; lastCycle = -1; remaining = 0; }
        if (cycle != lastCycle) {
            lastCycle = cycle;
            remaining = cycles++ < 3 ? 1 + random.nextInt(2) : 0;
        }
        if (remaining == 0) return Double.NaN;
        // Close and medium predominate; far remains possible, without adding extra takes.
        if (range == CompositionPreset.Range.FAR && random.nextInt(4) != 0) return Double.NaN;
        if (random.nextInt(4) == 0 && remaining == 2) return Double.NaN;
        remaining--;
        return dawn ? -90 : 90;
    }
}
