package com.ji.afkcinematic.diagnostic;

import com.ji.afkcinematic.config.PersistentCinematicMode;
import com.ji.afkcinematic.session.CinematicSession;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Immutable local preview/export. No transport, filesystem, raw logs or personal game identifiers. */
public final class DiagnosticReport {
    public static final int MAX_BYTES = 65_536;
    public static final int MAX_MESSAGE_BYTES = 4_096;
    public static final int REPORT_SCHEMA = 3;
    public static final int DIAGNOSTIC_SCHEMA = 3;
    public enum Loader { fabric, neoforge }
    public enum Os { windows, macos, linux, unknown }
    public enum Window { glfw, sdl, unknown }
    public enum Graphics { opengl, vulkan, unknown }
    public enum Music { VANILLA, MIXED, CUSTOM }
    public enum HookStatus { expected, installed, observed, missing, degraded }
    public enum FindingCategory { INTERNAL_ERROR, HOOK_MISSING, DEGRADED, START_REJECTED, CANCELLATION, RESTORATION_ERROR }
    public enum Hook {
        CAMERA("camera.pose"), CAMERA_CONSUMER("camera.consume"), INPUT("input.dispatch"), LOOK("input.acceptedMouse"), POST_MOVEMENT("input.postMovement"),
        HUD("hud.compose"), PLAYER("render.player"), CULLING("render.culling"),
        SLEEP("sleep.screen"), FISHING_PARTICLES("fishing.particles"), FISHING_BITE("fishing.bite"),
        AUDIO("audio.selection"), RELOAD("resources.reload"), WORLD("lifecycle.world");
        private final String id;
        Hook(String id) { this.id = id; }
        public String id() { return id; }
    }
    public enum Compatibility {
        controller, alternateRenderer, compatibilityLayer, duplicateJi, animationModels, skinLayers, smoothF5, spatialGui
    }
    public record Build(String ji, String mc, Loader loader, String loaderVersion, Map<Loader, String> api) {
        public Build {
            text(ji, 128); text(mc, 32); Objects.requireNonNull(loader); text(loaderVersion, 128);
            api = Map.copyOf(api); api.values().forEach(value -> text(value, 128));
        }
    }
    public record Runtime(String javaVersion, String javaVendor, Os os, String osVersion, String architecture,
                          Window window, Graphics graphics) {
        public Runtime {
            text(javaVersion, 128); text(javaVendor, 128); Objects.requireNonNull(os); text(osVersion, 128);
            text(architecture, 32); Objects.requireNonNull(window); Objects.requireNonNull(graphics);
        }
    }
    public record Mod(String id, String version) {}
    public record Configuration(boolean enabled, int afkSeconds, int shotSeconds, int cycles,
                                PersistentCinematicMode mode, Music music, boolean customDirConfigured) {
        public Configuration {
            range(afkSeconds, 10, 600); range(shotSeconds, 5, 60);
            if (cycles != -1) range(cycles, 1, 20);
            Objects.requireNonNull(mode); Objects.requireNonNull(music);
        }
    }
    public record Snapshot(Build build, Runtime runtime, List<Mod> mods, Configuration cfg,
                           CinematicSession.Snapshot session, boolean epochChanged,
                           Map<Hook, HookStatus> hooks, Map<Compatibility, Boolean> compat,
                           SemanticTrace.Snapshot trace, List<DiagnosticIncident> incidents) {
        public Snapshot(Build build, Runtime runtime, List<Mod> mods, Configuration cfg,
                        CinematicSession.Snapshot session, boolean epochChanged, Map<Hook, HookStatus> hooks,
                        Map<Compatibility, Boolean> compat, SemanticTrace.Snapshot trace) {
            this(build,runtime,mods,cfg,session,epochChanged,hooks,compat,trace,List.of());
        }
        public Snapshot {
            Objects.requireNonNull(build); Objects.requireNonNull(runtime); mods = List.copyOf(mods);
            Objects.requireNonNull(cfg); Objects.requireNonNull(session); hooks = Map.copyOf(hooks);
            compat = Map.copyOf(compat); Objects.requireNonNull(trace); incidents=List.copyOf(incidents);
            if(incidents.size()>IncidentLedger.CAPACITY)throw new IllegalArgumentException("Too many incidents");
        }
    }
    /** payload is the exact UTF-8 content shown, copied and written by local UI adapters. */
    public record Preview(String payload, int bytes, int omittedMods, int omittedTrace, int invalidMetadata) {
        public Preview {
            Objects.requireNonNull(payload);
            if (bytes != payload.getBytes(StandardCharsets.UTF_8).length || bytes > MAX_BYTES
                    || omittedMods < 0 || omittedTrace < 0 || invalidMetadata < 0) {
                throw new IllegalArgumentException("Invalid preview bytes or omissions");
            }
        }
        public byte[] utf8() { return payload.getBytes(StandardCharsets.UTF_8); }
    }
    private DiagnosticReport() {}

