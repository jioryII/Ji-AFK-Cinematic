package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.music.CinematicMusicManager;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundSystem.class)
public class SoundSystemMixin {

    @Inject(method = "getSoundVolume", at = @At("RETURN"), cancellable = true)
    private void onGetSoundVolume(SoundCategory category, CallbackInfoReturnable<Float> cir) {
        if (category == SoundCategory.MUSIC) {
            float original = cir.getReturnValue();
            cir.setReturnValue(original * CinematicMusicManager.vanillaMusicVolumeMultiplier);
        }
    }
}
