package com.ji.afkcinematic.files;
import java.nio.file.*;
import java.io.IOException;
import java.util.Comparator;
/** Publish an owned generated directory; preserve its previous generation for recovery. */
public final class DirectoryTransaction {
    private DirectoryTransaction() {}
    public static void publish(Path staging, Path target) throws IOException {
        Path backup = target.resolveSibling(target.getFileName() + ".previous");
        publish(staging, target, backup);
    }
    public static void publish(Path staging, Path target, Path backup) throws IOException {
        Files.createDirectories(target.getParent());
        Files.createDirectories(backup.getParent());
        delete(backup);
        boolean previous = Files.exists(target);
        if (previous) Files.move(target, backup);
        try { Files.move(staging, target); }
        catch (IOException failure) { if (previous) Files.move(backup, target); throw failure; }
    }
    public static void delete(Path directory) throws IOException {
        if (!Files.exists(directory)) return;
        try (var entries = Files.walk(directory)) {
            for (Path entry : entries.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(entry);
        }
    }
    public static void rollback(Path target) throws IOException {
        Path backup=target.resolveSibling(target.getFileName()+".previous");
        rollback(target, backup);
    }
    public static void rollback(Path target, Path backup) throws IOException {
        Path rejected=backup.resolveSibling(backup.getFileName()+".rejected");
        delete(rejected);
        if(Files.exists(target))Files.move(target,rejected);
        try { if(Files.exists(backup))Files.move(backup,target); }
        catch(IOException failure){if(Files.exists(rejected))Files.move(rejected,target);throw failure;}
        delete(rejected);
    }
}
