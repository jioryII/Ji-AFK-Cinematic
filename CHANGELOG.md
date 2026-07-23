# Changelog

All notable changes to Ji AFK Cinematic will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)
within each Minecraft version track (MCVM).

## [2.2.1] - 2026-07-22

### Fixed

- **Real volume slider in mod menu (all 8 versions):** the "Music Volume" row was a static green
  mock (`context.fill(...)`) drawn at render time with no `SliderWidget` registered, so it
  could not be dragged and was always at 100%. Now it is a real `SliderWidget` (Yarn) /
  `AbstractSliderButton` (Mojmap) wired to `cinematicMusicVolume` in `ConfigScreen` (mirrors the
  pattern used by `camera_speed`). The decorative green line and label are gone.
- **Menu layout (all 8 versions):** the screen title (`§6§lJi AFK Cinematic`) and the
  `By jiory_` subtitle used to be drawn at Y=8 / Y=20 (pinned to the top edge). They are now
  drawn at Y=55 / Y=65 and the grid of sliders/buttons starts at `yLeft = yRight = 85`, the
  same spacing Ji Zoom Cinematic uses. The whole panel sits lower with no overlap.
- **78 broken `*.json` lang files caused Minecraft to fall back to raw translation keys
  (all 8 versions):** a previous PowerShell-based edit left a trailing comma before the
  closing `}` in the `overlay.ji_afkcinematic.toggle.off` entry. JSON parsers rejected the
  file, Minecraft silently dropped it, and the UI rendered truncated keys like
  `atio.menu_keys.labelF7 + H`. All 80 lang files (10 locales × 8 versions) are now valid
  JSON; the trailing comma is gone in every file.
- **B1 HUD no longer hidden on 26.x (release-blocker):** the HUD orchestrator was not being
  cancelled at all in `B1-26.1-26.2`. The mixin targeted `@Mixin(Hud.class)` (a sub-component)
  with the wrong method signature, so vanilla UI kept rendering and only the hand was hidden
  (via the third-person camera). The mixin is now `@Mixin(Gui.class)` and cancels
  `extractRenderState(...)` at HEAD, the same top-level entry Ji-Zoom-Cinematic already uses.
  Two signatures are targeted because Mojang changed `extractRenderState` between 26.1.2 and
  26.2; the 26.2 variant reconstructs `GuiGraphicsExtractor` via reflection so manual
  letterbox drawing inside the cancelled method keeps working on both.
- **B1 in-game hand rendering:** `GameRenderer#renderItemInHand` is now cancelled during the
  cinematic via a new dedicated mixin (`GameRendererMixin`, registered in
  `ji-afk-cinematic.mixins.json`). Hides the hand explicitly instead of relying on the
  third-person camera trick.
- **Report button never opened Discord on 26.x:** the previous B1 implementation wrapped
  `java.awt.Desktop.getDesktop().browse(...)`, which silently fails in the MC runtime. All 8
  versions now use `ConfirmLinkScreen` (Yarn `opening(...)`, Mojmap `confirmLink(...)`) for
  the official "Open this webpage?" confirmation and `Util.getOperatingSystem().open(...)`
  for the actual link.
- **Dead F7 native keybinding removed from A7:** A7 was the only version that registered an
  MC-native `KeyBinding` for F7 (`JiAFKCinematic.java`). The keybinding was never consulted
  (`wasPressed()` was called nowhere). The whole registration block, the `configKeyBinding`
  field, and the `KeyBindingHelper` / `KeyBinding` / `InputUtil` imports are removed. The
  mod's own 2-key sequence detector (`KeyboardMixin` + `KeySequenceTracker`) keeps handling
  F7+H and Ctrl+H. The orphan lang keys `key.ji_afkcinematic.open_config`,
  `category.ji_afkcinematic.keys` and `key.category.ji_afkcinematic.keys` are also removed
  from all 80 lang files.
- **BOM UTF-8 emitted by PowerShell `Set-Content`:** a previous round of edits had been
  silently inserting a BOM in some `.java`/`.gradle`/`.json` files. Cleaned from all sources
  so they compile cleanly under strict Java+Gradle parsers.

### Changed

- **Two-tone keybind buttons (all 8 versions):** the "Open Menu: F7 + H" / "Quick toggle: Ctrl + H"
  buttons now render the label in white and the key chord in yellow (`Formatting.WHITE` /
  `Formatting.YELLOW` on Yarn, `ChatFormatting` on Mojmap). The lang keys were split into
  `*.label` (e.g. `menu_keys.label = "Abrir menú: "`, `toggle_keys.label = "Acceso directo: "`)
  while the key chord is built in Java, so the colour split is not affected by `§` codes.
- **"Acceso del menú" → "Abrir menú" (10 ES locales × 8 versions = 80 lang files).** English
  "Menu shortcut" → "Open Menu". Toggle remains "Acceso directo" / "Quick toggle".
- **In-game toggle overlay text uses the full mod name (all 8 versions):**
  `§aJi AFK está activo` → `§aJi AFK Cinematic: activado`
  `§cJi AFK está inactivo` → `§cJi AFK Cinematic: desactivado`
  EN: `§aJi AFK is active` → `§aJi AFK Cinematic: enabled`,
  `§cJi AFK is inactive` → `§cJi AFK Cinematic: disabled`.
- **`build-all.ps1` hard-fail on benign `Note:`:** the build script previously crashed the
  whole run when a `Note: ... uses unchecked or unsafe operations` was logged (a Java
  compiler informational, not an error). It now completes the copy step regardless and logs
  a yellow WARNING if no JAR is produced.
- **`build-all.ps1` target names** updated to `2.2.1` for all 8 groups.

### Removed

- **Lang key `report.confirm.*` (10 locales × 8 versions = 80 removed).** The custom
  `ConfirmScreen` is gone; the official Minecraft `ConfirmLinkScreen` provides the title,
  message, accept and cancel texts in the user's language.
- **`openReportConfirmation()` private method (8 versions).** The handler is now a single
  `ConfirmLinkScreen.opening(this, url)` / `ConfirmLinkScreen.confirmLink(this, url)` passed
  directly to `ButtonWidget.builder`.
- **Decorative green bar under the music volume label (8 versions).** Deleted the
  `context.fill(...)` 6-px green line and its background strip — replaced by the real slider.
- **Native F7 `KeyBinding` in A7-1.21.11** (the only version that registered it).
  See Fixed → "Dead F7 native keybinding removed from A7".
- **Three orphan lang keys** in all 80 lang files: `key.ji_afkcinematic.open_config`,
  `category.ji_afkcinematic.keys`, `key.category.ji_afkcinematic.keys`. Previously only
  referenced by the now-removed A7 native keybinding.