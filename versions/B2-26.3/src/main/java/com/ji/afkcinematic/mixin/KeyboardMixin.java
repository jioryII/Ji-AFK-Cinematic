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
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true, require = 0)
    private void onKeyPress(long window, int action, net.minecraft.client.input.KeyEvent event, CallbackInfo ci) {
        int keyCode = event.key();
        if (action == InputConstants.RELEASE) {
            if (KeySequenceTracker.isBindableKeyCode(keyCode)) KeySequenceTracker.onKeyReleased(keyCode);
            return;
        }
        if (action != InputConstants.PRESS) return;
        if (keyCode == InputConstants.KEY_ESCAPE
                && CinematicManager.getState() != CinematicState.IDLE
                && com.ji.afkcinematic.ScreenHelper.getCurrentScreen(Minecraft.getInstance()) == null) {
            CinematicManager.cancelForPause();
            KeySequenceTracker.resetAll();
            ci.cancel();
            return;
        }
        boolean cinematicActive = CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE;
        ModConfig shortcutConfig = ConfigManager.getConfig();
        boolean toggleStep = !(shortcutConfig.toggleKey1 == -1 && shortcutConfig.toggleKey2 == -1)
                && KeySequenceTracker.isToggleSequenceStep(keyCode, shortcutConfig.toggleKey1, shortcutConfig.toggleKey2);
        boolean immediateStep = !(shortcutConfig.immediateKey1 == -1 && shortcutConfig.immediateKey2 == -1)
                && KeySequenceTracker.isImmediateSequenceStep(keyCode, shortcutConfig.immediateKey1, shortcutConfig.immediateKey2);
        if (!cinematicActive || (!toggleStep && !immediateStep)) registerKeyboardActivity(event);
        if (!CinematicInputPolicy.shouldProcessModShortcuts(
                com.ji.afkcinematic.ScreenHelper.getCurrentScreen(Minecraft.getInstance()) instanceof ChatScreen,
                cinematicActive, ConfigManager.getConfig().persistentMode)) {
            KeySequenceTracker.resetAll();
            return;
        }
        if (!KeySequenceTracker.isBindableKeyCode(keyCode)) {
            KeySequenceTracker.resetAll();
            return;
        }
        processShortcuts(window, keyCode, cinematicActive);
    }

    private void registerKeyboardActivity(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key();
        if (CinematicManager.isFishingCinematic()) {
            Minecraft client = Minecraft.getInstance();
            if (client.options.keyUp.matches(event) || client.options.keyDown.matches(event)
                    || client.options.keyLeft.matches(event) || client.options.keyRight.matches(event)) {
                AFKDetector.registerActivity();
            }
            return;
        }
        Minecraft client = Minecraft.getInstance();
        boolean chatOpen = com.ji.afkcinematic.ScreenHelper.getCurrentScreen(client) instanceof ChatScreen;
        CinematicInputPolicy.Event inputEvent = keyCode == InputConstants.KEY_ESCAPE
                ? CinematicInputPolicy.Event.ESCAPE
                : client.options.keyChat.matches(event) || client.options.keyCommand.matches(event)
                    ? CinematicInputPolicy.Event.CHAT_OPEN
                    : chatOpen
                        ? CinematicInputPolicy.Event.CHAT_INPUT
                        : CinematicInputPolicy.Event.GAMEPLAY_ACTION;
        if (CinematicInputPolicy.shouldRegisterActivity(
                CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE,
                ConfigManager.getConfig().persistentMode, chatOpen, inputEvent)) {
            AFKDetector.registerActivity();
        }
    }

    private void processShortcuts(long window, int keyCode, boolean cinematicWasActive) {
        ModConfig cfg = ConfigManager.getConfig();
        Minecraft client = Minecraft.getInstance();

        // Si el keybind de menu esta deshabilitado (ambos slots en -1), skip.
        if (!(cfg.menuKey1 == -1 && cfg.menuKey2 == -1)) {
            int[] menuFirst = KeySequenceTracker.acceptedFirstKeys(cfg.menuKey1);
            if (KeySequenceTracker.checkMenu(keyCode, menuFirst, cfg.menuKey2)) {
                if (com.ji.afkcinematic.ScreenHelper.getCurrentScreen(client) == null) {
                    com.ji.afkcinematic.ScreenHelper.setScreen(client, new ConfigScreen(null));
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
