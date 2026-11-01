package com.ji.afkcinematic.input;

import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import com.ji.afkcinematic.input.RuntimeKeystroke;
import java.util.*;

/** Native key aliases and configuration bridge; resolution lives in the shared core. */
public final class RuntimeShortcuts {
    private static final ShortcutController CONTROLLER = new ShortcutController(1_500_000_000L);
    private static Map<ShortcutController.Action, ShortcutController.Chord> previous = Map.of();
    private static final Map<Integer, RuntimeKeystroke> pressedInputs = new HashMap<>();

    private RuntimeShortcuts() {}

    public static Map<ShortcutController.Action, ShortcutController.Chord> bindings(ModConfig cfg) {
        return Map.of(ShortcutController.Action.MENU, chord(cfg.menuKey1, cfg.menuKey2),
                ShortcutController.Action.TOGGLE, chord(cfg.toggleKey1, cfg.toggleKey2),
                ShortcutController.Action.IMMEDIATE, chord(cfg.immediateKey1, cfg.immediateKey2));
    }

    private static ShortcutController.Chord chord(int first, int second) {
        if (!KeySequenceTracker.isBindableKeyCode(first) || !KeySequenceTracker.isBindableKeyCode(second)) {
            return ShortcutController.Chord.disabled();
        }
        // A single modifier is one physical key; aliases apply only to chord prefixes.
        if (first == second) return new ShortcutController.Chord(Set.of(first), second);
        Set<Integer> keys = new HashSet<>();
        for (int key : KeySequenceTracker.acceptedFirstKeys(first)) keys.add(key);
        return new ShortcutController.Chord(keys, second);
    }

    public static java.util.List<ShortcutController.Conflict> conflicts(ModConfig config) {
        ShortcutController preview = new ShortcutController(1_500_000_000L);
        preview.configure(bindings(config));
        return preview.conflicts();
    }

    public static ShortcutController.Decision press(RuntimeKeystroke input, boolean allowed) {
        int key = input.key();
        Map<ShortcutController.Action, ShortcutController.Chord> bindings = bindings(ConfigManager.getConfig());
        if (!previous.equals(bindings)) {
            CONTROLLER.configure(bindings);
            previous = bindings;
            if (!CONTROLLER.conflicts().isEmpty()) {
                com.ji.afkcinematic.JiAFKCinematic.LOGGER.warn(
                        "Conflicting Ji shortcuts {}. Bindings preserved; priority is menu, toggle, immediate. Review Controls.",
                        CONTROLLER.conflicts());
            }
        }
        if (!KeySequenceTracker.isBindableKeyCode(key)) {
            CONTROLLER.cancelPending();
            return new ShortcutController.Decision(ShortcutController.Action.NONE, false, false, true);
        }
        pressedInputs.putIfAbsent(key, input);
        ShortcutController.Decision decision = CONTROLLER.press(key, System.nanoTime(), allowed);
        if (decision.consume()) clearConsumedBindings();
        return decision;
    }

    /**
     * A prefix may already have reached vanilla before Ji wins the chord.
     * Clear only mappings owned by the actual gesture, including queued clicks.
     * Called again before vanilla samples input in case focus/raw polling restored them.
     */
    public static void clearConsumedBindings() {
        Set<Integer> consumedKeys = CONTROLLER.consumedKeys();
        if (consumedKeys.isEmpty()) return;
        for (KeyMapping binding : Minecraft.getInstance().options.keyMappings) {
            for (int key : consumedKeys) {
                RuntimeKeystroke input = pressedInputs.get(key);
                if (input == null || !(binding.matches(input.key(), input.scancode()))) continue;
                binding.setDown(false);
                // Toggle controls ignore a false edge. A true edge toggles the
                // remaining down state off; ordinary controls already cleared.
                if (binding instanceof ToggleKeyMapping && binding.isDown()) binding.setDown(true);
                while (binding.consumeClick()) { }
                break;
            }
        }
    }

    public static void release(int key) {
        CONTROLLER.release(key);
        pressedInputs.remove(key);
    }
    public static boolean repeatIsConsumed(int key) { return CONTROLLER.repeatIsConsumed(key); }
    public static void cancelPending() { CONTROLLER.cancelPending(); }
    public static void reset() { CONTROLLER.reset(); pressedInputs.clear(); }
}
