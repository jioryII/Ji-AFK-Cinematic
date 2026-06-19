package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Redirect(method = "setupTerrain", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;chunkCullingEnabled:Z"))
    private boolean disableOcclusionCullingInCinematic(MinecraftClient client) {
        if (CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE) {
            return false;
        }
        return client.chunkCullingEnabled;
    }

    @Inject(method = "isRenderingReady", at = @At("HEAD"), cancellable = true)
    private void forcePlayerRenderingReady(net.minecraft.util.math.BlockPos pos, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE) {
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
            if (client.player != null) {
                // If the block is close to the player, force it to be "ready" to guarantee player entity rendering
                if (client.player.squaredDistanceTo(net.minecraft.util.math.Vec3d.ofCenter(pos)) < 4096.0) {
                    cir.setReturnValue(true);
                }
            }
        }
    }
}
