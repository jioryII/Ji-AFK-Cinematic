package com.ji.afkcinematic.music;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ji.afkcinematic.JiAFKCinematic;
import com.ji.afkcinematic.platform.ClientEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class ThirdPartyMusicRegistry {
    private static final String MANIFEST_PATH = "ji_afk_cinematic/music.json";
    private static volatile ResolvedMusicCatalog.Snapshot snapshot = new ResolvedMusicCatalog.Snapshot(List.of(), Map.of());

    private ThirdPartyMusicRegistry() {}

    private static final com.ji.afkcinematic.files.ManagedJobs CATALOG = new com.ji.afkcinematic.files.ManagedJobs("Ji-AFK-Catalog");
    public static void shutdown() { CATALOG.close(); }
    private static boolean initialized;
    public static void init() {
        if (initialized) return;
        initialized = true;
        ClientEvents.RESOURCE_RELOAD.register(ThirdPartyMusicRegistry::reloadFrom);
    }

    public static List<SoundEvent> getTracks() { return snapshot.declared(); }

    static Map<String, ResolvedMusicCatalog.Track> recordings() { return snapshot.recordings(); }

    static void reloadFrom(ResourceManager manager) {
        long token = CATALOG.replace();
        CATALOG.submit(() -> ResolvedMusicCatalog.prepare(manager, prepare(manager))).whenComplete((identifiers, failure) -> net.minecraft.client.Minecraft.getInstance().execute(() -> {
            if (!CATALOG.current(token)) return;
            if (failure != null) { JiAFKCinematic.LOGGER.warn("Music catalog preparation failed; previous snapshot retained", failure); return; }
        snapshot = identifiers;
        CinematicMusicManager.onThirdPartyMusicReloaded();
        JiAFKCinematic.LOGGER.info("Loaded {} cinematic recordings ({} declared custom events)", snapshot.recordings().size(), snapshot.declared().size());
        }));
    }
    private static List<ResourceLocation> prepare(ResourceManager manager) {
        LinkedHashSet<ResourceLocation> merged = new LinkedHashSet<>();
        Map<ResourceLocation, List<Resource>> manifests = manager.listResourceStacks(
                "ji_afk_cinematic", id -> id.getPath().equals(MANIFEST_PATH));
        manifests.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (json.has("replace") && json.get("replace").getAsBoolean()) merged.clear();
                    if (!json.has("tracks") || !json.get("tracks").isJsonArray()) continue;
                    for (JsonElement element : json.getAsJsonArray("tracks")) {
                        String sound = element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
                                ? element.getAsString()
                                : element.isJsonObject() && element.getAsJsonObject().has("sound")
                                    ? element.getAsJsonObject().get("sound").getAsString() : null;
                        if (sound == null) continue;
                        ResourceLocation id = ResourceLocation.tryParse(sound);
                        if (id != null) merged.add(id);
                        else JiAFKCinematic.LOGGER.warn("Ignoring invalid cinematic music id '{}' from {}", element, resource.sourcePackId());
                    }
                } catch (Exception e) {
                    JiAFKCinematic.LOGGER.warn("Failed to read cinematic music manifest {} from {}", entry.getKey(), resource.sourcePackId(), e);
                }
            }
        });
        return List.copyOf(merged);
    }
}
