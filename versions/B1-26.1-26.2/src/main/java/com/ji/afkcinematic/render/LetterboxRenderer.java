package com.ji.afkcinematic.render;

import com.ji.afkcinematic.cinematic.EasingFunctions;
import com.ji.afkcinematic.config.ConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.client.DeltaTracker;

public class LetterboxRenderer {
    private static final float BAR_RATIO = 0.16f;
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("ji-afk-cinematic", "letterbox");

    private enum State {
        HIDDEN, FADING_IN, VISIBLE, FADING_OUT
    }
    private static State letterboxState = State.HIDDEN;
    private static float lastProgress = 0.0f;
    private static float currentProgress = 0.0f;

    private static long lastTime = 0;

    private static final float FADE_IN_MS = 3000.0f; // 3 seconds
    private static final float FADE_OUT_MS = 400.0f; // 0.4 seconds

    public static void init() {
        HudElementRegistry.addLast(HUD_ID, (drawContext, deltaTracker) ->
            render(drawContext, deltaTracker.getGameTimeDeltaPartialTick(true))
        );
    }

    public static void fadeIn() {
        if (!ConfigManager.getConfig().enableLetterbox) return;
        letterboxState = State.FADING_IN;
        lastTime = System.nanoTime() / 1_000_000L;
    }

    public static void fadeOut() {
        
        letterboxState = State.FADING_OUT;
        lastTime = System.nanoTime() / 1_000_000L;
    }

    public static void reset() {
        letterboxState = State.HIDDEN;
        lastProgress = 0.0f;
        currentProgress = 0.0f;
        lastTime = 0;
    }

    public static void tick() {
        // Obsolete: Transition is now handled in render loop via system time
    }

    public static void renderFromHud(GuiGraphicsExtractor drawContext, float tickDelta) {
        render(drawContext, tickDelta);
    }

    private static void render(GuiGraphicsExtractor drawContext, float tickDelta) {
        long now = System.nanoTime() / 1_000_000L;
        if (lastTime == 0) lastTime = now;
        long dt = now - lastTime;
        lastTime = now;
        
        if (letterboxState == State.FADING_IN) {
            currentProgress += (dt / FADE_IN_MS);
            if (currentProgress >= 1.0f) { currentProgress = 1.0f; letterboxState = State.VISIBLE; }
        } else if (letterboxState == State.FADING_OUT) {
            currentProgress -= (dt / FADE_OUT_MS);
            if (currentProgress <= 0.0f) { currentProgress = 0.0f; letterboxState = State.HIDDEN; }
        }
        
        Minecraft client = Minecraft.getInstance();
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();

        int targetBarHeight = (int) (height * BAR_RATIO);
        float lerpedProgress = Math.max(0.0f, Math.min(1.0f, currentProgress));
        
        if (lerpedProgress <= 0.0f && letterboxState == State.HIDDEN) return;

        float eased = EasingFunctions.easeInOutCubic(lerpedProgress);
        float currentBarHeight = Math.round(targetBarHeight * eased);
        int alpha = (int) (255 * eased);

        int color = (alpha << 24) | 0x000000;

        // Top bar
        drawContext.pose().pushMatrix();
        drawContext.pose().translate(0f, currentBarHeight - targetBarHeight);
        drawContext.fill(0, 0, width, targetBarHeight, color);
        drawContext.pose().popMatrix();

        // Bottom bar
        drawContext.pose().pushMatrix();
        drawContext.pose().translate(0f, targetBarHeight - currentBarHeight);
        drawContext.fill(0, height - targetBarHeight, width, height, color);
        drawContext.pose().popMatrix();
    }
}