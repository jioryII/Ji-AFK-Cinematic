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
    private static int animationTicks = 0;

    private static final int FADE_IN_TICKS = 60;
    private static final int FADE_OUT_TICKS = 40;

    public static void init() {
        // 1.21.11: RenderTickCounter has getTickProgress(boolean), not getTickDelta(boolean)
        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) ->
            render(drawContext, renderTickCounter.getTickProgress(false))
        );
    }

    public static void fadeIn() {
        if (!ConfigManager.getConfig().enableLetterbox) return;
        letterboxState = State.FADING_IN;
        animationTicks = 0;
    }

    public static void fadeOut() {
        if (letterboxState == State.HIDDEN) return;
        letterboxState = State.FADING_OUT;
        animationTicks = 0;
    }

    public static void reset() {
        letterboxState = State.HIDDEN;
        animationTicks = 0;
    }

    public static void tick() {
        if (letterboxState == State.FADING_IN) {
            animationTicks++;
            if (animationTicks >= FADE_IN_TICKS) {
                letterboxState = State.VISIBLE;
            }
        } else if (letterboxState == State.FADING_OUT) {
            animationTicks++;
            if (animationTicks >= FADE_OUT_TICKS) {
                letterboxState = State.HIDDEN;
            }
        }
    }

    public static void renderFromHud(DrawContext drawContext, float tickDelta) {
        render(drawContext, tickDelta);
    }

    private static void render(DrawContext drawContext, float tickDelta) {
        if (letterboxState == State.HIDDEN) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        int targetBarHeight = (int) (height * BAR_RATIO);
        int currentBarHeight = targetBarHeight;
        int alpha = 255;

        if (letterboxState == State.FADING_IN) {
            float progress = (animationTicks + tickDelta) / FADE_IN_TICKS;
            progress = Math.min(1.0f, progress);
            float eased = EasingFunctions.easeInOutCubic(progress);
            currentBarHeight = (int) (targetBarHeight * eased);
            alpha = (int) (255 * eased);
        } else if (letterboxState == State.FADING_OUT) {
            float progress = (animationTicks + tickDelta) / FADE_OUT_TICKS;
            progress = Math.min(1.0f, progress);
            float eased = EasingFunctions.easeOutCubic(progress);
            currentBarHeight = (int) (targetBarHeight * (1.0f - eased));
            alpha = (int) (255 * (1.0f - eased));
        }

        int color = (alpha << 24) | 0x000000;

        // Top bar
        drawContext.fill(0, 0, width, currentBarHeight, color);
        // Bottom bar
        drawContext.fill(0, height - currentBarHeight, width, height, color);
    }
}
