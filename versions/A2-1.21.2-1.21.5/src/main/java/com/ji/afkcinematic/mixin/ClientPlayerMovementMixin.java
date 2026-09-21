package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.input.PersistentMovementLock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerMovementMixin {
    @Inject(method = "tickMovement", at = @At("HEAD"), require = 0)
    private void beforeMovement(CallbackInfo ci) { clear(); }

    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/Input;tick()V", shift = At.Shift.AFTER), require = 0)
    private void afterModernInput(CallbackInfo ci) { clear(); }

    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/Input;tick(ZF)V", shift = At.Shift.AFTER), require = 0)
    private void afterLegacyInput(CallbackInfo ci) { clear(); }

    private static void clear() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) PersistentMovementLock.clear(client.player.input);
    }
}
