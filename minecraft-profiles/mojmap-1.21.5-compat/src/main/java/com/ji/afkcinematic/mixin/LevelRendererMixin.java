package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "collectVisibleEntities", at = @At("RETURN"), require = 1)
    private void onCollectVisibleEntities(Camera camera, Frustum frustum, List<Entity> entities,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE) {
            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;
            if (player != null) {
                // 1.21.8 collects entities before rendering, rather than extracting
                // LevelRenderState. Let vanilla render each collected entity once.
                if (!entities.contains(player)) entities.add(player);
            }
        }
    }

    @Inject(method = "isSectionCompiled", at = @At("HEAD"), cancellable = true)
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
