package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.input.KeySequenceTracker;
import com.ji.afkcinematic.input.RuntimeLookInput;
import com.ji.afkcinematic.input.CinematicInputPolicy;

import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.diagnostic.SemanticTrace;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mouse mixin for AFK detection (Mojmap 26.x).
 * Tracks cursor movement and click activity to reset the AFK timer.
 * Does NOT cancel any events â€” that would freeze the user's input
 * if cinematic state ever desynchronises.
 */
@Mixin(MouseHandler.class)
public class MouseMixin {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"), require = 1)
    private void onVanillaTurnStart(double elapsed, CallbackInfo ci) {
        RuntimeLookInput.beginVanillaTurn(accumulatedDX, accumulatedDY);
    }

    @Inject(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V", shift = At.Shift.AFTER), require = 1)
    private void onVanillaTurnEnd(double elapsed, CallbackInfo ci) {
        if (RuntimeLookInput.endVanillaTurn()) {
            registerMouseActivity(CinematicInputPolicy.Event.LOOK, SemanticTrace.Source.ACCEPTED_MOUSE);
        }
    }


    @Inject(method = "onMove", at = @At("HEAD"), require = 0)
    private void onCursorMove(long window, double x, double y, CallbackInfo ci) {
        if (RuntimeLookInput.onCursorMove(window, x, y)) {
            registerMouseActivity(CinematicInputPolicy.Event.LOOK, SemanticTrace.Source.RAW_CURSOR);
        }
        // NOTE: do NOT cancel onMove here. The cinematic camera rotation is handled
        // inside CameraMixin via Camera.update, so cancelling the OS-level mouse event
        // was redundant and (if state ever desyncs) could freeze the user's cursor,
        // preventing all button interactions in 26.x.
    }

    @Inject(method = "onButton", at = @At("HEAD"), require = 0)
    private void onMouseClick(long window, net.minecraft.client.input.MouseButtonInfo input, int action, CallbackInfo ci) {
        if (!RuntimeLookInput.acceptWindow(window)) return;
        if (action == GLFW.GLFW_PRESS) registerMouseActivity(CinematicInputPolicy.Event.GAMEPLAY_ACTION, SemanticTrace.Source.MOUSE_BUTTON);
        KeySequenceTracker.resetAll();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), require = 0)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (!RuntimeLookInput.acceptWindow(window) || horizontal == 0.0 && vertical == 0.0) return;
        if (com.ji.afkcinematic.ScreenHelper.getCurrentScreen(Minecraft.getInstance()) instanceof ConfigScreen screen) {
            screen.scrollContent(vertical);
            return;
        }
        registerMouseActivity(CinematicInputPolicy.Event.GAMEPLAY_ACTION, SemanticTrace.Source.SCROLL);
    }

    private void registerMouseActivity(CinematicInputPolicy.Event event, SemanticTrace.Source source) {
        Minecraft client = Minecraft.getInstance();
        boolean chatOpen = com.ji.afkcinematic.ScreenHelper.getCurrentScreen(client) instanceof ChatScreen;
        if (CinematicInputPolicy.shouldRegisterActivity(
                CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE,
                CinematicManager.isFishingCinematic(),
                ConfigManager.getConfig().persistentMode, chatOpen, event)) {
            AFKDetector.registerActivity(event, source);
        }
    }
}
