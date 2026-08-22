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
    private void onKeyPress(long window, int action, net.minecraft.client.input.KeyInput input, CallbackInfo ci) {
        int keyCode = input.key();
        if (action == GLFW.GLFW_RELEASE) {
            if (KeySequenceTracker.isBindableKeyCode(keyCode)) KeySequenceTracker.onKeyReleased(keyCode);
            return;
        }
        if (action != GLFW.GLFW_PRESS) return;
        registerKeyboardActivity(keyCode);
        boolean cinematicActive = CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE;
        if (!CinematicInputPolicy.shouldProcessModShortcuts(
                MinecraftClient.getInstance().currentScreen instanceof ChatScreen,
                cinematicActive, ConfigManager.getConfig().persistentMode)) {
            KeySequenceTracker.resetAll();
            return;
        }
        if (!KeySequenceTracker.isBindableKeyCode(keyCode)) {
            KeySequenceTracker.resetAll();
            return;
        }
        processShortcuts(window, keyCode);
    }

    private void registerKeyboardActivity(int keyCode) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean chatOpen = client.currentScreen instanceof ChatScreen;
        CinematicInputPolicy.Event event = keyCode == GLFW.GLFW_KEY_ESCAPE
                ? CinematicInputPolicy.Event.ESCAPE_KEY
                : keyCode == GLFW.GLFW_KEY_T
                    ? CinematicInputPolicy.Event.OPEN_CHAT_KEY
                    : CinematicInputPolicy.Event.KEY_PRESS;
        if (CinematicInputPolicy.shouldRegisterActivity(
                CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE,
                ConfigManager.getConfig().persistentMode, chatOpen, event)) {
            AFKDetector.registerActivity();
        }
    }

    private void processShortcuts(long window, int keyCode) {
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
                if (!cfg.modEnabled) {
                    CinematicManager.forceDeactivate();
                }
                KeySequenceTracker.resetSequence(false);
                return;
            }
        }
    }
}
