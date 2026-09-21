package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Optimizacion exclusiva del renderer vanilla. Sodium reemplaza setupTerrain,
 * asi que el plugin de mixins omite esta clase cuando Sodium esta cargado.
 */
@Mixin(WorldRenderer.class)
public class WorldRendererCullingMixin {

    @Redirect(
        method = "setupTerrain",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;chunkCullingEnabled:Z"),
        require = 0
    )
    private boolean disableOcclusionCullingInCinematic(MinecraftClient client) {
        return CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE
            ? false
            : client.chunkCullingEnabled;
    }
}
