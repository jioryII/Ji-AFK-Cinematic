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
     * environment shots begin at characterPoolSize. Distance quotas are 6/6/3; pure pools may repeat identities with new visual variants.
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

        int[] totals = {6, 6, 3};
        List<int[]> allocations = new ArrayList<>();
        for (int close = 0; close <= 6; close++) for (int medium = 0; medium <= 6; medium++) {
            int far = characterCount - close - medium;
            if (far < 0 || far > 3) continue;
            // Prefer all unique identities; pure pools necessarily repeat one close and medium.
            if (characterCount >= 2 && characterCount <= 13 && (close < 1 || close > 5 || medium < 1 || medium > 5)) continue;
            allocations.add(new int[]{close, medium, far});
        }
        int[] chosen = allocations.get(random.nextInt(allocations.size()));
        List<Integer> edit = new ArrayList<>();
        for (int band = 0; band < 3; band++) for (int pool = 0; pool < 2; pool++) {
            List<Integer> candidates = new ArrayList<>();
            for (var preset : com.ji.afkcinematic.cinematic.director.CompositionPreset.values())
                if (preset.character == (pool == 0) && preset.range().ordinal() == band)
                    candidates.add((pool == 0 ? 0 : characterPoolSize) + preset.ordinal() % 15);
            Collections.shuffle(candidates, random);
            int count = pool == 0 ? chosen[band] : totals[band] - chosen[band];
            for (int i = 0; i < count; i++) edit.add(candidates.get(i % candidates.size()));
        }
        Collections.shuffle(edit, random);
        for (int i = 1; i < edit.size(); i++) if (edit.get(i).equals(edit.get(i-1)))
            for (int j = i+1; j < edit.size(); j++) if (!edit.get(j).equals(edit.get(i-1))) { Collections.swap(edit,i,j); break; }
        int[] result = edit.stream().mapToInt(Integer::intValue).toArray();
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
