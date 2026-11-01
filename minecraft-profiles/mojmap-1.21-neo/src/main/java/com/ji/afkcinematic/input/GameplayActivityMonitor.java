package com.ji.afkcinematic.input;

import com.ji.afkcinematic.ScreenHelper;
import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.platform.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.Input;
import net.minecraft.world.phys.Vec2;

/** Samples processed vanilla input so controller providers require no direct dependency. */
public final class GameplayActivityMonitor {
    private static final float MOVE_EPSILON_SQUARED = 0.0001F;
    private static Object previousPlayer;
    private static boolean previousChatOpen;
    private static boolean initialized;

    private GameplayActivityMonitor() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        ClientEvents.START_CLIENT_TICK.register(client -> {
            AFKDetector.synchronizeContext();
            RuntimeShortcuts.clearConsumedBindings();
        });
        ClientEvents.END_CLIENT_TICK.register(GameplayActivityMonitor::tick);
    }

    private static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            reset();
            return;
        }
        boolean chatOpen = ScreenHelper.getCurrentScreen(client) instanceof ChatScreen;
        if (previousPlayer != client.player) {
            previousPlayer = client.player;
            RuntimeLookInput.rebase();
            previousChatOpen = chatOpen;
            return;
        }
        if (chatOpen && !previousChatOpen) register(CinematicInputPolicy.Event.CHAT_OPEN, false);
        previousChatOpen = chatOpen;

        if (!chatOpen && RuntimeLookInput.isGameplayFocused()) {
            Vec2 movement = client.player.input.getMoveVector();
            Input keys = client.player.input;
            if (movement.lengthSquared() > MOVE_EPSILON_SQUARED) register(CinematicInputPolicy.Event.MOVE, false);
            // A retained sprint toggle is not new activity while stationary.
            if (keys.jumping || keys.shiftKeyDown) register(CinematicInputPolicy.Event.JUMP_SNEAK, false);
            if (RuntimeLookInput.pollRotation()) {
                register(CinematicInputPolicy.Event.LOOK, false);
            }
        }
    }

    private static void register(CinematicInputPolicy.Event event, boolean chatOpen) {
        if (CinematicInputPolicy.shouldRegisterActivity(
                CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE,
                CinematicManager.isFishingCinematic(),
                ConfigManager.getConfig().persistentMode, chatOpen, event)) {
            AFKDetector.registerActivity(event, event == CinematicInputPolicy.Event.LOOK
                    ? com.ji.afkcinematic.diagnostic.SemanticTrace.Source.PLAYER_ROTATION
                    : com.ji.afkcinematic.diagnostic.SemanticTrace.Source.PROCESSED_INPUT);
        }
    }

    private static void reset() {
        RuntimeLookInput.reset();
        previousPlayer = null;
        previousChatOpen = false;
    }
}
