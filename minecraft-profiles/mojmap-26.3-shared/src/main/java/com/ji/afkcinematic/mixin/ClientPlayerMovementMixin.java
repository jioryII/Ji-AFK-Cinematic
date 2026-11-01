package com.ji.afkcinematic.mixin;
import com.ji.afkcinematic.diagnostic.HookCapabilities;
import com.ji.afkcinematic.diagnostic.DiagnosticReport.Hook;

import com.ji.afkcinematic.input.PersistentMovementLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class ClientPlayerMovementMixin {
    @Inject(method = "aiStep", at = @At("HEAD"), require = 1)
    private void beforeMovement(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) PersistentMovementLock.clear(client.player.input);
    }

    // Both vanilla input and NeoForge MovementInputUpdateEvent finish before this call.
    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/tutorial/Tutorial;onInput(Lnet/minecraft/client/player/ClientInput;)V"), require = 1)
    private void afterInput(CallbackInfo ci) {
        HookCapabilities.observed(Hook.POST_MOVEMENT);
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) PersistentMovementLock.clear(client.player.input);
    }
}
