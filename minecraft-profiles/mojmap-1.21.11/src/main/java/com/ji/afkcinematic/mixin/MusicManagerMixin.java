package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.music.CinematicMusicManager;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses vanilla background music from starting while our cinematic
 * music is playing. Mirrors A1's MusicTrackerMixin but targets the
 * Mojmap-renamed MusicManager class (Yarn: MusicTracker).
 */
@Mixin(MusicManager.class)
public class MusicManagerMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void jiAfk$cancelVanillaMusicDuringCinematic(CallbackInfo ci) {
        if (CinematicMusicManager.isOurMusicPlaying) {
            ci.cancel();
        }
    }
}
