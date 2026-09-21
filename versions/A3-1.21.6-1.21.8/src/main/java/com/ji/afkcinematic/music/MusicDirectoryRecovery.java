package com.ji.afkcinematic.music;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/** Keeps local music paths usable without deleting files that occupy them. */
final class MusicDirectoryRecovery {
    private MusicDirectoryRecovery() {}

    static Path ensure(Path folder) throws IOException {
        Path parent = folder.getParent();
        if (parent != null && !Files.isDirectory(parent)) ensure(parent);
        if (Files.isDirectory(folder)) return folder;

        if (Files.exists(folder, LinkOption.NOFOLLOW_LINKS)) {
            Path backup;
            int attempt = 0;
            do {
                backup = folder.resolveSibling(folder.getFileName() + ".backup-" + System.currentTimeMillis()
                        + "-" + attempt++);
            } while (Files.exists(backup, LinkOption.NOFOLLOW_LINKS));
            Files.move(folder, backup);
        }
        Files.createDirectory(folder);
        return folder;
    }
}
