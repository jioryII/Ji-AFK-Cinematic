package com.ji.afkcinematic.diagnostic;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Direct transformed-bytecode evidence; class application alone is not injection evidence. */
public final class MixinInjectionEvidence {
    private static final String MERGED = "Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;";

    /** Names/descriptors are internal ABI evidence, never user or world data. */
    public record Invocation(String handler, String handlerDescriptor, String targetMethod, String targetDescriptor) {}
    public record Evidence(List<Invocation> invocations) {
        public Evidence { invocations = List.copyOf(invocations); }
        public boolean invokes(String originalHandler) {
            return invocations.stream().anyMatch(call -> call.handler().equals(originalHandler)
                    || call.handler().endsWith("$" + originalHandler));
        }
        public boolean hasInvocation() { return !invocations.isEmpty(); }
    }

    private MixinInjectionEvidence() {}

    public static Evidence inspect(ClassNode target, String mixinClassName) {
        if (target == null || target.name == null || mixinClassName == null) return new Evidence(List.of());
        String owner = mixinClassName.replace('.', '/');
        List<MethodNode> handlers = target.methods.stream()
                .filter(method -> owner.equals(mergedOwner(method))).toList();
        List<Invocation> calls = new ArrayList<>();
        for (MethodNode vanilla : target.methods) {
            // Calls made only by another merged handler do not demonstrate that
            // vanilla actually dispatches this injector.
            if (hasMergedAnnotation(vanilla.visibleAnnotations) || hasMergedAnnotation(vanilla.invisibleAnnotations)) continue;
            for (AbstractInsnNode instruction : vanilla.instructions) {
                if (!(instruction instanceof MethodInsnNode invoke) || !target.name.equals(invoke.owner)) continue;
                int opcode = invoke.getOpcode();
                if (opcode != Opcodes.INVOKEVIRTUAL && opcode != Opcodes.INVOKESTATIC
                        && opcode != Opcodes.INVOKESPECIAL && opcode != Opcodes.INVOKEINTERFACE) continue;
                for (MethodNode handler : handlers) {
                    if (invoke.name.equals(handler.name) && invoke.desc.equals(handler.desc)) {
                        calls.add(new Invocation(handler.name, handler.desc, vanilla.name, vanilla.desc));
                    }
                }
            }
        }
        return new Evidence(calls);
    }

    private static String mergedOwner(MethodNode method) {
        String value = mergedOwner(method.visibleAnnotations);
        return value != null ? value : mergedOwner(method.invisibleAnnotations);
    }

    private static String mergedOwner(List<AnnotationNode> annotations) {
        if (annotations == null) return null;
        for (AnnotationNode annotation : annotations) {
            if (!MERGED.equals(annotation.desc) || annotation.values == null) continue;
            for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                if ("mixin".equals(annotation.values.get(i)) && annotation.values.get(i + 1) instanceof String owner) {
                    return owner.replace('.', '/');
                }
            }
        }
        return null;
    }

    private static boolean hasMergedAnnotation(List<AnnotationNode> annotations) {
        return annotations != null && annotations.stream().anyMatch(annotation -> MERGED.equals(annotation.desc));
    }
}
