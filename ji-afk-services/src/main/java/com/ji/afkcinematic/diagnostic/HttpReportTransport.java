package com.ji.afkcinematic.diagnostic;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Explicit submission of an immutable reviewed preview. No background queue or automatic retries. */
public final class HttpReportTransport {
    public static final URI PUBLIC_API = URI.create("https://ji-afk-cinematic.ji-mods.com/api/v1/");
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private final DiagnosticReport.Preview reviewed;
    private final boolean suggestion;
    private final Duration responseTimeout;
    private volatile Receipt receipt;
    private CompletableFuture<Receipt> inFlight;
    private final String key = UUID.randomUUID().toString();

    private static final FeedbackGate GATE = new FeedbackGate(() -> System.nanoTime()/1_000_000);
    public static long cooldownSeconds() { return cooldownSeconds(false); }
    public static long cooldownSeconds(boolean suggestion) { return GATE.seconds(suggestion); }
    public static final class CooldownException extends IOException {
        public final long seconds;
        public CooldownException(long seconds){super("COOLDOWN");this.seconds=seconds;}
    }
    public record Receipt(String id, boolean updatesQueued) {}
    public HttpReportTransport(DiagnosticReport.Preview reviewed, boolean suggestion) {
        this(reviewed, suggestion, Duration.ofSeconds(15));
    }
    HttpReportTransport(DiagnosticReport.Preview reviewed, boolean suggestion, Duration responseTimeout) {
        this.reviewed = java.util.Objects.requireNonNull(reviewed);
        this.suggestion = suggestion;
        this.responseTimeout = java.util.Objects.requireNonNull(responseTimeout);
    }
    public String payload() { return reviewed.payload(); }
    public String idempotencyKey() { return key; }
    public CompletableFuture<Receipt> send(boolean explicitlyConfirmed) {
        return sendTo(PUBLIC_API, explicitlyConfirmed);
    }
    static void resetTestCooldown(){GATE.reset();}
    // Package-private loopback hook for representative transport tests; production only uses PUBLIC_API.
    synchronized CompletableFuture<Receipt> sendTo(URI base, boolean explicitlyConfirmed) {
        if(!explicitlyConfirmed)return CompletableFuture.failedFuture(new IllegalStateException("CONSENT_REQUIRED"));
        if(receipt!=null)return CompletableFuture.completedFuture(receipt);
        if(inFlight!=null)return inFlight;
        boolean loopback = "http".equals(base.getScheme()) && "127.0.0.1".equals(base.getHost());
        if (!PUBLIC_API.equals(base) && !loopback) return CompletableFuture.failedFuture(new IllegalArgumentException("RECEIVER_REJECTED"));
        final String fingerprint;
        try { fingerprint=base+":"+suggestion+":"+java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(reviewed.utf8())); }
        catch(java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        var attempt=GATE.reserve(suggestion,fingerprint);
        if(!attempt.owner())return attempt.future();
        var request = HttpRequest.newBuilder(base.resolve(suggestion ? "suggestions" : "reports"))
                .timeout(responseTimeout).header("Content-Type", "application/json; charset=utf-8")
                .header("Idempotency-Key", key).header("X-Ji-Consent", "reviewed-payload")
                .POST(HttpRequest.BodyPublishers.ofByteArray(reviewed.utf8())).build();
        // Covers the entire response, including a receiver that sends headers then stalls.
        // The subscriber bounds memory before collecting any body, and cancels oversized replies.
        var network = CLIENT.sendAsync(request, ignored -> new BoundedReceiptBody());
        var bounded = network.copy().orTimeout(responseTimeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        bounded.whenComplete((ignored, error) -> { if (error instanceof java.util.concurrent.TimeoutException) network.cancel(true); });
        var decoded = bounded.thenApply(response -> {
            try {
                byte[] bytes = response.body();
                if(response.statusCode()==429){long seconds=180;try{seconds=Long.parseLong(response.headers().firstValue("Retry-After").orElse("180"));}catch(NumberFormatException ignored){}GATE.defer(suggestion,seconds);throw new CooldownException(cooldownSeconds(suggestion));}
                if (response.statusCode() != 201) throw new IOException("HTTP_" + response.statusCode());
                if (bytes.length > 8192) throw new IOException("INVALID_RECEIPT");
                String json = StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                        .decode(java.nio.ByteBuffer.wrap(bytes)).toString();
                Object value = JsonData.parse(json, 2);
                if (!(value instanceof java.util.Map<?, ?> data) || !"received".equals(data.get("status"))
                        || !(data.get("id") instanceof String id) || !id.matches("[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}")
                        || !java.util.Set.of("id", "status", "updates", "duplicate").containsAll(data.keySet())
                        || data.containsKey("duplicate") && !(data.get("duplicate") instanceof Boolean)
                        || data.containsKey("updates") && !java.util.Set.of("queued", "unavailable", "not_requested").contains(data.get("updates"))) throw new IOException("INVALID_RECEIPT");
                receipt=new Receipt(UUID.fromString(id).toString(),"queued".equals(data.get("updates")));
                return receipt;
            } catch (IOException | IllegalArgumentException error) { throw new CompletionException(error); }
        });
        inFlight = attempt.future();
        decoded.whenComplete((value,error) -> GATE.finish(suggestion,fingerprint,attempt,value,error instanceof CompletionException && error.getCause()!=null?error.getCause():error));
        var result = inFlight;
        // Clear only this attempt, so a failed explicit retry uses the same idempotency key.
        result.whenComplete((ignored, failure) -> { synchronized (this) { if (inFlight == result) inFlight = null; } });
        return result;
    }

    private static final class BoundedReceiptBody implements HttpResponse.BodySubscriber<byte[]> {
        private final CompletableFuture<byte[]> body = new CompletableFuture<>();
        private final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        private java.util.concurrent.Flow.Subscription subscription;
        public java.util.concurrent.CompletionStage<byte[]> getBody() { return body; }
        public void onSubscribe(java.util.concurrent.Flow.Subscription value) { subscription = value; value.request(1); }
        public void onNext(java.util.List<java.nio.ByteBuffer> buffers) {
            for (var buffer : buffers) {
                if (buffer.remaining() > 8192 - bytes.size()) {
                    subscription.cancel(); body.completeExceptionally(new IOException("INVALID_RECEIPT")); return;
                }
                byte[] chunk = new byte[buffer.remaining()]; buffer.get(chunk); bytes.writeBytes(chunk);
            }
            subscription.request(1);
        }
        public void onError(Throwable error) { body.completeExceptionally(error); }
        public void onComplete() { body.complete(bytes.toByteArray()); }
    }
}
