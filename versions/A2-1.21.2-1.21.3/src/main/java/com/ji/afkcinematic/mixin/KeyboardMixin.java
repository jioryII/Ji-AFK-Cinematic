package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.input.KeySequenceTracker;
import com.ji.afkcinematic.input.CinematicInputPolicy;

import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.ConfigScreen;
import com.ji.afkcinematic.config.ModConfig;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.render.ToggleToastManager;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"), require = 0)
    private void onKeyPress(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (action == GLFW.GLFW_RELEASE) {
            if (KeySequenceTracker.isBindableKeyCode(key)) KeySequenceTracker.onKeyReleased(key);
            return;
        }
        if (action != GLFW.GLFW_PRESS) return;
        boolean cinematicActive = CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE;
        ModConfig shortcutConfig = ConfigManager.getConfig();
        boolean toggleStep = !(shortcutConfig.toggleKey1 == -1 && shortcutConfig.toggleKey2 == -1)
                && KeySequenceTracker.isToggleSequenceStep(key, shortcutConfig.toggleKey1, shortcutConfig.toggleKey2);
        boolean immediateStep = !(shortcutConfig.immediateKey1 == -1 && shortcutConfig.immediateKey2 == -1)
                && KeySequenceTracker.isImmediateSequenceStep(key, shortcutConfig.immediateKey1, shortcutConfig.immediateKey2);
        if (!cinematicActive || (!toggleStep && !immediateStep)) registerKeyboardActivity(key, scanCode);
        if (!CinematicInputPolicy.shouldProcessModShortcuts(
                MinecraftClient.getInstance().currentScreen instanceof ChatScreen,
                cinematicActive, ConfigManager.getConfig().persistentMode)) {
            KeySequenceTracker.resetAll();
            return;
        }
        if (!KeySequenceTracker.isBindableKeyCode(key)) {
            KeySequenceTracker.resetAll();
            return;
        }
        processShortcuts(window, key, cinematicActive);
    }

    private void registerKeyboardActivity(int keyCode, int scanCode) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean chatOpen = client.currentScreen instanceof ChatScreen;
        CinematicInputPolicy.Event event = keyCode == GLFW.GLFW_KEY_ESCAPE
                ? CinematicInputPolicy.Event.ESCAPE
                : client.options.chatKey.matchesKey(keyCode, scanCode)
                    || client.options.commandKey.matchesKey(keyCode, scanCode)
                        ? CinematicInputPolicy.Event.CHAT_OPEN
                        : chatOpen
                            ? CinematicInputPolicy.Event.CHAT_INPUT
                            : CinematicInputPolicy.Event.GAMEPLAY_ACTION;
        if (CinematicInputPolicy.shouldRegisterActivity(
                CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE,
                ConfigManager.getConfig().persistentMode, chatOpen, event)) {
            AFKDetector.registerActivity();
        }
    }

    private void processShortcuts(long window, int keyCode, boolean cinematicWasActive) {
        ModConfig cfg = ConfigManager.getConfig();
        MinecraftClient client = MinecraftClient.getInstance();

        // Si el keybind de menu esta deshabilitado (ambos slots en -1), skip.
        if (!(cfg.menuKey1 == -1 && cfg.menuKey2 == -1)) {
            int[] menuFirst = KeySequenceTracker.acceptedFirstKeys(cfg.menuKey1);
            if (KeySequenceTracker.checkMenu(keyCode, menuFirst, cfg.menuKey2)) {
                if (client.currentScreen == null) {
                    client.setScreen(new ConfigScreen(client.currentScreen));
                }
                KeySequenceTracker.resetSequence(true);
                return;
            }
        }

        // Idem para toggle.
        if (!(cfg.toggleKey1 == -1 && cfg.toggleKey2 == -1)) {
            int[] toggleFirst = KeySequenceTracker.acceptedFirstKeys(cfg.toggleKey1);
            if (KeySequenceTracker.checkToggle(keyCode, toggleFirst, cfg.toggleKey2)) {
                cfg.modEnabled = !cfg.modEnabled;
                ConfigManager.saveConfig();
                ToggleToastManager.show(cfg.modEnabled);
                if (!cfg.modEnabled && cinematicWasActive) CinematicManager.forceDeactivate();
                KeySequenceTracker.resetSequence(false);
                return;
            }
        }

        if (!(cfg.immediateKey1 == -1 && cfg.immediateKey2 == -1)) {
            int[] immediateFirst = KeySequenceTracker.acceptedFirstKeys(cfg.immediateKey1);
            if (KeySequenceTracker.checkImmediate(keyCode, immediateFirst, cfg.immediateKey2)) {
                if (cinematicWasActive) CinematicManager.forceDeactivate();
                else if (cfg.modEnabled) CinematicManager.toggleImmediate();
                KeySequenceTracker.resetImmediateSequence();
            }
        }
    }
}
