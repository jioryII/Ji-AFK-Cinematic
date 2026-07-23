package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {

    @Inject(method = "play", at = @At("HEAD"), cancellable = true, require = 0)
    private void jiAfk$filterCinemaDisc(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (CinematicManager.getState() != CinematicState.CINEMATIC_ACTIVE) return;
        if (instance == null) return;
        Identifier id = instance.getIdentifier();
        if (id == null) return;
        String path = id.getPath();
        if ("music_disc.5".equals(path)
                || "music_disc.11".equals(path)
                || "music_disc.13".equals(path)) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}
