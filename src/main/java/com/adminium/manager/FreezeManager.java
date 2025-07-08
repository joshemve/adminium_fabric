package com.adminium.manager;

public class FreezeManager {
    private static boolean frozen = false;

    public static boolean isFrozen() {
        return frozen;
    }

    public static void setFrozen(boolean frozen) {
        FreezeManager.frozen = frozen;
    }
} 