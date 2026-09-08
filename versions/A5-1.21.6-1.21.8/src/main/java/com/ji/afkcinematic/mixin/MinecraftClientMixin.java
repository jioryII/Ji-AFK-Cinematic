package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.input.CinematicInputPolicy;
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
        registerGameplayAction();
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), require = 0)
    private void onItemUse(CallbackInfo ci) {
        registerGameplayAction();
    }

    private static void registerGameplayAction() {
        if (CinematicInputPolicy.shouldRegisterActivity(
                CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE,
                ConfigManager.getConfig().persistentMode,
                false,
                CinematicInputPolicy.Event.GAMEPLAY_ACTION)) {
            AFKDetector.registerActivity();
        }
    }
}
