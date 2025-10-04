package com.adminium.mod.container;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.collection.DefaultedList;

import java.util.UUID;

/**
 * Container wrapper exposing a ServerPlayerEntity's inventory (main, hotbar, armour, offhand) as 54 slots.
 * Slot mapping:
 * 0-35  : main + hotbar (PlayerInventory.items)
 * 36-39 : armour (boots, leggings, chestplate, helmet)
 * 40    : offhand
 * 41-53 : empty filler (not backed by anything)
 */
public class InvseeContainer implements Inventory {
    private final ServerPlayerEntity target;
    private final DefaultedList<ItemStack> dummy = DefaultedList.ofSize(13, ItemStack.EMPTY);

    public InvseeContainer(ServerPlayerEntity target) {
        this.target = target;
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < 54; i++) if (!getStack(i).isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot < 36) {
            return target.getInventory().getStack(slot);
        } else if (slot < 40) {
            int armorIdx = slot - 36; // 0-3
            return target.getInventory().armor.get(armorIdx);
        } else if (slot == 40) {
            return target.getInventory().offhand.get(0);
        } else {
            return dummy.get(slot - 41);
        }
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack stack = getStack(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;

        ItemStack result = stack.split(amount);
        setStack(slot, stack);
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack stack = getStack(slot);
        setStack(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot < 36) {
            target.getInventory().setStack(slot, stack);
        } else if (slot < 40) {
            int armorIdx = slot - 36;
            target.getInventory().armor.set(armorIdx, stack);
        } else if (slot == 40) {
            target.getInventory().offhand.set(0, stack);
        } else {
            dummy.set(slot - 41, stack);
        }
    }

    @Override
    public void markDirty() {
        // No-op for player inventory
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        target.getInventory().clear();
        for (int i = 0; i < dummy.size(); i++) {
            dummy.set(i, ItemStack.EMPTY);
        }
    }

    public UUID getTargetUUID() {
        return target.getUuid();
    }

    public String getTargetName() {
        return target.getName().getString();
    }
}