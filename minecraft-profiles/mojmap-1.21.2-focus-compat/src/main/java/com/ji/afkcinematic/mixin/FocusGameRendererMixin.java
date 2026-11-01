package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.focus.FocusRenderer;
import com.ji.afkcinematic.focus.FocusScene;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class FocusGameRendererMixin {
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getProjectionMatrix(F)Lorg/joml/Matrix4f;", ordinal = 0), require = 1)
    private void jiFocus$extract(DeltaTracker delta, CallbackInfo ci) {
        FocusScene.extract(((GameRenderer)(Object)this).getMainCamera(), delta.getGameTimeDeltaPartialTick(true), true);
        FocusRenderer.beginFrame();
    }
    @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"), index = 7, require = 1)
    private Matrix4f jiFocus$projection(Matrix4f projection) { FocusRenderer.projection(projection); return projection; }
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V", shift = At.Shift.AFTER), require = 1)
    private void jiFocus$capture(DeltaTracker delta, CallbackInfo ci) { FocusRenderer.captureDepth(Minecraft.getInstance().getMainRenderTarget()); }
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(I)V", ordinal = 0), require = 1)
    private void jiFocus$composite(DeltaTracker delta, boolean renderWorld, CallbackInfo ci) {
        if (renderWorld && Minecraft.getInstance().level != null) FocusRenderer.render(Minecraft.getInstance().getMainRenderTarget());
    }
    @Inject(method = "close", at = @At("HEAD"), require = 1)
    private void jiFocus$close(CallbackInfo ci) { FocusRenderer.close(); }
}
