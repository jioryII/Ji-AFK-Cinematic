package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.input.KeySequenceTracker;
import com.ji.afkcinematic.input.CinematicInputPolicy;

import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mouse mixin for AFK detection (Mojmap 26.x).
 * Tracks cursor movement and click activity to reset the AFK timer.
 * Does NOT cancel any events — that would freeze the user's input
 * if cinematic state ever desynchronises.
 */
@Mixin(MouseHandler.class)
public class MouseMixin {

    @Inject(method = "onMove", at = @At("HEAD"), require = 0)
    private void onCursorMove(long window, double x, double y, CallbackInfo ci) {
        // Register activity on every cursor movement — any single pixel cancels AFK / cinematic.
        registerMouseActivity(CinematicInputPolicy.Event.MOUSE_MOVE);
        // NOTE: do NOT cancel onMove here. The cinematic camera rotation is handled
        // inside CameraMixin via Camera.update, so cancelling the OS-level mouse event
        // was redundant and (if state ever desyncs) could freeze the user's cursor,
        // preventing all button interactions in 26.x.
    }

    @Inject(method = "onButton", at = @At("HEAD"), require = 0)
    private void onMouseClick(long window, net.minecraft.client.input.MouseButtonInfo input, int action, CallbackInfo ci) {
        if (action == GLFW.GLFW_PRESS) registerMouseActivity(CinematicInputPolicy.Event.MOUSE_CLICK);
        KeySequenceTracker.resetAll();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), require = 0)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        registerMouseActivity(CinematicInputPolicy.Event.MOUSE_SCROLL);
    }

    private void registerMouseActivity(CinematicInputPolicy.Event event) {
        Minecraft client = Minecraft.getInstance();
        boolean chatOpen = com.ji.afkcinematic.ScreenHelper.getCurrentScreen(client) instanceof ChatScreen;
        if (CinematicInputPolicy.shouldRegisterActivity(
                CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE,
                ConfigManager.getConfig().persistentMode, chatOpen, event)) {
            AFKDetector.registerActivity();
        }
    }
}
