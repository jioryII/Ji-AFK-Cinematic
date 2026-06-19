package com.ji.afkcinematic;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.MusicSound;
import net.minecraft.registry.entry.RegistryEntry;
public class TestType {
    public static void check() {
        Object x = SoundEvents.MUSIC_GAME;
        System.out.println(x.getClass().getName());
    }
}
