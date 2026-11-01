package com.ji.afkcinematic.session;

import com.ji.afkcinematic.config.PersistentCinematicMode;
import com.ji.afkcinematic.diagnostic.SemanticTrace;
import java.util.Objects;

/** Client-thread state. Adapters own world identities and output resources, never the AFK clock. */
public final class CinematicSession {
    public enum State { NO_WORLD, IDLE_WAITING, ACTIVE, RESTORING, BLOCKED }
    public enum Type { NONE, AFK, MANUAL, FISHING, SLEEP }
    public enum Reason {
        NONE, ACTIVITY, ESCAPE, DAMAGE, DEATH, DISABLED, CYCLE_LIMIT,
        WORLD_CHANGED, DISCONNECT, SHUTDOWN, ADAPTER_FAILURE, USER_REQUEST, SLEEP,
        MOVE, LOOK, JUMP_SNEAK, GAMEPLAY_ACTION, CHAT_OPEN, CHAT_INPUT,
        NO_WORLD, SAME_TICK, ALREADY_ACTIVE, RESTORING, LOCKED_OUT, HOOK_MISSING,
        SCREEN_OPEN, COMPAT_TRANSITION, CAMERA_CONFLICT
    }
    public record Snapshot(State state, Type type, PersistentCinematicMode mode,
                           Reason lastStop, int idleTicks, long epoch, Type lastAttempt, Reason lastRejection) {}
    /** An identity token: a delayed completion cannot finish another session's restoration. */
    public static final class Restoration {
        private final long epoch;
        private final boolean blocked;
        private Restoration(long epoch, boolean blocked) {
            this.epoch = epoch;
            this.blocked = blocked;
        }
    }

    private State state = State.NO_WORLD;
    private Type type = Type.NONE;
    private PersistentCinematicMode mode = PersistentCinematicMode.NORMAL;
    private Reason lastStop = Reason.NONE;
    private Type lastAttempt = Type.NONE;
    private Reason lastRejection = Reason.NONE;
    private boolean enabled = true;
    private boolean timeoutDelivered;
    private int idleTicks;
    private long epoch;
    private long activationId;
    private long tick;
    private long lastStartTick = -1;
    private long lastStopTick = -1;
    private Restoration restoration;
    private final SemanticTrace trace = new SemanticTrace();

    public Snapshot snapshot() { return new Snapshot(state, type, mode, lastStop, idleTicks, epoch, lastAttempt, lastRejection); }
    public boolean active() { return state == State.ACTIVE; }
    public long activationId() { return activationId; }
    public SemanticTrace.Snapshot traceSnapshot() { return trace.snapshot(tick); }
    public void recordActivity(SemanticTrace.Source source, SemanticTrace.Detail detail) {
        trace.record(tick, SemanticTrace.Event.ACTIVITY, source, detail);
    }
    public void beginTick() { tick++; }

    /** A presentation is neither player activity nor an AFK opportunity. */
    public void holdForPresentation() {
        idleTicks = 0;
        timeoutDelivered = false;
    }

    /** Must follow teardown of old outputs, even for direct world/player replacement. */
    public void replaceWorld(boolean available) {
        requireRestored();
        epoch++;
        idleTicks = 0;
        timeoutDelivered = false;
        type = Type.NONE;
        mode = PersistentCinematicMode.NORMAL;
        state = available ? (enabled ? State.IDLE_WAITING : State.BLOCKED) : State.NO_WORLD;
        trace.record(tick, SemanticTrace.Event.RESET, SemanticTrace.Source.LIFECYCLE,
                available ? SemanticTrace.Detail.WORLD_CHANGED : SemanticTrace.Detail.DISCONNECT);
    }

    public void setEnabled(boolean value) {
        if (enabled == value) return;
        requireRestored();
        enabled = value;
        idleTicks = 0;
        timeoutDelivered = false;
        if (!value) lastStop = Reason.DISABLED;
        if (state != State.NO_WORLD) state = value ? State.IDLE_WAITING : State.BLOCKED;
        if (!value) trace.record(tick, SemanticTrace.Event.STOP, SemanticTrace.Source.POLICY, SemanticTrace.Detail.DISABLED);
    }

    public boolean advanceIdle(boolean paused, int thresholdTicks) {
        if (thresholdTicks < 1) throw new IllegalArgumentException("AFK threshold must be positive");
        if (!enabled || paused || state != State.IDLE_WAITING) return false;
        if (idleTicks < Integer.MAX_VALUE) idleTicks++;
        if (idleTicks < thresholdTicks || timeoutDelivered || lastStopTick == tick) return false;
        timeoutDelivered = true;
        return true;
    }

