package com.ji.afkcinematic.config;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Objects;

/**
 * Persists UTF-8 configuration through a forced temporary file in the same directory.
 * A rolling backup holds the bytes preceding each replacement; the original backup
 * is created explicitly before migration and is never replaced by this service.
 * This repository is synchronous: its caller owns scheduling and lifecycle.
 */
public final class AtomicConfigFile {
    private final Path path;
    private final Mover mover;

    public AtomicConfigFile(Path path) {
        this(path, AtomicConfigFile::move);
    }

    AtomicConfigFile(Path path, Mover mover) {
        this.path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        this.mover = Objects.requireNonNull(mover, "mover");
    }

    public String read() throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    public Path backupPath() {
        return path.resolveSibling(path.getFileName() + ".bak");
    }

    public Path originalBackupPath() {
        return path.resolveSibling(path.getFileName() + ".original.bak");
    }

    /**
     * Confirms a readable original backup before a migration can write the source.
     * Existing backups are retained even when the live file has since changed.
     */
    public synchronized Path preserveOriginal() throws IOException {
        Path backup = originalBackupPath();
        if (readExisting(backup) != null) return backup;
        byte[] original = readExisting(path);
        if (original == null) throw new NoSuchFileException(path.toString());
        Files.createDirectories(path.getParent());
        try (TemporaryFile temporary = stage(original)) {
            try {
                // Do not use REPLACE_EXISTING or ATOMIC_MOVE here: atomic moves
                // may replace an existing target even without REPLACE_EXISTING.
                Files.move(temporary.path, backup);
                verify(backup, original);
            } catch (FileAlreadyExistsException alreadyCreated) {
                // Another repository instance may have won creation. Confirm
                // readability, but never replace that first original snapshot.
                if (readExisting(backup) == null) throw alreadyCreated;
            }
        }
        return backup;
    }

    /**
     * Replaces the configuration only after staging all UTF-8 bytes and confirming
     * the previous-file backup. Unsupported atomic moves use a verified replace;
     * a failed or incomplete replacement restores the previous bytes. If the
     * filesystem also refuses restoration, the confirmed .bak remains available
     * and the restoration failure is attached to the original exception.
     */
    public synchronized void write(String contents) throws IOException {
        byte[] next = Objects.requireNonNull(contents, "contents").getBytes(StandardCharsets.UTF_8);
        Files.createDirectories(path.getParent());
        try (TemporaryFile temporary = stage(next)) {
            byte[] previous = readExisting(path);
            if (previous != null) {
                try (TemporaryFile backup = stage(previous)) {
                    replace(backup.path, backupPath());
                    verify(backupPath(), previous);
                }
            }
            try {
                replace(temporary.path, path);
                verify(path, next);
            } catch (IOException | RuntimeException failure) {
                try {
                    restoreIfChanged(previous);
                } catch (IOException | RuntimeException restorationFailure) {
                    failure.addSuppressed(restorationFailure);
                }
                throw failure;
            }
        }
    }

    private TemporaryFile stage(byte[] contents) throws IOException {
        TemporaryFile temporary = new TemporaryFile(Files.createTempFile(
                path.getParent(), path.getFileName() + "-", ".tmp"));
        try {
            try (FileChannel channel = FileChannel.open(temporary.path, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(contents);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            return temporary;
        } catch (IOException | RuntimeException failure) {
            try {
                temporary.close();
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    private void replace(Path source, Path target) throws IOException {
        try {
            mover.move(source, target, true);
        } catch (AtomicMoveNotSupportedException unsupported) {
            mover.move(source, target, false);
        }
    }

    private void restoreIfChanged(byte[] previous) throws IOException {
        byte[] current;
        boolean readable = true;
        try {
            current = readExisting(path);
        } catch (IOException unreadable) {
            current = null;
            readable = false;
        }
        if (readable && Arrays.equals(previous, current)) return;
        if (previous == null) {
            Files.deleteIfExists(path);
            return;
        }
        try (TemporaryFile restoration = stage(previous)) {
            try {
                move(restoration.path, path, true);
            } catch (AtomicMoveNotSupportedException unsupported) {
                move(restoration.path, path, false);
            }
            verify(path, previous);
        }
    }

    private static byte[] readExisting(Path file) throws IOException {
        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (NoSuchFileException missing) {
            return null;
        }
        if (!attributes.isRegularFile()) {
            throw new IOException("Configuration or backup is not a regular file: " + file);
        }
        return Files.readAllBytes(file);
    }

    private static void verify(Path file, byte[] expected) throws IOException {
        if (!Arrays.equals(expected, readExisting(file))) {
            throw new IOException("Configuration replacement could not be verified: " + file);
        }
    }

    private static void move(Path source, Path target, boolean atomic) throws IOException {
        if (atomic) {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @FunctionalInterface
    interface Mover {
        void move(Path source, Path target, boolean atomic) throws IOException;
    }

    private static final class TemporaryFile implements AutoCloseable {
        private final Path path;

        private TemporaryFile(Path path) {
            this.path = path;
        }

        @Override
        public void close() throws IOException {
            Files.deleteIfExists(path);
        }
    }
}
