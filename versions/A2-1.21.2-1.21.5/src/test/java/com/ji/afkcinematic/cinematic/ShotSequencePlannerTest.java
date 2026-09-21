package com.ji.afkcinematic.cinematic;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ShotSequencePlannerTest {
    @Test
    void everyMixProducesExactlyFifteenUniqueComposedShots() {
        for (int percentage = 0; percentage <= 100; percentage += 10) {
            int[] plan = ShotSequencePlanner.plan(percentage, 15, 15, 1234L + percentage);
            assertEquals(15, plan.length);
            Set<Integer> unique = new HashSet<>();
            int characterCount = 0;
            for (int index : plan) {
                assertTrue(index >= 0 && index < 30);
                unique.add(index);
                if (index < 15) characterCount++;
            }
            assertEquals(15, unique.size());
            assertTrue(Math.abs(characterCount - 15 * percentage / 100.0f) < 1.0f);
        }
    }

    @Test
    void fractionalMixesMatchTheirWeightAcrossManySequences() {
        int characters = 0;
        int sequences = 1000;
        for (int seed = 0; seed < sequences; seed++) {
            for (int index : ShotSequencePlanner.plan(30, 15, 15, seed)) {
                if (index < 15) characters++;
            }
        }
        assertEquals(4.5, characters / (double) sequences, 0.1);
    }

    @Test
    void sameSeedProducesSameEdit() {
        assertArrayEquals(
                ShotSequencePlanner.plan(30, 15, 15, 99L),
                ShotSequencePlanner.plan(30, 15, 15, 99L));
    }

    @Test
    void percentageNormalizationClampsAndSnaps() {
        assertEquals(0, ShotSequencePlanner.normalizePercentage(-5));
        assertEquals(10, ShotSequencePlanner.normalizePercentage(14));
        assertEquals(20, ShotSequencePlanner.normalizePercentage(15));
        assertEquals(100, ShotSequencePlanner.normalizePercentage(108));
    }
}
