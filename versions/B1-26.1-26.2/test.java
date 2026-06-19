package com.ji.afkcinematic;

import net.minecraft.client.Minecraft;
import net.fabricmc.api.ModInitializer;

public class Test implements ModInitializer {
    @Override
    public void onInitialize() {
        try {
            Minecraft client = Minecraft.getInstance();
            Object opt = null;
            for (java.lang.reflect.Method m : client.options.getClass().getMethods()) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == net.minecraft.sounds.SoundSource.class) {
                    System.out.println("FOUND METHOD: " + m.getName());
                    opt = m.invoke(client.options, net.minecraft.sounds.SoundSource.MUSIC);
                }
            }
            if (opt == null) System.out.println("NOT FOUND OPTION METHOD");
        } catch(Exception e) { e.printStackTrace(); }
    }
}
