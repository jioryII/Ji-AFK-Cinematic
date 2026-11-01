package com.ji.afkcinematic.focus.mixin;

import com.ji.afkcinematic.focus.FocusRenderer;
import net.minecraft.client.renderer.ShaderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderManager.class)
public class FocusReloadMixin {
    @Inject(method = "apply(Lcom/mojang/renderpearl/api/device/GpuDevice;Lnet/minecraft/client/renderer/ShaderManager$PendingResults;)V", at = @At("HEAD"), require = 1)
    private void jiFocus$reload(CallbackInfo ci) { FocusRenderer.resourcesReloaded(); }
}
