package com.ji.afkcinematic.mixin;
import com.ji.afkcinematic.diagnostic.HookCapabilities;
import com.ji.afkcinematic.diagnostic.DiagnosticReport.Hook;

import com.ji.afkcinematic.input.KeySequenceTracker;
import com.ji.afkcinematic.input.RuntimeShortcuts;
import com.ji.afkcinematic.input.ShortcutController;
import com.ji.afkcinematic.input.CinematicInputPolicy;

import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.ConfigScreen;
import com.ji.afkcinematic.config.ModConfig;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.render.ToggleToastManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true, require = 0)
    private void onKeyPress(long window, int keyCode, int scanCode, int action, int modifiers, CallbackInfo ci) {
        var event = new com.ji.afkcinematic.input.RuntimeKeystroke(keyCode, scanCode, modifiers);
        if (!com.ji.afkcinematic.input.RuntimeLookInput.acceptWindow(window)) {
            RuntimeShortcuts.reset();
            return;
        }
        HookCapabilities.observed(Hook.INPUT);
        if (action == GLFW.GLFW_RELEASE) {
            RuntimeShortcuts.release(keyCode);
            return;
        }
        if (action != GLFW.GLFW_PRESS) {
            if (RuntimeShortcuts.repeatIsConsumed(keyCode)) ci.cancel();
            return;
        }
        AFKDetector.synchronizeContext();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE
                && CinematicManager.getState() != CinematicState.IDLE
                && com.ji.afkcinematic.ScreenHelper.getCurrentScreen(Minecraft.getInstance()) == null) {
            CinematicManager.cancelForPause();
            KeySequenceTracker.resetAll();
            ci.cancel();
            return;
        }
        boolean cinematicActive = CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE;
        boolean allowed = !(com.ji.afkcinematic.ScreenHelper.getCurrentScreen(Minecraft.getInstance()) instanceof ChatScreen)
                && !(com.ji.afkcinematic.ScreenHelper.getCurrentScreen(Minecraft.getInstance()) instanceof ConfigScreen)
                && !(com.ji.afkcinematic.ScreenHelper.getCurrentScreen(Minecraft.getInstance()) instanceof com.ji.afkcinematic.config.MusicManagementScreen)
                && !com.ji.afkcinematic.config.SupportScreen.isSupportScreen(com.ji.afkcinematic.ScreenHelper.getCurrentScreen(Minecraft.getInstance()));
        ShortcutController.Decision decision = RuntimeShortcuts.press(event, allowed);
        if (decision.action() != ShortcutController.Action.NONE) {
            processShortcut(decision.action(), cinematicActive);
            ci.cancel();
            return;
        }
        if (decision.consume()) {
            ci.cancel();
            return;
        }
        if (decision.newPress() && !(cinematicActive && decision.prefix())) registerKeyboardActivity(event);
    }

    private void registerKeyboardActivity(com.ji.afkcinematic.input.RuntimeKeystroke event) {
        int keyCode = event.key();
        Minecraft chatClient = Minecraft.getInstance();
        boolean inChat = com.ji.afkcinematic.ScreenHelper.getCurrentScreen(chatClient) instanceof ChatScreen;
        if (ConfigManager.getConfig().persistentMode != com.ji.afkcinematic.config.PersistentCinematicMode.PERSISTENT
                && ConfigManager.getConfig().chatVisibility == com.ji.afkcinematic.config.CinematicChatVisibility.HIDDEN
                && (inChat || chatClient.options.keyChat.matches(event.key(), event.scancode())
                || chatClient.options.keyCommand.matches(event.key(), event.scancode()))) {
            AFKDetector.registerActivity(CinematicInputPolicy.Event.CHAT_OPEN,
                    com.ji.afkcinematic.diagnostic.SemanticTrace.Source.KEYBOARD);
            return;
        }
        if (CinematicManager.isFishingCinematic()) {
            Minecraft client = Minecraft.getInstance();
            if (client.options.keyUp.matches(event.key(), event.scancode()) || client.options.keyDown.matches(event.key(), event.scancode())
                    || client.options.keyLeft.matches(event.key(), event.scancode()) || client.options.keyRight.matches(event.key(), event.scancode())) {
                AFKDetector.registerActivity(CinematicInputPolicy.Event.MOVE,
                        com.ji.afkcinematic.diagnostic.SemanticTrace.Source.KEYBOARD);
            }
            return;
        }
        Minecraft client = Minecraft.getInstance();
        boolean chatOpen = com.ji.afkcinematic.ScreenHelper.getCurrentScreen(client) instanceof ChatScreen;
        // Let vanilla receive sprint normally, without cancelling a stationary camera.
        // Conflicting registered actions retain their usual activity semantics.
        if (!chatOpen && CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE
                && isStationarySprintOnly(event)) return;
        CinematicInputPolicy.Event inputEvent = keyCode == GLFW.GLFW_KEY_ESCAPE
                ? CinematicInputPolicy.Event.ESCAPE
                : client.options.keyChat.matches(event.key(), event.scancode()) || client.options.keyCommand.matches(event.key(), event.scancode())
                    ? CinematicInputPolicy.Event.CHAT_OPEN
                    : chatOpen
                        ? CinematicInputPolicy.Event.CHAT_INPUT
                        : CinematicInputPolicy.Event.GAMEPLAY_ACTION;
        if (CinematicInputPolicy.shouldRegisterActivity(
                CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE,
                ConfigManager.getConfig().persistentMode, chatOpen, inputEvent)) {
            AFKDetector.registerActivity(inputEvent,
                    com.ji.afkcinematic.diagnostic.SemanticTrace.Source.KEYBOARD);
        }
    }

    private boolean isStationarySprintOnly(com.ji.afkcinematic.input.RuntimeKeystroke event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !client.options.keySprint.matches(event.key(), event.scancode())) return false;
        boolean anotherAction = false;
        for (net.minecraft.client.KeyMapping binding : client.options.keyMappings) {
            if (binding != client.options.keySprint && binding.matches(event.key(), event.scancode())) {
                anotherAction = true;
                break;
            }
        }
        boolean movementHeld = client.options.keyUp.isDown() || client.options.keyDown.isDown()
                || client.options.keyLeft.isDown() || client.options.keyRight.isDown();
        var motion = client.player.getDeltaMovement();
        return CinematicInputPolicy.isStationarySprintOnly(true, anotherAction, movementHeld,
                motion.x * motion.x + motion.z * motion.z);
    }

    private void processShortcut(ShortcutController.Action action, boolean cinematicWasActive) {
        ModConfig cfg = ConfigManager.getConfig();
        Minecraft client = Minecraft.getInstance();
        switch (action) {
            case MENU -> {
                if (com.ji.afkcinematic.ScreenHelper.getCurrentScreen(client) == null) { com.ji.afkcinematic.ScreenHelper.setScreen(client, new ConfigScreen(null)); }
            }
            case TOGGLE -> {
                ConfigManager.toggleEnabledAsync().thenAccept(success -> {
                    if (success) {
                        ToggleToastManager.show(ConfigManager.getConfig().modEnabled);
                        AFKDetector.synchronizeContext();
                    } else ToggleToastManager.showSaveFailure();
                });
            }
            case IMMEDIATE -> {
                if (cinematicWasActive) CinematicManager.forceDeactivate();
                else if (cfg.modEnabled) CinematicManager.toggleImmediate();
                else AFKDetector.session().rejectStart(com.ji.afkcinematic.session.CinematicSession.Type.MANUAL,
                        com.ji.afkcinematic.session.CinematicSession.Reason.DISABLED);
            }
            case NONE -> { }
        }
    }
}
