package com.ji.afkcinematic.music;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import com.ji.afkcinematic.config.AtomicConfigFile;
import com.ji.afkcinematic.files.CoalescingTextWriter;
import java.util.concurrent.CompletionStage;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Small persisted cooldown for tracks that actually reached the sound engine. */
final class RecentMusicHistory {
    private static final int MAX_ENTRIES = 8;
    // Eight accepted IDs of at most 255 UTF-16 code units fit even with UTF-8.
    private static final int MAX_FILE_BYTES = 8192;
    private final Deque<String> recent = new ArrayDeque<>();
    private final CoalescingTextWriter writer;
    private List<String> loadedEntries = List.of();
    private boolean writeProtected;
    @FunctionalInterface interface Reader { byte[] read(Path file) throws IOException; }
    private static final Reader FILE_READER = new FileReader();
    private static final class FileReader implements Reader {
        @Override public byte[] read(Path file) throws IOException { return readBounded(file); }
    }

    RecentMusicHistory(Path file) {
        this(file, FILE_READER);
    }

    RecentMusicHistory(Path file, Reader reader) {
        AtomicConfigFile repository = new AtomicConfigFile(file);
        writer = CoalescingTextWriter.withDeferredInitialization(text -> {
            if (writeProtected) throw new IOException("Unreadable history is retained");
            // A record accepted before loading completes contains only runtime entries.
            // Put disk entries first so newer records keep precedence and the same bound.
            Deque<String> snapshot = new ArrayDeque<>(loadedEntries);
            for (String id : text.split("\\R")) if (valid(id)) add(snapshot, id);
            repository.write(String.join("\n", snapshot) + "\n");
        }, "Ji-AFK-History-IO", () -> load(file, reader));
    }

    private static byte[] readBounded(Path file) throws IOException {
        if (!Files.isRegularFile(file)) return new byte[0];
        try (var input = Files.newInputStream(file)) { return input.readNBytes(MAX_FILE_BYTES + 1); }
    }

    private void load(Path file, Reader reader) {
        Deque<String> loaded = new ArrayDeque<>();
        try {
                byte[] bytes = reader.read(file);
                if (bytes.length > MAX_FILE_BYTES) throw new IOException("History exceeds the bounded file size");
                String text = StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes)).toString();
                for (String id : text.split("\\R")) {
                    if (valid(id)) add(loaded, id);
                }
        } catch (IOException | SecurityException ignored) {
            loaded.clear();
            writeProtected = true;
        }
        loadedEntries = List.copyOf(loaded);
        synchronized (this) {
            for (String id : recent) add(loaded, id);
            recent.clear();
            recent.addAll(loaded);
        }
    }

    boolean isReady() { return writer.isReady(); }
    CompletionStage<Void> readyAsync() { return writer.readyAsync(); }

    synchronized boolean contains(String id, int poolSize) {
        if (id == null || poolSize < 2) return false;
        int cooldown = Math.min(4, Math.max(1, (poolSize - 1) / 2));
        int checked = 0;
        for (var iterator = recent.descendingIterator(); iterator.hasNext() && checked < cooldown; checked++) {
            if (id.equals(iterator.next())) return true;
        }
        return false;
    }

    synchronized void record(String id) {
        if (!valid(id)) return;
        add(recent, id);
        writer.write(String.join("\n", List.copyOf(recent)) + "\n");
    }

    CompletionStage<Void> flushAsync() { return writer.flushAsync(); }
    CompletionStage<Void> closeAsync() { return writer.closeAsync(); }

    private static void add(Deque<String> entries, String id) {
        entries.remove(id);
        entries.addLast(id);
        while (entries.size() > MAX_ENTRIES) entries.removeFirst();
    }

    private static boolean valid(String id) {
        return id != null && !id.isBlank() && id.length() < 256
                && !id.contains("\n") && !id.contains("\r");
    }
}
