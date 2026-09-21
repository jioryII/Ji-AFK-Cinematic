package com.ji.afkcinematic.cinematic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Builds the fifteen-shot edit without depending on Minecraft runtime classes. */
public final class ShotSequencePlanner {
    public static final int VISIBLE_SHOT_COUNT = 15;

    private ShotSequencePlanner() {}

    /**
     * Returns pool indexes where character shots are [0, characterPoolSize) and
     * environment shots begin at characterPoolSize. Every selected preset is unique.
     */
    public static int[] plan(int characterPercentage, int characterPoolSize,
                             int environmentPoolSize, long seed) {
        if (characterPoolSize < VISIBLE_SHOT_COUNT || environmentPoolSize < VISIBLE_SHOT_COUNT) {
            throw new IllegalArgumentException("Both shot pools must contain at least fifteen presets");
        }

        int normalized = normalizePercentage(characterPercentage);
        Random random = new Random(seed);
        float exactCharacterCount = VISIBLE_SHOT_COUNT * normalized / 100.0f;
        int characterCount = (int) Math.floor(exactCharacterCount);
        if (unitInterval(seed) < exactCharacterCount - characterCount) characterCount++;
        int environmentCount = VISIBLE_SHOT_COUNT - characterCount;

        List<Integer> characters = indexes(0, characterPoolSize);
        List<Integer> environments = indexes(characterPoolSize, environmentPoolSize);
        Collections.shuffle(characters, random);
        Collections.shuffle(environments, random);

        // Evenly distribute the minority category like an editor spacing cutaways,
        // then rotate the pattern so repeated cinematics do not open identically.
        boolean[] characterSlots = new boolean[VISIBLE_SHOT_COUNT];
        for (int i = 0; i < VISIBLE_SHOT_COUNT; i++) {
            characterSlots[i] = ((i + 1) * characterCount / VISIBLE_SHOT_COUNT)
                    > (i * characterCount / VISIBLE_SHOT_COUNT);
        }
        int rotation = random.nextInt(VISIBLE_SHOT_COUNT);

        int[] result = new int[VISIBLE_SHOT_COUNT];
        int nextCharacter = 0;
        int nextEnvironment = 0;
        for (int i = 0; i < VISIBLE_SHOT_COUNT; i++) {
            boolean character = characterSlots[(i + rotation) % VISIBLE_SHOT_COUNT];
            result[i] = character
                    ? characters.get(nextCharacter++)
                    : environments.get(nextEnvironment++);
        }

        if (nextCharacter != characterCount || nextEnvironment != environmentCount) {
            throw new IllegalStateException("Shot mix planner produced an invalid category count");
        }
        return result;
    }

    public static int normalizePercentage(int value) {
        int clamped = Math.max(0, Math.min(100, value));
        return Math.round(clamped / 10.0f) * 10;
    }

    private static List<Integer> indexes(int offset, int size) {
        List<Integer> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(offset + i);
        }
        return values;
    }

    private static double unitInterval(long seed) {
        long mixed = seed + 0x9E3779B97F4A7C15L;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        return (mixed >>> 11) * 0x1.0p-53;
    }
}
