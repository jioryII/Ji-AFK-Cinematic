package com.ji.afkcinematic.diagnostic;

import java.util.EnumMap;
import java.util.Map;

/** Installation and execution are separate evidence. A loaded target class alone proves neither. */
public final class HookCapabilities {
    private static final EnumMap<DiagnosticReport.Hook, DiagnosticReport.HookStatus> STATES =
            new EnumMap<>(DiagnosticReport.Hook.class);
    static { for (var hook : DiagnosticReport.Hook.values()) STATES.put(hook, DiagnosticReport.HookStatus.expected); }
    private HookCapabilities() {}
    public static synchronized void installed(DiagnosticReport.Hook hook) {
        if (STATES.get(hook) != DiagnosticReport.HookStatus.observed) STATES.put(hook, DiagnosticReport.HookStatus.installed);
    }
    public static synchronized void observed(DiagnosticReport.Hook hook) { STATES.put(hook, DiagnosticReport.HookStatus.observed); }
    public static synchronized void missing(DiagnosticReport.Hook hook) {
        if (STATES.get(hook) != DiagnosticReport.HookStatus.observed) STATES.put(hook, DiagnosticReport.HookStatus.missing);
    }
    public static synchronized void degraded(DiagnosticReport.Hook hook) { STATES.put(hook, DiagnosticReport.HookStatus.degraded); }
    public static synchronized Map<DiagnosticReport.Hook, DiagnosticReport.HookStatus> snapshot() { return Map.copyOf(STATES); }
}
