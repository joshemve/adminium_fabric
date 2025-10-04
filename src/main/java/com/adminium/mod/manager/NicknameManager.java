package com.adminium.mod.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores persistent player nicknames. Keyed by UUID.
 */
public class NicknameManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final File FILE = new File("config/adminium_nicknames.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<UUID, String> NICKS = new HashMap<>();

    // batching
    private static boolean DIRTY = false;
    private static int dirtyTicks = 0; // counts server ticks since last change

    public static void load() {
        NICKS.clear();
        if (!FILE.exists()) {
            save();
            return;
        }
        try (FileReader r = new FileReader(FILE)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            root.entrySet().forEach(e -> {
                try {
                    UUID id = UUID.fromString(e.getKey());
                    NICKS.put(id, e.getValue().getAsString());
                } catch (IllegalArgumentException ignored) {}
            });
            LOGGER.info("Loaded {} nicknames", NICKS.size());
        } catch (IOException e) {
            LOGGER.error("Failed to load nicknames", e);
        }
    }

    public static void save() {
        try {
            FILE.getParentFile().mkdirs();
            JsonObject root = new JsonObject();
            for (var entry : NICKS.entrySet()) {
                root.addProperty(entry.getKey().toString(), entry.getValue());
            }
            try (FileWriter w = new FileWriter(FILE)) {
                GSON.toJson(root, w);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save nicknames", e);
        }
    }

    public static void setNickname(UUID uuid, String nick) {
        if (nick == null || nick.isBlank()) {
            NICKS.remove(uuid);
        } else {
            NICKS.put(uuid, nick);
        }
        markDirty();
    }

    public static void clearNickname(UUID uuid) {
        NICKS.remove(uuid);
        markDirty();
    }

    public static String getNickname(UUID uuid) {
        return NICKS.get(uuid);
    }

    public static boolean hasNickname(UUID uuid) {
        return NICKS.containsKey(uuid);
    }

    private static void markDirty() {
        DIRTY = true;
        dirtyTicks = 0;
    }

    // This should be called from the main mod's tick handler
    public static void tickHandler() {
        if (!DIRTY) return;
        dirtyTicks++;
        if (dirtyTicks >= 100) { // 100 ticks ≈ 5 seconds
            save();
            DIRTY = false;
            dirtyTicks = 0;
        }
    }
} 