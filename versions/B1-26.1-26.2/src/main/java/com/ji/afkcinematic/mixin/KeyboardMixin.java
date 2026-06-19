package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.afk.AFKDetector;
import net.minecraft.client.KeyboardHandler;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keyboard mixin for AFK detection.
 * 1.21.11 signature: onKey(long window, int action, int input)
 * Key capture for config screen is now handled by ConfigScreen.keyPressed(int).
 */
@Mixin(net.minecraft.client.KeyboardHandler.class)
public class KeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void onKeyPress(long window, int action, net.minecraft.client.input.KeyEvent event, CallbackInfo ci) {
        // Register activity for AFK detection on any key event
        AFKDetector.registerActivity();

        // Direct keybind intercept for 26.x (Guaranteed to work without reflections)
        int keyCode = com.ji.afkcinematic.config.ConfigManager.getConfig().configKeyCode;
        if (keyCode != -1 && action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            if (org.lwjgl.glfw.GLFW.glfwGetKey(window, keyCode) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
                net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
                if (com.ji.afkcinematic.ScreenHelper.getCurrentScreen(client) == null) {
                    com.ji.afkcinematic.ScreenHelper.setScreen(client, new com.ji.afkcinematic.config.ConfigScreen(null));
                }
            }
        }
    }
}
