package com.ji.afkcinematic.music;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BalancedShuffleBagTest {
    @Test
    void everyEntryAppearsExactlyOncePerCycle() {
        BalancedShuffleBag<Integer> bag = new BalancedShuffleBag<>(new Random(7L));
        bag.replace(List.of(1, 2, 3, 4, 5));
        Set<Integer> firstCycle = new HashSet<>();
        for (int i = 0; i < 5; i++) firstCycle.add(bag.next());
        assertEquals(Set.of(1, 2, 3, 4, 5), firstCycle);
    }

    @Test
    void cyclesNeverRepeatAtTheirBoundary() {
        BalancedShuffleBag<Integer> bag = new BalancedShuffleBag<>(new Random(19L));
        bag.replace(List.of(1, 2, 3, 4));
        Integer last = null;
        for (int cycle = 0; cycle < 50; cycle++) {
            Integer first = bag.next();
            if (last != null) assertNotEquals(last, first);
            for (int i = 1; i < 4; i++) last = bag.next();
        }
    }
}
