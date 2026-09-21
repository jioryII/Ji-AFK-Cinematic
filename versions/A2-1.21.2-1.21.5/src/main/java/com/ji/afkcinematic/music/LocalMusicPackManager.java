package com.ji.afkcinematic.music;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ji.afkcinematic.JiAFKCinematic;
import com.ji.afkcinematic.config.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Builds an always-enabled local resource pack from config/ji-afk-cinematic/music/*.ogg. */
public final class LocalMusicPackManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PACK_ID = "file/ji-afk-cinematic-local";
    private static final String NAMESPACE = "ji_afk_cinematic_local";
    private static final String FINGERPRINT_FILE = ".ji-afk-source.sha256";

    private static int startupTrackCount;
    private LocalMusicPackManager() {}

    public static Path getMusicFolder() {
        return resolveMusicFolder(ConfigManager.getConfig().customMusicDirectory);
    }

    private static Path resolveMusicFolder(String configuredPath) {
        if (configuredPath != null && !configuredPath.isBlank()) {
            try { return Path.of(configuredPath).toAbsolutePath().normalize(); }
            catch (Exception e) { JiAFKCinematic.LOGGER.warn("Invalid custom music directory: {}", configuredPath); }
        }
        return FabricLoader.getInstance().getConfigDir().resolve("ji-afk-cinematic").resolve("music");
    }

    public static void initialize() {
        startupTrackCount = rebuild(false);
    }

    public static void completeStartupLoad(MinecraftClient client) {
        if (startupTrackCount == 0) return;
        startupTrackCount = 0;
        if (!client.options.resourcePacks.contains(PACK_ID)) client.options.resourcePacks.add(PACK_ID);
        client.options.incompatibleResourcePacks.remove(PACK_ID);
        client.options.write();
        client.getResourcePackManager().scanPacks();
        client.getResourcePackManager().setEnabledProfiles(client.options.resourcePacks);
        // The generated pack was selected during client initialization and joins
        // Minecraft's normal first resource load. Reloading here showed Mojang's
        // loading screen a second time.
        CinematicMusicManager.onResourcesReloaded();
    }

    public static void openMusicFolder() { openMusicFolder(ConfigManager.getConfig().customMusicDirectory); }
    public static void openMusicFolder(String configuredPath) {
        try {
            Path folder = MusicDirectoryRecovery.ensure(resolveMusicFolder(configuredPath));
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            ProcessBuilder opener = os.contains("win")
                    ? new ProcessBuilder("explorer.exe", folder.toString())
                    : os.contains("mac") ? new ProcessBuilder("open", folder.toString())
                    : new ProcessBuilder("xdg-open", folder.toString());
            opener.start();
        } catch (Exception e) {
            JiAFKCinematic.LOGGER.warn("Could not open local music folder", e);
        }
    }

    public static String chooseMusicFolder(String configuredPath) {
        try {
            Path initial = MusicDirectoryRecovery.ensure(resolveMusicFolder(configuredPath));
            if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
                String selected = chooseWindowsMusicFolder(initial);
                if (selected != null) return selected.isBlank() ? null : selected;
            }
            Class<?> dialogs = Class.forName("org.lwjgl.util.tinyfd.TinyFileDialogs");
            Method selectFolder = dialogs.getMethod("tinyfd_selectFolderDialog", CharSequence.class, CharSequence.class);
            String selected = (String) selectFolder.invoke(null, "Choose cinematic music folder", initial.toString());
            if (selected == null || selected.isBlank()) return null;
            return MusicDirectoryRecovery.ensure(Path.of(selected)).toAbsolutePath().normalize().toString();
        } catch (Exception e) {
            JiAFKCinematic.LOGGER.warn("Could not choose a custom music folder", e);
            return null;
        }
    }

    /** Opens the native picker without blocking Minecraft's render thread. */
    public static void chooseMusicFolderAsync(String configuredPath, Consumer<String> callback) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean restoreFullscreen = client.getWindow().isFullscreen();
        if (restoreFullscreen) client.getWindow().toggleFullscreen();

        Thread pickerThread = new Thread(() -> {
            if (restoreFullscreen) {
                try { Thread.sleep(250L); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            String selected = chooseMusicFolder(configuredPath);
            client.execute(() -> {
                try {
                    callback.accept(selected);
                } finally {
                    if (restoreFullscreen && client.options.getFullscreen().getValue()
                            && !client.getWindow().isFullscreen()) {
                        client.getWindow().toggleFullscreen();
                    }
                }
            });
        }, "Ji-AFK-Music-Folder-Picker");
        pickerThread.setDaemon(true);
        pickerThread.start();
    }

    private static String chooseWindowsMusicFolder(Path initial) throws Exception {
        Path script = Files.createTempFile("ji-afk-folder-picker-", ".ps1");
        try (var source = LocalMusicPackManager.class.getResourceAsStream("/ji-afk-modern-folder-picker.ps1")) {
            if (source == null) return null;
            Files.copy(source, script, StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            ProcessBuilder picker = new ProcessBuilder("powershell.exe", "-NoProfile", "-STA", "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-File", script.toString());
            picker.environment().put("JI_AFK_INITIAL_MUSIC_FOLDER", initial.toString());
            picker.environment().put("JI_AFK_FOLDER_PICKER_TITLE", net.minecraft.text.Text.translatable("config.ji_afkcinematic.change_music_folder").getString());
            picker.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process process = picker.start();
            String selected = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.waitFor() != 0) return null;
            if (selected.isBlank()) return "";
            return MusicDirectoryRecovery.ensure(Path.of(selected)).toAbsolutePath().normalize().toString();
        } finally {
            Files.deleteIfExists(script);
        }
    }

    public static int rebuildAndReload() {
        return rebuild(true);
    }

    /** Copies and indexes local tracks away from the render thread, then reloads on it. */
    public static void rebuildAndReloadAsync(Consumer<Integer> callback) {
        MinecraftClient client = MinecraftClient.getInstance();
        Thread rebuildThread = new Thread(() -> {
            int trackCount = rebuild(false, false);
            client.execute(() -> activateGeneratedPack(client, true, () -> callback.accept(trackCount)));
        }, "Ji-AFK-Music-Pack-Rebuild");
        rebuildThread.setDaemon(true);
        rebuildThread.start();
    }

    /** Detects additions, removals, renames, or content changes in local .ogg files. */
    public static boolean hasSourceChanges() {
        try {
            Path musicFolder = MusicDirectoryRecovery.ensure(getMusicFolder());
            Path marker = getGeneratedPackFolder().resolve(FINGERPRINT_FILE);
            return !Files.exists(marker) || !Files.readString(marker, StandardCharsets.UTF_8)
                    .equals(calculateSourceFingerprint(musicFolder));
        } catch (Exception e) {
            JiAFKCinematic.LOGGER.warn("Could not inspect the local music folder", e);
            return true;
        }
    }

    private static int rebuild(boolean reload) {
        return rebuild(reload, true);
    }

    private static int rebuild(boolean reload, boolean activatePack) {
        try {
            Path musicFolder = MusicDirectoryRecovery.ensure(getMusicFolder());
            Path pack = getGeneratedPackFolder();
            Path sounds = pack.resolve("assets").resolve(NAMESPACE).resolve("sounds").resolve("music");
            MusicDirectoryRecovery.ensure(sounds);
            try (var oldFiles = Files.list(sounds)) {
                for (Path old : oldFiles.filter(Files::isRegularFile).toList()) Files.deleteIfExists(old);
            }

            Map<String, Object> soundDefinitions = new LinkedHashMap<>();
            List<String> manifestTracks = new ArrayList<>();
            int index = 0;
            try (var files = Files.list(musicFolder)) {
                for (Path source : files.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg"))
                        .sorted().toList()) {
                    String base = source.getFileName().toString().replaceFirst("(?i)\\.ogg$", "");
                    String slug = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]+", "_");
                    if (slug.isBlank()) slug = "track";
                    slug = slug + "_" + index++;
                    Files.copy(source, sounds.resolve(slug + ".ogg"), StandardCopyOption.REPLACE_EXISTING);
                    Map<String, Object> sound = Map.of("sounds", List.of(Map.of(
                            "name", NAMESPACE + ":music/" + slug, "stream", true)));
                    soundDefinitions.put("local." + slug, sound);
                    manifestTracks.add(NAMESPACE + ":local." + slug);
                }
            }

            Map<String, Object> packMeta = Map.of("pack", Map.of(
                    "pack_format", 34,
                    "supported_formats", Map.of("min_inclusive", 34, "max_inclusive", 999),
                    "description", "Ji AFK Cinematic local music"));
            Files.writeString(pack.resolve("pack.mcmeta"), GSON.toJson(packMeta), StandardCharsets.UTF_8);
            Files.writeString(pack.resolve("assets").resolve(NAMESPACE).resolve("sounds.json"),
                    GSON.toJson(soundDefinitions), StandardCharsets.UTF_8);
            Path manifest = pack.resolve("assets").resolve(NAMESPACE).resolve("ji_afk_cinematic").resolve("music.json");
            Files.createDirectories(manifest.getParent());
            Files.writeString(manifest, GSON.toJson(Map.of("replace", false, "tracks", manifestTracks)), StandardCharsets.UTF_8);
            Files.writeString(pack.resolve(FINGERPRINT_FILE), calculateSourceFingerprint(musicFolder), StandardCharsets.UTF_8);

            MinecraftClient client = MinecraftClient.getInstance();
            if (activatePack && client != null && client.options != null) {
                activateGeneratedPack(client, reload, () -> {});
            }
            JiAFKCinematic.LOGGER.info("Prepared {} local cinematic music track(s)", manifestTracks.size());
            return manifestTracks.size();
        } catch (Exception e) {
            JiAFKCinematic.LOGGER.warn("Could not rebuild local cinematic music pack", e);
            return 0;
        }
    }

    private static void activateGeneratedPack(MinecraftClient client, boolean reload, Runnable completion) {
        if (!client.options.resourcePacks.contains(PACK_ID)) client.options.resourcePacks.add(PACK_ID);
        client.options.incompatibleResourcePacks.remove(PACK_ID);
        client.options.write();
        client.getResourcePackManager().scanPacks();
        client.getResourcePackManager().setEnabledProfiles(client.options.resourcePacks);
        CinematicMusicManager.onThirdPartyMusicReloaded();
        if (!reload) {
            completion.run();
            return;
        }
        client.reloadResources().whenComplete((unused, error) -> client.execute(() -> {
            if (error != null) {
                JiAFKCinematic.LOGGER.warn("Could not reload resources after updating local music", error);
            } else {
                CinematicMusicManager.onResourcesReloaded();
            }
            completion.run();
        }));
    }

    private static Path getGeneratedPackFolder() {
        return FabricLoader.getInstance().getGameDir().resolve("resourcepacks").resolve("ji-afk-cinematic-local");
    }

    private static String calculateSourceFingerprint(Path musicFolder) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var files = Files.list(musicFolder)) {
            for (Path source : files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg"))
                    .sorted().toList()) {
                digest.update(source.getFileName().toString().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(Files.readAllBytes(source));
                digest.update((byte) 0);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
