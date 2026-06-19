package com.ji.afkcinematic;
import net.minecraft.client.sound.SoundManager;
import java.lang.reflect.Method;
public class TestType2 {
    public static void check() {
        for (Method m : SoundManager.class.getMethods()) {
            System.out.println(m.getName() + " " + m.getParameterCount());
            for (Class<?> p : m.getParameterTypes()) {
                System.out.println("  " + p.getName());
            }
        }
    }
}
