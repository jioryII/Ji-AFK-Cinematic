package com.ji.afkcinematic.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ji.afkcinematic.JiAFKCinematic;
import com.ji.afkcinematic.input.PortableKeyBinding;
import com.ji.afkcinematic.input.LegacyGlfwKeyMapper;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("ji-afk-cinematic.json");
    private static ModConfig config = new ModConfig();

    public static void loadConfig() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = new BufferedReader(new FileReader(CONFIG_PATH.toFile()))) {
                    JsonObject raw = JsonParser.parseReader(reader).getAsJsonObject();
                    if (raw.has("chatVisibility")) {
                        String legacyChat = raw.get("chatVisibility").getAsString();
                        if ("PERSISTENT_MODES".equals(legacyChat) || "ALWAYS".equals(legacyChat)) {
                            raw.addProperty("chatVisibility", "VISIBLE");
                        }
                    }
                    ModConfig loaded = GSON.fromJson(raw, ModConfig.class);
                    if (loaded != null) {
                        // A missing field is different from an intentional 0%. Preserve
                        // explicit zero while migrating legacy files to the balanced default.
                        if (!raw.has("characterShotPercentage")) loaded.characterShotPercentage = 30;
                        if (!raw.has("persistentMode")) {
                            boolean legacyPersistent = raw.has("persistentCinematics")
                                    && raw.get("persistentCinematics").getAsBoolean();
                            loaded.persistentMode = legacyPersistent
                                    ? PersistentCinematicMode.INTERACTIVE
                                    : PersistentCinematicMode.NORMAL;
                        }
                        if (!raw.has("cameraRotationEnabled")) loaded.cameraRotationEnabled = false;
                        if (!raw.has("cinematicMusicVolume")) loaded.cinematicMusicVolume = 0.5f;
                        if (!raw.has("chatVisibility")) loaded.chatVisibility = CinematicChatVisibility.VISIBLE;
                        if (!raw.has("musicMode")) {
                            loaded.musicMode = raw.has("thirdPartyMusic") && raw.get("thirdPartyMusic").getAsBoolean()
                                    ? MusicMode.MIXED : MusicMode.VANILLA;
                        }
                        if (!raw.has("sleepLetterboxMode")) {
                            boolean enabled = !raw.has("sleepLetterboxEnabled")
                                    || raw.get("sleepLetterboxEnabled").getAsBoolean();
                            loaded.sleepLetterboxMode = enabled
                                    ? SleepLetterboxMode.MODERATE : SleepLetterboxMode.DISABLED;
                        }
                        if (!raw.has("customMusicDirectory")) loaded.customMusicDirectory = "";
                        if (!raw.has("fishingCinematicEnabled")) loaded.fishingCinematicEnabled = true;
                        if (!raw.has("fishingCinematicThresholdSeconds")) loaded.fishingCinematicThresholdSeconds = 10;
                        if (loaded.configVersion < 11) {
                            if (!raw.has("menuKey1")) loaded.menuKey1 = 296;
                            if (!raw.has("menuKey2")) loaded.menuKey2 = 72;
                            if (!raw.has("toggleKey1")) loaded.toggleKey1 = 341;
                            if (!raw.has("toggleKey2")) loaded.toggleKey2 = 72;
                            if (!raw.has("immediateKey1")) loaded.immediateKey1 = 296;
                            if (!raw.has("immediateKey2")) loaded.immediateKey2 = 73;
                        }
                        // Finish the historical shortcut migration while these are
                        // still GLFW values. From this point on, portable names own
                        // the conversion to the active backend.
                        if (loaded.configVersion < 8) {
                            if (loaded.toggleKey1 == 296 && loaded.toggleKey2 == 73) {
                                loaded.toggleKey1 = 341;
                                loaded.toggleKey2 = 72;
                            }
                            loaded.immediateKey1 = 296;
                            loaded.immediateKey2 = 73;
                        }
                        boolean hasAnyPortableKey = raw.has("menuKey1Name") || raw.has("menuKey2Name")
                                || raw.has("toggleKey1Name") || raw.has("toggleKey2Name")
                                || raw.has("immediateKey1Name") || raw.has("immediateKey2Name");
                        boolean legacyGlfwCodes = !hasAnyPortableKey
                                && LegacyGlfwKeyMapper.isSupported(loaded.menuKey1)
                                && LegacyGlfwKeyMapper.isSupported(loaded.menuKey2)
                                && LegacyGlfwKeyMapper.isSupported(loaded.toggleKey1)
                                && LegacyGlfwKeyMapper.isSupported(loaded.toggleKey2)
                                && LegacyGlfwKeyMapper.isSupported(loaded.immediateKey1)
                                && LegacyGlfwKeyMapper.isSupported(loaded.immediateKey2);
                        if (!raw.has("menuKey1Name")) {
                            if (legacyGlfwCodes) loaded.menuKey1 = LegacyGlfwKeyMapper.migrate(loaded.menuKey1);
                            loaded.menuKey1Name = PortableKeyBinding.nameOf(loaded.menuKey1);
                        }
                        if (!raw.has("menuKey2Name")) {
                            if (legacyGlfwCodes) loaded.menuKey2 = LegacyGlfwKeyMapper.migrate(loaded.menuKey2);
                            loaded.menuKey2Name = PortableKeyBinding.nameOf(loaded.menuKey2);
                        }
                        if (!raw.has("toggleKey1Name")) {
                            if (legacyGlfwCodes) loaded.toggleKey1 = LegacyGlfwKeyMapper.migrate(loaded.toggleKey1);
                            loaded.toggleKey1Name = PortableKeyBinding.nameOf(loaded.toggleKey1);
                        }
                        if (!raw.has("toggleKey2Name")) {
                            if (legacyGlfwCodes) loaded.toggleKey2 = LegacyGlfwKeyMapper.migrate(loaded.toggleKey2);
                            loaded.toggleKey2Name = PortableKeyBinding.nameOf(loaded.toggleKey2);
                        }
                        if (!raw.has("immediateKey1Name")) {
                            if (legacyGlfwCodes) loaded.immediateKey1 = LegacyGlfwKeyMapper.migrate(loaded.immediateKey1);
                            loaded.immediateKey1Name = PortableKeyBinding.nameOf(loaded.immediateKey1);
                        }
                        if (!raw.has("immediateKey2Name")) {
                            if (legacyGlfwCodes) loaded.immediateKey2 = LegacyGlfwKeyMapper.migrate(loaded.immediateKey2);
                            loaded.immediateKey2Name = PortableKeyBinding.nameOf(loaded.immediateKey2);
                        }
                        loaded.applyPortableKeyNames();
                        config = loaded;
                    }
                }
                JiAFKCinematic.LOGGER.info("Configuration loaded from {}", CONFIG_PATH);
            } else {
                saveConfig();
                JiAFKCinematic.LOGGER.info("Default configuration created at {}", CONFIG_PATH);
            }
        } catch (Exception e) {
            JiAFKCinematic.LOGGER.error("Failed to load config, using defaults", e);
            config = new ModConfig();
        }
        migrateIfNeeded();
        config.recalculate();
    }

    /**
     * Forward-only config migration. Bumps {@link ModConfig#configVersion} to
     * {@link ModConfig#CURRENT_CONFIG_VERSION}, applying any field defaults that a
     * prior version's config file would be missing. Gson normally applies field
     * defaults via the no-arg constructor, but some UnsafeAllocator paths can bypass
     * them, so new fields are re-asserted here defensively.
     */
    private static void migrateIfNeeded() {
        if (config.configVersion >= ModConfig.CURRENT_CONFIG_VERSION) {
            return;
        }
        JiAFKCinematic.LOGGER.info("Migrating config v{} -> v{}",
                config.configVersion, ModConfig.CURRENT_CONFIG_VERSION);

        // v1 -> v2: personalized shot mix and persistent-chat mode. loadConfig
        // distinguishes an absent percentage from the user's intentional 0%.

        // v2 -> v3: legacy smoothing/safety controls were retired and camera
        // rotation became a single option. Missing fields are handled while parsing.

        // v3 -> v4: adopt the calmer composition defaults unless the user had
        // already customized the old 50/50 mix. Rotation now starts disabled.
        if (config.configVersion < 4) {
            if (config.characterShotPercentage == 50) config.characterShotPercentage = 30;
            config.cameraRotationEnabled = false;
        }

        // v4 -> v5: the old boolean persistent-chat option becomes a three-state
        // policy. A legacy enabled value is preserved as INTERACTIVE by loadConfig().
        if (config.configVersion < 5 && config.persistentMode == null) {
            config.persistentMode = PersistentCinematicMode.NORMAL;
        }

        // v5 -> v6: chat-only HUD policy, unlimited cycles and opt-in pack music.
        if (config.configVersion < 6) {
            if (config.chatVisibility == null) {
                config.chatVisibility = CinematicChatVisibility.VISIBLE;
            }
            config.thirdPartyMusic = false;
        }

        // v6 -> v7: binary chat visibility and an immediate F7 + I cinematic toggle.
        if (config.configVersion < 7) {
            if (config.chatVisibility == null) config.chatVisibility = CinematicChatVisibility.VISIBLE;
        }

        // v7 -> v8: restore Ctrl+H as the enable/disable shortcut and keep
        // F7+I as a separate immediate-cinematic action. The old third-party
        // toggle migrates naturally to VANILLA or MIXED.
        if (config.configVersion < 8) {
            if (config.musicMode == null) {
                config.musicMode = config.thirdPartyMusic ? MusicMode.MIXED : MusicMode.VANILLA;
            }
        }

        if (config.configVersion < 12) {
            config.fishingCinematicThresholdSeconds = 10;
        }
        if (config.configVersion < 13 && config.sleepLetterboxMode == null) {
            config.sleepLetterboxMode = SleepLetterboxMode.MODERATE;
        }
        config.configVersion = ModConfig.CURRENT_CONFIG_VERSION;
        config.recalculate();
        saveConfig();
    }

    public static void saveConfig() {
        try {
            config.syncPortableKeyNames();
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = new BufferedWriter(new FileWriter(CONFIG_PATH.toFile()))) {
                GSON.toJson(config, writer);
            }
            JiAFKCinematic.LOGGER.info("Configuration saved to {}", CONFIG_PATH);
        } catch (IOException e) {
            JiAFKCinematic.LOGGER.error("Failed to save config", e);
        }
    }

    public static ModConfig getConfig() {
        return config;
    }

    public static void setConfig(ModConfig newConfig) {
        config = newConfig;
        config.recalculate();
        saveConfig();
    }
}
