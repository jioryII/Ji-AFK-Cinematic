package com.ji.afkcinematic.render;

import com.ji.afkcinematic.ScreenHelper;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.EasingFunctions;
import com.ji.afkcinematic.config.ConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.resources.Identifier;

/** Animates only the cinematic bars; camera cuts remain immediate. */
public final class LetterboxRenderer {
    private static final float BAR_RATIO = 0.16f;
    private static final float FADE_IN_MS = 3000.0f;
    private static final float FADE_OUT_MS = 400.0f;
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("ji-afk-cinematic", "letterbox");

    private enum State { HIDDEN, FADING_IN, VISIBLE, FADING_OUT }

    private static volatile State state = State.HIDDEN;
    private static volatile boolean cinematicActive;
    private static volatile boolean sleepActive;
    private static volatile float progress;
    private static volatile long lastTimeMs;

    private LetterboxRenderer() {}

    public static void init() {
        HudElementRegistry.addLast(HUD_ID, (graphics, delta) -> {
            if (HUDController.isHidden()
                    || ScreenHelper.getCurrentScreen(Minecraft.getInstance()) instanceof InBedChatScreen) return;
            render(graphics);
        });
    }

    public static void fadeIn() {
        if (!ConfigManager.getConfig().enableLetterbox) return;
        cinematicActive = true;
        beginFadeIn();
    }

    public static void fadeOut() {
        cinematicActive = false;
        if (!sleepActive) beginFadeOut();
    }

    /** Cancels cinematic bars immediately while preserving bars owned by sleep. */
    public static void cancelCinematic() {
        cinematicActive = false;
        if (sleepActive) {
            return;
        } else {
            hideImmediately();
        }
    }

    public static void reset() {
        cinematicActive = false;
        sleepActive = false;
        hideImmediately();
    }

    public static void tick() {}

    public static void setSleepActive(boolean active) {
        if (sleepActive == active) return;
        sleepActive = active;
        if (active) beginFadeIn();
        else if (!cinematicActive) beginFadeOut();
    }

    public static boolean isSleepActive() { return sleepActive; }
    public static boolean isVisible() { return state != State.HIDDEN; }

    public static void renderFromHud(GuiGraphicsExtractor graphics, float tickDelta) { render(graphics); }

    private static void beginFadeIn() {
        if (state == State.VISIBLE || state == State.FADING_IN) return;
        state = State.FADING_IN;
        lastTimeMs = nowMs();
    }

    private static void beginFadeOut() {
        if (state == State.HIDDEN || state == State.FADING_OUT) return;
        state = State.FADING_OUT;
        lastTimeMs = nowMs();
    }

    private static void hideImmediately() {
        state = State.HIDDEN;
        progress = 0.0f;
        lastTimeMs = 0L;
    }

    private static long nowMs() { return System.nanoTime() / 1_000_000L; }

    private static void render(GuiGraphicsExtractor graphics) {
        long now = nowMs();
        if (lastTimeMs == 0L) lastTimeMs = now;
        long elapsed = Math.max(0L, now - lastTimeMs);
        lastTimeMs = now;

        if (state == State.FADING_IN) {
            progress = Math.min(1.0f, progress + elapsed / FADE_IN_MS);
            if (progress >= 1.0f) state = State.VISIBLE;
        } else if (state == State.FADING_OUT) {
            progress = Math.max(0.0f, progress - elapsed / FADE_OUT_MS);
            if (progress <= 0.0f) state = State.HIDDEN;
        }
        if (state == State.HIDDEN && progress <= 0.0f) return;

        Minecraft client = Minecraft.getInstance();
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        int targetHeight = Math.round(height * BAR_RATIO);
        float eased = EasingFunctions.easeInOutCubic(Math.max(0.0f, Math.min(1.0f, progress)));
        int shownHeight = Math.round(targetHeight * eased);
        int alpha = Math.round(255.0f * eased);
        if (CinematicManager.isFishingBiteActive()) {
            double seconds = System.nanoTime() / 1_000_000_000.0;
            alpha = Math.max(0, Math.min(255,
                    Math.round(alpha * (0.975f + 0.025f * (float) Math.sin(seconds * Math.PI * 4.0)))));
        }
        int color = alpha << 24;

        graphics.pose().pushMatrix();
        graphics.pose().translate(0f, shownHeight - targetHeight);
        graphics.fill(0, 0, width, targetHeight, color);
        graphics.pose().popMatrix();

        graphics.pose().pushMatrix();
        graphics.pose().translate(0f, targetHeight - shownHeight);
        graphics.fill(0, height - targetHeight, width, height, color);
        graphics.pose().popMatrix();
    }
}
