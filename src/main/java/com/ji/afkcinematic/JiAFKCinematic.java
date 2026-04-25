package com.ji.afkcinematic;

import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.ConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.render.LetterboxRenderer;
import net.minecraft.client.MinecraftClient;

public class JiAFKCinematic implements ClientModInitializer {
    public static final String MOD_ID = "ji-afk-cinematic";
    public static final String MOD_NAME = "Ji AFK Cinematic";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing {} v2.0.0 for Minecraft 1.21.11", MOD_NAME);

        // Load configuration first
        ConfigManager.loadConfig();

        AFKDetector.init();
        CinematicManager.init();
        LetterboxRenderer.init();

        // Tick events
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LetterboxRenderer.tick();
            handleConfigKeybind(client);
        });
    }

    private void handleConfigKeybind(MinecraftClient client) {
        if (client.player == null || client.currentScreen != null) return;

        int keyCode = ConfigManager.getConfig().configKeyCode;
        long windowHandle = client.getWindow().getHandle();

        if (GLFW.glfwGetKey(windowHandle, keyCode) == GLFW.GLFW_PRESS) {
            client.setScreen(new ConfigScreen(null));
        }
    }
}
