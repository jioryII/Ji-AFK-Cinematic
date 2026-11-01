package com.ji.afkcinematic.music;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** File-only preparation. Originals stay in the chosen folder; the generated pack is disposable. */
public final class LocalMusicLibrary {
    public record Track(Path source, String id, String sha256) { }
    public record Scan(List<Track> tracks, int skipped, String fingerprint) {
        public Scan { tracks = List.copyOf(tracks); }
    }
    private LocalMusicLibrary() { }

    public static Scan scan(Path folder, String resourceMetadata) throws IOException {
        Files.createDirectories(folder);
        Path sourceRoot = folder.toRealPath();
        MessageDigest fingerprint = sha256();
        fingerprint.update(resourceMetadata.getBytes(StandardCharsets.UTF_8));
        List<Track> tracks = new ArrayList<>();
        int skipped = 0;
        try (var files = Files.walk(sourceRoot)) {
            for (Path file : files.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)).sorted().toList()) {
                String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!name.endsWith(".ogg")) {
                    if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".flac") || name.endsWith(".opus")) skipped++;
                    continue;
                }
                String relative = sourceRoot.relativize(file).toString().replace('\\', '/');
                // Generated identities must not hide a recording excluded by its source path.
                if (CinematicMusicPolicy.isForbidden(relative) || !vorbis(file)) { skipped++; continue; }
                String id = "track_" + HexFormat.of().formatHex(sha256().digest(relative.getBytes(StandardCharsets.UTF_8)));
                String content;
                try (var input = new DigestInputStream(Files.newInputStream(file), sha256())) {
                    input.transferTo(java.io.OutputStream.nullOutputStream());
                    content = HexFormat.of().formatHex(input.getMessageDigest().digest());
                }
                fingerprint.update(relative.getBytes(StandardCharsets.UTF_8));
                fingerprint.update((byte) 0);
                fingerprint.update(content.getBytes(StandardCharsets.US_ASCII));
                tracks.add(new Track(file, id, content));
            }
        }
        if (tracks.isEmpty() && skipped > 0) throw new IOException("No playable Ogg Vorbis tracks; previous pack retained");
        return new Scan(tracks, skipped, HexFormat.of().formatHex(fingerprint.digest()));
    }

    public static void copyTracks(Scan scan, Path sounds) throws IOException {
        Files.createDirectories(sounds);
        for (Track track : scan.tracks()) {
            try (var input = new DigestInputStream(Files.newInputStream(track.source()), sha256())) {
                Files.copy(input, sounds.resolve(track.id() + ".ogg"));
                if (!HexFormat.of().formatHex(input.getMessageDigest().digest()).equals(track.sha256()))
                    throw new IOException("Music changed during preparation; retry after the copy finishes");
            }
        }
    }

    public static void requireIndependentSource(Path source, List<Path> outputs) throws IOException {
        Path folder = resolved(source.toAbsolutePath().normalize());
        for (Path output : outputs) {
            Path owned = resolved(output.toAbsolutePath().normalize());
            if (folder.startsWith(owned) || owned.startsWith(folder))
                throw new IOException("Choose a source folder separate from generated music/cache/export folders");
        }
    }
    private static Path resolved(Path path) throws IOException {
        if (Files.exists(path)) return path.toRealPath();
        return path.getParent() == null ? path : resolved(path.getParent()).resolve(path.getFileName());
    }

    private static boolean vorbis(Path file) throws IOException {
        byte[] bytes;
        try (var input = Files.newInputStream(file)) { bytes = input.readNBytes(4096); }
        if (bytes.length < 35 || bytes[0] != 'O' || bytes[1] != 'g' || bytes[2] != 'g' || bytes[3] != 'S') return false;
        byte[] signature = {1, 'v', 'o', 'r', 'b', 'i', 's'};
        for (int i = 27; i <= bytes.length - signature.length; i++) {
            boolean matches = true;
            for (int j = 0; j < signature.length; j++) matches &= bytes[i + j] == signature[j];
            if (matches) return true;
        }
        return false;
    }

    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
