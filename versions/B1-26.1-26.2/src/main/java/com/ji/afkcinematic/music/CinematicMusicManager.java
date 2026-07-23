package com.ji.afkcinematic.music;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CinematicMusicManager {
    public static boolean isOurMusicPlaying = false;
    private static CinematicMusicInstance currentInstance;
    
    public enum FadeState { IDLE, FADE_OUT_GAME, FADE_IN_CINEMATIC, FADE_OUT_CINEMATIC }
    private static FadeState state = FadeState.IDLE;
    private static float originalMusicVolume = -1.0f;
    private static float currentFade = 1.0f;
    private static final float FADE_SPEED = 0.01f;

    private static final List<Object> shuffleBag = new ArrayList<>();
    private static int shuffleIndex = 0;

    public static void forceReloadMusic() { shuffleBag.clear(); }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick(Minecraft.getInstance()));
    }

    public static void checkAndPlayMusic() {
        if (isOurMusicPlaying) return;
        state = FadeState.FADE_OUT_GAME;
        originalMusicVolume = getMusicOptionVolume(Minecraft.getInstance());
        currentFade = 1.0f;
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
            } else {
                isOurMusicPlaying = false;
                state = FadeState.IDLE;
                if (originalMusicVolume != -1.0f) {
                    setMusicOptionVolume(Minecraft.getInstance(), originalMusicVolume);
                    originalMusicVolume = -1.0f;
                }
            }
        }
    }

    public static void forceStop() {
        if (currentInstance != null) {
            currentInstance.forceStop();
            currentInstance = null;
        }
        isOurMusicPlaying = false;
        state = FadeState.IDLE;
        if (originalMusicVolume != -1.0f) {
            setMusicOptionVolume(Minecraft.getInstance(), originalMusicVolume);
            originalMusicVolume = -1.0f;
        }
    }

    private static void tick(Minecraft client) {
        if (state == FadeState.IDLE) {
            if (isOurMusicPlaying && currentInstance != null) {
                if (currentInstance.isStopped() || (client.getSoundManager() != null && !client.getSoundManager().isActive(currentInstance))) {
                    playCinematicMusicSafe(client);
                }
            }
            return;
        }

        if (state == FadeState.FADE_OUT_GAME) {
            currentFade -= FADE_SPEED;
            if (currentFade <= 0.0f) {
                currentFade = 0.0f;
                setMusicOptionVolume(client, 0.0f);
                stopVanillaMusic(client);
                playCinematicMusicSafe(client);
                state = FadeState.FADE_IN_CINEMATIC;
            } else {
                setMusicOptionVolume(client, originalMusicVolume * currentFade);
            }
        } else if (state == FadeState.FADE_IN_CINEMATIC) {
            currentFade += FADE_SPEED;
            if (currentFade >= 1.0f) {
                currentFade = 1.0f;
                state = FadeState.IDLE;
            }
            setMusicOptionVolume(client, originalMusicVolume * currentFade);
        } else if (state == FadeState.FADE_OUT_CINEMATIC) {
            if (currentInstance == null || currentInstance.isStopped()) {
                if (originalMusicVolume != -1.0f) {
                    setMusicOptionVolume(client, originalMusicVolume);
                    originalMusicVolume = -1.0f;
                }
                state = FadeState.IDLE;
                isOurMusicPlaying = false;
            }
        }
    }

    private static float getMusicOptionVolume(Minecraft client) {
        try {
            return client.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MUSIC);
        } catch (Exception e) {
            return 1.0f;
        }
    }

        private static void setMusicOptionVolume(Minecraft client, float volume) {
        try {
            Object optionInstance = null;
            for (java.lang.reflect.Method m : client.options.getClass().getMethods()) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == net.minecraft.sounds.SoundSource.class && m.getReturnType().getName().contains("Option")) {
                    optionInstance = m.invoke(client.options, net.minecraft.sounds.SoundSource.MUSIC);
                    break;
                }
            }
            if (optionInstance != null) {
                for (java.lang.reflect.Method m : optionInstance.getClass().getMethods()) {
                    if (m.getName().equals("set") && m.getParameterCount() == 1) {
                        m.invoke(optionInstance, (double) volume);
                        break;
                    }
                }
            }
            if (client.getSoundManager() != null) {
                for (java.lang.reflect.Method m : client.getSoundManager().getClass().getMethods()) {
                    if (m.getName().equals("updateSourceVolume")) {
                        if (m.getParameterCount() == 1) {
                            m.invoke(client.getSoundManager(), net.minecraft.sounds.SoundSource.MUSIC);
                            break;
                        } else if (m.getParameterCount() == 2) {
                            m.invoke(client.getSoundManager(), net.minecraft.sounds.SoundSource.MUSIC, volume);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {}
    }
    private static void stopVanillaMusic(Minecraft client) {
        if (client.getMusicManager() != null) {
            client.getMusicManager().stopPlaying();
        }
    }

    private static void fillShuffleBag() {
        shuffleBag.clear();
        addTrackSafe(() -> SoundEvents.MUSIC_GAME);
        addTrackSafe(() -> SoundEvents.MUSIC_CREATIVE);
        addTrackSafe(() -> SoundEvents.MUSIC_MENU);
        addTrackSafe(() -> SoundEvents.MUSIC_END);
        addTrackSafe(() -> SoundEvents.MUSIC_CREDITS);
        
        if (com.ji.afkcinematic.config.ConfigManager.getConfig().extendedMusic) {
            addTrackSafe(() -> SoundEvents.MUSIC_DISC_CAT);
            addTrackSafe(() -> SoundEvents.MUSIC_DISC_BLOCKS);
            addTrackSafe(() -> SoundEvents.MUSIC_DISC_CHIRP);
            addTrackSafe(() -> SoundEvents.MUSIC_DISC_FAR);
            addTrackSafe(() -> SoundEvents.MUSIC_DISC_MALL);
            addTrackSafe(() -> SoundEvents.MUSIC_DISC_MELLOHI);
            addTrackSafe(() -> SoundEvents.MUSIC_DISC_STAL);
            addTrackSafe(() -> SoundEvents.MUSIC_DISC_STRAD);
            addTrackSafe(() -> SoundEvents.MUSIC_DISC_WARD);
            addTrackSafe(() -> SoundEvents.MUSIC_DISC_WAIT);
            addTrackSafe(() -> SoundEvents.MUSIC_DISC_PIGSTEP);
            addTrackSafe(() -> SoundEvents.MUSIC_DISC_OTHERSIDE);
            addTrackSafe(() -> SoundEvents.MUSIC_DISC_RELIC);
            addTrackSafe(() -> SoundEvents.MUSIC_DISC_CREATOR);
            addTrackSafe(() -> SoundEvents.MUSIC_DISC_CREATOR_MUSIC_BOX);
            addTrackSafe(() -> SoundEvents.MUSIC_DISC_PRECIPICE);
        }
        
        Collections.shuffle(shuffleBag);
        shuffleIndex = 0;
    }

    private static void addTrackSafe(java.util.function.Supplier<Object> getter) {
        try {
            Object track = getter.get();
            if (track != null) shuffleBag.add(track);
        } catch (Throwable e) {}
    }

    private static Object getNextTrack() {
        if (shuffleBag.isEmpty() || shuffleIndex >= shuffleBag.size()) {
            fillShuffleBag();
        }
        if (shuffleBag.isEmpty()) return null;
        return shuffleBag.get(shuffleIndex++);
    }

    private static void playCinematicMusicSafe(Minecraft client) {
        try {
            Object track = getNextTrack();
            if (track == null) return;
            
            net.minecraft.sounds.SoundEvent soundEvent;
            if (track instanceof net.minecraft.sounds.SoundEvent) {
                soundEvent = (net.minecraft.sounds.SoundEvent) track;
            } else {
                soundEvent = ((net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent>) track).value();
            }
            
            if (currentInstance != null) {
                currentInstance.forceStop();
                currentInstance = null;
            }
            currentInstance = new CinematicMusicInstance(soundEvent);
            if (client.getSoundManager() != null) {
                client.getSoundManager().play(currentInstance);
            }
        } catch (Exception e) {}
    }
}
