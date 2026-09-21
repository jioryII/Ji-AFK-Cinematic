package com.ji.afkcinematic.render;

import com.ji.afkcinematic.cinematic.EasingFunctions;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.cinematic.CinematicManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class LetterboxRenderer {
    private static final float BAR_RATIO = 0.16f;
    private static volatile boolean cinematicActive;
    private static volatile boolean sleepActive;

    private enum State {
        HIDDEN, FADING_IN, VISIBLE, FADING_OUT
    }
    private static volatile State letterboxState = State.HIDDEN;
    private static volatile float lastProgress = 0.0f;
    private static volatile float currentProgress = 0.0f;

    private static final float FADE_IN_TICKS = 60.0f; // 3 seconds
    private static final float FADE_OUT_TICKS = 8.0f; // 0.4 seconds

    public static void init() {
        // 1.21.11: RenderTickCounter has getTickProgress(boolean), not getTickDelta(boolean)
        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            if (HUDController.isHidden() || sleepActive) return;
            render(drawContext, renderTickCounter.getTickDelta(false));
        });
    }

    public static void setSleepActive(boolean active) {
        if (sleepActive == active) return;
        sleepActive = active;
        if (active) beginFadeIn();
        else if (!cinematicActive) beginFadeOut();
    }
    public static boolean isSleepActive() { return sleepActive; }

    public static void fadeIn() {
        if (!ConfigManager.getConfig().enableLetterbox) return;
        cinematicActive = true;
        beginFadeIn();
    }

    public static void fadeOut() {
        cinematicActive = false;
        if (!sleepActive) beginFadeOut();
    }

    public static void cancelCinematic() {
        cinematicActive = false;
        if (!sleepActive) hideImmediately();
    }

    public static void reset() {
        cinematicActive = false;
        sleepActive = false;
        hideImmediately();
    }

    private static void beginFadeIn() {
        if (letterboxState == State.VISIBLE || letterboxState == State.FADING_IN) return;
        letterboxState = State.FADING_IN;
    }

    private static void beginFadeOut() {
        if (letterboxState == State.HIDDEN || letterboxState == State.FADING_OUT) return;
        letterboxState = State.FADING_OUT;
    }

    private static void hideImmediately() {
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
        int alpha = CinematicManager.isFishingBiteActive()
                ? 248 + (int) (7 * Math.sin(CinematicManager.getBiteTicks() * Math.PI / 10.0))
                : (int) (255 * eased);

        int color = (alpha << 24) | 0x000000;

        // Top bar
        drawContext.getMatrices().push();
        drawContext.getMatrices().translate(0f, currentBarHeight - targetBarHeight, 0f);
        drawContext.fill(0, 0, width, targetBarHeight, color);
        drawContext.getMatrices().pop();

        // Bottom bar
        drawContext.getMatrices().push();
        drawContext.getMatrices().translate(0f, targetBarHeight - currentBarHeight, 0f);
        drawContext.fill(0, height - targetBarHeight, width, height, color);
        drawContext.getMatrices().pop();
    }
}
