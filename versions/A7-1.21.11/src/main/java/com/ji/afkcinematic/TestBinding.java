package com.ji.afkcinematic;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class TestBinding {
    public static void test(KeyBinding binding) {
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(binding);
    }
}
