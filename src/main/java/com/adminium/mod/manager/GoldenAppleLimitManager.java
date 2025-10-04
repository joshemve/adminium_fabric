package com.adminium.mod.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class GoldenAppleLimitManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final File FILE = new File("config/adminium_golden_apple_limit.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean enabled = true; // default ON per request
    private static int limit = 4;          // default cap

    public static boolean isEnabled() { return enabled; }
    public static int getLimit() { return limit; }

    public static void setEnabled(boolean on) { enabled = on; save(); }
    public static void toggle() { enabled = !enabled; save(); }

    public static void setLimit(int newLimit) {
        limit = Math.max(0, newLimit);
        save();
    }

    public static void load() {
        if (!FILE.exists()) { save(); return; }
        try (FileReader r = new FileReader(FILE)) {
            JsonObject json = GSON.fromJson(r, JsonObject.class);
            if (json != null) {
                if (json.has("enabled")) enabled = json.get("enabled").getAsBoolean();
                if (json.has("limit")) limit = json.get("limit").getAsInt();
            }
        } catch (IOException ex) {
            LOGGER.error("Failed to load golden apple limit config", ex);
        }
    }

    public static void save() {
        try {
            FILE.getParentFile().mkdirs();
            JsonObject json = new JsonObject();
            json.addProperty("enabled", enabled);
            json.addProperty("limit", limit);
            try (FileWriter w = new FileWriter(FILE)) {
                GSON.toJson(json, w);
            }
        } catch (IOException ex) {
            LOGGER.error("Failed to save golden apple limit config", ex);
        }
    }

    public static boolean isGoldenApple(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem() == Items.GOLDEN_APPLE || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
    }

    public static int countGoldenApples(ServerPlayerEntity player) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (isGoldenApple(s)) total += s.getCount();
        }
        return total;
    }

    /**
     * Enforce the cap by trimming excess apples from the player's inventory and dropping overflow at their feet.
     * Runs server-side only.
     */
    public static void enforceCap(ServerPlayerEntity player) {
        if (!enabled) return;
        int cap = limit;
        if (cap <= 0) {
            // remove all golden apples if cap set to 0
            removeAndDrop(player, Integer.MAX_VALUE);
            return;
        }
        int total = countGoldenApples(player);
        if (total <= cap) return;
        int overflow = total - cap;
        removeAndDrop(player, overflow);
    }

    private static void removeAndDrop(ServerPlayerEntity player, int toRemove) {
        int remaining = toRemove;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (isGoldenApple(s)) {
                int take = Math.min(remaining, s.getCount());
                ItemStack removed = s.split(take);
                remaining -= removed.getCount();
                drop(player, removed);
            }
        }
        if (toRemove > 0) {
            player.containerMenu.broadcastChanges();
            player.inventoryMenu.broadcastChanges();
        }
    }

    private static void drop(ServerPlayerEntity player, ItemStack stack) {
        if (stack.isEmpty()) return;
        World level = player.level();
        ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), stack);
        level.addFreshEntity(itemEntity);
    }
}
