package com.ji.afkcinematic.qa;

import com.ji.afkcinematic.JiAFKCinematic;
import com.ji.afkcinematic.ScreenHelper;
import com.ji.afkcinematic.cinematic.CameraController;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.ConfigScreen;
import com.ji.afkcinematic.config.ModConfig;
import com.ji.afkcinematic.config.PersistentCinematicMode;
import com.ji.afkcinematic.input.CinematicInputPolicy;
import com.ji.afkcinematic.diagnostic.MixinState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.List;

/** Opt-in in-game smoke probe used by the external test lab. It is inert in normal games. */
public final class RuntimeProbe {
    public static final String ENABLE_PROPERTY = "ji.afkcinematic.runtimeTest";
    public static final String PASS_MARKER = "JI_RUNTIME_TEST_PASS";
    private static final List<String> CRITICAL_MIXINS = List.of(
            "CameraMixin", "InGameHudMixin", "KeyboardMixin", "MouseMixin", "MinecraftClientMixin");
    private static int waitingTicks;
    private static int phase;

    private RuntimeProbe() {}

    public static void initIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        JiAFKCinematic.LOGGER.info("JI_RUNTIME_TEST_START");
        ClientTickEvents.END_CLIENT_TICK.register(RuntimeProbe::tick);
    }

    private static void tick(Minecraft client) {
        if (phase >= 3) return;
        if (++waitingTicks > 1_200) fail("world/menu probe timed out");
        if (client.player == null || client.level == null) return;
        if (phase == 0) {
            for (String mixin : CRITICAL_MIXINS) check(MixinState.didApply(mixin), "critical mixin did not apply: " + mixin);
            ModConfig config = ConfigManager.getConfig();
            config.characterShotPercentage = 30;
            config.persistentMode = PersistentCinematicMode.INTERACTIVE;
            config.enableMusic = false;
            config.enableLetterbox = false;
            config.recalculate();
            CinematicManager.onAFKDetected();
            check(CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE, "cinematic did not enter active state");
            check(CameraController.getShotCount() == 15, "visible sequence is not exactly 15 shots");
            check(CameraController.getCharacterPresetCount() == 15, "character pool is not 15 shots");
            check(CameraController.getEnvironmentPresetCount() == 15, "environment pool is not 15 shots");
            int characterShots = CameraController.getActiveCharacterShotCount();
            check(characterShots == 4 || characterShots == 5, "30% mix did not select 4-5 character shots");
            check(CameraController.getActiveEnvironmentShotCount() == 15 - characterShots,
                    "environment mix did not complement character shots");
            check(!activity(true, CinematicInputPolicy.Event.OPEN_CHAT_KEY), "chat key incorrectly cancels persistent cinematic");
            check(!activity(false, CinematicInputPolicy.Event.MOUSE_MOVE), "mouse movement incorrectly cancels persistent cinematic");
            check(!activity(true, CinematicInputPolicy.Event.MOUSE_CLICK), "chat click incorrectly cancels persistent cinematic");
            check(activity(false, CinematicInputPolicy.Event.ESCAPE_KEY), "escape does not cancel persistent cinematic");
            ScreenHelper.setScreen(client, new ChatScreen("", false));
            phase = 1;
            return;
        }
        if (phase == 1) {
            check(ScreenHelper.getCurrentScreen(client) instanceof ChatScreen,
                    "chat screen did not remain open for a frame");
            check(CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE,
                    "opening chat canceled the persistent cinematic");
            ScreenHelper.setScreen(client, new ConfigScreen(null));
            phase = 2;
            return;
        }
        if (phase == 2) {
            check(ScreenHelper.getCurrentScreen(client) instanceof ConfigScreen,
                    "configuration screen did not remain open for a frame");
            ScreenHelper.setScreen(client, null);
            CinematicManager.forceDeactivate();
            check(CinematicManager.getState() == CinematicState.IDLE, "cinematic teardown did not restore idle state");
            phase = 3;
            JiAFKCinematic.LOGGER.info(PASS_MARKER);
        }
    }

    private static boolean activity(boolean chatOpen, CinematicInputPolicy.Event event) {
        return CinematicInputPolicy.shouldRegisterActivity(
                true, PersistentCinematicMode.INTERACTIVE, chatOpen, event);
    }

    private static void check(boolean condition, String message) { if (!condition) fail(message); }
    private static void fail(String message) { throw new IllegalStateException("JI_RUNTIME_TEST_FAIL: " + message); }
}
