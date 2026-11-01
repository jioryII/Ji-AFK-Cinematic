package com.ji.afkcinematic.music;

import com.ji.afkcinematic.platform.ClientEvents;
import net.minecraft.client.Minecraft;


public class CinematicMusicManager {
    public static boolean isOurMusicPlaying;
    private static CinematicMusicInstance currentInstance;
    public enum FadeState { IDLE, FADE_OUT_GAME, FADE_IN_CINEMATIC, FADE_OUT_CINEMATIC }
    private static FadeState state = FadeState.IDLE;
    /** Temporary amplitude. Never written into Minecraft's saved music preference. */
    public static float vanillaMusicVolumeMultiplier = 1.0f;
    private static float currentFade = 1.0f;
    private static final float FADE_SPEED = 0.01f;
    private static int missingTrackRetryTicks;
    private static boolean previousMusicStopped;
    private static PersistentMusicSchedule schedule;
    private static PersistentMusicSchedule.Reservation pendingReservation;
    private static boolean catalogDirty = true, pendingMixed;
    private static final java.util.Map<String, Long> failedUntil = new java.util.HashMap<>();
    private static long musicTick;
    private static RecentMusicHistory recentHistory;
    private static RecentMusicHistory recentSoundHistory;
    private static String pendingTrackId;

