package com.ji.afkcinematic.music;

import java.util.*;
import java.util.random.RandomGenerator;

/** One vote per recording, persistent cycles, and transactional playback reservations. */
public final class RecordingQueue {
    private final RandomGenerator random;
    private final List<String> remaining = new ArrayList<>();
    private final Set<String> played = new LinkedHashSet<>();
    private Set<String> catalog = Set.of();
    private String last;
    public RecordingQueue(RandomGenerator random) { this.random = Objects.requireNonNull(random); }
    public void restore(List<String> remaining, Set<String> played, String last) {
        this.remaining.clear(); this.remaining.addAll(new LinkedHashSet<>(remaining));
        this.played.clear(); this.played.addAll(played); this.last = last;
    }
    public void reconcile(Collection<String> ids) {
        catalog = new LinkedHashSet<>(ids);
        remaining.removeIf(id -> !catalog.contains(id) || played.contains(id));
        played.retainAll(catalog);
        Set<String> known = new HashSet<>(remaining); known.addAll(played);
        List<String> added = new ArrayList<>();
        for (String id : catalog) if (known.add(id)) added.add(id);
        for (int i = added.size() - 1; i > 0; i--) Collections.swap(added, i, random.nextInt(i + 1));
        // Uniform linear merge preserves old pending order without quadratic insertion work.
        List<String> merged = new ArrayList<>(remaining.size() + added.size());
        int old = 0, fresh = 0;
        while (old < remaining.size() || fresh < added.size()) {
            if (fresh == added.size() || (old < remaining.size()
                    && random.nextInt(remaining.size() - old + added.size() - fresh) < remaining.size() - old))
                merged.add(remaining.get(old++));
            else merged.add(added.get(fresh++));
        }
        remaining.clear(); remaining.addAll(merged);
    }
    /** Reserving or cancelling never consumes a recording. Failed IDs are skipped by the caller. */
    public String reserve(java.util.function.Predicate<String> eligible) {
        if (remaining.isEmpty() && !catalog.isEmpty()) {
            played.clear(); remaining.addAll(catalog);
            for (int i = remaining.size() - 1; i > 0; i--) Collections.swap(remaining, i, random.nextInt(i + 1));
        }
        for (String id : remaining) if (!id.equals(last) && eligible.test(id)) return id;
        for (String id : remaining) if (eligible.test(id)) return id;
        return null;
    }
    public boolean confirm(String id) {
        if (!remaining.remove(id)) return false;
        played.add(id); last = id; return true;
    }
    public List<String> remaining() { return List.copyOf(remaining); }
    public Set<String> played() { return Collections.unmodifiableSet(new LinkedHashSet<>(played)); }
    public String last() { return last; }
}
