package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mouse mixin for AFK detection and cinematic interaction (Mojmap 26.x).
 * Optimized to remove redundant logic and clean up imports.
 */
@Mixin(MouseHandler.class)
public class MouseMixin {

    private double lastX = 0;
    private double lastY = 0;

    @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
    private void onCursorMove(long window, double x, double y, CallbackInfo ci) {
        // Register activity if movement exceeds threshold
        if (Math.abs(x - lastX) > 2.0 || Math.abs(y - lastY) > 2.0) {
            AFKDetector.registerActivity();
            lastX = x;
            lastY = y;
        }

        // Cancel event if cinematic is active to prevent mouse input from affecting camera
        if (CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE) {
            ci.cancel();
        }
    }

    @Inject(method = "onButton", at = @At("HEAD"))
    private void onMouseClick(long window, net.minecraft.client.input.MouseButtonInfo input, int action, CallbackInfo ci) {
        AFKDetector.registerActivity();
    }

    @Inject(method = "onScroll", at = @At("HEAD"))
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        AFKDetector.registerActivity();
    }
}
