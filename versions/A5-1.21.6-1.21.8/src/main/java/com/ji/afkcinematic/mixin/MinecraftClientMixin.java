package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.afk.AFKDetector;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "doAttack", at = @At("HEAD"), require = 0)
    private void onAttack(CallbackInfoReturnable<Boolean> cir) {
        AFKDetector.registerActivity();
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), require = 0)
    private void onItemUse(CallbackInfo ci) {
        AFKDetector.registerActivity();
    }
}
