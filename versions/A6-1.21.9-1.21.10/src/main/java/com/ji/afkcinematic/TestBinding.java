package com.ji.afkcinematic;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class TestBinding {
    public static void test() {
        KeyBinding test = new KeyBinding(
            "key.test",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            KeyBinding.Category.create(net.minecraft.util.Identifier.of("ji_afkcinematic", "keys"))
        );
    }
}
