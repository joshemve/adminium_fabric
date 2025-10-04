package com.adminium.mod.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class WarpManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, WarpLocation> warps = new HashMap<>();
    private static final File CONFIG_FILE = new File("config/adminium_warps.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static MinecraftServer server;
    
    public static void setServer(MinecraftServer minecraftServer) {
        server = minecraftServer;
    }
    
    public static void setWarp(String name, ServerPlayerEntity player) {
        BlockPos pos = player.blockPosition();
        RegistryKey<World> dimension = player.level().dimension();
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        
        WarpLocation warp = new WarpLocation(name, pos, dimension, yaw, pitch);
        warps.put(name.toLowerCase(), warp);
        save();
    }
    
    public static boolean warpExists(String name) {
        return warps.containsKey(name.toLowerCase());
    }
    
    public static boolean teleportToWarp(String name, ServerPlayerEntity player) {
        WarpLocation warp = warps.get(name.toLowerCase());
        if (warp == null) {
            return false;
        }
        
        ServerWorld targetLevel = server.getLevel(warp.dimension);
        if (targetLevel == null) {
            LOGGER.warn("Warp {} references invalid dimension: {}", name, warp.dimension);
            return false;
        }
        
        // Teleport player
        player.teleportTo(targetLevel, warp.pos.getX() + 0.5, warp.pos.getY(), warp.pos.getZ() + 0.5, warp.yaw, warp.pitch);
        return true;
    }
    
    public static boolean deleteWarp(String name) {
        boolean removed = warps.remove(name.toLowerCase()) != null;
        if (removed) {
            save();
        }
        return removed;
    }
    
    public static Set<String> getWarpNames() {
        return warps.keySet();
    }
    
    public static WarpLocation getWarp(String name) {
        return warps.get(name.toLowerCase());
    }
    
    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }
        
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            
            warps.clear();
            for (String warpName : json.keySet()) {
                JsonObject warpData = json.getAsJsonObject(warpName);
                
                String name = warpData.get("name").getAsString();
                int x = warpData.get("x").getAsInt();
                int y = warpData.get("y").getAsInt();
                int z = warpData.get("z").getAsInt();
                String dimensionStr = warpData.get("dimension").getAsString();
                float yaw = warpData.get("yaw").getAsFloat();
                float pitch = warpData.get("pitch").getAsFloat();
                
                BlockPos pos = new BlockPos(x, y, z);
                RegistryKey<World> dimension = RegistryKey.create(net.minecraft.core.registries.Registries.DIMENSION, 
                    new Identifier(dimensionStr));
                
                WarpLocation warp = new WarpLocation(name, pos, dimension, yaw, pitch);
                warps.put(name.toLowerCase(), warp);
            }
            
            LOGGER.info("Loaded {} warps", warps.size());
        } catch (IOException e) {
            LOGGER.error("Failed to load warps config", e);
        }
    }
    
    public static void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            
            JsonObject json = new JsonObject();
            for (Map.Entry<String, WarpLocation> entry : warps.entrySet()) {
                WarpLocation warp = entry.getValue();
                JsonObject warpData = new JsonObject();
                
                warpData.addProperty("name", warp.name);
                warpData.addProperty("x", warp.pos.getX());
                warpData.addProperty("y", warp.pos.getY());
                warpData.addProperty("z", warp.pos.getZ());
                warpData.addProperty("dimension", warp.dimension.location().toString());
                warpData.addProperty("yaw", warp.yaw);
                warpData.addProperty("pitch", warp.pitch);
                
                json.add(entry.getKey(), warpData);
            }
            
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save warps config", e);
        }
    }
    
    public static class WarpLocation {
        public final String name;
        public final BlockPos pos;
        public final RegistryKey<World> dimension;
        public final float yaw;
        public final float pitch;
        
        public WarpLocation(String name, BlockPos pos, RegistryKey<World> dimension, float yaw, float pitch) {
            this.name = name;
            this.pos = pos;
            this.dimension = dimension;
            this.yaw = yaw;
            this.pitch = pitch;
        }
        
        public String getDimensionName() {
            String dimensionStr = dimension.location().toString();
            switch (dimensionStr) {
                case "minecraft:overworld":
                    return "Overworld";
                case "minecraft:the_nether":
                    return "Nether";
                case "minecraft:the_end":
                    return "End";
                default:
                    return dimensionStr;
            }
        }
    }
} 