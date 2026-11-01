package com.ji.afkcinematic.config;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Ordered, bounded background persistence. Each accepted snapshot receives its own result.
 * AtomicConfigFile owns backup/rollback; the adapter commits client state only after success.
 */
public final class SerialConfigWriter {
    @FunctionalInterface interface Writer { void write(String snapshot) throws IOException; }
    private final Writer writer;
    private final ThreadPoolExecutor worker;
    public SerialConfigWriter(AtomicConfigFile file) { this(file::write); }
    SerialConfigWriter(Writer writer) {
        this.writer = Objects.requireNonNull(writer);
        worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(64), task -> {
            var thread = new Thread(task, "Ji-AFK-Config-IO"); thread.setDaemon(true); return thread;
        });
    }
    public CompletableFuture<Void> write(String snapshot) {
        Objects.requireNonNull(snapshot);
        var result = new CompletableFuture<Void>();
        try {
            worker.execute(() -> {
                try { writer.write(snapshot); result.complete(null); }
                catch (IOException | RuntimeException error) { result.completeExceptionally(error); }
            });
        } catch (java.util.concurrent.RejectedExecutionException error) { result.completeExceptionally(error); }
        return result;
    }
    /** Stop accepting new writes and drain accepted work, with a bounded shutdown wait. */
    public boolean shutdown(Duration timeout) {
        worker.shutdown();
        try { return worker.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); return false; }
    }
}
