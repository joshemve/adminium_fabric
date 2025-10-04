package com.adminium.mod.manager;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FreezeManager {
    private static boolean frozen = false;
    private static final Map<UUID, Vec3> frozenPositions = new HashMap<>();

    public static boolean isFrozen() {
        return frozen;
    }

    public static void setFrozen(boolean frozen) {
        FreezeManager.frozen = frozen;
        if (!frozen) {
            // Clear stored positions when unfreezing
            frozenPositions.clear();
        }
    }
    
    public static void storeFrozenPosition(ServerPlayerEntity player) {
        if (frozen && !player.hasPermissions(2)) {
            frozenPositions.put(player.getUUID(), player.position());
        }
    }
    
    public static Vec3 getFrozenPosition(UUID playerUUID) {
        return frozenPositions.get(playerUUID);
    }
    
    public static boolean hasFrozenPosition(UUID playerUUID) {
        return frozenPositions.containsKey(playerUUID);
    }
} 