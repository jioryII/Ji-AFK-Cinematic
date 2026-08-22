package com.ji.afkcinematic.music;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CinematicMusicManager {
    public static boolean isOurMusicPlaying = false;
    private static CinematicMusicInstance currentInstance;
    
    public enum FadeState { IDLE, FADE_OUT_GAME, FADE_IN_CINEMATIC, FADE_OUT_CINEMATIC }
    private static FadeState state = FadeState.IDLE;
    // Public hook kept for parity with A-tracks (unused in B1; legacy SoundSystemMixin
    // reference in A1-A3). Harmless static float.
    public static float vanillaMusicVolumeMultiplier = 1.0f;
    private static float originalMusicVolume = -1.0f;
    private static float currentFade = 1.0f;
    private static final float FADE_SPEED = 0.01f;

    private static final List<Object> trackPool = new ArrayList<>();
    private static final BalancedShuffleBag<Object> shuffleBag = new BalancedShuffleBag<>();
    // Cached reflection: all three method lookups used to scan getMethods() every fade
    // tick (~200 times per cinematic start/stop). Resolved lazily once and reused.
    private static java.lang.reflect.Method cachedGetSourceOption;  // options.getXxxVolume(SoundSource)
    private static java.lang.reflect.Method cachedOptionSet;        // OptionInstance.set(double)
    private static java.lang.reflect.Method cachedUpdateSourceVolume; // SoundManager.updateSourceVolume
    private static boolean reflectionResolved = false;

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
        if (!Float.isFinite(volume)) return; // guard against NaN/Inf reaching the sound system
        try {
            Object soundManager = client.getSoundManager();
            Object options = client.options;
            if (!reflectionResolved && options != null) {
                resolveReflection(options, soundManager);
            }

            if (cachedGetSourceOption != null && options != null) {
                Object optionInstance = cachedGetSourceOption.invoke(options, net.minecraft.sounds.SoundSource.MUSIC);
                if (optionInstance != null && cachedOptionSet != null) {
                    cachedOptionSet.invoke(optionInstance, (double) volume);
                }
            }
            if (soundManager != null && cachedUpdateSourceVolume != null) {
                if (cachedUpdateSourceVolume.getParameterCount() == 1) {
                    cachedUpdateSourceVolume.invoke(soundManager, net.minecraft.sounds.SoundSource.MUSIC);
                } else if (cachedUpdateSourceVolume.getParameterCount() == 2) {
                    cachedUpdateSourceVolume.invoke(soundManager, net.minecraft.sounds.SoundSource.MUSIC, volume);
                }
            }
        } catch (Exception e) {
            com.ji.afkcinematic.JiAFKCinematic.LOGGER.warn("Failed to set music option volume to {}", volume, e);
        }
    }

    private static void resolveReflection(Object options, Object soundManager) {
        reflectionResolved = true;
        try {
            if (options != null) {
                for (java.lang.reflect.Method m : options.getClass().getMethods()) {
                    if (m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == net.minecraft.sounds.SoundSource.class
                        && m.getReturnType().getName().contains("Option")) {
                        cachedGetSourceOption = m;
                        break;
                    }
                }
            }
            if (cachedGetSourceOption != null) {
                Object optInstance = cachedGetSourceOption.invoke(options, net.minecraft.sounds.SoundSource.MUSIC);
                if (optInstance != null) {
                    for (java.lang.reflect.Method m : optInstance.getClass().getMethods()) {
                        if (m.getName().equals("set") && m.getParameterCount() == 1) {
                            cachedOptionSet = m;
                            break;
                        }
                    }
                }
            }
            if (soundManager != null) {
                for (java.lang.reflect.Method m : soundManager.getClass().getMethods()) {
                    if (m.getName().equals("updateSourceVolume")) {
                        cachedUpdateSourceVolume = m;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            com.ji.afkcinematic.JiAFKCinematic.LOGGER.warn("Could not fully resolve music-volume reflection; volume restore may be partial", e);
        }
    }

    private static void stopVanillaMusic(Minecraft client) {
        if (client.getMusicManager() != null) {
            client.getMusicManager().stopPlaying();
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
        } catch (Exception e) {
            com.ji.afkcinematic.JiAFKCinematic.LOGGER.warn("Failed to play cinematic music", e);
        }
    }
}
