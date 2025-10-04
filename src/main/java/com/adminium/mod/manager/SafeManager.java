package com.adminium.mod.manager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SafeManager {
    private static final Set<UUID> safePlayers = new HashSet<>();

    public static boolean isInSafeMode(UUID playerUUID) {
        return safePlayers.contains(playerUUID);
    }

    public static void setSafeMode(UUID playerUUID, boolean safe) {
        if (safe) {
            safePlayers.add(playerUUID);
        } else {
            safePlayers.remove(playerUUID);
        }
    }

    public static void toggleSafeMode(UUID playerUUID) {
        if (safePlayers.contains(playerUUID)) {
            safePlayers.remove(playerUUID);
        } else {
            safePlayers.add(playerUUID);
        }
    }
} 