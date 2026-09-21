package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.cinematic.CinematicManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes vanilla's real fish-approach particle before it is rendered. */
@Mixin(ClientWorld.class)
public class ClientWorldFishingParticleMixin {
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V",
            at = @At("HEAD"), require = 0)
    private void jiAfk$observeFishingApproach(ParticleEffect effect,
                                              double x, double y, double z,
                                              double velocityX, double velocityY, double velocityZ,
                                              CallbackInfo ci) {
        if (effect.getType() == ParticleTypes.FISHING) {
            CinematicManager.onFishingApproachParticle(new Vec3d(x, y, z));
        }
    }

    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;ZDDDDDD)V",
            at = @At("HEAD"), require = 0)
    private void jiAfk$observeFishingApproachFlags(ParticleEffect effect, boolean force,
                                                   double x, double y, double z,
                                                   double velocityX, double velocityY, double velocityZ,
                                                   CallbackInfo ci) {
        if (effect.getType() == ParticleTypes.FISHING) {
            CinematicManager.onFishingApproachParticle(new Vec3d(x, y, z));
        }
    }

    /**
     * 1.21.4 and 1.21.5 added an "important" flag to the same intermediary
     * particle method. Keeping both optional descriptors lets this group follow
     * the real fish-approach path on either side of that binary boundary.
     */
    // 1.21.4/1.21.5 added the second boolean after the mappings used to
    // compile this shared artifact. Target its stable intermediary signature
    // directly so Loom does not leave an unusable named selector in the jar.
    @Inject(method = "method_8466(Lnet/minecraft/class_2394;ZZDDDDDD)V",
            at = @At("HEAD"), require = 0, remap = false)
    private void jiAfk$observeFishingApproachImportant(ParticleEffect effect,
                                                       boolean force, boolean important,
                                                       double x, double y, double z,
                                                       double velocityX, double velocityY, double velocityZ,
                                                       CallbackInfo ci) {
        if (effect.getType() == ParticleTypes.FISHING) {
            CinematicManager.onFishingApproachParticle(new Vec3d(x, y, z));
        }
    }
}