    public static Preview suggestion(Build build, String message) {
        return feedback(true, build, null, message, "", "", false, "en");
    }

    /** Optional contact belongs to the support envelope, never the automatic diagnostic. */
    public static Preview feedback(boolean suggestion, Build build, Preview diagnostic, String message,
                                   String name, String email, boolean updates, String language) {
        message(message);
        Objects.requireNonNull(build);
        var report = object("schema", REPORT_SCHEMA, "type", suggestion ? "suggestion" : "bug", "message", message,
                "build", object("ji", build.ji(), "mc", build.mc(), "loader", build.loader().name(), "id", BuildIdentity.id()));
        var contact = new TreeMap<String, Object>();
        name = normalizeContact(Objects.requireNonNull(name)); email = normalizeContact(Objects.requireNonNull(email));
        if (name.isEmpty() != email.isEmpty()) throw new IllegalArgumentException("CONTACT_REQUIRED");
        if (!name.isEmpty()) { if (!validText(name, 60) || name.contains("<") || name.contains(">")) throw new IllegalArgumentException("INVALID_CONTACT"); contact.put("name", name); }
        if (!email.isEmpty()) {
            if (!validEmail(email)) throw new IllegalArgumentException("INVALID_CONTACT");
            contact.put("email", email);
        }
        if (!contact.isEmpty()) report.put("contact", contact);
        if (updates) {
            if (name.length() < 2 || email.isEmpty() || !("es".equals(language) || "en".equals(language))) throw new IllegalArgumentException("CONTACT_REQUIRED");
            report.put("updates", object("consent", true, "language", language, "source", "mod"));
        }
        String payload = json(report);
        if (!suggestion && diagnostic != null) payload = payload.substring(0, payload.length()-1) + ",\"diagnostic\":" + diagnostic.payload() + "}";
        int bytes = payload.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > MAX_BYTES) throw new IllegalArgumentException("REPORT_TOO_LARGE");
        return new Preview(payload, bytes, diagnostic == null ? 0 : diagnostic.omittedMods(),
                diagnostic == null ? 0 : diagnostic.omittedTrace(), diagnostic == null ? 0 : diagnostic.invalidMetadata());
    }

    public static Preview bug(Snapshot snapshot, String message) { return bug(snapshot,message,MAX_BYTES); }

    public static Preview bug(Snapshot snapshot, String message, int maximumBytes) {
        if(maximumBytes<8192||maximumBytes>MAX_BYTES)throw new IllegalArgumentException("REPORT_TOO_LARGE");
        message(message);
        var api = new TreeMap<String, Object>();
        snapshot.build.api.forEach((loader, version) -> api.put(loader.name(), version));
        var hooks = new TreeMap<String, Object>();
        snapshot.hooks.forEach((hook, status) -> hooks.put(hook.id(), status.name()));
        var compat = new TreeMap<String, Object>();
        snapshot.compat.forEach((key, value) -> compat.put(key.name(), value));
        int invalid = 0;
        var validMods = new ArrayList<Mod>();
        for (Mod mod : snapshot.mods) {
            if (mod.id == null || !mod.id.matches("[a-z0-9_.-]{1,128}") || !validText(mod.version, 128)
                    || mod.version.indexOf('/') >= 0 || mod.version.indexOf('\\') >= 0) { ++invalid; continue; }
            if (!validMods.contains(mod)) validMods.add(mod);
        }
        validMods.sort(Comparator.comparing(Mod::id).thenComparing(Mod::version));
        int omittedMods = Math.max(0, validMods.size() - 300);
        var mods = new ArrayList<Object>(validMods.stream().limit(300).map(mod -> (Object) List.of(mod.id, mod.version)).toList());
        var samples = snapshot.trace.samples();
        int omittedTrace = (int) Math.min(Integer.MAX_VALUE,
                (long) snapshot.trace.omitted() + Math.max(0, samples.size() - SemanticTrace.CAPACITY));
        var trace = samples.stream().skip(Math.max(0, samples.size() - SemanticTrace.CAPACITY))
                .map(sample -> List.of(sample.relativeTick(), sample.event().name(), sample.source().name(), sample.detail().name())).toList();
        var incidents = new ArrayList<Object>(IncidentData.encode(snapshot.incidents));
        int omittedIncidents = 0;
        var cfg = snapshot.cfg;
        var session = snapshot.session;
        var report = object("schema", DIAGNOSTIC_SCHEMA, "type", "bug", "message", message,
                "build", object("ji", snapshot.build.ji, "id", BuildIdentity.id(), "mc", snapshot.build.mc,
                        "loader", List.of(snapshot.build.loader.name(), snapshot.build.loaderVersion), "api", api),
                "runtime", object("java", List.of(snapshot.runtime.javaVersion, snapshot.runtime.javaVendor),
                        "os", List.of(snapshot.runtime.os.name(), snapshot.runtime.osVersion, snapshot.runtime.architecture),
                        "window", snapshot.runtime.window.name(), "graphics", snapshot.runtime.graphics.name()),
                "mods", mods,
                "cfg", object("enabled", cfg.enabled, "afkSeconds", cfg.afkSeconds, "shotSeconds", cfg.shotSeconds,
                        "cycles", cfg.cycles, "mode", cfg.mode.name(), "music", cfg.music.name(), "customDirConfigured", cfg.customDirConfigured),
                "session", object("state", session.state().name(), "type", session.type().name(), "mode", session.mode().name(),
                        "lastStop", session.lastStop().name(), "lastAttempt", session.lastAttempt().name(),
                        "lastRejection", session.lastRejection().name(), "idleTicks", session.idleTicks(), "epochChanged", snapshot.epochChanged),
                "hooks", hooks, "compat", compat, "trace", trace, "findings", findings(snapshot), "incidents", incidents);
        while (true) {
            report.put("omitted", object("mods", omittedMods, "trace", omittedTrace, "invalidMetadata", invalid, "incidents", omittedIncidents));
            String payload = json(report);
            int bytes = payload.getBytes(StandardCharsets.UTF_8).length;
            if (bytes <= maximumBytes) return new Preview(payload, bytes, omittedMods, omittedTrace, invalid);
            if (!mods.isEmpty()) { mods.removeLast(); ++omittedMods; }
            else if (!incidents.isEmpty()) { incidents.removeFirst(); ++omittedIncidents; }
            else throw new IllegalArgumentException("REPORT_TOO_LARGE");
        }
    }

    private static Preview preview(Map<String, Object> report, int mods, int trace, int invalid) {
        String payload = json(report);
        int bytes = payload.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > MAX_BYTES) throw new IllegalArgumentException("REPORT_TOO_LARGE");
        return new Preview(payload, bytes, mods, trace, invalid);
    }
    private static TreeMap<String, Object> object(Object... pairs) {
        var result = new TreeMap<String, Object>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }
    private static void message(String value) {
        if (value == null || value.codePoints().allMatch(DiagnosticReport::blankCodePoint))
            throw new IllegalArgumentException("EMPTY_MESSAGE");
        if (!wellFormedUnicode(value)) throw new IllegalArgumentException("INVALID_UNICODE");
        if (value.getBytes(StandardCharsets.UTF_8).length > MAX_MESSAGE_BYTES) throw new IllegalArgumentException("MESSAGE_TOO_LARGE");
    }
    /** Same whitespace set as ECMAScript String.trim(), without changing the user's text. */
    private static boolean blankCodePoint(int c) {
        return c >= 0x0009 && c <= 0x000d || c == 0x0020 || c == 0x00a0 || c == 0x1680
                || c >= 0x2000 && c <= 0x200a || c == 0x2028 || c == 0x2029
                || c == 0x202f || c == 0x205f || c == 0x3000 || c == 0xfeff;
    }
    private static String normalizeContact(String value) {
        int start = 0, end = value.length();
        while (start < end && blankCodePoint(value.codePointAt(start))) start += Character.charCount(value.codePointAt(start));
        while (start < end && blankCodePoint(value.codePointBefore(end))) end -= Character.charCount(value.codePointBefore(end));
        return java.text.Normalizer.normalize(value.substring(start, end), java.text.Normalizer.Form.NFC);
    }
    private static boolean validEmail(String value) {
        if (!validText(value, 254) || !value.matches("[A-Za-z0-9.!#$%&'*+/=?^_{}|~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+")) return false;
        String[] pieces = value.split("@", -1);
        if (pieces[0].startsWith(".") || pieces[0].endsWith(".") || pieces[0].contains("..")) return false;
        for (String label : pieces[1].split("\\.")) if (label.length() > 63 || label.startsWith("-") || label.endsWith("-")) return false;
        return true;
    }
    /** Findings come exclusively from typed Ji state; no global log harvesting or external blame. */
    private static List<Object> findings(Snapshot snapshot) {
        var result = new ArrayList<Object>();
        snapshot.hooks.entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey().id())).forEach(entry -> {
            if (entry.getValue() == HookStatus.missing || entry.getValue() == HookStatus.degraded)
                result.add(object("category", entry.getValue() == HookStatus.missing ? FindingCategory.HOOK_MISSING.name() : FindingCategory.DEGRADED.name(),
                        "component", entry.getKey().id(), "reason", entry.getValue().name()));
        });
        if (snapshot.session.lastRejection() != CinematicSession.Reason.NONE)
            result.add(object("category", FindingCategory.START_REJECTED.name(), "component", "session", "reason", snapshot.session.lastRejection().name()));
        if (snapshot.session.lastStop() != CinematicSession.Reason.NONE)
            result.add(object("category", snapshot.session.lastStop() == CinematicSession.Reason.ADAPTER_FAILURE ? FindingCategory.INTERNAL_ERROR.name() : FindingCategory.CANCELLATION.name(),
                    "component", "session", "reason", snapshot.session.lastStop().name()));
        if (snapshot.trace.samples().stream().anyMatch(sample -> sample.event() == SemanticTrace.Event.RESTORED && sample.detail() == SemanticTrace.Detail.ADAPTER_FAILURE))
            result.add(object("category", FindingCategory.RESTORATION_ERROR.name(), "component", "session", "reason", "ADAPTER_FAILURE"));
        return result;
    }
    private static void text(String value, int max) {
        if (!validText(value, max)) throw new IllegalArgumentException("INVALID_METADATA");
    }
    private static boolean validText(String value, int max) {
        return value != null && !value.isEmpty() && value.codePointCount(0, value.length()) <= max
                && value.chars().noneMatch(c -> c < 32) && wellFormedUnicode(value);
    }
    private static boolean wellFormedUnicode(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i >= value.length() || !Character.isLowSurrogate(value.charAt(i))) return false;
            } else if (Character.isLowSurrogate(c)) return false;
        }
        return true;
    }
    private static void range(int value, int min, int max) {
        if (value < min || value > max) throw new IllegalArgumentException("INVALID_CONFIGURATION");
    }
    static String json(Object value) {
        if (value instanceof String text) {
            var out = new StringBuilder("\"");
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                switch (c) {
                    case '"' -> out.append("\\\""); case '\\' -> out.append("\\\\");
                    case '\b' -> out.append("\\b"); case '\f' -> out.append("\\f");
                    case '\n' -> out.append("\\n"); case '\r' -> out.append("\\r"); case '\t' -> out.append("\\t");
                    // Minecraft parses section-sign formatting even in a literal text component.
                    // Escaping preserves readable JSON; decoding restores the original values.
                    default -> {
                        if (c < 32 || c == '\u00a7' || c == '\u061c' || c == '\u200e' || c == '\u200f'
                                || c >= '\u202a' && c <= '\u202e' || c >= '\u2066' && c <= '\u2069')
                            out.append(String.format("\\u%04x", (int) c));
                        else out.append(c);
                    }
                }
            }
            return out.append('"').toString();
        }
        if (value instanceof Map<?, ?> map) return "{" + String.join(",", map.entrySet().stream()
                .map(entry -> json(entry.getKey()) + ":" + json(entry.getValue())).toList()) + "}";
        if (value instanceof List<?> list) return "[" + String.join(",", list.stream().map(DiagnosticReport::json).toList()) + "]";
        if (value instanceof Boolean || value instanceof Integer || value instanceof Long) return value.toString();
        throw new IllegalArgumentException("Unsupported report value");
    }
}
