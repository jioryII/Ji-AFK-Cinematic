package com.ji.afkcinematic.random;

import java.util.EnumMap;
import java.util.random.RandomGenerator;
import java.util.SplittableRandom;

/** Independent non-cryptographic streams. Clock granularity cannot bias a decision. */
public final class RandomStreams {
    public enum Domain { MUSIC, MUSIC_SOURCE, CAMERA_PLAN, CAMERA_VIEW, HORIZON, FISHING, COSMETIC }
    private static final RandomStreams SESSION = new RandomStreams(new java.security.SecureRandom().nextLong());
    private final EnumMap<Domain, RandomGenerator> streams = new EnumMap<>(Domain.class);
    public RandomStreams(long seed) {
        for (Domain domain : Domain.values()) streams.put(domain,
                // Direct construction avoids ServiceLoader provider duplication in mod module layers.
                new SplittableRandom(mix(seed ^ ((domain.ordinal() + 1L) * 0x9e3779b97f4a7c15L))));
    }
    public static long seed(Domain domain) { return SESSION.nextSeed(domain); }
    public synchronized long nextSeed(Domain domain) { return streams.get(domain).nextLong(); }
    public static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
