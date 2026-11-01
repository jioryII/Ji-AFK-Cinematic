![Ji AFK Cinematic](https://raw.githubusercontent.com/jioryII/Ji-Assets/main/ji-afk/portada-ji-afk.png)

# Ji AFK Cinematic 2.4.0

A client-side cinematic camera for Minecraft: AFK shots, fishing scenes, sleep letterboxing and configurable music.

Fabric **2.4.0** and NeoForge **2.4.0-beta.1.1** support these Minecraft groups:

| Group | Minecraft |
|---|---|
| A1 | 1.21â€“1.21.1 |
| A2 | 1.21.2â€“1.21.5 |
| A3 | 1.21.6â€“1.21.8 |
| A4 | 1.21.9â€“1.21.11 |
| B1 | 26.1â€“26.2 |
| B2 | 26.3 |

Choose one JAR from [compilaciones](compilaciones/) for your Minecraft version and loader. Fabric requires Fabric API; Mod Menu is optional. NeoForge uses its native loader. Install Java 21 for 1.21.x and Java 25 for 26.x. Nothing is required on the server.

- **F7 + H:** settings.
- **F7 + U:** enable or disable the mod.
- **F7 + I:** start or stop a cinematic.

Shortcuts can be changed. The initial shot mix is 50% character and 50% environment; existing saved choices are preserved. Music supports vanilla tracks, your local Ogg Vorbis library, or both. See [local music](LOCAL-MUSIC.md) and [resource-pack music](RESOURCE-PACK-MUSIC.md).

Optical focus is temporarily disabled and its setting is hidden. Its source and shaders remain available for future work.

## Build

Install JDK 21 and JDK 25, set `JAVA_HOME` to JDK 25, and install Python 3.11 or newer. Gradle discovers the JDK 21 toolchain from a standard installation, or use `--java21-home /path/to/jdk-21` if it is not detected.

```sh
python tools/build-release.py
```

This compiles the 34 native adapters and assembles twelve grouped JARs in `compilaciones/`. It does not start Minecraft. Use `--offline` after dependencies and assets have been cached, and `--output NAME` to preserve previous grouped builds. Each `versions/` project can also be built separately with its wrapper and `-Pminecraft_version=VERSION`.

Fabric groups contain their version-specific implementations as nested dependencies. NeoForge groups isolate differing classes, mixins and shader resources and select the implementation matching the loader's Minecraft version.

[Website](https://ji-afk-cinematic.ji-mods.com) Â· [Support](https://ji-afk-cinematic.ji-mods.com/en/support/) Â· [Discord](https://discord.gg/sE27D5SNaq) Â· [Modrinth](https://modrinth.com/mod/ji-afk-cinematic)

Licensed under the [Ji Mods Source License](LICENSE).
