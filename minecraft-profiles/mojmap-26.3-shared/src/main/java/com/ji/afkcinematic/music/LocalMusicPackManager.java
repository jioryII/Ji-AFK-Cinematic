package com.ji.afkcinematic.music;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ji.afkcinematic.JiAFKCinematic;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.PlatformAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public final class LocalMusicPackManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PACK_ID = "file/ji-afk-cinematic-local";
    private static final String NAMESPACE = "ji_afk_cinematic_local";
    private static final String FINGERPRINT_FILE = ".ji-afk-source.sha256";
    private static int startupTrackCount;
    private static final com.ji.afkcinematic.files.ManagedJobs MUSIC_JOBS = new com.ji.afkcinematic.files.ManagedJobs("Ji-AFK-Music");
    private static final com.ji.afkcinematic.files.ManagedJobs PICKER_JOBS = new com.ji.afkcinematic.files.ManagedJobs("Ji-AFK-Folder");
    private static long previewJob;
    private static volatile boolean sourceChanges = true;
    private static boolean restoreFullscreen;
    private static volatile Process folderPickerProcess;
    public static void cancelScreenJobs() {
        if (previewJob != 0) { MUSIC_JOBS.cancel(); previewJob = 0; }
        PICKER_JOBS.cancel();
        Process picker = folderPickerProcess; if (picker != null) picker.destroyForcibly();
        restoreFullscreen();
    }
    public static void shutdown() { cancelScreenJobs(); queuedRebuild = null; MUSIC_JOBS.close(); PICKER_JOBS.close(); EXPORT_JOBS.close(); }
    private static void restoreFullscreen() {
        Minecraft client = Minecraft.getInstance();
        if (restoreFullscreen) {
            restoreFullscreen = false;
            if (client.options.fullscreen().get()) client.getWindow().setFullscreen(true);
        }
    }
    private LocalMusicPackManager() {}

    public static Path getMusicFolder() {
        return resolveMusicFolder(ConfigManager.getConfig().customMusicDirectory);
    }

    private static Path resolveMusicFolder(String configuredPath) {
        if (configuredPath != null && !configuredPath.isBlank()) {
            try { Path folder = Path.of(configuredPath); return (folder.isAbsolute() ? folder : PlatformAccess.gameDir().resolve(folder)).toAbsolutePath().normalize(); }
            catch (Exception e) { JiAFKCinematic.LOGGER.warn("Invalid custom music directory: {}", configuredPath); }
        }
        return PlatformAccess.configDir().resolve("ji-afk-cinematic").resolve("music");
    }
    public static void initialize() {
        ConfigManager.whenLoaded().thenRun(() -> {
            startupRequested = true;
            if (clientStarted) rebuildAndReloadAsync(count -> { });
        });
    }
    public static void completeStartupLoad(Minecraft client) {
        clientStarted = true;
        if (startupRequested) rebuildAndReloadAsync(count -> { });
    }
    public static void openMusicFolder() { openMusicFolder(ConfigManager.getConfig().customMusicDirectory); }
    public static void openMusicFolder(String configuredPath) {
        PICKER_JOBS.submit(() -> {
        try {
            Path folder = Files.createDirectories(resolveMusicFolder(configuredPath));
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            ProcessBuilder opener = os.contains("win")
                    ? new ProcessBuilder("explorer.exe", folder.toString())
                    : os.contains("mac") ? new ProcessBuilder("open", folder.toString())
                    : new ProcessBuilder("xdg-open", folder.toString());
            opener.start();
        } catch (Exception e) { JiAFKCinematic.LOGGER.warn("Could not open local music folder", e); }
        return null; });
    }

    public static String chooseMusicFolder(String configuredPath) { return chooseMusicFolder(configuredPath, 0); }
    private static String chooseMusicFolder(String configuredPath, long owner) {
        try {
            if (owner != 0 && !PICKER_JOBS.current(owner)) return null;
            Path initial = Files.createDirectories(resolveMusicFolder(configuredPath));
            if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
                String selected = chooseWindowsMusicFolder(initial, owner);
                if (selected != null) return selected.isBlank() ? null : selected;
            }
            // Minecraft ships tinyfd at runtime, but not every supported toolchain exposes it
            // on the mod compile classpath. It remains the portable fallback.
            Class<?> dialogs = Class.forName("org.lwjgl.util.tinyfd.TinyFileDialogs");
            Method selectFolder = dialogs.getMethod(
                    "tinyfd_selectFolderDialog", CharSequence.class, CharSequence.class);
            String selected = (String) selectFolder.invoke(
                    null, "Choose cinematic music folder", initial.toString());
            if (selected == null || selected.isBlank()) return null;
            return Files.createDirectories(Path.of(selected)).toAbsolutePath().normalize().toString();
        } catch (Exception e) {
            JiAFKCinematic.LOGGER.warn("Could not choose a custom music folder", e);
            return null;
        }
    }

    /** Opens the native picker without blocking Minecraft's render thread. */
    public static void chooseMusicFolderAsync(String path, Consumer<String> callback) {
        chooseMusicFolderAsync(path, () -> true, callback);
    }
    public static void chooseMusicFolderAsync(String path, java.util.function.BooleanSupplier valid, Consumer<String> callback) {
        Minecraft client = Minecraft.getInstance();
        long id = PICKER_JOBS.replace();
        restoreFullscreen = client.options.fullscreen().get();
        if (restoreFullscreen) client.getWindow().setFullscreen(false);
        PICKER_JOBS.submit(() -> chooseMusicFolder(path, id)).whenComplete((selected, error) -> client.execute(() -> {
            if (!PICKER_JOBS.current(id)) return;
            try { if (valid.getAsBoolean()) callback.accept(error == null ? selected : null); }
            finally { restoreFullscreen(); }
        }));
    }

    private static String chooseWindowsMusicFolder(Path initial, long owner) throws Exception {
        Path script = Files.createTempFile("ji-afk-folder-picker-", ".ps1");
        try (var source = LocalMusicPackManager.class
                .getResourceAsStream("/ji-afk-modern-folder-picker.ps1")) {
            if (source == null) return null;
            Files.copy(source, script, StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            ProcessBuilder picker = new ProcessBuilder("powershell.exe", "-NoProfile", "-STA",
                    "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-File", script.toString());
            picker.environment().put("JI_AFK_INITIAL_MUSIC_FOLDER", initial.toString());
            picker.environment().put("JI_AFK_FOLDER_PICKER_TITLE",
                    Component.translatable("config.ji_afkcinematic.change_music_folder").getString());
            picker.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process process = picker.start(); folderPickerProcess = process;
            try {
                long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(90);
                while (!process.waitFor(250, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    if (System.nanoTime() >= deadline || owner != 0 && !PICKER_JOBS.current(owner)) return "";
                }
                if (owner != 0 && !PICKER_JOBS.current(owner)) return "";
                String selected = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                if (process.exitValue() != 0 || selected.isBlank()) return "";
                return Files.createDirectories(Path.of(selected)).toAbsolutePath().normalize().toString();
            } finally {
                if (process.isAlive()) process.destroyForcibly();
                if (folderPickerProcess == process) folderPickerProcess = null;
            }
        } finally {
            Files.deleteIfExists(script);
        }
    }
    private record Prepared(Path staging, int count, int skipped) { }
    private static boolean clientStarted;
    private static boolean startupRequested;
    private static boolean rebuilding;
    private static Runnable queuedRebuild;
    private static int lastSkippedCount;
    private static final com.ji.afkcinematic.files.ManagedJobs EXPORT_JOBS = new com.ji.afkcinematic.files.ManagedJobs("Ji-AFK-Music-Export");

    public static int getLastSkippedCount() { return lastSkippedCount; }
    public static Path getMusicFolder(String path) { return resolveMusicFolder(path); }
    @Deprecated public static int rebuildAndReload() { rebuildAndReloadAsync(count -> { }); return startupTrackCount; }
    public static void rebuildAndReloadAsync(Consumer<Integer> callback) { requestRebuild(() -> true, callback, false); }
    public static void rebuildAndReloadAsync(java.util.function.BooleanSupplier valid, Consumer<Integer> callback) {
        requestRebuild(valid, callback, true);
    }
    private static void requestRebuild(java.util.function.BooleanSupplier valid, Consumer<Integer> callback, boolean preview) {
        if (rebuilding) { queuedRebuild = () -> requestRebuild(valid, callback, preview); return; }
        if (!valid.getAsBoolean()) return;
        rebuilding = true;
        Minecraft client = Minecraft.getInstance();
        Path folder = getMusicFolder();
        boolean wasSelected = client.options.resourcePacks.contains(PACK_ID);
        // Native version metadata is captured on the client, before any file work starts.
        String metadata = GSON.toJson(ClientResourcePackMetadata.create());
        long id = MUSIC_JOBS.replace();
        previewJob = preview ? id : 0;
        MUSIC_JOBS.submit(() -> prepare(folder, metadata)).whenComplete((prepared, failure) -> client.execute(() -> {
            if (failure != null || !MUSIC_JOBS.current(id) || !valid.getAsBoolean()) {
                if (failure != null) JiAFKCinematic.LOGGER.warn("Music preparation failed; previous pack retained", failure);
                if (prepared != null) MUSIC_JOBS.submit(() -> { cleanup(prepared.staging()); return null; });
                finishRebuild(id, valid, callback, -1);
                return;
            }
            lastSkippedCount = prepared.skipped();
            if (prepared.staging() == null) {
                activateGeneratedPack(client, false, prepared.count(), success -> finishRebuild(id, valid, callback, success ? prepared.count() : -1));
                return;
            }
            MUSIC_JOBS.submit(() -> {
                try {
                    if (!MUSIC_JOBS.current(id)) { cleanup(prepared.staging()); return false; }
                    com.ji.afkcinematic.files.DirectoryTransaction.publish(prepared.staging(), getGeneratedPackFolder(), backupFolder());
                    return true;
                } catch (Exception error) {
                    JiAFKCinematic.LOGGER.warn("Music publication failed; previous pack retained", error);
                    cleanup(prepared.staging()); return false;
                }
            }).whenComplete((published, error) -> client.execute(() -> {
                if (error != null || !Boolean.TRUE.equals(published)) { finishRebuild(id, valid, callback, -1); return; }
                if (!MUSIC_JOBS.current(id) || !valid.getAsBoolean()) { rollback(client, wasSelected, () -> finishRebuild(id, valid, callback, -1)); return; }
                activateGeneratedPack(client, true, prepared.count(), success -> {
                    if (!success || !MUSIC_JOBS.current(id) || !valid.getAsBoolean()) {
                        rollback(client, wasSelected, () -> finishRebuild(id, valid, callback, -1));
                    } else finishRebuild(id, valid, callback, prepared.count());
                });
            }));
        }));
    }

    private static void finishRebuild(long id, java.util.function.BooleanSupplier valid, Consumer<Integer> callback, int count) {
        boolean current = MUSIC_JOBS.current(id) && valid.getAsBoolean();
        sourceChanges = count < 0;
        if (count >= 0) startupTrackCount = count;
        previewJob = 0; rebuilding = false;
        Runnable next = queuedRebuild; queuedRebuild = null;
        try { if (current) callback.accept(count); }
        finally { if (next != null) next.run(); }
    }

    private static void rollback(Minecraft client, boolean wasSelected, Runnable complete) {
        MUSIC_JOBS.submit(() -> {
            try { com.ji.afkcinematic.files.DirectoryTransaction.rollback(getGeneratedPackFolder(), backupFolder()); return true; }
            catch (Exception error) { JiAFKCinematic.LOGGER.warn("Music pack rollback failed", error); return false; }
        }).whenComplete((restored, error) -> client.execute(() -> {
            if (Boolean.TRUE.equals(restored)) {
                if (wasSelected && !client.options.resourcePacks.contains(PACK_ID)) client.options.resourcePacks.add(PACK_ID);
                if (!wasSelected) client.options.resourcePacks.remove(PACK_ID);
                client.getResourcePackRepository().reload();
                client.getResourcePackRepository().setSelected(client.options.resourcePacks);
                client.reloadResourcePacks().whenComplete((ignored, failure) -> client.execute(complete));
            } else complete.run();
        }));
    }

    private static void cleanup(Path folder) {
        if (folder == null) return;
        try { com.ji.afkcinematic.files.DirectoryTransaction.delete(folder); }
        catch (Exception error) { JiAFKCinematic.LOGGER.warn("Could not remove staged music pack", error); }
    }
    public static boolean hasSourceChanges() { return sourceChanges; }
    public static void inspectSourceChangesAsync(Consumer<Boolean> callback) {
        Path folder = getMusicFolder();
        String metadata = GSON.toJson(ClientResourcePackMetadata.create());
        MUSIC_JOBS.submit(() -> {
            try {
                validateSourceFolder(folder);
            var scan = LocalMusicLibrary.scan(folder, metadata);
                Path marker = getGeneratedPackFolder().resolve(FINGERPRINT_FILE);
                return !Files.isRegularFile(marker) || !Files.readString(marker).equals(scan.fingerprint());
            } catch (Exception error) { return true; }
        }).whenComplete((changed, error) -> Minecraft.getInstance().execute(() -> {
            sourceChanges = error != null || Boolean.TRUE.equals(changed); callback.accept(sourceChanges);
        }));
    }

    private static Prepared prepare(Path folder, String metadata) {
        Path staging = null;
        try {
            Files.createDirectories(cacheFolder());
            preserveLegacySidecars();
            validateSourceFolder(folder);
            var scan = LocalMusicLibrary.scan(folder, metadata);
            Path generated = getGeneratedPackFolder();
            boolean reusable = Files.isRegularFile(generated.resolve(FINGERPRINT_FILE))
                    && Files.readString(generated.resolve(FINGERPRINT_FILE)).equals(scan.fingerprint())
                    && Files.isRegularFile(generated.resolve("pack.mcmeta"))
                    && Files.readString(generated.resolve("pack.mcmeta")).equals(metadata)
                    && Files.isRegularFile(generated.resolve("assets/" + NAMESPACE + "/sounds.json"))
                    && Files.isRegularFile(generated.resolve("assets/" + NAMESPACE + "/ji_afk_cinematic/music.json"));
            for (var track : scan.tracks()) {
                Path copy = generated.resolve("assets/" + NAMESPACE + "/sounds/music/" + track.id() + ".ogg");
                reusable &= Files.isRegularFile(copy) && Files.size(copy) == Files.size(track.source());
            }
            if (reusable) return new Prepared(null, scan.tracks().size(), scan.skipped());
            staging = Files.createTempDirectory(cacheFolder(), "staging-");
            writePack(staging, scan, metadata, NAMESPACE);
            Files.writeString(staging.resolve(FINGERPRINT_FILE), scan.fingerprint(), StandardCharsets.UTF_8);
            JiAFKCinematic.LOGGER.info("Prepared {} local music tracks ({} unsupported audio files skipped)", scan.tracks().size(), scan.skipped());
            return new Prepared(staging, scan.tracks().size(), scan.skipped());
        } catch (Exception error) { cleanup(staging); throw new java.util.concurrent.CompletionException(error); }
    }

    private static void writePack(Path pack, LocalMusicLibrary.Scan scan, String metadata, String namespace) throws Exception {
        LocalMusicLibrary.copyTracks(scan, pack.resolve("assets/" + namespace + "/sounds/music"));
        Map<String, Object> definitions = new LinkedHashMap<>();
        List<String> tracks = new ArrayList<>();
        for (var track : scan.tracks()) {
            definitions.put("local." + track.id(), Map.of("sounds", List.of(Map.of("name", namespace + ":music/" + track.id(), "stream", true))));
            tracks.add(namespace + ":local." + track.id());
        }
        Files.writeString(pack.resolve("pack.mcmeta"), metadata, StandardCharsets.UTF_8);
        Files.writeString(pack.resolve("assets/" + namespace + "/sounds.json"), GSON.toJson(definitions), StandardCharsets.UTF_8);
        Path manifest = pack.resolve("assets/" + namespace + "/ji_afk_cinematic/music.json");
        Files.createDirectories(manifest.getParent());
        Files.writeString(manifest, GSON.toJson(Map.of("replace", false, "tracks", tracks)), StandardCharsets.UTF_8);
        Files.writeString(pack.resolve("README.txt"), "Ji AFK Cinematic music pack / Paquete musical\n"
                + "Requires Minecraft's resource format shown in pack.mcmeta. Other versions: export again from that client.\n"
                + "Activate the ZIP in Resource Packs and choose Custom or Mixed music in Ji AFK Cinematic.\n"
                + "This pack is independent of the original music folder. Only share music you may redistribute.\n"
                + "For another song: add Ogg Vorbis under assets/" + namespace + "/sounds/music/, declare its streaming event in sounds.json,\n"
                + "and add namespace:event to assets/" + namespace + "/ji_afk_cinematic/music.json. Do not include .ogg in sound event paths.\n"
                + "Alternatively put songs in Ji's chosen music folder and export again: JSON is generated for you.\n"
                + "Activa el ZIP en Paquetes de recursos y elige Personalizada o Mixta. Para agregar canciones sin editar JSON,\n"
                + "pon archivos Ogg Vorbis en la carpeta elegida del mod (admite subcarpetas) y vuelve a exportar.\n", StandardCharsets.UTF_8);
    }

    /** Explicit export: a standalone pack, with its own namespace, outside the disposable cache. */
    private static volatile java.util.concurrent.CompletableFuture<Path> exportInFlight;
    private static Path exportInFlightFolder;
    public static boolean isExportingMusicPack() { return exportInFlight!=null; }

    /** Share work across screens; unchanged content reuses its existing standalone ZIP. */
    public static synchronized void exportMusicPackAsync(String directory, java.util.function.BooleanSupplier valid, Consumer<Path> callback) {
        Path folder=resolveMusicFolder(directory);
        if(exportInFlight!=null) {
            if(folder.equals(exportInFlightFolder))deliverExport(exportInFlight,valid,callback);
            else Minecraft.getInstance().execute(()->{if(valid.getAsBoolean())callback.accept(null);});
            return;
        }
        String metadata=GSON.toJson(ClientResourcePackMetadata.create());
        Path exports=PlatformAccess.gameDir().resolve("ji-afk-cinematic-packs");
        var future=EXPORT_JOBS.submit(()->{
            Path staging=null,partial=null;
            try {
                validateSourceFolder(folder);
                var scan=LocalMusicLibrary.scan(folder,metadata);
                String unique=scan.fingerprint().substring(0,32),namespace="ji_afk_music_"+unique;
                Files.createDirectories(exports);
                Path exported=exports.resolve("ji-afk-music-"+unique+".zip");
                if(Files.exists(exported,java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                    if(reusableExport(exported,scan.fingerprint(),namespace))return exported;
                    throw new java.io.IOException("Existing export was changed or damaged; preserved without overwriting");
                }
                staging=Files.createTempDirectory(exports,".staging-");
                writePack(staging,scan,metadata,namespace);
                Files.writeString(staging.resolve(FINGERPRINT_FILE),scan.fingerprint(),StandardCharsets.UTF_8);
                if(scan.tracks().isEmpty()) {
                    Files.writeString(staging.resolve("assets/"+namespace+"/sounds.json.example"),
                            GSON.toJson(Map.of("my_song",Map.of("sounds",List.of(Map.of("name",namespace+":music/my_song","stream",true))))));
                    Files.writeString(staging.resolve("assets/"+namespace+"/ji_afk_cinematic/music.json.example"),
                            GSON.toJson(Map.of("replace",false,"tracks",List.of(namespace+":my_song"))));
                }
                partial=exports.resolve(".ji-afk-music-"+java.util.UUID.randomUUID()+".zip.part");
                try(var zip=new java.util.zip.ZipOutputStream(Files.newOutputStream(partial,java.nio.file.StandardOpenOption.CREATE_NEW));var files=Files.walk(staging)) {
                    for(Path file:files.sorted().toList()) {
                        if(file.equals(staging))continue;
                        boolean directoryEntry=Files.isDirectory(file);
                        String entry=staging.relativize(file).toString().replace('\\','/')+(directoryEntry?"/":"");
                        zip.putNextEntry(new java.util.zip.ZipEntry(entry));
                        if(!directoryEntry)Files.copy(file,zip);
                        zip.closeEntry();
                    }
                }
                try { Files.move(partial,exported); }
                catch(java.nio.file.FileAlreadyExistsException otherInstance) {
                    if(!reusableExport(exported,scan.fingerprint(),namespace))throw otherInstance;
                }
                return exported;
            } catch(Exception error) {JiAFKCinematic.LOGGER.warn("Could not export music pack",error);return (Path)null;}
            finally {
                cleanup(staging);
                if(partial!=null)try{Files.deleteIfExists(partial);}catch(Exception ignored){}
            }
        });
        exportInFlightFolder=folder;exportInFlight=future;
        future.whenComplete((result,error)->{
            synchronized(LocalMusicPackManager.class) {
                if(exportInFlight==future){exportInFlight=null;exportInFlightFolder=null;}
            }
        });
        deliverExport(future,valid,callback);
    }
    private static boolean reusableExport(Path exported,String fingerprint,String namespace) {
        if(!Files.isRegularFile(exported,java.nio.file.LinkOption.NOFOLLOW_LINKS))return false;
        try(var zip=new java.util.zip.ZipFile(exported.toFile())) {
            var marker=zip.getEntry(FINGERPRINT_FILE);
            if(marker==null||zip.getEntry("pack.mcmeta")==null||zip.getEntry("assets/"+namespace+"/sounds.json")==null
                    ||zip.getEntry("assets/"+namespace+"/ji_afk_cinematic/music.json")==null)return false;
            try(var input=zip.getInputStream(marker)) {
                byte[] bytes=input.readNBytes(129);
                return bytes.length<=128&&new String(bytes,StandardCharsets.UTF_8).strip().equals(fingerprint);
            }
        } catch(java.io.IOException changed) {return false;}
    }
    private static void deliverExport(java.util.concurrent.CompletableFuture<Path> future,java.util.function.BooleanSupplier valid,Consumer<Path> callback) {
        future.whenComplete((result,error)->Minecraft.getInstance().execute(()->{
            if(valid.getAsBoolean())callback.accept(error==null?result:null);
        }));
    }

    public static void openMusicPackExports() {
        EXPORT_JOBS.submit(() -> { openFolderPath(PlatformAccess.gameDir().resolve("ji-afk-cinematic-packs")); return null; });
    }
    private static void validateSourceFolder(Path source) throws java.io.IOException {
        LocalMusicLibrary.requireIndependentSource(source,
                List.of(getGeneratedPackFolder(), cacheFolder(), PlatformAccess.gameDir().resolve("ji-afk-cinematic-packs")));
    }

    private static void openFolderPath(Path path) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            (os.contains("win") ? new ProcessBuilder("explorer.exe", path.toString())
                    : os.contains("mac") ? new ProcessBuilder("open", path.toString())
                    : new ProcessBuilder("xdg-open", path.toString())).start();
        } catch (Exception error) { JiAFKCinematic.LOGGER.warn("Could not open music folder", error); }
    }

    private static void activateGeneratedPack(Minecraft client, boolean changed, int count, Consumer<Boolean> completion) {
        boolean selected = client.options.resourcePacks.contains(PACK_ID);
        if (count > 0 && !selected) client.options.resourcePacks.add(PACK_ID);
        if (count == 0) client.options.resourcePacks.remove(PACK_ID);
        client.options.incompatibleResourcePacks.remove(PACK_ID);
        if (!changed && selected == (count > 0) || count == 0 && !selected) { completion.accept(true); return; }
        client.getResourcePackRepository().reload();
        client.getResourcePackRepository().setSelected(client.options.resourcePacks);
        client.reloadResourcePacks().whenComplete((unused, error) -> client.execute(() -> {
            if (error != null) JiAFKCinematic.LOGGER.warn("Local music resource reload failed", error);
            else CinematicMusicManager.onResourcesReloaded();
            completion.accept(error == null);
        }));
    }
    private static Path cacheFolder() { return PlatformAccess.configDir().resolve("ji-afk-cinematic/cache/local-music"); }
    private static Path backupFolder() { return cacheFolder().resolve("previous"); }
    private static Path getGeneratedPackFolder() { return PlatformAccess.gameDir().resolve("resourcepacks/ji-afk-cinematic-local"); }
    private static void preserveLegacySidecars() throws Exception {
        Path packs = getGeneratedPackFolder().getParent();
        if (!Files.isDirectory(packs)) return;
        try (var folders = Files.list(packs)) {
            for (Path folder : folders.filter(Files::isDirectory).toList()) {
                String name = folder.getFileName().toString();
                if ((name.equals("ji-afk-cinematic-local.previous") || name.equals("ji-afk-cinematic-local.rejected")
                        || name.startsWith("ji-afk-cinematic-staging-")) && Files.isRegularFile(folder.resolve(FINGERPRINT_FILE)))
                    Files.move(folder, cacheFolder().resolve("legacy-" + java.util.UUID.randomUUID()));
            }
        }
    }
}
