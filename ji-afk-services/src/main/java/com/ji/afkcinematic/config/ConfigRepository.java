package com.ji.afkcinematic.config;
import com.ji.afkcinematic.files.ManagedJobs;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
/** Shared file ownership. Decoders return isolated snapshots on the repository worker. */
public final class ConfigRepository {
    private final Path path;
    private final AtomicConfigFile file;
    private final ManagedJobs jobs = new ManagedJobs("Ji-AFK-Configuration");
    public ConfigRepository(Path path) { this.path = path; file = new AtomicConfigFile(path); }
    public <T> CompletableFuture<T> read(java.util.function.Function<String,T> decode) {
        return jobs.submit(() -> {
            try { return decode.apply(Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : null); }
            catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        });
    }
    public CompletableFuture<Void> write(String content) { return jobs.submit(() -> { writeNow(content); return null; }); }
    public void writeNow(String content) {
        try { file.write(content); } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }
    public Path preserveOriginal() throws java.io.IOException { return file.preserveOriginal(); }
    public boolean shutdown(Duration timeout) { return jobs.drain(timeout); }
}
