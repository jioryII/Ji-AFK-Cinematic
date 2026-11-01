package com.ji.afkcinematic.music;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.valueproviders.MultipliedFloats;
import java.util.*;

/** Prepared on the catalog worker, then atomically published on the client thread. */
final class ResolvedMusicCatalog {
    record Track(String id, Sound sound, SoundEvent event, int sources) {}
    record Snapshot(List<SoundEvent> declared, Map<String, Track> recordings) {}
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(SoundEventRegistration.class,
            new SoundEventRegistrationSerializer()).create();
    static Snapshot prepare(ResourceManager manager, List<ResourceLocation> declared) {
        Map<ResourceLocation,List<Sound>> events = new TreeMap<>();
        for (String namespace : new TreeSet<>(manager.getNamespaces())) {
            ResourceLocation file = ResourceLocation.fromNamespaceAndPath(namespace, "sounds.json");
            for (var resource : manager.getResourceStack(file)) {
                try (var reader = resource.openAsReader()) {
                    var json = JsonParser.parseReader(reader).getAsJsonObject();
                    for (var entry : json.entrySet()) {
                        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, entry.getKey());
                        try {
                            SoundEventRegistration registration = GSON.fromJson(entry.getValue(), SoundEventRegistration.class);
                            if (registration.isReplace()) events.remove(id);
                            events.computeIfAbsent(id, ignored -> new ArrayList<>()).addAll(registration.getSounds());
                        } catch (RuntimeException invalid) {
                            com.ji.afkcinematic.JiAFKCinematic.LOGGER.warn("Ignoring invalid sound event {} in cinematic catalog", id, invalid);
                        }
                    }
                } catch (Exception failure) {
                    com.ji.afkcinematic.JiAFKCinematic.LOGGER.warn("Ignoring unreadable sound definitions {} from {}", file, resource.sourcePackId(), failure);
                }
            }
        }
        Map<String,Track> result = new LinkedHashMap<>();
        for (ResourceLocation id : events.keySet()) {
            if (id.getNamespace().equals("minecraft") && (id.getPath().startsWith("music.") || id.getPath().startsWith("music_disc.")))
                add(manager, events, result, id, id.getPath().startsWith("music_disc.") ? 2 : 1);
        }
        for (ResourceLocation id : declared) add(manager, events, result, id, 4);
        return new Snapshot(declared.stream().map(SoundEvent::createVariableRangeEvent).toList(), Map.copyOf(result));
    }
    private static void add(ResourceManager manager, Map<ResourceLocation,List<Sound>> events, Map<String,Track> result, ResourceLocation root, int source) {
        if (CinematicMusicPolicy.isForbidden(root.toString())) return;
        Map<String,Sound> leaves = flatten(manager, events, root, new HashSet<>(), 0, new int[]{65536});
        for (var leaf : leaves.entrySet()) {
            if (result.size() >= 16384 && !result.containsKey(leaf.getKey())) throw new IllegalStateException("Music catalog recording limit");
            var previous = result.get(leaf.getKey());
            result.put(leaf.getKey(), previous == null ? new Track(leaf.getKey(), leaf.getValue(), SoundEvent.createVariableRangeEvent(root), source)
                    : new Track(previous.id, previous.sound, previous.event, previous.sources | source));
        }
    }
    private static Map<String,Sound> flatten(ResourceManager manager, Map<ResourceLocation,List<Sound>> events, ResourceLocation id, Set<ResourceLocation> visiting, int depth, int[] budget) {
        if (--budget[0] < 0 || Thread.currentThread().isInterrupted()) throw new IllegalStateException("Music graph traversal limit");
        Map<String,Sound> leaves = new LinkedHashMap<>();
        if (depth > 32 || CinematicMusicPolicy.isForbidden(id.toString()) || !visiting.add(id)) return leaves;
        for (Sound sound : events.getOrDefault(id, List.of())) {
            if (sound.getType() == Sound.Type.FILE) {
                if (!CinematicMusicPolicy.isForbidden(sound.getLocation().toString()) && manager.getResource(sound.getPath()).isPresent())
                    leaves.putIfAbsent(sound.getLocation().toString(), sound);
            } else {
                for (Sound child : flatten(manager, events, sound.getLocation(), visiting, depth + 1, budget).values()) {
                    Sound combined = new Sound(child.getLocation(), new MultipliedFloats(child.getVolume(), sound.getVolume()),
                            new MultipliedFloats(child.getPitch(), sound.getPitch()), child.getWeight(), Sound.Type.FILE,
                            child.shouldStream() || sound.shouldStream(), child.shouldPreload(), child.getAttenuationDistance());
                    leaves.putIfAbsent(child.getLocation().toString(), combined);
                }
            }
            if (leaves.size() > 16384) throw new IllegalArgumentException("Music event exceeds catalog limit");
        }
        visiting.remove(id);
        return leaves;
    }
}
