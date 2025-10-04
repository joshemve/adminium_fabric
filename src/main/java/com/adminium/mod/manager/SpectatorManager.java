package com.adminium.mod.manager;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpectatorManager {
    private static final Map<UUID, GameType> PREVIOUS = new HashMap<>();

    public static boolean isSpectating(UUID uuid) {
        return PREVIOUS.containsKey(uuid);
    }

    public static void enableSpectator(ServerPlayerEntity player) {
        if (isSpectating(player.getUUID())) return;
        PREVIOUS.put(player.getUUID(), player.gameMode.getGameModeForPlayer());
        player.setGameMode(GameType.SPECTATOR);
        player.addEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, -1, 0, false, false, false));
        // Tick handler will enforce abilities
    }

    public static void disableSpectator(ServerPlayerEntity player) {
        GameType prev = PREVIOUS.remove(player.getUUID());
        if (prev == null) return;
        player.setGameMode(prev);
        player.removeEffect(StatusEffects.INVISIBILITY);
        // Abilities are reset via tick handler on next tick, or immediately here
        player.noPhysics = false;
        if (prev == GameType.CREATIVE) {
            // Keep creative flight exactly as player had it; do not forcibly disable
            player.getAbilities().mayfly = true;
            // leave flying flag as-is so mid-air players stay airborne
        } else {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
        }
        player.onUpdateAbilities();
    }

    public static void toggle(ServerPlayerEntity player) {
        if (isSpectating(player.getUUID())) {
            disableSpectator(player);
        } else {
            enableSpectator(player);
        }
    }

    /** Called when a player permanently leaves the server to avoid memory leaks */
    public static void cleanup(java.util.UUID uuid) {
        PREVIOUS.remove(uuid);
    }
} 