package com.ji.afkcinematic.music;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ji.afkcinematic.JiAFKCinematic;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.awt.Desktop;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds an always-enabled local resource pack from config/ji-afk-cinematic/music/*.ogg. */
public final class LocalMusicPackManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PACK_ID = "file/ji-afk-cinematic-local";
    private static final String NAMESPACE = "ji_afk_cinematic_local";
    private static final String FINGERPRINT_FILE = ".ji-afk-source.sha256";

    private LocalMusicPackManager() {}

    public static Path getMusicFolder() {
        return FabricLoader.getInstance().getConfigDir().resolve("ji-afk-cinematic").resolve("music");
    }

    public static void initialize() {
        rebuild(false);
    }

    public static void openMusicFolder() {
        try {
            Path folder = getMusicFolder();
            Files.createDirectories(folder);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(folder.toFile());
        } catch (Exception e) {
            JiAFKCinematic.LOGGER.warn("Could not open local music folder", e);
        }
    }

    public static int rebuildAndReload() {
        return rebuild(true);
    }

    /** Detects additions, removals, renames, or content changes in local .ogg files. */
    public static boolean hasSourceChanges() {
        try {
            Path musicFolder = getMusicFolder();
            Files.createDirectories(musicFolder);
            Path marker = getGeneratedPackFolder().resolve(FINGERPRINT_FILE);
            return !Files.exists(marker) || !Files.readString(marker, StandardCharsets.UTF_8)
                    .equals(calculateSourceFingerprint(musicFolder));
        } catch (Exception e) {
            JiAFKCinematic.LOGGER.warn("Could not inspect the local music folder", e);
            return true;
        }
    }

    private static int rebuild(boolean reload) {
        try {
            Path musicFolder = getMusicFolder();
            Files.createDirectories(musicFolder);
            Path pack = getGeneratedPackFolder();
            Path sounds = pack.resolve("assets").resolve(NAMESPACE).resolve("sounds").resolve("music");
            Files.createDirectories(sounds);
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
            if (client != null && client.options != null) {
                if (!client.options.resourcePacks.contains(PACK_ID)) client.options.resourcePacks.add(PACK_ID);
                client.options.incompatibleResourcePacks.remove(PACK_ID);
                client.options.write();
                // Updating options alone does not update the live pack manager. Rescan
                // first so a newly generated local pack participates in this reload.
                client.getResourcePackManager().scanPacks();
                client.getResourcePackManager().setEnabledProfiles(client.options.resourcePacks);
                CinematicMusicManager.onThirdPartyMusicReloaded();
                if (reload) {
                    client.reloadResources().whenComplete((unused, error) -> {
                        if (error != null) {
                            JiAFKCinematic.LOGGER.warn("Could not reload resources after updating local music", error);
                        } else {
                            client.execute(CinematicMusicManager::onResourcesReloaded);
                        }
                    });
                }
            }
            JiAFKCinematic.LOGGER.info("Prepared {} local cinematic music track(s)", manifestTracks.size());
            return manifestTracks.size();
        } catch (Exception e) {
            JiAFKCinematic.LOGGER.warn("Could not rebuild local cinematic music pack", e);
            return 0;
        }
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
