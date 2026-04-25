package com.ji.afkcinematic.music;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CinematicMusicManager {
    private static final Random random = new Random();
    private static SoundInstance currentMusicInstance = null;

    public static void checkAndPlayMusic() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getSoundManager() == null || client.player == null) return;

        // If our music is already playing, do nothing
        if (currentMusicInstance != null && client.getSoundManager().isPlaying(currentMusicInstance)) {
            return;
        }

        // Stop any currently playing vanilla background music so they don't overlap
        if (client.getMusicTracker() != null) {
            client.getMusicTracker().stop();
        }

        List<SoundEvent> musicPool = new ArrayList<>();
        musicPool.add(SoundEvents.MUSIC_CREATIVE.value());
        musicPool.add(SoundEvents.MUSIC_GAME.value());

        SoundEvent selectedMusic = musicPool.get(random.nextInt(musicPool.size()));

        currentMusicInstance = PositionedSoundInstance.music(selectedMusic);
        client.getSoundManager().play(currentMusicInstance);
    }

    public static void stopMusic() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getSoundManager() != null && currentMusicInstance != null) {
            client.getSoundManager().stop(currentMusicInstance);
            currentMusicInstance = null;
        }
    }
}
