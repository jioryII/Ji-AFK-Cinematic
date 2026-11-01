import java.nio.file.Files;
import java.nio.file.Path;
import org.objectweb.asm.*;

/** Assembly tool: keep a single FML entrypoint while preserving variant constructors. */
public final class StripModAnnotation {
    public static void main(String[] args) throws Exception {
        for (String arg : args) {
            Path path = Path.of(arg);
            ClassReader reader = new ClassReader(Files.readAllBytes(path));
            ClassWriter writer = new ClassWriter(0);
            reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                @Override public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    if (descriptor.equals("Lnet/neoforged/fml/common/Mod;")) return null;
                    return super.visitAnnotation(descriptor, visible);
                }
            }, 0);
            Files.write(path, writer.toByteArray());
        }
    }
}
