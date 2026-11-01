package com.ji.afkcinematic.cinematic;

/** Per-session fishing decisions, independent of a native entity or world. */
public final class FishingFocusState {
    public int startTicks = -1, focusTicks, sinceApproach, postBite, postCast;
    public boolean bite, seen, focus, cinematic, blocked;
    public void approach() { sinceApproach = 0; focus = true; bite = seen = false; focusTicks = postBite = postCast = 0; }
    /** True once vanilla bite recovery or approach expiration ends the focus. */
    public boolean update(boolean bitingNow) {
        if (!focus) return false;
        focusTicks++; sinceApproach++; bite = bitingNow;
        if (bite) { seen = true; postBite = 0; return false; }
        return seen ? ++postBite >= 100 : sinceApproach > 12;
    }
    public void reset() {
        startTicks = -1; focusTicks = sinceApproach = postBite = postCast = 0;
        bite = seen = focus = cinematic = blocked = false;
    }
}
