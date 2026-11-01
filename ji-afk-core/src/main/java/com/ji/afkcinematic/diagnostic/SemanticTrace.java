package com.ji.afkcinematic.diagnostic;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;

/** Client-thread semantic events only. Never accepts raw input, identifiers, paths or coordinates. */
public final class SemanticTrace {
    public static final int CAPACITY = 48;
    public enum Event { START, START_REJECTED, STOP, ACTIVITY, RESET, INPUT_SAMPLE, HOOK, RESTORED }
    public enum Source {
        SHORTCUT, RAW_CURSOR, ACCEPTED_MOUSE, PLAYER_ROTATION, PROCESSED_INPUT, KEYBOARD,
        MOUSE_BUTTON, SCROLL, CONTROLLER, POLICY, LIFECYCLE, TIMER, ADAPTER
    }
    public enum Detail {
        NONE, MANUAL, AFK_TIMEOUT, FISHING, SLEEP, ACTIVITY, ESCAPE, DAMAGE, DEATH,
        DISABLED, CYCLE_LIMIT, WORLD_CHANGED, DISCONNECT, SHUTDOWN, FOCUS_CHANGED,
        SCREEN_CHANGED, HOOK_MISSING, ADAPTER_FAILURE, MOVE, LOOK, JUMP_SNEAK,
        GAMEPLAY_ACTION, CHAT_OPEN, CHAT_INPUT, SPRINT_LOGICAL, GESTURE_CONSUMED,
        NOISE_FILTERED, RECAPTURE, USER_REQUEST, NO_WORLD, SAME_TICK, ALREADY_ACTIVE,
        RESTORING, LOCKED_OUT, SCREEN_OPEN, COMPAT_TRANSITION, CAMERA_CONFLICT
    }
    private record Entry(long tick, Event event, Source source, Detail detail) {}
    public record Sample(int relativeTick, Event event, Source source, Detail detail) {
        public Sample {
            if (relativeTick > 0 || relativeTick == Integer.MIN_VALUE) throw new IllegalArgumentException("Invalid relative tick");
            Objects.requireNonNull(event); Objects.requireNonNull(source); Objects.requireNonNull(detail);
        }
    }
    public record Snapshot(List<Sample> samples, int omitted) {
        public Snapshot {
            samples = List.copyOf(samples);
            if (omitted < 0) throw new IllegalArgumentException("Invalid omitted count");
        }
    }
    private final ArrayDeque<Entry> entries = new ArrayDeque<>(CAPACITY);
    private long lastTick;
    private int omitted;

    public void record(long tick, Event event, Source source, Detail detail) {
        if (tick < 0 || tick < lastTick) throw new IllegalArgumentException("Non-monotonic trace tick");
        Objects.requireNonNull(event);
        Objects.requireNonNull(source);
        Objects.requireNonNull(detail);
        Entry previous = entries.peekLast();
        if (previous != null && previous.tick == tick && previous.event == event
                && previous.source == source && previous.detail == detail) return;
        if (entries.size() == CAPACITY) {
            entries.removeFirst();
            if (omitted < Integer.MAX_VALUE) ++omitted;
        }
        entries.addLast(new Entry(tick, event, source, detail));
        lastTick = tick;
    }

    public Snapshot snapshot(long now) {
        if (now < lastTick) throw new IllegalArgumentException("Snapshot precedes recorded event");
        return new Snapshot(entries.stream().map(entry -> new Sample(
                (int) Math.max(-Integer.MAX_VALUE, entry.tick - now), entry.event, entry.source, entry.detail)).toList(), omitted);
    }
}
