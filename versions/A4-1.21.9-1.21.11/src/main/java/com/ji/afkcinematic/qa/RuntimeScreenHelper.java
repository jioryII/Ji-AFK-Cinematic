package com.ji.afkcinematic.qa;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;

/** Isolates the 1.21.9+ ChatScreen constructor from the common runtime probe. */
final class RuntimeScreenHelper {
    private RuntimeScreenHelper() {}

    static void openEmptyChat(MinecraftClient client) {
        client.setScreen(new ChatScreen("", false));
    }
}
