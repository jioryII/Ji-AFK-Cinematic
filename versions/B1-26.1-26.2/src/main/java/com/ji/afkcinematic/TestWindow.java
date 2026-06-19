package com.ji.afkcinematic;

import net.minecraft.client.Minecraft;
import java.lang.reflect.Method;

public class TestWindow {
    public static void test(Minecraft client) {
        Object w = client.getWindow();
        for (Method m : w.getClass().getMethods()) {
            System.out.println(m.getName() + " -> " + m.getReturnType());
        }
    }
}