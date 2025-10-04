package com.adminium.mod.manager;

public class PvpManager {
    private static boolean pvpEnabled = true;

    public static boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public static void setPvpEnabled(boolean enabled) {
        pvpEnabled = enabled;
    }
} 