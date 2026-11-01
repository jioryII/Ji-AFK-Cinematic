package com.ji.afkcinematic.diagnostic;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.concurrent.*;

/** One tiny marker per visited world. Neither paths nor server addresses are stored. */
public final class WorldHintRepository {
    private final Path directory;
    private final Executor executor;
    public record Entry(String fingerprint,boolean seen) {}
    public WorldHintRepository(Path config) { this(config,ForkJoinPool.commonPool()); }
    WorldHintRepository(Path config,Executor executor) { this.directory=config.resolve("ji-afk-cinematic/seen-worlds");this.executor=executor; }
    public CompletableFuture<Entry> lookup(Path localWorld,String remoteIdentity) {
        return CompletableFuture.supplyAsync(()->{
            try {
                String identity=remoteIdentity;
                if(localWorld!=null) {
                    var path=localWorld.toAbsolutePath().normalize();var attrs=Files.readAttributes(path,BasicFileAttributes.class);
                    identity="local:"+path+":"+attrs.creationTime()+":"+attrs.fileKey();
                }
                if(identity==null)throw new java.io.IOException("WORLD_IDENTITY_UNAVAILABLE");
                String key=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8)));
                return new Entry(key,Files.isRegularFile(directory.resolve(key+".seen"),LinkOption.NOFOLLOW_LINKS));
            } catch(java.io.IOException|java.security.NoSuchAlgorithmException error) { throw new CompletionException(error); }
        },executor);
    }
    public CompletableFuture<Void> mark(Entry entry) {
        if(!entry.fingerprint.matches("[a-f0-9]{64}"))return CompletableFuture.failedFuture(new IllegalArgumentException("INVALID_WORLD_FINGERPRINT"));
        return CompletableFuture.runAsync(()->{
            try { Files.createDirectories(directory);Files.writeString(directory.resolve(entry.fingerprint+".seen"),"seen\n",StandardOpenOption.CREATE_NEW); }
            catch(FileAlreadyExistsException alreadySeen) { /* A second game instance may have shown it. */ }
            catch(java.io.IOException error) { throw new CompletionException(error); }
        },executor);
    }
}
