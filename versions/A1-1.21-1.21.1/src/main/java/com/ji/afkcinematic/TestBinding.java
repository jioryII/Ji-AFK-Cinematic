package com.ji.afkcinematic;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;

public class TestBinding {
    public static void test() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getSoundManager() != null) {
            for (java.lang.reflect.Method m : client.getSoundManager().getClass().getMethods()) {
                System.out.println("Method: " + m.getName());
            }
        }
    }
}
