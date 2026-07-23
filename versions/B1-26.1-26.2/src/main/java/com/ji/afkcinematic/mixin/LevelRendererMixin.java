package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "extractVisibleEntities", at = @At("TAIL"), require = 0)
    private void onExtractVisibleEntities(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState state, CallbackInfo ci) {
        if (CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE) {
            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;
            if (player != null) {
                float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
                EntityRenderState entityState = client.getEntityRenderDispatcher().extractEntity(player, tickDelta);
                if (entityState != null) {
                    state.entityRenderStates.add(entityState);
                }
            }
        }
    }

    @Inject(method = "isRenderingReady", at = @At("HEAD"), cancellable = true, require = 0)
    private void jiAfk$forceNearbyBlocksReady(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null && pos != null) {
                double cx = pos.getX() + 0.5;
                double cy = pos.getY() + 0.5;
                double cz = pos.getZ() + 0.5;
                double dx = client.player.getX() - cx;
                double dy = client.player.getY() - cy;
                double dz = client.player.getZ() - cz;
                if ((dx * dx + dy * dy + dz * dz) < 4096.0) {
                    cir.setReturnValue(true);
                }
            }
        }
    }
}
