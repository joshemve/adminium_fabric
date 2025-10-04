package com.adminium.mod.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GodManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<UUID> godModePlayers = new HashSet<>();
    private static final File CONFIG_FILE = new File("config/adminium_god_mode.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    public static boolean isInGodMode(UUID playerUUID) {
        return godModePlayers.contains(playerUUID);
    }
    
    public static void setGodMode(UUID playerUUID, boolean enabled) {
        if (enabled) {
            godModePlayers.add(playerUUID);
            LOGGER.info("God mode enabled for player {}", playerUUID);
        } else {
            godModePlayers.remove(playerUUID);
            LOGGER.info("God mode disabled for player {}", playerUUID);
        }
        save();
    }
    
    public static Set<UUID> getGodModePlayers() {
        return new HashSet<>(godModePlayers);
    }
    
    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }
        
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            
            godModePlayers.clear();
            if (json.has("godModePlayers")) {
                Type setType = new TypeToken<Set<String>>(){}.getType();
                Set<String> uuidStrings = GSON.fromJson(json.get("godModePlayers"), setType);
                
                for (String uuidString : uuidStrings) {
                    try {
                        UUID uuid = UUID.fromString(uuidString);
                        godModePlayers.add(uuid);
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("Invalid UUID in god mode data: {}", uuidString);
                    }
                }
            }
            
            LOGGER.info("Loaded {} players in god mode", godModePlayers.size());
        } catch (IOException e) {
            LOGGER.error("Failed to load god mode data", e);
        }
    }
    
    public static void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            
            JsonObject json = new JsonObject();
            
            // Convert UUIDs to strings for JSON storage
            Set<String> uuidStrings = new HashSet<>();
            for (UUID uuid : godModePlayers) {
                uuidStrings.add(uuid.toString());
            }
            
            json.add("godModePlayers", GSON.toJsonTree(uuidStrings));
            
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save god mode data", e);
        }
    }
} 