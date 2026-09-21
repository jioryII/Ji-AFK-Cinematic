package com.ji.afkcinematic.input;

import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.config.ConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;

/** Samples processed vanilla input so controller mods are covered without a hard dependency. */
public final class GameplayActivityMonitor {
    private static final float MOVE_EPSILON_SQUARED = 0.0001F;
    private static Object previousPlayer;
    private static float previousYaw;
    private static float previousPitch;
    private static boolean previousChatOpen;

    private GameplayActivityMonitor() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(GameplayActivityMonitor::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            reset();
            return;
        }

        boolean chatOpen = client.currentScreen instanceof ChatScreen;
        if (previousPlayer != client.player) {
            previousPlayer = client.player;
            previousYaw = client.player.getYaw();
            previousPitch = client.player.getPitch();
            previousChatOpen = chatOpen;
            return;
        }

        if (chatOpen && !previousChatOpen) register(CinematicInputPolicy.Event.CHAT_OPEN, false);
        previousChatOpen = chatOpen;

        if (!chatOpen) {
            Vec2f movement = client.player.input.getMovementInput();
            PlayerInput keys = client.player.input.playerInput;
            if (movement.lengthSquared() > MOVE_EPSILON_SQUARED) {
                register(CinematicInputPolicy.Event.MOVE, false);
            }
            if (keys.jump() || keys.sneak() || keys.sprint()) {
                register(CinematicInputPolicy.Event.JUMP_SNEAK, false);
            }
            float yaw = client.player.getYaw();
            float pitch = client.player.getPitch();
            if (Float.compare(yaw, previousYaw) != 0 || Float.compare(pitch, previousPitch) != 0) {
                register(CinematicInputPolicy.Event.LOOK, false);
            }
            previousYaw = yaw;
            previousPitch = pitch;
        }
    }

    private static void register(CinematicInputPolicy.Event event, boolean chatOpen) {
        if (CinematicInputPolicy.shouldRegisterActivity(
                CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE,
                CinematicManager.isFishingCinematic(),
                ConfigManager.getConfig().persistentMode,
                chatOpen,
                event)) {
            AFKDetector.registerActivity();
        }
    }

    private static void reset() {
        previousPlayer = null;
        previousChatOpen = false;
    }
}
