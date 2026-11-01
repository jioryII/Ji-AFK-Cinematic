package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.focus.FocusRenderer;
import com.ji.afkcinematic.focus.FocusScene;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class FocusGameRendererMixin {
    @Inject(method = "extractCamera", at = @At("RETURN"), require = 1)
    private void jiFocus$extract(float partialTick, CallbackInfo ci) {
        FocusScene.extract(((GameRenderer)(Object)this).getMainCamera(), partialTick, true);
    }
    @Inject(method = "render", at = @At("HEAD"), require = 1)
    private void jiFocus$begin(CallbackInfo ci) { FocusRenderer.beginFrame(); }
    @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PerspectiveProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"), index = 0, require = 1)
    private Matrix4f jiFocus$worldProjection(Matrix4f matrix) { FocusRenderer.projection(matrix); return matrix; }
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearDepthTexture(Lcom/mojang/blaze3d/textures/GpuTexture;D)V"), require = 1)
    private void jiFocus$depthBeforeHand(CallbackInfo ci) {
        FocusRenderer.captureDepth(net.minecraft.client.Minecraft.getInstance().getMainRenderTarget());
    }
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/FogRenderer;endFrame()V"), require = 1)
    private void jiFocus$beforeGui(CallbackInfo ci) {
        FocusRenderer.render(net.minecraft.client.Minecraft.getInstance().getMainRenderTarget());
    }
    @Inject(method = "close", at = @At("HEAD"), require = 1)
    private void jiFocus$close(CallbackInfo ci) { FocusRenderer.close(); }
}
