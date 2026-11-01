package com.ji.afkcinematic.music;

import com.ji.afkcinematic.config.AtomicConfigFile;
import com.ji.afkcinematic.files.CoalescingTextWriter;
import com.ji.afkcinematic.random.RandomStreams;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.random.RandomGenerator;

/** IO is serialized off the client thread; only confirmed playback advances either queue. */
public final class PersistentMusicSchedule {
    public record Reservation(String id, boolean custom, boolean bothAvailable) {}
    private final RecordingQueue vanilla, custom;
    private final RandomGenerator sources;
    private final CoalescingTextWriter writer;
    private int balance;
    private boolean protectedFile;
    public PersistentMusicSchedule(Path file) {
        this(file, new SplittableRandom(RandomStreams.seed(RandomStreams.Domain.MUSIC)),
                new SplittableRandom(RandomStreams.seed(RandomStreams.Domain.MUSIC_SOURCE)));
    }
    PersistentMusicSchedule(Path file, RandomGenerator random, RandomGenerator sources) {
        vanilla = new RecordingQueue(random); custom = new RecordingQueue(random); this.sources = sources;
        var repository = new AtomicConfigFile(file);
        writer = CoalescingTextWriter.withDeferredInitialization(text -> {
            if (protectedFile) throw new java.io.IOException("Unrecognized schedule retained");
            repository.write(text);
        }, "Ji-AFK-Music-Schedule", () -> load(file));
    }
    public boolean ready() { writer.readyAsync(); return writer.isReady(); }
    public CompletionStage<Void> closeAsync() { return writer.closeAsync(); }
    public synchronized void reconcile(Collection<String> vanillaIds, Collection<String> customIds) {
        if (!writer.isReady()) return;
        vanilla.reconcile(vanillaIds); custom.reconcile(customIds); save();
    }
    public synchronized Reservation reserve(boolean useVanilla, boolean useCustom, java.util.function.Predicate<String> eligible) {
        if (!writer.isReady()) return null;
        String v = useVanilla ? vanilla.reserve(eligible) : null;
        String c = useCustom ? custom.reserve(eligible) : null;
        if (v == null && c == null) return null;
        boolean chooseCustom = c != null && (v == null || (balance >= 2 || (balance > -2 && sources.nextBoolean())));
        return new Reservation(chooseCustom ? c : v, chooseCustom, v != null && c != null);
    }
    public synchronized void confirm(Reservation reservation, boolean mixed) {
        if (reservation == null) return;
        if ((reservation.custom ? custom : vanilla).confirm(reservation.id)) {
            if (mixed && reservation.bothAvailable) balance = Math.max(-2, Math.min(2, balance + (reservation.custom ? -1 : 1)));
            save();
        }
    }
    private void save() {
        StringBuilder text = new StringBuilder("JI_AFK_RECORDINGS_1\n").append(balance).append('\n');
        append(text, "VR", vanilla.remaining()); append(text, "VP", vanilla.played());
        append(text, "CR", custom.remaining()); append(text, "CP", custom.played());
        if (vanilla.last() != null) append(text, "VL", List.of(vanilla.last()));
        if (custom.last() != null) append(text, "CL", List.of(custom.last()));
        writer.write(text.toString());
    }
    private static void append(StringBuilder text, String kind, Collection<String> ids) {
        for (String id : ids) text.append(kind).append(' ').append(Base64.getUrlEncoder().withoutPadding()
                .encodeToString(id.getBytes(StandardCharsets.UTF_8))).append('\n');
    }
    private void load(Path file) {
        if (!Files.exists(file)) return;
        try {
            byte[] bytes;
            try (var input = Files.newInputStream(file)) { bytes = input.readNBytes(16_777_217); }
            if (bytes.length > 16_777_216) throw new IllegalArgumentException("Schedule too large");
            var lines = new String(bytes, StandardCharsets.UTF_8).lines().toList();
            if (lines.size() < 2 || !lines.getFirst().equals("JI_AFK_RECORDINGS_1")) throw new IllegalArgumentException("Schedule version");
            Map<String,List<String>> parts = new HashMap<>();
            for (String line : lines.subList(2, lines.size())) {
                String[] pair = line.split(" ", 2);
                if (pair.length != 2 || !Set.of("VR","VP","CR","CP","VL","CL").contains(pair[0])) throw new IllegalArgumentException("Schedule entry");
                String id = new String(Base64.getUrlDecoder().decode(pair[1]), StandardCharsets.UTF_8);
                if (id.length() > 512 || !id.contains(":")) throw new IllegalArgumentException("Recording id");
                parts.computeIfAbsent(pair[0], ignored -> new ArrayList<>()).add(id);
            }
            synchronized (this) {
                balance = Math.max(-2, Math.min(2, Integer.parseInt(lines.get(1))));
                vanilla.restore(parts.getOrDefault("VR", List.of()), new LinkedHashSet<>(parts.getOrDefault("VP",List.of())), parts.containsKey("VL") ? parts.get("VL").getFirst() : null);
                custom.restore(parts.getOrDefault("CR", List.of()), new LinkedHashSet<>(parts.getOrDefault("CP",List.of())), parts.containsKey("CL") ? parts.get("CL").getFirst() : null);
            }
        } catch (Exception invalid) { protectedFile = true; }
    }
}
