package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.input.KeySequenceTracker;

import com.ji.afkcinematic.afk.AFKDetector;
import net.minecraft.client.MouseHandler;
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

    private double lastX = 0;
    private double lastY = 0;

    @Inject(method = "onMove", at = @At("HEAD"), require = 0)
    private void onCursorMove(long window, double x, double y, CallbackInfo ci) {
        // Register activity on every cursor movement — any single pixel cancels AFK / cinematic.
        AFKDetector.registerActivity();
        lastX = x;
        lastY = y;
        // NOTE: do NOT cancel onMove here. The cinematic camera rotation is handled
        // inside CameraMixin via Camera.update, so cancelling the OS-level mouse event
        // was redundant and (if state ever desyncs) could freeze the user's cursor,
        // preventing all button interactions in 26.x.
    }

    @Inject(method = "onButton", at = @At("HEAD"), require = 0)
    private void onMouseClick(long window, net.minecraft.client.input.MouseButtonInfo input, int action, CallbackInfo ci) {
        AFKDetector.registerActivity();
        KeySequenceTracker.resetAll();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), require = 0)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        AFKDetector.registerActivity();
    }
}
