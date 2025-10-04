package com.adminium.mod.events;

import com.adminium.mod.manager.GoldenAppleLimitManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.UUID;

public class GoldenAppleCapEvents {
    // No global scanning or per-tick work; enforcement occurs at pickup/craft time only
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        // Deprecated by onEntityItemPickup. We now enforce caps before items enter inventory
        // to avoid visual glitches and excess syncing.
        // Intentionally left blank.
    }

    // TODO: Convert to Fabric event
        public static void onEntityItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity player)) return;
        if (!GoldenAppleLimitManager.isEnabled()) return;

        var itemEntity = event.getItem();
        ItemStack entityStack = itemEntity.getItem();
        if (!GoldenAppleLimitManager.isGoldenApple(entityStack)) return;

        int cap = GoldenAppleLimitManager.getLimit();
        if (cap <= 0) {
            // No apples allowed at all
            event.setCanceled(true);
            return;
        }

        int current = GoldenAppleLimitManager.countGoldenApples(player);
        int remainingAllow = cap - current;
        if (remainingAllow <= 0) {
            // Already at/over cap: block pickup entirely, leave item on ground
            event.setCanceled(true);
            return;
        }

        int stackCount = entityStack.getCount();
        if (stackCount > remainingAllow) {
            int leftover = stackCount - remainingAllow;
            // Shrink the item being picked up to the allowed amount
            entityStack.setCount(remainingAllow);
            // Spawn leftover as a new entity so nothing is lost
            net.minecraft.world.entity.item.ItemEntity leftoverEntity =
                new net.minecraft.world.entity.item.ItemEntity(
                    player.level(), itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                    new ItemStack(entityStack.getItem(), leftover));
            // Avoid immediate re-pickup attempts in the same tick
            leftoverEntity.setDefaultPickUpDelay();
            player.level().addFreshEntity(leftoverEntity);
        }
        // Allow pickup to proceed for the allowed amount
    }

    // TODO: Convert to Fabric event
        public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity player)) return;
        ItemStack crafted = event.getCrafting();
        if (GoldenAppleLimitManager.isEnabled() && GoldenAppleLimitManager.isGoldenApple(crafted)) {
            // Enforce immediately to avoid a follow-up tick scan
            GoldenAppleLimitManager.enforceCap(player);
        }
    }
}
