import net.minecraft.client.render.RenderTickCounter;
import java.lang.reflect.Method;
public class Test {
    public static void main(String[] args) {
        for (Method m : RenderTickCounter.class.getMethods()) {
            System.out.println(m.getName());
            for (Class<?> p : m.getParameterTypes()) {
                System.out.println("  " + p.getName());
            }
        }
    }
}
