package com.ji.afkcinematic;

import net.minecraft.client.util.InputUtil;
import java.lang.reflect.Method;

public class TestKeyBind {
    public static void test() {
        for (Method m : InputUtil.class.getDeclaredMethods()) {
            System.out.println(m.getName() + " -> " + m.getParameterTypes().length);
        }
    }
}