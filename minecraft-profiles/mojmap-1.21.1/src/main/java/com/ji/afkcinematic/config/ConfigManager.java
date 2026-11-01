package com.ji.afkcinematic.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ji.afkcinematic.JiAFKCinematic;
import com.ji.afkcinematic.PlatformAccess;
import com.ji.afkcinematic.input.PortableKeyBinding;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = PlatformAccess.configDir().resolve("ji-afk-cinematic.json");
    private static final ConfigRepository REPOSITORY = new ConfigRepository(CONFIG_PATH);
    private static ModConfig queuedConfig;
    private static int pendingWrites;
    private static long loadEpoch;
    private static ModConfig config = new ModConfig();
    private static JsonObject preservedFields = new JsonObject();
    private static boolean writeProtected;

    private record Loaded(ModConfig value, JsonObject fields, boolean protectedFile) {}
    private static volatile boolean loading;
    private static java.util.concurrent.CompletableFuture<Void> initialLoad = java.util.concurrent.CompletableFuture.completedFuture(null);
    public static boolean isLoading() { return loading; }
    public static java.util.concurrent.CompletableFuture<Void> whenLoaded() { return initialLoad; }
    public static void loadConfig() { loadConfigAsync(); }
    public static java.util.concurrent.CompletableFuture<Void> loadConfigAsync() {
        if (loading) return initialLoad;
        if (pendingWrites != 0) {
            return java.util.concurrent.CompletableFuture.completedFuture(null); // A durable commit owns this generation.
        }
        loading = true;
        long epoch = ++loadEpoch;
        var completed = new java.util.concurrent.CompletableFuture<Void>();
        initialLoad = completed;
        REPOSITORY.read(ConfigManager::decode).whenComplete((loaded, failure) ->
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                if (epoch == loadEpoch) {
                    if (failure == null) {
                        config = loaded.value(); preservedFields = loaded.fields(); writeProtected = loaded.protectedFile();
                        config.applyPortableKeyNames(); config.recalculate();
                    } else { writeProtected = true; JiAFKCinematic.LOGGER.error("Configuration read failed; live settings retained", failure); }
                    loading = false;
                }
                completed.complete(null);
            }));
        return completed;
    }
    private static Loaded decode(String content) {
        ModConfig config = new ModConfig();
        JsonObject preservedFields = new JsonObject();
        boolean writeProtected = false;
        try {
            if (content != null) {
                    JsonObject raw = JsonParser.parseString(content).getAsJsonObject();
                    if (raw.has("persistentMode") && "INTERACTIVE".equals(raw.get("persistentMode").getAsString())) raw.addProperty("persistentMode", "NORMAL");
                    // Keep unrecognized fields, including extension settings, when saving.
                    preservedFields = raw.deepCopy();
                    if (raw.has("chatVisibility")) {
                        String legacyChat = raw.get("chatVisibility").getAsString();
                        if ("PERSISTENT_MODES".equals(legacyChat) || "ALWAYS".equals(legacyChat)) {
                            raw.addProperty("chatVisibility", "VISIBLE");
                        }
                    }
                    ModConfig loaded = GSON.fromJson(raw, ModConfig.class);
                    if (loaded != null) {
                        if (!raw.has("configVersion")) loaded.configVersion = 1;
                        if (loaded.configVersion > ModConfig.CURRENT_CONFIG_VERSION) {
                            writeProtected = true;
                            JiAFKCinematic.LOGGER.warn(
                                    "Config schema v{} is newer than supported v{}; saving is disabled",
                                    loaded.configVersion, ModConfig.CURRENT_CONFIG_VERSION);
                        }
                        // A missing field is different from an intentional 0%. Preserve
                        // explicit zero while migrating legacy files to the balanced default.
                        if (!raw.has("characterShotPercentage")) loaded.characterShotPercentage = 50;
                        if (!raw.has("persistentMode")) {
                            boolean legacyPersistent = raw.has("persistentCinematics")
                                    && raw.get("persistentCinematics").getAsBoolean();
                            loaded.persistentMode = legacyPersistent
                                    ? PersistentCinematicMode.NORMAL
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
                        if (!raw.has("menuKey1Name")) loaded.menuKey1Name = PortableKeyBinding.nameOf(loaded.menuKey1);
                        if (!raw.has("menuKey2Name")) loaded.menuKey2Name = PortableKeyBinding.nameOf(loaded.menuKey2);
                        if (!raw.has("toggleKey1Name")) loaded.toggleKey1Name = PortableKeyBinding.nameOf(loaded.toggleKey1);
                        if (!raw.has("toggleKey2Name")) loaded.toggleKey2Name = PortableKeyBinding.nameOf(loaded.toggleKey2);
                        if (!raw.has("immediateKey1Name")) loaded.immediateKey1Name = PortableKeyBinding.nameOf(loaded.immediateKey1);
                        if (!raw.has("immediateKey2Name")) loaded.immediateKey2Name = PortableKeyBinding.nameOf(loaded.immediateKey2);
                        config = loaded;
                    }

            }
            config.applyPortableKeyNames();
            int previousVersion = config.configVersion;
            if (content != null) writeProtected = migrateIfNeeded(config, writeProtected);
            config.recalculate();
            if (!writeProtected && (content == null || previousVersion != config.configVersion))
                REPOSITORY.writeNow(GSON.toJson(snapshot(config, preservedFields)));
        } catch (Exception error) {
            JiAFKCinematic.LOGGER.error("Invalid configuration; original file preserved", error);
            writeProtected = true;
        }
        return new Loaded(config, preservedFields, writeProtected);
    }

    private static boolean migrateIfNeeded(ModConfig config, boolean writeProtected) {
        if (writeProtected || config.configVersion >= ModConfig.CURRENT_CONFIG_VERSION) {
            return writeProtected;
        }
        try {
            Path originalBackup = REPOSITORY.preserveOriginal();
            // A pre-existing backup also needs to be a readable JSON object.
            // Never overwrite it to repair a failed or invalid prior backup.
            JsonParser.parseString(Files.readString(originalBackup, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            writeProtected = true;
            JiAFKCinematic.LOGGER.error("Config migration cancelled: original backup could not be confirmed", e);
            return writeProtected;
        }
        JiAFKCinematic.LOGGER.info("Migrating config v{} -> v{}",
                config.configVersion, ModConfig.CURRENT_CONFIG_VERSION);

        // v1 -> v2: personalized shot mix and persistent-chat mode. loadConfig
        // distinguishes an absent percentage from the user's intentional 0%.

        // v2 -> v3: legacy smoothing/safety controls were retired and camera
        // rotation became a single option. Missing fields are handled while parsing.

        // v3 -> v4: adopt the calmer composition defaults unless the user had
        // already customized the mix. Keep explicit percentages; rotation starts disabled.
        if (config.configVersion < 4) {
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

        // Legacy audio choice is independent of keyboard assignments.
        if (config.configVersion < 8 && config.musicMode == null) {
            config.musicMode = config.thirdPartyMusic ? MusicMode.MIXED : MusicMode.VANILLA;
        }

        if (config.configVersion < 12) config.fishingCinematicThresholdSeconds = 10;
        if (config.configVersion < 13 && config.sleepLetterboxMode == null) {
            config.sleepLetterboxMode = SleepLetterboxMode.MODERATE;
        }

        // v15: replace only historical stock toggles; keep custom bindings and disabled shortcuts.
        if (config.configVersion < 15 && com.ji.afkcinematic.input.ShortcutDefaults.isLegacyToggle(
                config.toggleKey1Name, config.toggleKey2Name)) {
            config.toggleKey1Name = "key.keyboard.f7";
            config.toggleKey2Name = "key.keyboard.u";
            config.applyPortableKeyNames();
        }
        // Numeric migrations and stable names must describe the same gesture before load applies it.
        config.syncPortableKeyNames();
        config.configVersion = ModConfig.CURRENT_CONFIG_VERSION;
        return writeProtected;
    }

    public static void saveConfig() {
        trySetConfigAsync(config);
    }

    /** Snapshot on the client, persist on the worker, commit on the client after success. */
    public static java.util.concurrent.CompletableFuture<Boolean> trySetConfigAsync(ModConfig requested) {
        if (writeProtected || loading) return java.util.concurrent.CompletableFuture.completedFuture(false);
        java.util.Objects.requireNonNull(requested, "requested");
        ModConfig candidate = GSON.fromJson(GSON.toJson(requested), ModConfig.class);
        candidate.recalculate(); candidate.syncPortableKeyNames();
        JsonObject snapshot = snapshot(candidate);
        long epoch = loadEpoch;
        queuedConfig = candidate; pendingWrites++;
        var result = new java.util.concurrent.CompletableFuture<Boolean>();
        REPOSITORY.write(GSON.toJson(snapshot)).whenComplete((ignored, failure) ->
                net.minecraft.client.Minecraft.getInstance().execute(() -> {
                    pendingWrites--;
                    boolean accepted = failure == null && epoch == loadEpoch;
                    if (accepted) { config = candidate; preservedFields = snapshot; }
                    else if (failure != null) JiAFKCinematic.LOGGER.error("Configuration save failed; previous live settings retained", failure);
                    if (pendingWrites == 0) queuedConfig = null;
                    result.complete(accepted);
                }));
        return result;
    }

    public static java.util.concurrent.CompletableFuture<Boolean> toggleEnabledAsync() {
        ModConfig candidate = GSON.fromJson(GSON.toJson(queuedConfig == null ? config : queuedConfig), ModConfig.class);
        candidate.modEnabled = !candidate.modEnabled;
        return trySetConfigAsync(candidate);
    }

    public static void shutdownWrites() {
        if (!REPOSITORY.shutdown(java.time.Duration.ofSeconds(5)))
            JiAFKCinematic.LOGGER.error("Configuration writer did not drain within the shutdown deadline");
    }

    private static JsonObject snapshot(ModConfig candidate) { return snapshot(candidate, preservedFields); }
    private static JsonObject snapshot(ModConfig candidate, JsonObject fields) {
        JsonObject snapshot = fields.deepCopy();
        for (String internal : new String[] {"useEasing", "easingIntensity", "cancelOnFallDamage",
                "cancelOnFire", "lowHealthThreshold", "thirdPartyMusic", "shotDurationTicks",
                "afkThresholdTicks", "fishingCinematicThresholdTicks"}) snapshot.remove(internal);
        GSON.toJsonTree(candidate).getAsJsonObject().entrySet().forEach(entry -> snapshot.add(entry.getKey(), entry.getValue()));
        return snapshot;
    }

    public static boolean isWriteProtected() {
        return writeProtected;
    }

    public static ModConfig getConfig() {
        return config;
    }

    /** Compatibility setter schedules the same ordered durable operation. */
    @Deprecated public static void setConfig(ModConfig value) { trySetConfigAsync(value); }
    /** Blocking compatibility calls are refused on the client thread. */
    @Deprecated public static boolean trySetConfig(ModConfig value) {
        if (net.minecraft.client.Minecraft.getInstance().isSameThread()) return false;
        var result = new java.util.concurrent.CompletableFuture<Boolean>();
        net.minecraft.client.Minecraft.getInstance().execute(() -> trySetConfigAsync(value).whenComplete((ok,error) -> {
            if (error == null) result.complete(ok); else result.completeExceptionally(error);
        }));
        try { return result.get(5, java.util.concurrent.TimeUnit.SECONDS); }
        catch (Exception failure) { return false; }
    }
}
