package com.ji.afkcinematic.music;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;

public class CinematicMusicInstance extends AbstractTickableSoundInstance {
    private float targetVolume = 1.0f;
    private final float fadeSpeed = 0.02f;

    public CinematicMusicInstance(SoundEvent sound) {
        super(sound, SoundSource.MASTER, RandomSource.create());
        this.looping = false;
        this.delay = 0;
        this.volume = 0.01f;
        this.targetVolume = com.ji.afkcinematic.config.ConfigManager.getConfig().cinematicMusicVolume;
        this.pitch = 1.0f;
        this.relative = true;
    }

    public void setTargetVolume(float target) {
        this.targetVolume = target;
    }

    public void fadeOutAndStop() {
        this.targetVolume = 0.0f;
    }

    public void forceStop() {
        this.stop();
    }

    public void refreshTargetVolume() {
        if (this.targetVolume > 0.0f) {
            this.targetVolume = com.ji.afkcinematic.config.ConfigManager.getConfig().cinematicMusicVolume;
        }
    }

    @Override
    public void tick() {
        if (this.volume < this.targetVolume) {
            this.volume += this.fadeSpeed;
            if (this.volume > this.targetVolume) {
                this.volume = this.targetVolume;
            }
        } else if (this.volume > this.targetVolume) {
            this.volume -= this.fadeSpeed;
            if (this.volume < this.targetVolume) {
                this.volume = this.targetVolume;
            }
        }

        if (this.targetVolume == 0.0f && this.volume <= 0.0f) {
            this.stop();
        }
    }
}
