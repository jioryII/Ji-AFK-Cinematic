package com.ji.afkcinematic.input;

import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.PersistentCinematicMode;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** Clears processed movement after every input provider (keyboard or controller) updates it. */
public final class PersistentMovementLock {
    private PersistentMovementLock() {}

    public static boolean isLocked() {
        return CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE
                && ConfigManager.getConfig().persistentMode == PersistentCinematicMode.PERSISTENT;
    }

    public static void clear(Object input) {
        if (!isLocked() || input == null) return;
        for (Class<?> type = input.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                try {
                    field.setAccessible(true);
                    Class<?> fieldType = field.getType();
                    if (fieldType == float.class) field.setFloat(input, 0.0F);
                    else if (fieldType == boolean.class) field.setBoolean(input, false);
                    else if (fieldType.isRecord()) {
                        Object empty = emptyBooleanRecord(fieldType);
                        if (empty != null) field.set(input, empty);
                    } else {
                        Object zeroVector = zeroFloatPair(fieldType);
                        if (zeroVector != null) field.set(input, zeroVector);
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Unknown input implementations remain compatible; known vanilla fields still clear.
                }
            }
        }
    }

    private static Object emptyBooleanRecord(Class<?> type) throws ReflectiveOperationException {
        var components = type.getRecordComponents();
        Class<?>[] parameterTypes = new Class<?>[components.length];
        Object[] values = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            parameterTypes[i] = components[i].getType();
            if (parameterTypes[i] != boolean.class) return null;
            values[i] = false;
        }
        Constructor<?> constructor = type.getDeclaredConstructor(parameterTypes);
        constructor.setAccessible(true);
        return constructor.newInstance(values);
    }

    private static Object zeroFloatPair(Class<?> type) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor(float.class, float.class);
            constructor.setAccessible(true);
            return constructor.newInstance(0.0F, 0.0F);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }
}
