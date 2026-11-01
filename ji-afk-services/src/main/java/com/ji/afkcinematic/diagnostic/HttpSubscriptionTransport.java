package com.ji.afkcinematic.diagnostic;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.io.IOException;

/** Explicit newsletter request. Never stored in diagnostics, configuration or a local queue. */
public final class HttpSubscriptionTransport {
    private static final URI ENDPOINT = HttpReportTransport.PUBLIC_API.resolve("notifications/subscribe");
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private HttpSubscriptionTransport() {}

    public static CompletableFuture<Void> subscribe(String name, String email, String language, boolean consent) {
        return sendTo(ENDPOINT, name, email, language, consent);
    }
    static CompletableFuture<Void> sendTo(URI endpoint, String name, String email, String language, boolean consent) {
        if (!consent) return CompletableFuture.failedFuture(new IllegalStateException("CONSENT_REQUIRED"));
        if (name == null || email == null || name.trim().codePointCount(0, name.trim().length()) < 2
                || name.trim().codePointCount(0, name.trim().length()) > 60
                || name.codePoints().anyMatch(c -> Character.isISOControl(c) || c == '<' || c == '>')
                || email.length() > 254 || !email.trim().matches("[A-Za-z0-9.!#$%&'*+/=?^_{}|~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+")
                || !("en".equals(language) || "es".equals(language))) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("INVALID_SUBSCRIPTION"));
        }
        if (!ENDPOINT.equals(endpoint) && !("http".equals(endpoint.getScheme()) && "127.0.0.1".equals(endpoint.getHost())))
            return CompletableFuture.failedFuture(new IllegalArgumentException("RECEIVER_REJECTED"));
        String payload = "{\"name\":" + quote(name.trim()) + ",\"email\":" + quote(email.trim())
                + ",\"language\":" + quote(language) + ",\"source\":\"mod\",\"consent\":true}";
        var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()).thenApply(response -> {
            try (var body = response.body()) {
                byte[] bytes = body.readNBytes(4097);
                if (response.statusCode() != 202 || bytes.length > 4096
                        || !new String(bytes, StandardCharsets.UTF_8).matches("\\s*\\{\\s*\"status\"\\s*:\\s*\"queued\"\\s*}\\s*"))
                    throw new IOException("SUBSCRIPTION_UNAVAILABLE");
                return null;
            } catch (IOException error) { throw new CompletionException(error); }
        });
    }
    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
