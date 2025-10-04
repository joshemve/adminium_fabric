package com.adminium.mod.manager;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.util.math.BlockPos;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WaterDamageManager {
    private static boolean waterDamageEnabled = false;
    private static int damageIntervalTicks = 5; // Default: 0.25 seconds (5 ticks)
    private static float damageAmount = 1.0F; // Default: 1 HP
    private static final Map<UUID, Long> lastDamageTime = new HashMap<>();

    public static boolean isWaterDamageEnabled() {
        return waterDamageEnabled;
    }

    public static void setWaterDamageEnabled(boolean enabled) {
        waterDamageEnabled = enabled;
        if (!enabled) {
            lastDamageTime.clear();
        }
    }

    public static int getDamageIntervalTicks() {
        return damageIntervalTicks;
    }

    public static void setDamageIntervalTicks(int ticks) {
        if (ticks < 1) ticks = 1; // Minimum 1 tick
        damageIntervalTicks = ticks;
    }

    public static float getDamageAmount() {
        return damageAmount;
    }

    public static void setDamageAmount(float amount) {
        if (amount < 0.5F) amount = 0.5F; // Minimum 0.5 HP
        damageAmount = amount;
    }
    public static void checkAndApplyWaterDamage(ServerPlayerEntity player) {
        if (!waterDamageEnabled) {
            return;
        }

        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        BlockPos pos = player.blockPosition();
        FluidState fluidState = player.level().getFluidState(pos);

        if (fluidState.is(Fluids.WATER) || fluidState.is(Fluids.FLOWING_WATER) || player.isInWater() || player.isInWaterOrBubble() || player.isUnderWater()) {
            UUID playerId = player.getUUID();
            // Use world time instead of player tick count to avoid respawn issues
            long currentTime = player.level().getGameTime();

            Long lastDamage = lastDamageTime.get(playerId);
            if (lastDamage == null || (currentTime - lastDamage) >= damageIntervalTicks) {
                player.hurt(player.damageSources().drown(), damageAmount);
                lastDamageTime.put(playerId, currentTime);
            }
        } else {
            // PlayerEntity left water, remove from tracking
            lastDamageTime.remove(player.getUUID());
        }
    }

    public static void clearPlayerTracking(UUID playerId) {
        lastDamageTime.remove(playerId);
    }
}