package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.afk.AFKDetector;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
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

    private double lastX = 0;
    private double lastY = 0;

    @Inject(method = "onCursorPos", at = @At("HEAD"))
    private void onCursorMove(long window, double x, double y, CallbackInfo ci) {
        if (Math.abs(x - lastX) > 2.0 || Math.abs(y - lastY) > 2.0) {
            AFKDetector.registerActivity();
            lastX = x;
            lastY = y;
        }
    }

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseClick(long window, MouseInput input, int action, CallbackInfo ci) {
        AFKDetector.registerActivity();
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"))
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        AFKDetector.registerActivity();
    }
}
