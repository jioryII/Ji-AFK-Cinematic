package com.ji.afkcinematic.music;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;

public class CinematicMusicInstance extends AbstractTickableSoundInstance {
    private final SoundEnvelope envelope = new SoundEnvelope(com.ji.afkcinematic.config.ConfigManager.getConfig().cinematicMusicVolume, 0.02f);

    private net.minecraft.client.resources.sounds.Sound recording;
    CinematicMusicInstance(ResolvedMusicCatalog.Track track) {
        this(track.event()); recording = track.sound();
    }
    public CinematicMusicInstance(SoundEvent sound) {
        super(sound, SoundSource.MASTER, RandomSource.create());
        this.looping = false;
        this.delay = 0;
        this.volume = envelope.volume();
        this.pitch = 1.0f;
        this.relative = true;
    }

    @Override
    public net.minecraft.client.sounds.WeighedSoundEvents resolve(
            net.minecraft.client.sounds.SoundManager manager) {
        var sounds = super.resolve(manager);
        if (recording != null && sounds != null) { this.sound = recording; return sounds; }
        var original = this.sound;
        if (sounds == null || original == null) return sounds;
        net.minecraft.client.resources.sounds.Sound safeFallback = null;
        for (int attempt = 0; attempt < 24; attempt++) {
            String id = this.sound.getLocation().toString();
            if (!CinematicMusicPolicy.isForbidden(id)) {
                if (safeFallback == null) safeFallback = this.sound;
                if (!CinematicMusicManager.recentlyPlayedSound(id)) return sounds;
            }
            var candidate = sounds.getSound(this.random);
            if (candidate != null) this.sound = candidate;
        }
        // Cooldown can relax; the permanent horror exclusion cannot.
        if (safeFallback != null) this.sound = safeFallback;
        else { this.sound = net.minecraft.client.sounds.SoundManager.EMPTY_SOUND; forceStop(); }
        return sounds;
    }

    @Override public boolean canStartSilent() { return true; }
    public void setTargetVolume(float target) { envelope.target(target); }
    public void fadeOutAndStop() { envelope.stop(); }
    public void forceStop() { envelope.forceStop(); this.stop(); }
    public void refreshTargetVolume() { envelope.target(com.ji.afkcinematic.config.ConfigManager.getConfig().cinematicMusicVolume); }
    @Override public void tick() {
        this.volume = envelope.tick();
        if (envelope.finished()) this.stop();
    }
}
