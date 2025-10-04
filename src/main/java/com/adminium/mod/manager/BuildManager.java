package com.adminium.mod.manager;

public class BuildManager {
    // True when building and PvP actions are permitted for regular players
    private static boolean buildEnabled = true;

    /**
     * Returns whether build actions (block break/place & PvP) are currently allowed.
     */
    public static boolean isBuildEnabled() {
        return buildEnabled;
    }

    /**
     * Sets whether build actions are allowed. "on" = true, "off" = false.
     */
    public static void setBuildEnabled(boolean enabled) {
        buildEnabled = enabled;
    }
} 