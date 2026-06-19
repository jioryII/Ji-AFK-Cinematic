package com.ji.afkcinematic;

import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.ConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.music.CinematicMusicManager;
import com.ji.afkcinematic.render.LetterboxRenderer;
import net.minecraft.client.Minecraft;

public class JiAFKCinematic implements ClientModInitializer {
    public static final String MOD_ID = "ji-afk-cinematic";
    public static final String MOD_NAME = "Ji AFK Cinematic";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing {} v2.2.0", MOD_NAME);
        ConfigManager.loadConfig();

        AFKDetector.init();
        CinematicManager.init();
        CinematicMusicManager.init();
        LetterboxRenderer.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LetterboxRenderer.tick();
            handleConfigKeybind(client);
        });
    }

    private boolean wasKeyDown = false;

    private void handleConfigKeybind(Minecraft client) {
        // Obsolete reflection block removed. Now handled directly via KeyboardMixin.
    }
}