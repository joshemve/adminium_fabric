package com.adminium.mod.client;

import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class ClientDisabledItemsData {
    private static Set<Identifier> disabledItems = new HashSet<>();

    public static void setDisabledItems(Set<Identifier> items) {
        disabledItems = new HashSet<>(items);
    }

    public static Set<Identifier> getDisabledItems() {
        return new HashSet<>(disabledItems);
    }

    public static boolean isItemDisabled(Identifier itemId) {
        return disabledItems.contains(itemId);
    }
} 