package com.ji.afkcinematic.render;

import com.ji.afkcinematic.cinematic.EasingFunctions;
import com.ji.afkcinematic.config.ConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class LetterboxRenderer {
    private static final float BAR_RATIO = 0.16f;

    private enum State {
        HIDDEN, FADING_IN, VISIBLE, FADING_OUT
    }
    private static State letterboxState = State.HIDDEN;
    private static float lastProgress = 0.0f;
    private static float currentProgress = 0.0f;

    private static final float FADE_IN_TICKS = 60.0f; // 3 seconds
    private static final float FADE_OUT_TICKS = 8.0f; // 0.4 seconds

    public static void init() {
        // 1.21.11: RenderTickCounter has getTickProgress(boolean), not getTickDelta(boolean)
        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) ->
            render(drawContext, renderTickCounter.getTickProgress(false))
        );
    }

    public static void fadeIn() {
        if (!ConfigManager.getConfig().enableLetterbox) return;
        letterboxState = State.FADING_IN;
    }

    public static void fadeOut() {
        
        letterboxState = State.FADING_OUT;
    }

    public static void reset() {
        letterboxState = State.HIDDEN;
        lastProgress = 0.0f;
        currentProgress = 0.0f;
    }

    public static void tick() {
        lastProgress = currentProgress;
        if (letterboxState == State.FADING_IN) {
            currentProgress += (1.0f / FADE_IN_TICKS);
            if (currentProgress >= 1.0f) { currentProgress = 1.0f; letterboxState = State.VISIBLE; }
        } else if (letterboxState == State.FADING_OUT) {
            currentProgress -= (1.0f / FADE_OUT_TICKS);
            if (currentProgress <= 0.0f) { currentProgress = 0.0f; letterboxState = State.HIDDEN; }
        }
    }

    public static void renderFromHud(DrawContext drawContext, float tickDelta) {
        render(drawContext, tickDelta);
    }

    private static void render(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        int targetBarHeight = (int) (height * BAR_RATIO);
        float lerpedProgress = lastProgress + (currentProgress - lastProgress) * tickDelta;
        if (lerpedProgress <= 0.0f && letterboxState == State.HIDDEN) return;

        float eased = EasingFunctions.easeInOutCubic(lerpedProgress);
        float currentBarHeight = targetBarHeight * eased;
        int alpha = (int) (255 * eased);

        int color = (alpha << 24) | 0x000000;

        // Top bar
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(0f, currentBarHeight - targetBarHeight);
        drawContext.fill(0, 0, width, targetBarHeight, color);
        drawContext.getMatrices().popMatrix();

        // Bottom bar
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(0f, targetBarHeight - currentBarHeight);
        drawContext.fill(0, height - targetBarHeight, width, height, color);
        drawContext.getMatrices().popMatrix();
    }
}