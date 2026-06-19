public class Runner {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("com.ji.afkcinematic.TestWindow");
        clazz.getMethod("test").invoke(null);
    }
}