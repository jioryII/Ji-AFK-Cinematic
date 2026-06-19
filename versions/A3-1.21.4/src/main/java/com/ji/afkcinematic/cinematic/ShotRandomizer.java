package com.ji.afkcinematic.cinematic;

import java.util.Random;

public class ShotRandomizer {
    private static final Random RANDOM = new Random();
    private static float lastStartAngle = -1;

    public static float getRandomStartAngle() {
        float angle;
        do {
            angle = RANDOM.nextFloat() * 360f;
        } while (lastStartAngle != -1 && Math.abs(angle - lastStartAngle) < 45f);
        
        lastStartAngle = angle;
        return angle;
    }

    public static boolean getRandomDirection() {
        return RANDOM.nextBoolean();
    }
    
    public static void reset() {
        lastStartAngle = -1;
    }
}
