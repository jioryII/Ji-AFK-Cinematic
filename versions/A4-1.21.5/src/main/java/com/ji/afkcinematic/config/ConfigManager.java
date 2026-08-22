package com.ji.afkcinematic.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ji.afkcinematic.JiAFKCinematic;
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

        config.configVersion = ModConfig.CURRENT_CONFIG_VERSION;
        saveConfig();
    }

    public static void saveConfig() {
        try {
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
