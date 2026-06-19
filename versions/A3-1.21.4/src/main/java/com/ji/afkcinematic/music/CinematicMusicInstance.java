package com.ji.afkcinematic.music;

import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.random.Random;

public class CinematicMusicInstance extends MovingSoundInstance {
    @Override
    public boolean shouldAlwaysPlay() {
        return true;
    }

    private float targetVolume = 1.0f;
    private final float fadeSpeed = 0.02f;

    public CinematicMusicInstance(SoundEvent sound) {
        super(sound, SoundCategory.MASTER, Random.create());
        this.repeat = false;
        this.repeatDelay = 0;
        this.volume = 0.01f;
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
        this.setDone();
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
            this.setDone();
        }
    }
}
