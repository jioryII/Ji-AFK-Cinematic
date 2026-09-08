package com.ji.afkcinematic.input;

import com.ji.afkcinematic.ScreenHelper;
import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.config.ConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

/** Samples processed vanilla input so controller providers require no direct dependency. */
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

    private static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            reset();
            return;
        }
        boolean chatOpen = ScreenHelper.getCurrentScreen(client) instanceof ChatScreen;
        if (previousPlayer != client.player) {
            previousPlayer = client.player;
            previousYaw = client.player.getYRot();
            previousPitch = client.player.getXRot();
            previousChatOpen = chatOpen;
            return;
        }
        if (chatOpen && !previousChatOpen) register(CinematicInputPolicy.Event.CHAT_OPEN, false);
        previousChatOpen = chatOpen;

        if (!chatOpen) {
            Vec2 movement = client.player.input.getMoveVector();
            Input keys = client.player.input.keyPresses;
            if (movement.lengthSquared() > MOVE_EPSILON_SQUARED) register(CinematicInputPolicy.Event.MOVE, false);
            if (keys.jump() || keys.shift() || keys.sprint()) register(CinematicInputPolicy.Event.JUMP_SNEAK, false);
            float yaw = client.player.getYRot();
            float pitch = client.player.getXRot();
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
                ConfigManager.getConfig().persistentMode, chatOpen, event)) {
            AFKDetector.registerActivity();
        }
    }

    private static void reset() {
        previousPlayer = null;
        previousChatOpen = false;
    }
}
