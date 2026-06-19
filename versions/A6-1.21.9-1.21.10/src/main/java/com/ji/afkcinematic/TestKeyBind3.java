package com.ji.afkcinematic;

import net.minecraft.client.option.KeyBinding;
import java.lang.reflect.Constructor;

public class TestKeyBind3 {
    public static void test() {
        for (Constructor<?> c : KeyBinding.class.getConstructors()) {
            System.out.print("Constructor(");
            for (Class<?> p : c.getParameterTypes()) {
                System.out.print(p.getName() + ", ");
            }
            System.out.println(")");
        }
    }
}