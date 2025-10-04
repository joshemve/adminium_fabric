package com.adminium.mod.client;

import com.adminium.mod.client.gui.BarrelConfigScreen;
import com.adminium.mod.manager.BarrelLootManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ClientBarrelHandler {
    public static void openBarrelConfig(Map<Identifier, BarrelLootManager.BarrelLootEntry> lootTable) {
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient.getInstance().setScreen(new BarrelConfigScreen(lootTable));
        });
    }
}