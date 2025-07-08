package com.adminium.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.lang.reflect.Type;

public class DisabledItemsManager {
    private static final Set<ResourceLocation> disabledItems = new HashSet<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = Paths.get("config", "adminium_disabled_items.json");

    public static void load() {
        if (!Files.exists(CONFIG_FILE)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            Type type = new TypeToken<Set<ResourceLocation>>() {}.getType();
            Set<ResourceLocation> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                disabledItems.clear();
                disabledItems.addAll(loaded);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(disabledItems, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void disableItem(Item item) {
        if(disabledItems.add(BuiltInRegistries.ITEM.getKey(item))) {
            save();
        }
    }

    public static void enableItem(Item item) {
        if(disabledItems.remove(BuiltInRegistries.ITEM.getKey(item))) {
            save();
        }
    }

    public static boolean isItemDisabled(Item item) {
        return disabledItems.contains(BuiltInRegistries.ITEM.getKey(item));
    }

    public static Set<ResourceLocation> getDisabledItems() {
        return new HashSet<>(disabledItems);
    }
} 