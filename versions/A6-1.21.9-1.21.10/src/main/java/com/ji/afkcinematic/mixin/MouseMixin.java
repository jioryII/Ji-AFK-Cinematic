package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.input.KeySequenceTracker;
import com.ji.afkcinematic.input.CinematicInputPolicy;

import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.config.ConfigManager;
import net.minecraft.client.Mouse;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.input.MouseInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mouse mixin for AFK detection.
 * 1.21.11 signatures:
 *   onCursorPos(long window, double x, double y)
 *   onMouseButton(long window, MouseInput input, int action)
 *   onMouseScroll(long window, double horizontal, double vertical)
 */
@Mixin(Mouse.class)
public class MouseMixin {

    @Inject(method = "onCursorPos", at = @At("HEAD"), require = 0)
    private void onCursorMove(long window, double x, double y, CallbackInfo ci) {
        registerMouseActivity(CinematicInputPolicy.Event.MOUSE_MOVE);
    }

    @Inject(method = "onMouseButton", at = @At("HEAD"), require = 0)
    private void onMouseClick(long window, MouseInput input, int action, CallbackInfo ci) {
        if (action == GLFW.GLFW_PRESS) registerMouseActivity(CinematicInputPolicy.Event.MOUSE_CLICK);
        KeySequenceTracker.resetAll();
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), require = 0)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        registerMouseActivity(CinematicInputPolicy.Event.MOUSE_SCROLL);
    }

    private void registerMouseActivity(CinematicInputPolicy.Event event) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean chatOpen = client.currentScreen instanceof ChatScreen;
        if (CinematicInputPolicy.shouldRegisterActivity(
                CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE,
                ConfigManager.getConfig().persistentMode, chatOpen, event)) {
            AFKDetector.registerActivity();
        }
    }
}
