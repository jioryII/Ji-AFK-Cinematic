package com.ji.afkcinematic.music;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ji.afkcinematic.JiAFKCinematic;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
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
    private static volatile List<SoundEvent> tracks = List.of();

    private ThirdPartyMusicRegistry() {}

    @SuppressWarnings("deprecation")
    public static void init() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override public Identifier getFabricId() {
                        return Identifier.fromNamespaceAndPath(JiAFKCinematic.MOD_ID, "third_party_music");
                    }
                    @Override public void onResourceManagerReload(ResourceManager manager) { reloadFrom(manager); }
                });
    }

    public static List<SoundEvent> getTracks() { return tracks; }

    static void reloadFrom(ResourceManager manager) {
        LinkedHashSet<Identifier> merged = new LinkedHashSet<>();
        Map<Identifier, List<Resource>> manifests = manager.listResourceStacks(
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
                        Identifier id = Identifier.tryParse(sound);
                        if (id != null) merged.add(id);
                        else JiAFKCinematic.LOGGER.warn("Ignoring invalid cinematic music id '{}' from {}", element, resource.sourcePackId());
                    }
                } catch (Exception e) {
                    JiAFKCinematic.LOGGER.warn("Failed to read cinematic music manifest {} from {}", entry.getKey(), resource.sourcePackId(), e);
                }
            }
        });
        List<SoundEvent> loaded = new ArrayList<>(merged.size());
        for (Identifier id : merged) loaded.add(SoundEvent.createVariableRangeEvent(id));
        tracks = List.copyOf(loaded);
        CinematicMusicManager.onThirdPartyMusicReloaded();
        JiAFKCinematic.LOGGER.info("Loaded {} declared third-party cinematic music event(s)", tracks.size());
    }
}
