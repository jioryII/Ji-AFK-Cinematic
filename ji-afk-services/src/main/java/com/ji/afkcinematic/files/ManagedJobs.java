package com.ji.afkcinematic.files;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
/** Serial work queue with generation ownership and bounded shutdown. */
public final class ManagedJobs implements AutoCloseable {
    private final AtomicLong generation = new AtomicLong();
    private final ExecutorService executor;
    public ManagedJobs(String name) {
        executor = Executors.newSingleThreadExecutor(task -> { Thread t = new Thread(task, name); t.setDaemon(true); return t; });
    }
    public long replace() { return generation.incrementAndGet(); }
    public boolean current(long id) { return id == generation.get() && !executor.isShutdown(); }
    public void cancel() { generation.incrementAndGet(); }
    public <T> CompletableFuture<T> submit(Supplier<T> work) {
        try { return CompletableFuture.supplyAsync(work, executor); }
        catch (RejectedExecutionException closed) { return CompletableFuture.failedFuture(closed); }
    }
    public boolean drain(Duration timeout) {
        executor.shutdown();
        try { return executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
    }
    @Override public void close() { cancel(); executor.shutdownNow(); }
}
