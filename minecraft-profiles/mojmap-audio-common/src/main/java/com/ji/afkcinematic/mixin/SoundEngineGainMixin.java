package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.music.CinematicMusicManager;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class SoundEngineGainMixin {
    @Inject(method = "calculateVolume(FLnet/minecraft/sounds/SoundSource;)F", at = @At("RETURN"), cancellable = true)
    private void jiAfk$temporaryMusicGain(float volume, SoundSource source, CallbackInfoReturnable<Float> cir) {
        if (source == SoundSource.MUSIC) cir.setReturnValue(cir.getReturnValue() * CinematicMusicManager.vanillaMusicVolumeMultiplier);
    }
}
