package com.adminium.mod.manager;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class BarrelProtectionManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final File CONFIG_FILE = new File("config/adminium_barrel_protection.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean barrelBreakingEnabled = false;

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);

            if (json != null && json.has("barrel_breaking_enabled")) {
                barrelBreakingEnabled = json.get("barrel_breaking_enabled").getAsBoolean();
            } else {
                barrelBreakingEnabled = false;
            }
        } catch (IOException | com.google.gson.JsonParseException e) {
            LOGGER.error("Failed to load barrel protection config", e);
        }
    }

    public static void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            
            JsonObject json = new JsonObject();
            json.addProperty("barrel_breaking_enabled", barrelBreakingEnabled);
            
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save barrel protection config", e);
        }
    }

    public static boolean isBarrelBreakingEnabled() {
        return barrelBreakingEnabled;
    }

    public static void setBarrelBreakingEnabled(boolean enabled) {
        if (barrelBreakingEnabled != enabled) {
            barrelBreakingEnabled = enabled;
            save();
            LOGGER.info("Barrel breaking set to {}", enabled ? "enabled" : "disabled");
        }
    }

    public static boolean toggleBarrelBreaking() {
        setBarrelBreakingEnabled(!barrelBreakingEnabled);
        return barrelBreakingEnabled;
    }
}
