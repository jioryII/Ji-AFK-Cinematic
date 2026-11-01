package com.ji.afkcinematic.cinematic;

/** Shared montage clock. Native adapters own outputs; this owns cycle decisions. */
public final class SessionEdit {
    public int ticks, cycle, remaining, index;
    public enum Cut { SHOT, CYCLE, LIMIT }
    public Cut next(int shots, int maximumCycles) {
        if (shots < 1) return Cut.LIMIT;
        index = (index + 1) % shots;
        if (index != 0 || ticks <= 0) return Cut.SHOT;
        cycle++;
        return maximumCycles != -1 && cycle >= maximumCycles ? Cut.LIMIT : Cut.CYCLE;
    }
    public boolean advance(int shots, int durationTicks, int maximumCycles) {
        ticks++;
        if (--remaining > 0) return false;
        index++;
        if (index >= shots) { index = 0; cycle++; }
        remaining = durationTicks;
        return maximumCycles != -1 && cycle >= maximumCycles;
    }
    public void reset() { ticks = cycle = remaining = index = 0; }
}
