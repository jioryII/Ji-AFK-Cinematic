package com.ji.afkcinematic.afk;

import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.session.CinematicSession;
import com.ji.afkcinematic.platform.ClientEvents;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;

/** Platform tick/lifecycle adapter; the shared session owns AFK time and lockout. */
public final class AFKDetector {
    private static final CinematicSession SESSION = new CinematicSession();
    private static final List<AFKListener> LISTENERS = new ArrayList<>();
    private static Object observedWorld;
    private static Object observedPlayer;
    private static boolean observedEnabled = true;
    private static boolean synchronizing;
    private static boolean initialized;
    private static CinematicSession.Reason resetReason = CinematicSession.Reason.NONE;
    private static CinematicSession.Reason activityReason = CinematicSession.Reason.ACTIVITY;

    public static void init() {
        if (initialized) return;
        initialized = true;
        ClientEvents.START_CLIENT_TICK.register(client -> {
            SESSION.beginTick();
            synchronizeContext();
        });
        ClientEvents.END_CLIENT_TICK.register(client -> {
            synchronizeContext();
            if (com.ji.afkcinematic.diagnostic.RuntimeResilience.obstruction() != CinematicSession.Reason.NONE) {
                SESSION.holdForPresentation(); return;
            }
            if (SESSION.advanceIdle(ConfigManager.isLoading() || client.isPaused() || isModScreen(), ConfigManager.getConfig().afkThresholdTicks)) {
                dispatch(AFKListener::onAFKTriggered);
            }
        });
    }

    public static CinematicSession session() { return SESSION; }
    public static CinematicSession.Reason getResetReason() { return resetReason; }

    /** Also called at immediate entry points, before the first tick in a newly joined world. */
    public static void synchronizeContext() {
        if (synchronizing) return;
        Minecraft client = Minecraft.getInstance();
        boolean available = client.player != null && client.level != null;
        Object world = available ? client.level : null;
        Object player = available ? client.player : null;
        boolean enabled = ConfigManager.getConfig().modEnabled;
        boolean changed = world != observedWorld || player != observedPlayer;
        if (!changed && enabled == observedEnabled) return;
        synchronizing = true;
        try {
            resetReason = changed ? (available ? CinematicSession.Reason.WORLD_CHANGED
                    : CinematicSession.Reason.DISCONNECT) : CinematicSession.Reason.DISABLED;
            if (changed) com.ji.afkcinematic.input.RuntimeShortcuts.reset();
            else com.ji.afkcinematic.input.RuntimeShortcuts.cancelPending();
            dispatch(AFKListener::onReset);
            if (changed) SESSION.replaceWorld(available);
            SESSION.setEnabled(enabled);
            observedWorld = world;
            observedPlayer = player;
            observedEnabled = enabled;
        } finally {
            synchronizing = false;
        }
    }

    public static void addListener(AFKListener listener) {
        if (!LISTENERS.contains(listener)) LISTENERS.add(listener);
    }

    private static void dispatch(java.util.function.Consumer<AFKListener> action) {
        new ArrayList<>(LISTENERS).forEach(action);
    }

    /** Editing our own settings/report is not gameplay input or AFK waiting. */
    private static boolean isModScreen() {
        var screen = Minecraft.getInstance().gui.screen();
        return screen instanceof com.ji.afkcinematic.config.MusicManagementScreen
                || screen instanceof com.ji.afkcinematic.config.ConfigScreen
                || com.ji.afkcinematic.config.SupportScreen.isSupportScreen(screen)
                || screen instanceof net.minecraft.client.gui.screens.ChatScreen
                && ConfigManager.getConfig().chatVisibility == com.ji.afkcinematic.config.CinematicChatVisibility.HIDDEN;
    }

    public static CinematicSession.Reason getActivityReason() { return activityReason; }
    public static void registerActivity() {
        registerActivity(null, com.ji.afkcinematic.diagnostic.SemanticTrace.Source.ADAPTER);
    }
    public static void registerActivity(com.ji.afkcinematic.input.CinematicInputPolicy.Event event,
                                        com.ji.afkcinematic.diagnostic.SemanticTrace.Source source) {
        synchronizeContext();
        var activityScreen = com.ji.afkcinematic.ScreenHelper.getCurrentScreen(Minecraft.getInstance());
        if (activityScreen instanceof com.ji.afkcinematic.config.MusicManagementScreen
                || activityScreen instanceof com.ji.afkcinematic.config.ConfigScreen
                || com.ji.afkcinematic.config.SupportScreen.isSupportScreen(activityScreen)
                || !SESSION.activity()) return;
        activityReason = event == null ? CinematicSession.Reason.ACTIVITY : CinematicSession.Reason.valueOf(event.name());
        SESSION.recordActivity(source, com.ji.afkcinematic.diagnostic.SemanticTrace.Detail.valueOf(activityReason.name()));
        dispatch(AFKListener::onActivityDetected);
    }

    public static void setLockedOut(boolean lockedOut) { SESSION.setLockedOut(lockedOut); }
}
