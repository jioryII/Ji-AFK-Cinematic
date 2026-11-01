package com.ji.afkcinematic.group;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

/** Resolves the loader version before any Minecraft-dependent variant is linked. */
public final class GroupSelection {
    private GroupSelection() {}

    public static Properties properties(String resource) {
        try (InputStream stream = GroupSelection.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Missing group manifest: " + resource);
            Properties result = new Properties();
            result.load(stream);
            return result;
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read group manifest: " + resource, error);
        }
    }

    public static String version() {
        try {
            Class<?> loader = Class.forName("net.neoforged.fml.loading.FMLLoader");
            Object info;
            try {
                info = loader.getMethod("versionInfo").invoke(null);
            } catch (NoSuchMethodException modernLoader) {
                Object current = loader.getMethod("getCurrent").invoke(null);
                info = loader.getMethod("getVersionInfo").invoke(current);
            }
            return (String) info.getClass().getMethod("mcVersion").invoke(info);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot select Ji AFK Cinematic for this Minecraft version", error);
        }
    }

    public static boolean isClient() {
        try {
            Class<?> environment = Class.forName("net.neoforged.fml.loading.FMLEnvironment");
            Object dist;
            try {
                dist = environment.getField("dist").get(null);
            } catch (NoSuchFieldException modernLoader) {
                dist = environment.getMethod("getDist").invoke(null);
            }
            return ((Enum<?>) dist).name().equals("CLIENT");
        } catch (ReflectiveOperationException error) {
            throw failure("Cannot resolve Minecraft distribution", error);
        }
    }

    public static RuntimeException failure(String message, ReflectiveOperationException error) {
        Throwable cause = error instanceof InvocationTargetException invocation ? invocation.getCause() : error;
        if (cause instanceof Error fatal) throw fatal;
        return new IllegalStateException(message, cause);
    }
}
