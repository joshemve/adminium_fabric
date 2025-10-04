package com.adminium.mod.manager;

public class PortalManager {
    private static boolean netherEnabled = true;
    private static boolean endEnabled = true;
    
    public static boolean isNetherEnabled() {
        return netherEnabled;
    }
    
    public static void setNetherEnabled(boolean enabled) {
        netherEnabled = enabled;
    }
    
    public static boolean isEndEnabled() {
        return endEnabled;
    }
    
    public static void setEndEnabled(boolean enabled) {
        endEnabled = enabled;
    }
} 