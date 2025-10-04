package com.adminium.mod.manager;

import net.minecraft.server.network.ServerPlayerEntity;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks players who have temporary flight permission enabled via /fly.
 */
public class FlyManager {
    private static final Set<UUID> ENABLED = new HashSet<>();

    public static boolean canFly(UUID id) {
        return ENABLED.contains(id);
    }

    public static void enable(ServerPlayerEntity player) {
        UUID id = player.getUUID();
        if (ENABLED.add(id)) {
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }
    }

    public static void disable(ServerPlayerEntity player) {
        UUID id = player.getUUID();
        if (ENABLED.remove(id)) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    public static void toggle(ServerPlayerEntity player) {
        if (canFly(player.getUUID())) {
            disable(player);
        } else {
            enable(player);
        }
    }

    public static void cleanup(UUID id) {
        ENABLED.remove(id);
    }
} 