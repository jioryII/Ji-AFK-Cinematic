package com.ji.afkcinematic.cinematic.director;

/** The existing thirty compositions, expressed once for all native profiles. */
public enum CompositionPreset {
    EYE_LEVEL_ARC(true, Motion.ARC, 5.2, 5.6, 1.9, 2.1, 12, 35, 1),
    LOW_HERO_ARC(true, Motion.ARC, 5.5, 5.9, .8, 1.1, 10, 145, -1),
    HIGH_PORTRAIT(true, Motion.PORTRAIT, 2.2, 1.9, 2.65, 2.4, 18, 215, 1),
    LEFT_SHOULDER(true, Motion.SHOULDER, 1.8, 2.1, 1.75, 1.75, 9, -28, 1),
    RIGHT_SHOULDER(true, Motion.SHOULDER, 1.8, 2.1, 1.75, 1.75, 9, 28, -1),
    SLOW_PUSH_IN(true, Motion.DOLLY, 6.8, 5.4, 1.9, 2.0, 3, 20, 1),
    SLOW_PULL_BACK(true, Motion.DOLLY, 15, 18, 7, 8, 5, 200, -1),
    CRANE_RISE(true, Motion.CRANE, 17, 19, 9, 12, 6, 310, 1),
    CRANE_DESCENT(true, Motion.CRANE, 19, 17, 13, 10, 6, 70, -1),
    AERIAL_THREE_QUARTER(true, Motion.ARC, 22, 23, 15, 15.5, 7, 155, 1),
    DISTANT_SILHOUETTE(true, Motion.ARC, 25, 27, 9, 10, 6, 265, -1),
    GROUND_TO_PORTRAIT(true, Motion.CRANE, 2.7, 2.8, .9, 1.7, 5, 330, 1),
    PROFILE_TRUCK(true, Motion.TRUCK, 5.5, 5.8, 1.65, 1.75, 12, 90, -1),
    TIGHT_PORTRAIT(true, Motion.PORTRAIT, 1.9, 1.7, 1.8, 1.8, 12, 225, 1),
    HEROIC_THREE_QUARTER(true, Motion.ARC, 4.6, 5.2, .85, 1.4, 9, 45, 1),

    HORIZON_REVEAL(false, Motion.REVEAL, 8, 14, 2, 7, 8, 15, 1, -90, -84, -4, -10),
    GROUND_SKIM(false, Motion.TRUCK, 2.5, 3.1, .65, .85, 5, 105, -1, -85, -70, 2, -2),
    CANOPY_GLIDE(false, Motion.CRANE, 20, 23, 14, 16, 6, 195, 1, -100, -75, 18, 8),
    DISTANT_VISTA(false, Motion.ARC, 12, 12, 5, 5, 4, 285, -1, -82, -74, 6, 6),
    PARALLAX_TRUCK(false, Motion.TRUCK, 7, 9, 3, 4, 28, 45, 1, -100, -62, 4, 8),
    SKYWARD_TILT(false, Motion.REVEAL, 2.8, 3.2, 2, 2.5, 4, 135, -1, -90, -88, -8, -32),
    VALLEY_CRANE(false, Motion.CRANE, 20, 24, 12, 16, 5, 225, 1, -92, -78, 8, 24),
    WIDE_ESTABLISHING(false, Motion.ARC, 25, 27, 15, 16, 6, 315, -1, -100, -80, 12, 9),
    HORIZON_ARC(false, Motion.ARC, 11, 13, 6, 8, 35, 75, 1, -90, -55, 6, 10),
    GRAND_LANDSCAPE(false, Motion.ARC, 10, 12, 4, 5.5, 9, 165, 1, -90, -76, 5, 2),
    RIVERLINE_DRIFT(false, Motion.TRUCK, 3, 3.6, 1.2, 1.5, 6, 255, -1, -96, -72, 1, 4),
    RIDGELINE_SWEEP(false, Motion.TRUCK, 23, 26, 13, 14, 6, 345, 1, -88, -70, 10, 7),
    CAVE_MOUTH_REVEAL(false, Motion.REVEAL, 2.4, 3, 1.2, 1.8, 5, 30, -1, -108, -76, 3, -6),
    CLOUDLINE_ASCENT(false, Motion.CRANE, 24, 25, 14, 18, 5, 120, 1, -92, -84, -4, -22),
    FOREGROUND_REVEAL(false, Motion.REVEAL, 2.5, 3.2, 1.8, 2.1, 5, 210, 1, -84, -66, 2, 6);

    public enum Motion { ARC, PORTRAIT, SHOULDER, DOLLY, TRUCK, CRANE, REVEAL }
    public enum Range { CLOSE, MEDIUM, FAR }
    /** Five identities per distance band in each pool; never inferred from enum order. */
    public Range range() {
        return switch (this) {
            case HIGH_PORTRAIT, LEFT_SHOULDER, RIGHT_SHOULDER, TIGHT_PORTRAIT, GROUND_TO_PORTRAIT,
                 GROUND_SKIM, SKYWARD_TILT, RIVERLINE_DRIFT, CAVE_MOUTH_REVEAL, FOREGROUND_REVEAL -> Range.CLOSE;
            case EYE_LEVEL_ARC, LOW_HERO_ARC, SLOW_PUSH_IN, PROFILE_TRUCK, HEROIC_THREE_QUARTER,
                 HORIZON_REVEAL, PARALLAX_TRUCK, HORIZON_ARC, GRAND_LANDSCAPE, DISTANT_VISTA -> Range.MEDIUM;
            default -> Range.FAR;
        };
    }
    public final boolean character;
    public final Motion motion;
    public final double startDistance, endDistance, startHeight, endHeight, sweep, angleOffset, direction;
    public final double startYawOffset, endYawOffset, startPitch, endPitch;

    CompositionPreset(boolean character, Motion motion, double sd, double ed, double sh, double eh,
                      double sweep, double angle, double direction) {
        this(character, motion, sd, ed, sh, eh, sweep, angle, direction, 0, 0, 0, 0);
    }
    CompositionPreset(boolean character, Motion motion, double sd, double ed, double sh, double eh,
                      double sweep, double angle, double direction, double sy, double ey, double sp, double ep) {
        this.character = character; this.motion = motion;
        startDistance = sd; endDistance = ed; startHeight = sh; endHeight = eh;
        this.sweep = sweep; angleOffset = angle; this.direction = direction;
        startYawOffset = sy; endYawOffset = ey; startPitch = sp; endPitch = ep;
    }
    public String id() { return (character ? "character/" : "environment/") + name().toLowerCase(java.util.Locale.ROOT); }
    public double requiredDepth() {
        if (character) return motion == Motion.SHOULDER ? 6 : motion == Motion.PORTRAIT ? 3 : 5;
        return this == CAVE_MOUTH_REVEAL || this == FOREGROUND_REVEAL || this == GROUND_SKIM ? 5 : 12;
    }
}
