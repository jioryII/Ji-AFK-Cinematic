package com.ji.afkcinematic;

import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.ConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.music.CinematicMusicManager;
import com.ji.afkcinematic.render.LetterboxRenderer;
import net.minecraft.client.MinecraftClient;

public class JiAFKCinematic implements ClientModInitializer {
    public static final String MOD_ID = "ji-afk-cinematic";
    public static final String MOD_NAME = "Ji AFK Cinematic";
        public static KeyBinding configKeyBinding;
    private static int lastKnownConfigCode = -1;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing {} v2.2.0", MOD_NAME);

        // Load configuration first
        ConfigManager.loadConfig();

        
        // Register keybinding
        configKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.ji_afkcinematic.open_config",
            InputUtil.Type.KEYSYM,
            org.lwjgl.glfw.GLFW.GLFW_KEY_F7,
            "category.ji_afkcinematic.keys"
        ));
        lastKnownConfigCode = ConfigManager.getConfig().configKeyCode;

        AFKDetector.init();
        CinematicManager.init();
        CinematicMusicManager.init();
        LetterboxRenderer.init();
        com.ji.afkcinematic.render.PlayerForceRenderer.init();
        com.ji.afkcinematic.render.PlayerForceRenderer.init();
        
        

        // Tick events
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LetterboxRenderer.tick();
            
            if (configKeyBinding != null) {
                while (configKeyBinding.wasPressed()) {
                    if (client.currentScreen == null) {
                        client.setScreen(new ConfigScreen(client.currentScreen));
                    }
                }
                
                try {
                    int configCode = ConfigManager.getConfig().configKeyCode;
                    int nativeCode = InputUtil.fromTranslationKey(configKeyBinding.getBoundKeyTranslationKey()).getCode();
                    
                    if (configCode != lastKnownConfigCode) {
                        configKeyBinding.setBoundKey(InputUtil.fromKeyCode(configCode, -1));
                        KeyBinding.updateKeysByCode();
                        lastKnownConfigCode = configCode;
                    } else if (nativeCode != configCode) {
                        ConfigManager.getConfig().configKeyCode = nativeCode;
                        ConfigManager.saveConfig();
                        lastKnownConfigCode = nativeCode;
                    }
                } catch (Exception e) {}
            }
        });
    }

}

