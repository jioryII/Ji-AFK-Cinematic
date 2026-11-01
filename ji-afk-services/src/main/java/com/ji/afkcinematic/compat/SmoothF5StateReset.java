package com.ji.afkcinematic.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

/** Reset only state owned by Smooth F5; foreign camera fields remain untouched. */
public final class SmoothF5StateReset {
    private static final String PACKAGE = "net.countered.smoothf5.";
    private static final ClassValue<Field[]> FIELDS = new ClassValue<>() {
        @Override protected Field[] computeValue(Class<?> type) {
            boolean ownedClass = type.getName().startsWith(PACKAGE);
            var result = new ArrayList<Field>();
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
                if (!ownedClass && !field.getName().startsWith("smooth_f5$")) continue;
                String name = field.getName();
                boolean state = name.endsWith("smoother") || name.equals("state") || name.endsWith("$state");
                boolean flag = field.getType() == boolean.class && (name.endsWith("wasDetached") || name.endsWith("wasMirrored")
                        || name.endsWith("isTransitioning") || name.endsWith("transDeltaReady") || name.endsWith("shouldSnapNextTail"));
                if (!state && !flag) continue;
                try { if (field.trySetAccessible()) result.add(field); }
                catch (RuntimeException inaccessible) { /* A changed optional bridge fails closed. */ }
            }
            return result.toArray(Field[]::new);
        }
    };
    private SmoothF5StateReset() { }
    public static void apply(Object camera, boolean detached, boolean mirrored) { apply(camera, detached, mirrored, 0); }
    private static void apply(Object target, boolean detached, boolean mirrored, int depth) {
        if (target == null || depth > 2) return;
        for (Field field : FIELDS.get(target.getClass())) {
            String name = field.getName();
            try {
                if (name.endsWith("wasDetached")) field.setBoolean(target, detached);
                else if (name.endsWith("wasMirrored")) field.setBoolean(target, mirrored);
                else if (field.getType() == boolean.class) field.setBoolean(target, false);
                else {
                    Object state = field.get(target);
                    if (state != null && state.getClass().getName().startsWith(PACKAGE)) apply(state, detached, mirrored, depth + 1);
                }
            } catch (ReflectiveOperationException | RuntimeException changed) { /* Never write another provider's state. */ }
        }
    }
}
