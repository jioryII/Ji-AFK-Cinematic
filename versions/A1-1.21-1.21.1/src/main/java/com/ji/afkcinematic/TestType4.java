package com.ji.afkcinematic;
import net.minecraft.client.sound.AbstractSoundInstance;
import java.lang.reflect.Constructor;
public class TestType4 {
    public static void check() {
        for (Constructor<?> c : AbstractSoundInstance.class.getDeclaredConstructors()) {
            System.out.println("AbstractSoundInstance constructor: " + c.getParameterCount());
            for (Class<?> p : c.getParameterTypes()) {
                System.out.println("  " + p.getName());
            }
        }
    }
}
