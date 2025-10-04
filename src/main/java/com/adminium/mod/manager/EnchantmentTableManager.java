package com.adminium.mod.manager;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class EnchantmentTableManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final File CONFIG_FILE = new File("config/adminium_enchantment_table.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static MinecraftServer server;
    private static int maxEnchantmentLevel = 0; // 0 = no limit, 1+ = max level allowed

    public static void setServer(MinecraftServer server) {
        EnchantmentTableManager.server = server;
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);

            if (json != null && json.has("max_enchantment_level")) {
                maxEnchantmentLevel = json.get("max_enchantment_level").getAsInt();
            } else {
                maxEnchantmentLevel = 0;
            }

            LOGGER.info("Loaded enchantment table config: max level = {}", maxEnchantmentLevel);
        } catch (IOException | JsonParseException e) {
            LOGGER.error("Failed to load enchantment table config", e);
        }
    }

    public static void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            
            JsonObject json = new JsonObject();
            json.addProperty("max_enchantment_level", maxEnchantmentLevel);
            
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save enchantment table config", e);
        }
    }

    public static int getMaxEnchantmentLevel() {
        return maxEnchantmentLevel;
    }

    public static void setMaxEnchantmentLevel(int level) {
        if (maxEnchantmentLevel != level) {
            maxEnchantmentLevel = Math.max(0, level);
            save();
            LOGGER.info("Max enchantment level set to {}", maxEnchantmentLevel);
        }
    }

    public static boolean isEnchantmentAllowed(ItemStack stack) {
        if (maxEnchantmentLevel <= 0 || stack.isEmpty()) {
            return true; // No restrictions
        }

        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        for (Integer level : enchantments.values()) {
            if (level > maxEnchantmentLevel) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasHighLevelEnchantments(ItemStack stack) {
        return !isEnchantmentAllowed(stack);
    }
}
