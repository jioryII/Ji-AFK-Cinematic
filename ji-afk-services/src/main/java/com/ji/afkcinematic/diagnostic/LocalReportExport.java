package com.ji.afkcinematic.diagnostic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Runs only on explicit export. No repository, queue for later sending or network code. */
public final class LocalReportExport {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "Ji AFK local report export"); thread.setDaemon(true); return thread;
    });
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);
    private LocalReportExport() {}

    public static CompletableFuture<Path> export(Path configDirectory, DiagnosticReport.Preview preview) {
        // Capture exact preview bytes before leaving the client thread.
        byte[] bytes = preview.utf8();
        Path directory = configDirectory.toAbsolutePath().normalize().resolve("ji-afk-cinematic-reports");
        return CompletableFuture.supplyAsync(() -> {
            Path temporary = null;
            try {
                Files.createDirectories(directory);
                if (Files.isSymbolicLink(directory)) throw new IOException("Report directory is a symbolic link");
                Path file = directory.resolve("ji-afk-report-" + DATE.format(Instant.now()) + "-" + UUID.randomUUID() + ".json");
                temporary = Files.createTempFile(directory, ".ji-report-", ".tmp");
                Files.write(temporary, bytes);
                try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException unavailable) { Files.move(temporary, file); }
                return file;
            } catch (IOException error) { throw new CompletionException(error); }
            finally {
                if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { /* preserve primary error */ }
            }
        }, IO);
    }
}
