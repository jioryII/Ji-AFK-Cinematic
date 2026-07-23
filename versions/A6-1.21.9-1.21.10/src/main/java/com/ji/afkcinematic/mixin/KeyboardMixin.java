package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.input.KeySequenceTracker;

import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.ConfigScreen;
import com.ji.afkcinematic.config.ModConfig;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.render.ToggleToastManager;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"), require = 0)
    private void onKeyPress(long window, int action, net.minecraft.client.input.KeyInput input, CallbackInfo ci) {
        AFKDetector.registerActivity();
        if (action != GLFW.GLFW_PRESS) return;
        processShortcuts(window, input.key());
    }

    private void processShortcuts(long window, int keyCode) {
        ModConfig cfg = ConfigManager.getConfig();
        MinecraftClient client = MinecraftClient.getInstance();

        int[] menuFirst = KeySequenceTracker.acceptedFirstKeys(cfg.menuKey1);
        int[] toggleFirst = KeySequenceTracker.acceptedFirstKeys(cfg.toggleKey1);

        if (KeySequenceTracker.checkMenu(keyCode, menuFirst, cfg.menuKey2)) {
            if (client.currentScreen == null) {
                client.setScreen(new ConfigScreen(client.currentScreen));
            }
            KeySequenceTracker.resetSequence(true);
            return;
        }

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