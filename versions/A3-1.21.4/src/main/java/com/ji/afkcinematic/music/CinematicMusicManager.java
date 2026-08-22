package com.ji.afkcinematic.music;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CinematicMusicManager {
    public static boolean isOurMusicPlaying = false;
    private static CinematicMusicInstance currentInstance;

    public enum FadeState { IDLE, FADE_OUT_GAME, FADE_IN_CINEMATIC, FADE_OUT_CINEMATIC }
    private static FadeState state = FadeState.IDLE;
    // Public hook for legacy SoundSystemMixin in A1-A3 (multiplies the vanilla volume
    // during a cinematic). Unused in A4+ and B1 but kept for binary compatibility.
    public static float vanillaMusicVolumeMultiplier = 1.0f;
    private static float originalMusicVolume = -1.0f;
    private static float currentFade = 1.0f;
    private static final float FADE_SPEED = 0.01f;

    private static final List<Object> trackPool = new ArrayList<>();
    private static final BalancedShuffleBag<Object> shuffleBag = new BalancedShuffleBag<>();
    // Cached reflection: SoundManager#updateSoundVolume is resolved once instead of
    // scanning getMethods() every fade tick (~200 times per cinematic start/stop).
    private static java.lang.reflect.Method cachedUpdateSoundVolume;
    private static boolean updateSoundVolumeMissing = false;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick(client));
    }

    public static void checkAndPlayMusic() {
        if (isOurMusicPlaying) return;
        state = FadeState.FADE_OUT_GAME;
        originalMusicVolume = getMusicOptionVolume(MinecraftClient.getInstance());
        currentFade = 1.0f;
        isOurMusicPlaying = true;
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
                    setMusicOptionVolume(MinecraftClient.getInstance(), originalMusicVolume);
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
            setMusicOptionVolume(MinecraftClient.getInstance(), originalMusicVolume);
            originalMusicVolume = -1.0f;
        }
    }

    private static void tick(MinecraftClient client) {
        if (state == FadeState.IDLE) {
            if (isOurMusicPlaying && currentInstance != null) {
                if (currentInstance.isDone() || (client.getSoundManager() != null && !client.getSoundManager().isPlaying(currentInstance))) {
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
            if (currentInstance == null || currentInstance.isDone()) {
                if (originalMusicVolume != -1.0f) {
                    setMusicOptionVolume(client, originalMusicVolume);
                    originalMusicVolume = -1.0f;
                }
                state = FadeState.IDLE;
                isOurMusicPlaying = false;
            }
        }
    }

    private static float getMusicOptionVolume(MinecraftClient client) {
        try {
            return client.options.getSoundVolumeOption(net.minecraft.sound.SoundCategory.MUSIC).getValue().floatValue();
        } catch (Exception e) {
            return 1.0f;
        }
    }

    private static void setMusicOptionVolume(MinecraftClient client, float volume) {
        if (!Float.isFinite(volume)) return; // guard against NaN/Inf reaching the sound system
        try {
            client.options.getSoundVolumeOption(net.minecraft.sound.SoundCategory.MUSIC).setValue((double) volume);
            if (client.getSoundManager() != null && !updateSoundVolumeMissing) {
                java.lang.reflect.Method m = cachedUpdateSoundVolume;
                if (m == null) {
                    for (java.lang.reflect.Method candidate : client.getSoundManager().getClass().getMethods()) {
                        if (candidate.getName().equals("updateSoundVolume")) {
                            cachedUpdateSoundVolume = candidate;
                            m = candidate;
                            break;
                        }
                    }
                    if (m == null) {
                        updateSoundVolumeMissing = true; // don't keep re-scanning a missing method
                    }
                }
                if (m != null) {
                    if (m.getParameterCount() == 1) {
                        m.invoke(client.getSoundManager(), net.minecraft.sound.SoundCategory.MUSIC);
                    } else if (m.getParameterCount() == 2) {
                        m.invoke(client.getSoundManager(), net.minecraft.sound.SoundCategory.MUSIC, volume);
                    }
                }
            }
        } catch (Exception e) {
            com.ji.afkcinematic.JiAFKCinematic.LOGGER.warn("Failed to set music option volume to {}", volume, e);
        }
    }

    private static void stopVanillaMusic(MinecraftClient client) {
        if (client.getMusicTracker() != null) {
            client.getMusicTracker().stop();
        }
    }

    private static void fillShuffleBag() {
        trackPool.clear();
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
        
        shuffleBag.replace(trackPool);
    }

    private static void addTrackSafe(java.util.function.Supplier<Object> getter) {
        try {
            Object track = getter.get();
            if (track != null && !isForbiddenTrack(track)) trackPool.add(track);
        } catch (Throwable e) {
            com.ji.afkcinematic.JiAFKCinematic.LOGGER.warn("Failed to resolve a cinematic music track", e);
        }
    }

    /** These narrative horror recordings are never eligible for cinematic playback. */
    private static boolean isForbiddenTrack(Object track) {
        return Objects.equals(track, SoundEvents.MUSIC_DISC_5)
                || Objects.equals(track, SoundEvents.MUSIC_DISC_11)
                || Objects.equals(track, SoundEvents.MUSIC_DISC_13);
    }

    private static Object getNextTrack() {
        if (shuffleBag.isCycleComplete()) {
            fillShuffleBag();
        }
        return shuffleBag.next();
    }

    private static void playCinematicMusicSafe(MinecraftClient client) {
        try {
            Object track = getNextTrack();
            if (track == null) return;
            
            net.minecraft.sound.SoundEvent soundEvent;
            if (track instanceof net.minecraft.sound.SoundEvent) {
                soundEvent = (net.minecraft.sound.SoundEvent) track;
            } else {
                soundEvent = ((net.minecraft.registry.entry.RegistryEntry<net.minecraft.sound.SoundEvent>) track).value();
            }
            
            if (currentInstance != null) {
                currentInstance.forceStop();
                currentInstance = null;
            }
            currentInstance = new CinematicMusicInstance(soundEvent);
            if (client.getSoundManager() != null) {
                client.getSoundManager().play(currentInstance);
            }
        } catch (Exception e) {
            com.ji.afkcinematic.JiAFKCinematic.LOGGER.warn("Failed to play cinematic music", e);
        }
    }
}
