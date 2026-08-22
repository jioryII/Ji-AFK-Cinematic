package com.ji.afkcinematic;

import com.ji.afkcinematic.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.music.CinematicMusicManager;
import com.ji.afkcinematic.render.CinematicHUDManager;
import com.ji.afkcinematic.render.LetterboxRenderer;
import net.minecraft.client.Minecraft;

public class JiAFKCinematic implements ClientModInitializer {
    public static final String MOD_ID = "ji-afk-cinematic";
    public static final String MOD_NAME = "Ji AFK Cinematic";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final int MIXIN_DIAGNOSTIC_DELAY_TICKS = 200;
    private static final java.util.Set<String> CRITICAL_MIXINS = java.util.Set.of(
            "CameraMixin", "InGameHudMixin", "GameRendererMixin",
            "KeyboardMixin", "MouseMixin", "MinecraftClientMixin");
    private static int mixinDiagnosticTicks;
    private static boolean mixinDiagnosticComplete;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing {} v2.3.0", MOD_NAME);
        ConfigManager.loadConfig();

        AFKDetector.init();
        CinematicManager.init();
        CinematicMusicManager.init();
        LetterboxRenderer.init();
        com.ji.afkcinematic.qa.RuntimeProbe.initIfEnabled();

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            // Full teardown on game close: restore music volume + perspective + HUD + letterbox.
            // Prevents the player's Music option from being persisted at 0 by vanilla options.txt.
            com.ji.afkcinematic.cinematic.CinematicManager.fullTeardown();
            CinematicHUDManager.forceRestore();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LetterboxRenderer.tick();
            if (!mixinDiagnosticComplete && ++mixinDiagnosticTicks >= MIXIN_DIAGNOSTIC_DELAY_TICKS) {
                mixinDiagnosticComplete = true;
                reportMissingCriticalMixins();
            }
        });

    }

    private static void reportMissingCriticalMixins() {
        // Wait until the client has had time to load every target class. Checking from
        // onInitializeClient is too early and reports valid, not-yet-applied mixins as missing.
        for (String critical : CRITICAL_MIXINS) {
            if (!com.ji.afkcinematic.diagnostic.MixinState.didApply(critical)) {
                LOGGER.warn("Critical mixin '{}' did not apply — the '{}' feature will be broken. " +
                        "This usually means a Minecraft API change; please report this version combination.",
                        critical, critical);
            }
        }
    }
}