    /** Policy has already decided this is legitimate activity. Outputs are restored by the adapter. */
    public boolean activity() {
        if (!enabled || state == State.NO_WORLD || state == State.RESTORING) return false;
        boolean changed = idleTicks != 0 || timeoutDelivered || state == State.BLOCKED || active();
        idleTicks = 0;
        timeoutDelivered = false;
        if (state == State.BLOCKED) state = State.IDLE_WAITING;
        return changed;
    }

    public boolean start(Type requested, PersistentCinematicMode requestedMode) {
        Objects.requireNonNull(requested);
        Objects.requireNonNull(requestedMode);
        if (requested == Type.NONE) throw new IllegalArgumentException("Missing session type");
        if (!enabled) return rejectStart(requested, Reason.DISABLED);
        if (state == State.NO_WORLD) return rejectStart(requested, Reason.NO_WORLD);
        if (lastStartTick == tick || lastStopTick == tick) return rejectStart(requested, Reason.SAME_TICK);
        if (state != State.IDLE_WAITING && !(state == State.BLOCKED && requested == Type.MANUAL)) {
            return rejectStart(requested, state == State.ACTIVE ? Reason.ALREADY_ACTIVE
                    : state == State.RESTORING ? Reason.RESTORING : Reason.LOCKED_OUT);
        }
        lastAttempt = requested;
        lastRejection = Reason.NONE;
        activationId++;
        state = State.ACTIVE;
        type = requested;
        mode = requestedMode;
        idleTicks = 0;
        timeoutDelivered = true;
        lastStartTick = tick;
        trace.record(tick, SemanticTrace.Event.START,
                requested == Type.AFK ? SemanticTrace.Source.TIMER : SemanticTrace.Source.ADAPTER,
                requested == Type.AFK ? SemanticTrace.Detail.AFK_TIMEOUT : SemanticTrace.Detail.valueOf(requested.name()));
        return true;
    }

    /** Adapters may reject before acquiring outputs, with a bounded, nonpersonal reason. */
    public boolean rejectStart(Type requested, Reason reason) {
        Objects.requireNonNull(requested); Objects.requireNonNull(reason);
        if (requested == Type.NONE || reason == Reason.NONE) throw new IllegalArgumentException("Missing rejection context");
        lastAttempt = requested;
        lastRejection = reason;
        trace.record(tick, SemanticTrace.Event.START_REJECTED, SemanticTrace.Source.POLICY,
                SemanticTrace.Detail.valueOf(reason.name()));
        return false;
    }

    /** Recasting can turn the current composition into fishing without acquiring outputs again. */
    public void enterFishing() {
        if (active() && type != Type.FISHING) {
            type = Type.FISHING;
            trace.record(tick, SemanticTrace.Event.START, SemanticTrace.Source.ADAPTER, SemanticTrace.Detail.FISHING);
        }
    }

    public Restoration beginRestoration(Reason reason, boolean blocked) {
        Objects.requireNonNull(reason);
        if (!active()) return null;
        lastStop = reason;
        lastStopTick = tick;
        idleTicks = 0;
        timeoutDelivered = false;
        state = State.RESTORING;
        restoration = new Restoration(epoch, blocked);
        trace.record(tick, SemanticTrace.Event.STOP, SemanticTrace.Source.POLICY, SemanticTrace.Detail.valueOf(reason.name()));
        return restoration;
    }

    public boolean completeRestoration(Restoration token, boolean success) {
        if (token == null || token != restoration || token.epoch != epoch || state != State.RESTORING) return false;
        type = Type.NONE;
        state = token.blocked || !success || !enabled ? State.BLOCKED : State.IDLE_WAITING;
        if (!success) lastStop = Reason.ADAPTER_FAILURE;
        restoration = null;
        trace.record(tick, SemanticTrace.Event.RESTORED, SemanticTrace.Source.ADAPTER,
                success ? SemanticTrace.Detail.NONE : SemanticTrace.Detail.ADAPTER_FAILURE);
        return true;
    }

    public void setLockedOut(boolean locked) {
        if (state == State.NO_WORLD || state == State.ACTIVE || state == State.RESTORING) return;
        state = locked || !enabled ? State.BLOCKED : State.IDLE_WAITING;
    }

    private void requireRestored() {
        if (state == State.ACTIVE || state == State.RESTORING) {
            throw new IllegalStateException("Restore outputs before changing world or enablement");
        }
    }
}
