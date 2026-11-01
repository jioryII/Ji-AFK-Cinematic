package com.ji.afkcinematic.input;

import java.util.*;

/** Resolves all Ji shortcuts together, before the event is offered to gameplay. */
public final class ShortcutController {
    public enum Action { MENU, TOGGLE, IMMEDIATE, NONE }
    public record Chord(Set<Integer> firstKeys, int secondKey) {
        public Chord { firstKeys = Set.copyOf(firstKeys); }
        public static Chord disabled() { return new Chord(Set.of(), -1); }
        public boolean enabled() { return !firstKeys.isEmpty() && secondKey >= 0; }
        private boolean single() { return firstKeys.size() == 1 && firstKeys.contains(secondKey); }
    }
    public record Conflict(Action first, Action second) {}
    public record Decision(Action action, boolean consume, boolean prefix, boolean newPress) {
        private static Decision none(boolean consume, boolean prefix, boolean newPress) {
            return new Decision(Action.NONE, consume, prefix, newPress);
        }
    }
    private record Armed(int firstKey, long atNanos) {}
    private final long timeoutNanos;
    private Map<Action, Chord> bindings = Map.of();
    private final EnumMap<Action, Armed> armed = new EnumMap<>(Action.class);
    private final Set<Integer> held = new HashSet<>();
    private final Set<Integer> consumed = new HashSet<>();
    private final Set<Integer> completedPrefixes = new HashSet<>();

    public ShortcutController(long timeoutNanos) {
        if (timeoutNanos < 1) throw new IllegalArgumentException("Positive timeout required");
        this.timeoutNanos = timeoutNanos;
    }

    public void configure(Map<Action, Chord> requested) {
        Map<Action, Chord> copy = Map.copyOf(requested);
        if (copy.containsKey(Action.NONE)) throw new IllegalArgumentException("NONE is not a binding");
        // Rebinding invalidates a pending sequence, but an already claimed
        // physical gesture remains consumed until each of its keys is released.
        if (!bindings.equals(copy)) { cancelPending(); bindings = copy; }
    }

    /** Priority preserves MENU, TOGGLE, IMMEDIATE; conflicting settings remain visible and unchanged. */
    public List<Conflict> conflicts() {
        List<Conflict> result = new ArrayList<>();
        Action[] actions = {Action.MENU, Action.TOGGLE, Action.IMMEDIATE};
        for (int i = 0; i < actions.length; i++) for (int j = i + 1; j < actions.length; j++) {
            Chord a = bindings.get(actions[i]), b = bindings.get(actions[j]);
            if (a != null && b != null && a.enabled() && b.enabled()
                    && a.secondKey == b.secondKey
                    && (a.single() || b.single() || !Collections.disjoint(a.firstKeys, b.firstKeys))) {
                result.add(new Conflict(actions[i], actions[j]));
            }
        }
        return List.copyOf(result);
    }

    public Decision press(int key, long nowNanos, boolean allowed) {
        if (!held.add(key)) return Decision.none(consumed.contains(key), false, false);
        if (!allowed) { cancelPending(); return Decision.none(false, false, true); }
        for (Action action : Action.values()) {
            Chord chord = bindings.get(action);
            if (chord == null || !chord.enabled()) continue;
            Armed pending = armed.get(action);
            boolean matched = chord.single() && key == chord.secondKey;
            if (pending != null) {
                long elapsed = nowNanos - pending.atNanos;
                matched |= (completedPrefixes.contains(pending.firstKey) || elapsed >= 0 && elapsed <= timeoutNanos) && held.contains(pending.firstKey)
                        && chord.firstKeys.contains(pending.firstKey) && key == chord.secondKey;
            }
            if (matched) {
                if (!chord.single()) {
                    consumed.add(pending.firstKey);
                    completedPrefixes.add(pending.firstKey);
                    // The winning prefix remains valid until release or context invalidation.
                    // Each new final-key PRESS is a gesture; duplicate PRESS/REPEAT is not.
                    armed.values().removeIf(value -> value.firstKey != pending.firstKey);
                } else cancelPending();
                consumed.add(key);
                return new Decision(action, true, false, true);
            }
        }
        // A fresh unrelated press breaks a pending chord; all sequences see the same event.
        cancelPending();
        for (var entry : bindings.entrySet()) {
            if (entry.getValue().enabled() && entry.getValue().firstKeys.contains(key)) {
                armed.put(entry.getKey(), new Armed(key, nowNanos));
            }
        }
        return Decision.none(false, !armed.isEmpty(), true);
    }

    public boolean repeatIsConsumed(int key) { return consumed.contains(key); }

    /** The actual winning prefix and final key, never all configured aliases. */
    public Set<Integer> consumedKeys() { return Set.copyOf(consumed); }

    /** Release is still offered to vanilla, since the first key may already have reached it. */
    public void release(int key) {
        held.remove(key);
        consumed.remove(key);
        completedPrefixes.remove(key);
        armed.values().removeIf(value -> value.firstKey == key);
    }

    public void cancelPending() { armed.clear(); completedPrefixes.clear(); }
    public void reset() { cancelPending(); held.clear(); consumed.clear(); }
}
