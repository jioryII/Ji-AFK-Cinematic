package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.input.CinematicInputPolicy;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), require = 0)
    private void onAttack(CallbackInfoReturnable<Boolean> cir) {
        if (CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE
                && ConfigManager.getConfig().persistentMode == com.ji.afkcinematic.config.PersistentCinematicMode.PERSISTENT) {
            AFKDetector.registerActivity(CinematicInputPolicy.Event.GAMEPLAY_ACTION, com.ji.afkcinematic.diagnostic.SemanticTrace.Source.ADAPTER);
        } else registerGameplayAction();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), require = 0)
    private void onItemUse(CallbackInfo ci) {
        registerGameplayAction();
    }

    @Inject(method={"setScreen", "setScreenAndShow"},at=@At("HEAD"),require=0)
    private void jiAfk$yieldPresentation(net.minecraft.client.gui.screens.Screen screen, CallbackInfo ci) {
        com.ji.afkcinematic.diagnostic.RuntimeResilience.beforeScreen(screen);
    }

    private static void registerGameplayAction() {
        if (CinematicInputPolicy.shouldRegisterActivity(
                CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE,
                CinematicManager.isFishingCinematic(),
                ConfigManager.getConfig().persistentMode,
                false,
                CinematicInputPolicy.Event.GAMEPLAY_ACTION)) {
            AFKDetector.registerActivity(CinematicInputPolicy.Event.GAMEPLAY_ACTION,
                    com.ji.afkcinematic.diagnostic.SemanticTrace.Source.ADAPTER);
        }
    }
}