    public static void init() {
        LocalMusicPackManager.initialize();
        com.ji.afkcinematic.platform.ClientEvents.CLIENT_STARTED.register(
                LocalMusicPackManager::completeStartupLoad);
        ThirdPartyMusicRegistry.init();
        ClientEvents.END_CLIENT_TICK.register(client -> tick(Minecraft.getInstance()));
    }
    /** Called only when the client is stopping; ordinary world exits keep the cooldown. */
    public static void shutdownHistory() {
        var historyWrites = java.util.stream.Stream.of(recentHistory, recentSoundHistory)
                .filter(java.util.Objects::nonNull)
                .map(history -> history.closeAsync().toCompletableFuture())
                .toArray(java.util.concurrent.CompletableFuture[]::new);
        try {
            var writes = new java.util.ArrayList<java.util.concurrent.CompletableFuture<?>>();
            for (var write : historyWrites) writes.add(write);
            if (schedule != null) writes.add(schedule.closeAsync().toCompletableFuture());
            java.util.concurrent.CompletableFuture.allOf(writes.toArray(java.util.concurrent.CompletableFuture[]::new)).get(2, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            com.ji.afkcinematic.JiAFKCinematic.LOGGER.warn("Interrupted while closing music history IO", interrupted);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException failure) {
            com.ji.afkcinematic.JiAFKCinematic.LOGGER.warn("Music history IO did not close cleanly", failure);
        }
    }
    public static void checkAndPlayMusic() {
        // Create cooldown storage on first music use, after the initial client load.
        if (recentHistory == null) {
            recentHistory = new RecentMusicHistory(com.ji.afkcinematic.PlatformAccess.configDir()
                    .resolve("ji-afk-cinematic/music-history.txt"));
            recentSoundHistory = new RecentMusicHistory(com.ji.afkcinematic.PlatformAccess.configDir()
                    .resolve("ji-afk-cinematic/music-sounds.txt"));
        }
        if (schedule == null) schedule = new PersistentMusicSchedule(com.ji.afkcinematic.PlatformAccess.configDir()
                .resolve("ji-afk-cinematic/music-schedule-v1.txt"));
        schedule.ready();
        if (recentHistory != null) recentHistory.readyAsync();
        if (recentSoundHistory != null) recentSoundHistory.readyAsync();
        Minecraft client = Minecraft.getInstance();
        if (isOurMusicPlaying && state != FadeState.FADE_OUT_CINEMATIC) {
            if (state == FadeState.IDLE) retryMissingTrack(client);
            return;
        }
        if (currentInstance != null) { currentInstance.forceStop(); currentInstance = null; }
        state = FadeState.FADE_OUT_GAME;
        currentFade = vanillaMusicVolumeMultiplier;
        isOurMusicPlaying = true;
        missingTrackRetryTicks = 0;
        previousMusicStopped = false;
    }
    public static void stopMusic() {
        if (isOurMusicPlaying) {
            state = FadeState.FADE_OUT_CINEMATIC;
            if (currentInstance != null) currentInstance.fadeOutAndStop();
        }
    }
    public static void forceStop() {
        if (currentInstance != null) { currentInstance.forceStop(); currentInstance = null; }
        isOurMusicPlaying = false;
        pendingTrackId = null;
        pendingReservation = null;
        missingTrackRetryTicks = 0;
        state = FadeState.IDLE;
        previousMusicStopped = false;
        currentFade = 1;
        setVanillaMusicGain(Minecraft.getInstance(), 1);
    }
    private static void tick(Minecraft client) {
        musicTick++;
        if (missingTrackRetryTicks > 0) missingTrackRetryTicks--;
        if (currentInstance != null) currentInstance.refreshTargetVolume();
        if (isOurMusicPlaying && !com.ji.afkcinematic.config.ConfigManager.getConfig().enableMusic) stopMusic();
        if (isOurMusicPlaying && state == FadeState.IDLE) retryMissingTrack(client);
        if (state == FadeState.FADE_OUT_GAME) {
            currentFade = Math.max(0, currentFade - FADE_SPEED);
            setVanillaMusicGain(client, currentFade);
            if (currentFade == 0) {
                stopVanillaMusic(client);
                previousMusicStopped = true;
                playCinematicMusicSafe(client);
                missingTrackRetryTicks = 20;
                state = FadeState.IDLE;
            }
        } else if (state == FadeState.FADE_OUT_CINEMATIC && (currentInstance == null || currentInstance.isStopped())) {
            currentInstance = null;
            if (!previousMusicStopped && currentFade < 1) {
                currentFade = Math.min(1, currentFade + FADE_SPEED);
                setVanillaMusicGain(client, currentFade);
                if (currentFade < 1) return;
            }
            if (previousMusicStopped) stopVanillaMusic(client);
            setVanillaMusicGain(client, 1);
            currentFade = 1;
            state = FadeState.IDLE;
            isOurMusicPlaying = false;
            previousMusicStopped = false;
            pendingTrackId = null;
        }
    }
    private static void setVanillaMusicGain(Minecraft client, float gain) {
        if (!Float.isFinite(gain)) return;
        vanillaMusicVolumeMultiplier = Math.max(0, Math.min(1, gain));
        AudioVolumeBridge.refresh(client);
    }

    private static void stopVanillaMusic(Minecraft client) {
        if (client.getMusicManager() != null) {
            client.getMusicManager().stopPlaying();
        }
        if (client.getSoundManager() != null) {
            client.getSoundManager().stop(null, net.minecraft.sounds.SoundSource.MUSIC);
        }
    }

    private static ResolvedMusicCatalog.Track getNextTrack() {
        if (schedule == null || !schedule.ready()) return null;
        var config = com.ji.afkcinematic.config.ConfigManager.getConfig();
        var all = ThirdPartyMusicRegistry.recordings();
        if (all.isEmpty()) return null; // Never erase persisted progress before the first catalog is ready.
        if (catalogDirty) {
            // Keep both sources while modes change; recordings shared with vanilla get one vote.
            var vanilla = new java.util.ArrayList<String>();
            var custom = new java.util.ArrayList<String>();
            all.values().stream().sorted(java.util.Comparator.comparing(ResolvedMusicCatalog.Track::id)).forEach(track -> {
                if ((track.sources() & 1) != 0 || (config.extendedMusic && (track.sources() & 2) != 0)) vanilla.add(track.id());
                else if ((track.sources() & 4) != 0) custom.add(track.id());
            });
            // CUSTOM can include recordings also declared by vanilla, but only once in MIXED.
            if (!config.musicMode.includesVanilla()) {
                custom.clear();
                all.values().stream().filter(t -> (t.sources() & 4) != 0).map(ResolvedMusicCatalog.Track::id).sorted().forEach(custom::add);
            }
            schedule.reconcile(vanilla, custom); catalogDirty = false;
        }
        pendingReservation = schedule.reserve(config.musicMode.includesVanilla(), config.musicMode.includesCustom(),
                id -> all.containsKey(id) && failedUntil.getOrDefault(id, 0L) <= musicTick);
        pendingMixed = config.musicMode.includesVanilla() && config.musicMode.includesCustom();
        return pendingReservation == null ? null : all.get(pendingReservation.id());
    }

    static boolean recentlyPlayedSound(String id) {
        return recentSoundHistory != null && recentSoundHistory.contains(id, 9);
    }

    public static void onSettingsChanged() {
        onResourcesReloaded();
    }

    static void onThirdPartyMusicReloaded() {
        catalogDirty = true;
        failedUntil.clear();
        missingTrackRetryTicks = 0;
    }

    static void onResourcesReloaded() {
        pendingTrackId = null; pendingReservation = null;
        onThirdPartyMusicReloaded();
        if (isOurMusicPlaying) {
            if (currentInstance != null) currentInstance.forceStop();
            currentInstance = null;
        }
    }

    private static void retryMissingTrack(Minecraft client) {
        boolean missing = currentInstance == null || currentInstance.isStopped()
                || (client.getSoundManager() != null && !client.getSoundManager().isActive(currentInstance));
        if (!missing && pendingTrackId != null && recentHistory != null) {
            recentHistory.record(pendingTrackId);
            if (schedule != null) schedule.confirm(pendingReservation, pendingMixed);
            pendingReservation = null;
            if (recentSoundHistory != null && currentInstance.getSound() != null) {
                recentSoundHistory.record(currentInstance.getSound().getLocation().toString());
            }
            pendingTrackId = null;
        }
        if (missing && missingTrackRetryTicks <= 0) {
            if (pendingTrackId != null) { failedUntil.put(pendingTrackId, musicTick + 1200); pendingTrackId = null; }
            playCinematicMusicSafe(client);
            missingTrackRetryTicks = 20;
        }
    }

    private static void playCinematicMusicSafe(Minecraft client) {
        try {
            var track = getNextTrack();
            if (track == null) return;

            if (currentInstance != null) {
                currentInstance.forceStop();
                currentInstance = null;
            }
            currentInstance = new CinematicMusicInstance(track);
            pendingTrackId = null;
            if (client.getSoundManager() != null) {
                client.getSoundManager().play(currentInstance);
                pendingTrackId = track.id();
            }
        } catch (Exception e) {
            com.ji.afkcinematic.JiAFKCinematic.LOGGER.warn("Failed to play cinematic music", e);
        }
    }
}
