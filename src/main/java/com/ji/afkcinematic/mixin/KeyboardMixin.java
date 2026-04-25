package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.afk.AFKDetector;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keyboard mixin for AFK detection.
 * 1.21.11 signature: onKey(long window, int action, KeyInput input)
 * Key capture for config screen is now handled by ConfigScreen.keyPressed(KeyInput).
 */
@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKeyPress(long window, int action, KeyInput input, CallbackInfo ci) {
        // Register activity for AFK detection on any key event
        AFKDetector.registerActivity();
    }
}
