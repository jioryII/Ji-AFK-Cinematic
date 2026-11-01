package com.ji.afkcinematic.files;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/** Serial IO with one running snapshot and at most one newer pending snapshot. */
public final class CoalescingTextWriter {
    public enum Result { WRITTEN, SUPERSEDED }
    @FunctionalInterface public interface Writer { void write(String text) throws IOException; }
    @FunctionalInterface public interface Initializer { void initialize() throws IOException; }
    private record Request(String text, CompletableFuture<Result> completion) {}
    private final Writer writer;
    private final ExecutorService executor;
    private final Initializer initializer;
    private final CompletableFuture<Void> ready;
    private boolean initializationScheduled;
    private Request pending;
    private boolean running;
    private boolean closed;
    private CompletableFuture<Void> idle;

    public CoalescingTextWriter(Writer writer, String threadName) {
        this(writer, threadName, null);
    }

    /** Initialization uses the same serial worker, before any accepted write. */
    public CoalescingTextWriter(Writer writer, String threadName, Initializer initializer) {
        this(writer, threadName, initializer, false);
    }

    /** Starts initialization on readiness, first write or close, never from construction. */
    public static CoalescingTextWriter withDeferredInitialization(Writer writer, String threadName, Initializer initializer) {
        return new CoalescingTextWriter(writer, threadName, Objects.requireNonNull(initializer), true);
    }

    private CoalescingTextWriter(Writer writer, String threadName, Initializer initializer, boolean deferred) {
        this.writer = Objects.requireNonNull(writer, "writer");
        this.initializer = initializer;
        Objects.requireNonNull(threadName, "threadName");
        executor = Executors.newSingleThreadExecutor(job -> {
            Thread thread = new Thread(job, threadName);
            thread.setDaemon(true);
            return thread;
        });
        ready = initializer == null ? CompletableFuture.completedFuture(null) : new CompletableFuture<>();
        idle = ready;
        initializationScheduled = initializer == null;
        if (!deferred) ensureInitializationScheduled();
    }

    public synchronized CompletionStage<Void> readyAsync() {
        ensureInitializationScheduled();
        return ready;
    }

    private synchronized void ensureInitializationScheduled() {
        if (!initializationScheduled) {
            initializationScheduled = true;
            executor.execute(this::runInitialization);
        }
    }

    public boolean isReady() { return ready.isDone() && !ready.isCompletedExceptionally(); }

    private void runInitialization() {
        try {
            initializer.initialize();
            ready.complete(null);
        } catch (IOException | RuntimeException failure) {
            ready.completeExceptionally(failure);
        }
    }

    /** The string is an immutable snapshot; this method performs no filesystem IO. */
    public synchronized CompletionStage<Result> write(String text) {
        Objects.requireNonNull(text, "text");
        if (closed) return CompletableFuture.failedFuture(new RejectedExecutionException("Writer is closed"));
        ensureInitializationScheduled();
        Request previous = pending;
        Request next = new Request(text, new CompletableFuture<>());
        pending = next;
        if (!running) {
            running = true;
            idle = new CompletableFuture<>();
            executor.execute(this::drain);
        }
        if (previous != null) previous.completion.complete(Result.SUPERSEDED);
        return next.completion;
    }

    /** Completes when the current batch, including coalesced replacements, drains. */
    public synchronized CompletionStage<Void> flushAsync() { return idle; }

    /** Rejects new writes and drains already accepted snapshots without blocking. */
    public synchronized CompletionStage<Void> closeAsync() {
        ensureInitializationScheduled();
        closed = true;
        executor.shutdown();
        return idle;
    }

    private void drain() {
        Throwable lastFailure = null;
        while (true) {
            Request request;
            CompletableFuture<Void> completedBatch;
            synchronized (this) {
                request = pending;
                pending = null;
                completedBatch = idle;
                if (request == null) running = false;
            }
            if (request == null) {
                if (lastFailure == null) completedBatch.complete(null);
                else completedBatch.completeExceptionally(lastFailure);
                return;
            }
            try {
                // Serial execution means initialization has finished; join never waits here.
                ready.join();
                writer.write(request.text);
                lastFailure = null;
                request.completion.complete(Result.WRITTEN);
            } catch (IOException | RuntimeException failure) {
                lastFailure = failure;
                request.completion.completeExceptionally(failure);
            }
        }
    }
}
