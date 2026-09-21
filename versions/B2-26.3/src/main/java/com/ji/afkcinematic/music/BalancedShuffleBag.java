package com.ji.afkcinematic.music;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Uniform shuffle bag: every entry plays once per cycle without boundary repeats. */
public final class BalancedShuffleBag<T> {
    private final Random random;
    private final List<T> entries = new ArrayList<>();
    private int index;
    private T lastReturned;

    public BalancedShuffleBag() {
        this(new Random());
    }

    BalancedShuffleBag(Random random) {
        this.random = Objects.requireNonNull(random);
    }

    public void replace(Collection<? extends T> values) {
        entries.clear();
        for (T value : values) {
            if (value != null && !entries.contains(value)) entries.add(value);
        }
        reshuffle();
    }

    public T next() {
        if (entries.isEmpty()) return null;
        if (index >= entries.size()) reshuffle();
        T value = entries.get(index++);
        lastReturned = value;
        return value;
    }

    public boolean isCycleComplete() {
        return entries.isEmpty() || index >= entries.size();
    }

    private void reshuffle() {
        Collections.shuffle(entries, random);
        index = 0;
        if (entries.size() > 1 && Objects.equals(entries.get(0), lastReturned)) {
            Collections.swap(entries, 0, 1);
        }
    }
}
