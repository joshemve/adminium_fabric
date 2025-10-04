package com.adminium.mod.manager;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.util.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DisabledItemsManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<Identifier> disabledItems = new HashSet<>();
    private static final Map<Identifier, Recipe<?>> recipeSnapshot = new LinkedHashMap<>();
    private static final Set<Identifier> appliedDisabledRecipeIds = new HashSet<>();
    private static final File CONFIG_FILE = new File("config/adminium_disabled_items.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static MinecraftServer server;
    private static boolean inventoryRemovalEnabled = false;

    public static void setServer(MinecraftServer server) {
        DisabledItemsManager.server = server;
        recipeSnapshot.clear();
        appliedDisabledRecipeIds.clear();
    }
    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);

            disabledItems.clear();
            JsonArray itemsArray = json != null && json.has("disabled_items")
                ? json.getAsJsonArray("disabled_items")
                : new JsonArray();

            for (JsonElement element : itemsArray) {
                String[] parts = element.getAsString().split(":");
                if (parts.length == 2) {
                    disabledItems.add(new Identifier(parts[0], parts[1]));
                }
            }

            if (json != null && json.has("inventory_removal_enabled")) {
                inventoryRemovalEnabled = json.get("inventory_removal_enabled").getAsBoolean();
            } else {
                inventoryRemovalEnabled = false;
            }
        } catch (IOException | JsonParseException e) {
            LOGGER.error("Failed to load disabled items config", e);
        }
    }

    public static void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            
            JsonObject json = new JsonObject();
            JsonArray itemsArray = new JsonArray();
            
            for (Identifier item : disabledItems) {
                itemsArray.add(item.toString());
            }
            
            json.add("disabled_items", itemsArray);
            json.addProperty("inventory_removal_enabled", inventoryRemovalEnabled);
            
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save disabled items config", e);
        }
    }

    public static boolean isInventoryRemovalEnabled() {
        return inventoryRemovalEnabled;
    }

    public static void setInventoryRemovalEnabled(boolean enabled) {
        if (inventoryRemovalEnabled != enabled) {
            inventoryRemovalEnabled = enabled;
            save();
            LOGGER.info("Disabled item inventory removal set to {}", enabled);
        }
    }

    public static boolean toggleInventoryRemovalEnabled() {
        setInventoryRemovalEnabled(!inventoryRemovalEnabled);
        return inventoryRemovalEnabled;
    }

    public static void disableItem(Identifier itemId) {
        if (disabledItems.add(itemId)) {
            save();
            updateRecipes();
        }
    }

    public static void enableItem(Identifier itemId) {
        if (disabledItems.remove(itemId)) {
            save();
            updateRecipes();
        }
    }

    public static boolean isItemDisabled(Identifier itemId) {
        return disabledItems.contains(itemId);
    }

    public static boolean isItemDisabled(Item item) {
        return isItemDisabled(ForgeRegistries.ITEMS.getKey(item));
    }

    public static Set<Identifier> getDisabledItems() {
        return new HashSet<>(disabledItems);
    }
    
    public static Set<Item> getDisabledItemsAsItems() {
        Set<Item> items = new HashSet<>();
        for (Identifier id : disabledItems) {
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private static void updateRecipes() {
        if (server != null) {
            removeDisabledItemRecipes();
            syncToAllPlayers(server);
        }
    }

    public static void captureRecipeSnapshot() {
        if (server == null) {
            return;
        }
        RecipeManager recipeManager = server.getRecipeManager();
        recipeSnapshot.clear();
        for (Recipe<?> recipe : recipeManager.getRecipes()) {
            recipeSnapshot.put(recipe.getId(), recipe);
        }
    }

    public static void removeDisabledItemRecipes() {
        if (server == null) {
            return;
        }

        if (recipeSnapshot.isEmpty()) {
            captureRecipeSnapshot();
        }

        RecipeManager recipeManager = server.getRecipeManager();
        RegistryAccess registryAccess = server.registryAccess();

        List<Recipe<?>> allowedRecipes = new ArrayList<>(recipeSnapshot.size());
        Set<Identifier> newlyDisabledRecipeIds = new HashSet<>();

        for (Recipe<?> recipe : recipeSnapshot.values()) {
            ItemStack result = recipe.getResultItem(registryAccess);
            if (!result.isEmpty() && isItemDisabled(result.getItem())) {
                newlyDisabledRecipeIds.add(recipe.getId());
            } else {
                allowedRecipes.add(recipe);
            }
        }

        Set<Identifier> previouslyDisabled = new HashSet<>(appliedDisabledRecipeIds);
        if (newlyDisabledRecipeIds.equals(previouslyDisabled)) {
            return; // No change in disabled recipes
        }

        recipeManager.replaceRecipes(allowedRecipes);
        appliedDisabledRecipeIds.clear();
        appliedDisabledRecipeIds.addAll(newlyDisabledRecipeIds);

        LOGGER.info("Applied disabled item filters to recipes. {} recipes disabled.", newlyDisabledRecipeIds.size());

        broadcastRecipeUpdates(allowedRecipes, previouslyDisabled, newlyDisabledRecipeIds);
    }

    private static void broadcastRecipeUpdates(List<Recipe<?>> allowedRecipes,
                                               Set<Identifier> previouslyDisabled,
                                               Set<Identifier> newlyDisabled) {
        if (server == null) {
            return;
        }

        List<ServerPlayerEntity> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return;
        }

        ClientboundUpdateRecipesPacket recipePacket = new ClientboundUpdateRecipesPacket(allowedRecipes);
        for (ServerPlayerEntity player : players) {
            player.connection.send(recipePacket);
        }

        clearDisabledResultSlots(players);

        Set<Identifier> newlyRemoved = new HashSet<>(newlyDisabled);
        newlyRemoved.removeAll(previouslyDisabled);

        if (!newlyRemoved.isEmpty()) {
            List<Recipe<?>> recipesToRemove = newlyRemoved.stream()
                .map(recipeSnapshot::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            if (!recipesToRemove.isEmpty()) {
                for (ServerPlayerEntity player : players) {
                    player.resetRecipes(recipesToRemove);
                }
            }
        }

        Set<Identifier> restored = new HashSet<>(previouslyDisabled);
        restored.removeAll(newlyDisabled);

        if (!restored.isEmpty()) {
            List<Recipe<?>> recipesToRestore = restored.stream()
                .map(recipeSnapshot::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            if (!recipesToRestore.isEmpty()) {
                for (ServerPlayerEntity player : players) {
                    player.awardRecipes(recipesToRestore);
                }
            }
        }
    }

    private static void clearDisabledResultSlots(List<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            ScreenHandler menu = player.containerMenu;
            if (menu instanceof CraftingMenu || menu instanceof InventoryMenu) {
                if (!menu.slots.isEmpty()) {
                    Slot resultSlot = menu.getSlot(0);
                    ItemStack current = resultSlot.getItem();
                    if (!current.isEmpty() && isItemDisabled(current.getItem())) {
                        resultSlot.set(ItemStack.EMPTY);
                        menu.broadcastChanges();
                    }
                }
            }
        }
    }

    public static void stripCraftedItem(ServerPlayerEntity player, ItemStack craftedStack) {
        if (!isInventoryRemovalEnabled()) {
            return;
        }
        if (craftedStack.isEmpty()) {
            return;
        }

        Inventory inventory = player.getInventory();
        int remaining = craftedStack.getCount();

        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack slotStack = inventory.getItem(slot);
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameTags(slotStack, craftedStack)) {
                int removed = Math.min(remaining, slotStack.getCount());
                slotStack.shrink(removed);
                if (slotStack.isEmpty()) {
                    inventory.setItem(slot, ItemStack.EMPTY);
                } else {
                    inventory.setItem(slot, slotStack);
                }
                remaining -= removed;
            }
        }

        if (remaining > 0) {
            ItemStack carried = player.containerMenu.getCarried();
            if (!carried.isEmpty() && ItemStack.isSameItemSameTags(carried, craftedStack)) {
                int removed = Math.min(remaining, carried.getCount());
                carried.shrink(removed);
                if (carried.isEmpty()) {
                    player.containerMenu.setCarried(ItemStack.EMPTY);
                }
                remaining -= removed;
            }
        }

        player.containerMenu.broadcastChanges();
    }

    public static void syncToPlayer(ServerPlayerEntity player) {
        // Send disabled items to the player
        com.adminium.mod.network.ModNetworking.CHANNEL.send(
            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
            new com.adminium.mod.network.S2CSyncDisabledItemsPacket(disabledItems)
        );
    }

    public static void syncToAllPlayers(MinecraftServer server) {
        if (server != null) {
            com.adminium.mod.network.ModNetworking.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                new com.adminium.mod.network.S2CSyncDisabledItemsPacket(disabledItems)
            );
        }
    }
}
