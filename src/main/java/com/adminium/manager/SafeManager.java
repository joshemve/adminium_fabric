package com.adminium.manager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SafeManager {
    private static final Set<UUID> safePlayers = new HashSet<>();

    public static void setSafe(UUID playerId, boolean isSafe) {
        if (isSafe) {
            safePlayers.add(playerId);
        } else {
            safePlayers.remove(playerId);
        }
    }

    public static boolean isSafe(UUID playerId) {
        return safePlayers.contains(playerId);
    }
} 