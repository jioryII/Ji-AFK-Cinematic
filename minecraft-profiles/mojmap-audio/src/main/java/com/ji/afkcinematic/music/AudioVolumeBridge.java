package com.ji.afkcinematic.music;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
public final class AudioVolumeBridge {
    private AudioVolumeBridge() {}
    public static void refresh(Minecraft client) {
        if (client != null && client.getSoundManager() != null) client.getSoundManager().refreshCategoryVolume(SoundSource.MUSIC);
    }
}
