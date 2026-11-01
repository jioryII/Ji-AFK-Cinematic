package com.ji.afkcinematic;

import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.platform.ClientEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.music.CinematicMusicManager;
import com.ji.afkcinematic.render.CinematicHUDManager;
import com.ji.afkcinematic.render.LetterboxRenderer;
import net.minecraft.client.Minecraft;

public class JiAFKCinematic {
    public static final String MOD_ID = "ji-afk-cinematic";
    public static final String MOD_NAME = "Ji AFK Cinematic";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final int MIXIN_DIAGNOSTIC_DELAY_TICKS = 200;
    private static final java.util.Set<String> CRITICAL_MIXINS = java.util.Set.of(
            "CameraMixin", "GameRendererMixin",
            "KeyboardMixin", "MouseMixin", "MinecraftClientMixin",
            "ClientLevelFishingParticleMixin");
    private static int mixinDiagnosticTicks;
    private static boolean mixinDiagnosticComplete;

    private static boolean initialized;

    public static void initializeClient() {
        if (initialized) return;
        initialized = true;
        LOGGER.info("Initializing {} v{}", MOD_NAME, currentVersion());
        ConfigManager.loadConfig();

        com.ji.afkcinematic.diagnostic.RuntimeResilience.init();
        com.ji.afkcinematic.diagnostic.MenuHintManager.init();
        com.ji.afkcinematic.input.GameplayActivityMonitor.init();
        AFKDetector.init();
        CinematicManager.init();
        CinematicMusicManager.init();
        LetterboxRenderer.init();
        com.ji.afkcinematic.render.SleepLetterboxManager.init();

        com.ji.afkcinematic.platform.ClientEvents.CLIENT_STOPPING.register(client -> {
            // Restore owned client state and drain accepted history writes before shutdown.
            com.ji.afkcinematic.cinematic.CinematicManager.fullTeardown();
            CinematicHUDManager.forceRestore();
            CinematicMusicManager.shutdownHistory();
            ConfigManager.shutdownWrites();
            com.ji.afkcinematic.music.LocalMusicPackManager.shutdown();
            com.ji.afkcinematic.music.ThirdPartyMusicRegistry.shutdown();
        });

        ClientEvents.END_CLIENT_TICK.register(client -> {
            LetterboxRenderer.tick();
            if (!mixinDiagnosticComplete && ++mixinDiagnosticTicks >= MIXIN_DIAGNOSTIC_DELAY_TICKS) {
                mixinDiagnosticComplete = true;
                reportMissingCriticalMixins();
            }
        });

    }

    private static String currentVersion() {
        return PlatformAccess.modVersion();
    }

    private static void reportMissingCriticalMixins() {
        // Wait until the client has had time to load every target class. Checking from
        // onInitializeClient is too early and reports valid, not-yet-applied mixins as missing.
        for (String critical : CRITICAL_MIXINS) {
            if (!com.ji.afkcinematic.diagnostic.MixinState.hasInstalledInjection(critical)) {
                LOGGER.warn("Critical mixin '{}' has no proven vanilla-to-handler invocation (class applied: {}). " +
                        "The target may be unloaded or its optional injector absent; inspect capabilities.",
                        critical, com.ji.afkcinematic.diagnostic.MixinState.didApply(critical));
            }
        }
        String minecraftVersion = PlatformAccess.minecraftVersion();
        String hudMixin = "InGameHudMixin";
        if (!com.ji.afkcinematic.diagnostic.MixinState.hasInstalledInjection(hudMixin)) {
            LOGGER.warn("HUD mixin '{}' has no proven compose-handler invocation (class applied: {}).",
                    hudMixin, com.ji.afkcinematic.diagnostic.MixinState.didApply(hudMixin));
        }
    }
}
