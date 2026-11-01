package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.cinematic.CinematicManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes vanilla's actual fish-approach particle before rendering/culling. */
@Mixin(ClientLevel.class)
public class ClientLevelFishingParticleMixin {
    @Inject(method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)V", at = @At("HEAD"), require = 1)
    private void jiAfk$observeFishingApproach(ParticleOptions options, boolean force,
                                              boolean decreased, double x, double y, double z,
                                              double velocityX, double velocityY, double velocityZ,
                                              CallbackInfo ci) {
        observe(options, x, y, z);
    }

    @Inject(method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V", at = @At("HEAD"), require = 1)
    private void jiAfk$observeFishingApproachDirect(ParticleOptions options, double x, double y, double z,
                                                  double velocityX, double velocityY, double velocityZ, CallbackInfo ci) {
        observe(options, x, y, z);
    }

    private static void observe(ParticleOptions options, double x, double y, double z) {
        if (options.getType() == ParticleTypes.FISHING) CinematicManager.onFishingApproachParticle(new Vec3(x, y, z));
    }
}
