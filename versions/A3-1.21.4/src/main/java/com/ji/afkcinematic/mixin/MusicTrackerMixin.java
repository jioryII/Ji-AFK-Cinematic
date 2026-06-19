package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.music.CinematicMusicManager;
import net.minecraft.client.sound.MusicTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicTracker.class)
public class MusicTrackerMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        if (CinematicMusicManager.isOurMusicPlaying) {
            ci.cancel();
        }
    }
}
