package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.input.KeySequenceTracker;

import com.ji.afkcinematic.afk.AFKDetector;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"), require = 0)
    private void onMouseClick(long window, net.minecraft.client.input.MouseInput input, int action, CallbackInfo ci) {
        AFKDetector.registerActivity();
        KeySequenceTracker.resetAll();
    }
}
