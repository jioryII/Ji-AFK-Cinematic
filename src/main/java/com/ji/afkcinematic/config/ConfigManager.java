package com.ji.afkcinematic.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
                    ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                    if (loaded != null) {
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
        config.recalculate();
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
