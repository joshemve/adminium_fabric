package com.adminium.mod.manager;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class BarrelLootManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final File CONFIG_FILE = new File("config/adminium_barrel_loot.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<Identifier, BarrelLootEntry> lootTable = new HashMap<>();
    private static final Set<BlockPos> lootedBarrels = new HashSet<>();
    private static MinecraftServer server;
    private static final Random RANDOM = new Random();

    public static class BarrelLootEntry {
        private final Identifier itemId;
        private double dropChance; // 0.0 to 100.0
        private int minCount;
        private int maxCount;

        public BarrelLootEntry(Identifier itemId, double dropChance, int minCount, int maxCount) {
            this.itemId = itemId;
            this.dropChance = dropChance;
            this.minCount = minCount;
            this.maxCount = maxCount;
        }

        public Identifier getItemId() {
            return itemId;
        }

        public double getDropChance() {
            return dropChance;
        }

        public void setDropChance(double chance) {
            this.dropChance = Math.max(0, Math.min(100, chance));
        }

        public int getMinCount() {
            return minCount;
        }

        public void setMinCount(int count) {
            this.minCount = Math.max(1, count);
        }

        public int getMaxCount() {
            return maxCount;
        }

        public void setMaxCount(int count) {
            this.maxCount = Math.max(minCount, count);
        }
    }

    public static void setServer(MinecraftServer server) {
        BarrelLootManager.server = server;
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            JsonArray lootArray = json.getAsJsonArray("loot_table");

            lootTable.clear();
            for (JsonElement element : lootArray) {
                JsonObject entry = element.getAsJsonObject();
                String itemIdStr = entry.get("item").getAsString();
                double chance = entry.get("chance").getAsDouble();
                int minCount = entry.has("min_count") ? entry.get("min_count").getAsInt() : 1;
                int maxCount = entry.has("max_count") ? entry.get("max_count").getAsInt() : 1;

                Identifier itemId = Identifier.tryParse(itemIdStr);
                if (itemId != null) {
                    lootTable.put(itemId, new BarrelLootEntry(itemId, chance, minCount, maxCount));
                }
            }

            if (json.has("looted_barrels")) {
                JsonArray lootedArray = json.getAsJsonArray("looted_barrels");
                lootedBarrels.clear();
                for (JsonElement element : lootedArray) {
                    JsonObject pos = element.getAsJsonObject();
                    lootedBarrels.add(new BlockPos(
                        pos.get("x").getAsInt(),
                        pos.get("y").getAsInt(),
                        pos.get("z").getAsInt()
                    ));
                }
            }

            LOGGER.info("Loaded {} barrel loot entries", lootTable.size());
        } catch (IOException | JsonParseException e) {
            LOGGER.error("Failed to load barrel loot config", e);
        }
    }

    public static void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();

            JsonObject json = new JsonObject();
            JsonArray lootArray = new JsonArray();

            for (BarrelLootEntry entry : lootTable.values()) {
                JsonObject entryJson = new JsonObject();
                entryJson.addProperty("item", entry.getItemId().toString());
                entryJson.addProperty("chance", entry.getDropChance());
                entryJson.addProperty("min_count", entry.getMinCount());
                entryJson.addProperty("max_count", entry.getMaxCount());
                lootArray.add(entryJson);
            }

            json.add("loot_table", lootArray);

            JsonArray lootedArray = new JsonArray();
            for (BlockPos pos : lootedBarrels) {
                JsonObject posJson = new JsonObject();
                posJson.addProperty("x", pos.getX());
                posJson.addProperty("y", pos.getY());
                posJson.addProperty("z", pos.getZ());
                lootedArray.add(posJson);
            }
            json.add("looted_barrels", lootedArray);

            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save barrel loot config", e);
        }
    }

    public static void addOrUpdateLootEntry(Identifier itemId, double chance, int minCount, int maxCount) {
        lootTable.put(itemId, new BarrelLootEntry(itemId, chance, minCount, maxCount));
        save();
    }

    public static void removeLootEntry(Identifier itemId) {
        lootTable.remove(itemId);
        save();
    }

    public static Map<Identifier, BarrelLootEntry> getLootTable() {
        return new HashMap<>(lootTable);
    }

    public static BarrelLootEntry getLootEntry(Identifier itemId) {
        return lootTable.get(itemId);
    }

    public static boolean isBarrelLooted(BlockPos pos) {
        return lootedBarrels.contains(pos);
    }

    public static void markBarrelLooted(BlockPos pos) {
        lootedBarrels.add(pos);
        save();
    }

    public static void resetBarrelLooted(BlockPos pos) {
        lootedBarrels.remove(pos);
        save();
    }

    public static void resetAllBarrels() {
        lootedBarrels.clear();
        save();
        LOGGER.info("Reset all barrel loot states");
    }

    public static List<ItemStack> generateLoot() {
        List<ItemStack> loot = new ArrayList<>();

        for (BarrelLootEntry entry : lootTable.values()) {
            if (RANDOM.nextDouble() * 100 < entry.getDropChance()) {
                Item item = ForgeRegistries.ITEMS.getValue(entry.getItemId());
                if (item != null) {
                    int count = entry.getMinCount() == entry.getMaxCount() ?
                        entry.getMinCount() :
                        entry.getMinCount() + RANDOM.nextInt(entry.getMaxCount() - entry.getMinCount() + 1);

                    if (count > 0) {
                        loot.add(new ItemStack(item, count));
                    }
                }
            }
        }

        return loot;
    }

    public static int fillAllBarrels() {
        if (server == null) return 0;

        // Reset all looted barrels and close them
        int resetCount = 0;

        for (net.minecraft.server.level.ServerWorld level : server.getAllLevels()) {
            // Close all open barrels
            for (BlockPos pos : new HashSet<>(lootedBarrels)) {
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof net.minecraft.world.level.block.BarrelBlock) {
                    if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)) {
                        net.minecraft.world.level.block.state.BlockState closedState =
                            state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN, Boolean.FALSE);
                        level.setBlock(pos, closedState, 3);
                    }
                    resetCount++;
                }
            }
        }

        lootedBarrels.clear();
        save();

        LOGGER.info("Reset {} barrels for fresh loot", resetCount);
        return resetCount;
    }

    public static void clearLootTable() {
        lootTable.clear();
        save();
    }
}