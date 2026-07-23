package com.ji.afkcinematic.music;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;

public class CinematicMusicManager {
    public static boolean isOurMusicPlaying = false;
    private static CinematicMusicInstance currentInstance;
    
    public enum FadeState { IDLE, FADE_OUT_GAME, FADE_IN_CINEMATIC, FADE_OUT_CINEMATIC }
    public static FadeState state = FadeState.IDLE;
    public static float vanillaMusicVolumeMultiplier = 1.0f;
    private static final float FADE_SPEED = 0.02f;

    

    

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick(client));
    }

    public static void checkAndPlayMusic() {
        if (isOurMusicPlaying) return;
        if (currentInstance != null && !currentInstance.isDone()) {
            currentInstance.forceStop();
        }
        state = FadeState.FADE_OUT_GAME;
        isOurMusicPlaying = true;
    }

    public static void updateVolume() {
        if (currentInstance != null) {
            currentInstance.refreshTargetVolume();
        }
    }

    public static void stopMusic() {
        if (isOurMusicPlaying) {
            state = FadeState.FADE_OUT_CINEMATIC;
            if (currentInstance != null) {
                currentInstance.fadeOutAndStop();
            }
            isOurMusicPlaying = false;
        }
    }

    public static void forceStop() {
        if (currentInstance != null) {
            currentInstance.forceStop();
            currentInstance = null;
        }
        isOurMusicPlaying = false;
        state = FadeState.IDLE;
    }

    private static void tick(MinecraftClient client) {
                if (state == FadeState.IDLE) {
            if (isOurMusicPlaying) {
                stopVanillaMusic(client);
            }
            return;
        }

        if (state == FadeState.FADE_OUT_GAME) {
            vanillaMusicVolumeMultiplier -= FADE_SPEED;
            if (vanillaMusicVolumeMultiplier <= 0.0f) {
                vanillaMusicVolumeMultiplier = 0.0f;
                stopVanillaMusic(client);
                vanillaMusicVolumeMultiplier = 1.0f;
                playCinematicMusicSafe(client);
                state = FadeState.FADE_IN_CINEMATIC;
            }
            updateVanillaVolume(client);
        } else if (state == FadeState.FADE_IN_CINEMATIC) {
            state = FadeState.IDLE;
        } else if (state == FadeState.FADE_OUT_CINEMATIC) {
            if (currentInstance == null || currentInstance.isDone()) {
                vanillaMusicVolumeMultiplier = 1.0f;
                state = FadeState.IDLE;
            }
        }
    }

    private static void updateVanillaVolume(MinecraftClient client) {
        try {
            float baseVolume = client.options.getSoundVolumeOption(net.minecraft.sound.SoundCategory.MUSIC).getValue().floatValue();
            if (client.getSoundManager() != null) {
                client.getSoundManager().updateSoundVolume(net.minecraft.sound.SoundCategory.MUSIC, baseVolume * vanillaMusicVolumeMultiplier);
            }
        } catch (Exception e) {}
    }

    private static void stopVanillaMusic(MinecraftClient client) {
        if (client.getMusicTracker() != null) {
            client.getMusicTracker().stop();
        }
    }

    private static void playCinematicMusicSafe(MinecraftClient client) {
        try {
            boolean extended = false;
            try { extended = com.ji.afkcinematic.config.ConfigManager.getConfig().extendedMusic; } catch (Exception e) {}
            
            Object randomTrack = null;
            if (extended) {
                int _totalWeight = 220;
                int _r = (int) (Math.random() * _totalWeight);
                if (_r < 10) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_GAME;
                else if (_r < 20) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_CREATIVE;
                else if (_r < 30) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_MENU;
                else if (_r < 40) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_END;
                else if (_r < 50) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_CREDITS;
                else if (_r < 60) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_DISC_CAT;
                else if (_r < 70) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_DISC_BLOCKS;
                else if (_r < 80) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_DISC_CHIRP;
                else if (_r < 90) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_DISC_FAR;
                else if (_r < 100) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_DISC_MALL;
                else if (_r < 110) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_DISC_MELLOHI;
                else if (_r < 120) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_DISC_STAL;
                else if (_r < 130) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_DISC_STRAD;
                else if (_r < 140) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_DISC_WARD;
                else if (_r < 150) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_DISC_WAIT;
                else if (_r < 160) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_DISC_PIGSTEP;
                else if (_r < 170) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_DISC_OTHERSIDE;
                                else if (_r < 190) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_DISC_RELIC;
                else if (_r < 200) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_DISC_CREATOR;
                else if (_r < 210) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_DISC_CREATOR_MUSIC_BOX;
                else randomTrack = net.minecraft.sound.SoundEvents.MUSIC_DISC_PRECIPICE;
            } else {
                int _totalWeight = 79;
                int _r = (int) (Math.random() * _totalWeight);
                if (_r < 35) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_GAME;
                else if (_r < 65) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_CREATIVE;
                else if (_r < 75) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_MENU;
                else if (_r < 78) randomTrack = net.minecraft.sound.SoundEvents.MUSIC_END;
                else randomTrack = net.minecraft.sound.SoundEvents.MUSIC_CREDITS;
            }
            net.minecraft.sound.SoundEvent soundEvent = null;

            if (randomTrack instanceof net.minecraft.registry.entry.RegistryEntry) {
                soundEvent = (net.minecraft.sound.SoundEvent) ((net.minecraft.registry.entry.RegistryEntry<?>) randomTrack).value();
            } else if (randomTrack instanceof net.minecraft.registry.RegistryKey) {
                soundEvent = net.minecraft.registry.Registries.SOUND_EVENT.get((net.minecraft.registry.RegistryKey<net.minecraft.sound.SoundEvent>) randomTrack);
            } else if (randomTrack instanceof net.minecraft.sound.SoundEvent) {
                soundEvent = (net.minecraft.sound.SoundEvent) randomTrack;
            }

            if (soundEvent != null) {
                currentInstance = new CinematicMusicInstance(soundEvent);
                if (client.getSoundManager() != null) {
                    client.getSoundManager().play(currentInstance);
                    com.ji.afkcinematic.JiAFKCinematic.LOGGER.info("[Music] Playing cinematic: {} (multiplier={})", soundEvent.id(), vanillaMusicVolumeMultiplier);
                }
            } else {
                com.ji.afkcinematic.JiAFKCinematic.LOGGER.warn("[Music] Failed to resolve SoundEvent from track");
            }
        } catch (Exception e) {
            com.ji.afkcinematic.JiAFKCinematic.LOGGER.error("[Music] Error playing cinematic music", e);
        }
    }
}

