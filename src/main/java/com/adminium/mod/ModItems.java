package com.adminium.mod;

import com.adminium.mod.item.PodWandItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class ModItems {
    public static Item POD_WAND;

    public static void register() {
        POD_WAND = Registry.register(Registries.ITEM,
            new Identifier(Adminium.MODID, "pod_wand"),
            new PodWandItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));

        // Add to creative tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(POD_WAND);
        });
    }
}