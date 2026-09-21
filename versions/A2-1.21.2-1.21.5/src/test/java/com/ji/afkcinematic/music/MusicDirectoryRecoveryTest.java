package com.ji.afkcinematic.music;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MusicDirectoryRecoveryTest {
    @TempDir Path temp;

    @Test
    void recreatesMissingMusicDirectory() throws Exception {
        Path music = temp.resolve("config/ji-afk-cinematic/music");
        MusicDirectoryRecovery.ensure(music);
        assertTrue(Files.isDirectory(music));
        Files.delete(music);
        MusicDirectoryRecovery.ensure(music);
        assertTrue(Files.isDirectory(music));
    }

    @Test
    void backsUpBlockingFileAndRecreatesDirectory() throws Exception {
        Path music = temp.resolve("music");
        Files.writeString(music, "original music data");
        MusicDirectoryRecovery.ensure(music);
        assertTrue(Files.isDirectory(music));
        try (var files = Files.list(temp)) {
            Path backup = files.filter(path -> path.getFileName().toString().startsWith("music.backup-"))
                    .findFirst().orElseThrow();
            assertEquals("original music data", Files.readString(backup));
        }
    }
}
