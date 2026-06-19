package com.ji.afkcinematic;
import net.minecraft.client.sound.MovingSoundInstance;
import java.lang.reflect.Constructor;
public class TestType3 {
    public static void check() {
        for (Constructor<?> c : MovingSoundInstance.class.getDeclaredConstructors()) {
            System.out.println("MovingSoundInstance constructor: " + c.getParameterCount());
            for (Class<?> p : c.getParameterTypes()) {
                System.out.println("  " + p.getName());
            }
        }
    }
}
