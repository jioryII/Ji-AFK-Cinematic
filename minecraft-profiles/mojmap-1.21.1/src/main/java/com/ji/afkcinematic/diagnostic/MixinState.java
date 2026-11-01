package com.ji.afkcinematic.diagnostic;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;

/** Class application, a vanilla-to-handler INVOKE, and execution are separate evidence. */
public final class MixinState {
    private static final Set<String> APPLIED = new LinkedHashSet<>();
    private static final Map<String, MixinInjectionEvidence.Evidence> INJECTIONS = new HashMap<>();

    private MixinState() {}

    /** Legacy class-application inventory. It never marks a capability installed or observed. */
    public static synchronized void markApplied(String mixinClassName) {
        if (mixinClassName != null) APPLIED.add(simple(mixinClassName));
    }

    public static synchronized void inspectApplied(String mixinClassName, ClassNode targetClass) {
        if (mixinClassName == null) return;
        markApplied(mixinClassName);
        String simple = simple(mixinClassName);
        MixinInjectionEvidence.Evidence evidence = MixinInjectionEvidence.inspect(targetClass, mixinClassName);
        INJECTIONS.put(simple, evidence);
        DiagnosticReport.Hook hook = criticalHook(simple);
        if (hook == null) return;
        if (hasInstalledInjection(simple)) {
            HookCapabilities.installed(hook);
        } else {
            // An unavailable alternative must not erase another valid route.
            var state = HookCapabilities.snapshot().get(hook);
            if (state == DiagnosticReport.HookStatus.expected || state == DiagnosticReport.HookStatus.missing) {
                HookCapabilities.missing(hook);
            }
        }
    }

    public static synchronized Set<String> getApplied() { return Set.copyOf(APPLIED); }

    /** Class application only; callers needing injector proof use hasInstalledInjection. */
    public static synchronized boolean didApply(String simpleName) { return APPLIED.contains(simpleName); }

    public static synchronized boolean hasInstalledInjection(String simpleName) {
        var evidence = INJECTIONS.get(simpleName);
        if (evidence == null) return false;
        List<String> required = switch (simpleName) {
            case "CameraMixin", "Camera211Mixin" -> List.of("onCameraUpdate");
            case "CameraConsumptionMixin" -> List.of("jiAfk$consumeCamera");
            case "KeyboardMixin" -> List.of("onKeyPress");
            case "ClientPlayerMovementMixin" -> List.of("afterModernInput", "afterLegacyInput", "afterInput");
            case "InGameHudMixin" -> List.of("onRender", "jiAfk$hideHudDuringCinematic");
            case "Hud262Mixin" -> List.of("jiAfk$hideHudDuringCinematic");
            default -> List.of();
        };
        if (simpleName.equals("MouseMixin")) return evidence.invokes("onVanillaTurnStart")
                && evidence.invokes("onVanillaTurnEnd");
        return required.isEmpty() ? evidence.hasInvocation() : required.stream().anyMatch(evidence::invokes);
    }

    /** Exact target/handler ABI used by diagnostics and bytecode verification tests. */
    public static synchronized List<MixinInjectionEvidence.Invocation> invocations(String simpleName) {
        var evidence = INJECTIONS.get(simpleName);
        return evidence == null ? List.of() : evidence.invocations();
    }

    private static DiagnosticReport.Hook criticalHook(String simpleName) {
        return switch (simpleName) {
            case "CameraMixin", "Camera211Mixin" -> DiagnosticReport.Hook.CAMERA;
            case "CameraConsumptionMixin" -> DiagnosticReport.Hook.CAMERA_CONSUMER;
            case "KeyboardMixin" -> DiagnosticReport.Hook.INPUT;
            case "MouseMixin" -> DiagnosticReport.Hook.LOOK;
            case "ClientPlayerMovementMixin" -> DiagnosticReport.Hook.POST_MOVEMENT;
            case "InGameHudMixin", "Hud262Mixin" -> DiagnosticReport.Hook.HUD;
            default -> null;
        };
    }

    private static String simple(String name) { return name.substring(name.lastIndexOf('.') + 1); }
}
