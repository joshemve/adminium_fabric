package com.adminium.mod.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.world.GameMode;
import org.slf4j.Logger;

import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BanManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, BanInfo> customBans = new HashMap<>();
    private static final File CONFIG_FILE = new File("config/adminium_bans.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static MinecraftServer server;
    
    public static void setServer(MinecraftServer minecraftServer) {
        server = minecraftServer;
    }
    
    public static void addBan(UUID playerUUID, String playerName, String reason, String bannedBy, boolean isHardcoreDeath) {
        BanInfo banInfo = new BanInfo(playerUUID, playerName, reason, bannedBy, 
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), isHardcoreDeath);
        customBans.put(playerUUID, banInfo);
        save();
        
        // Also add to vanilla ban list for actual enforcement
        if (server != null) {
            BannedPlayerList banList = server.getPlayerManager().getUserBanList();
            GameProfile profile = new GameProfile(playerUUID, playerName);
            BannedPlayerEntry banEntry = new BannedPlayerEntry(profile, null, bannedBy, null, reason);
            banList.add(banEntry);
            
            LOGGER.info("Added player {} to vanilla ban list with reason: {}", playerName, reason);
            
            // Also kick the player if they're currently online
            ServerPlayerEntity onlinePlayer = server.getPlayerList().getPlayer(playerUUID);
            if (onlinePlayer != null) {
                onlinePlayer.connection.disconnect(net.minecraft.network.chat.Text.literal(
                    "§cYou have been banned from this server!\n" +
                    "§7Reason: " + reason + "\n" +
                    "§7Banned by: " + bannedBy
                ));
            }
        }
    }
    
    public static void removeBan(UUID playerUUID) {
        // Capture info before removal for potential profile construction
        BanInfo previous = customBans.remove(playerUUID);
        save();
        
        // Also remove from vanilla ban list
        if (server != null) {
            UserBanList banList = server.getPlayerList().getBans();

            // Prefer a cached profile if available
            GameProfile profile = server.getProfileCache().get(playerUUID)
                .orElse(new GameProfile(playerUUID, previous != null ? previous.playerName : null));

            // If the ban list contains this profile, remove it; if not, still attempt removal using constructed profile
            boolean wasBanned = banList.isBanned(profile);
            banList.remove(profile);

            if (wasBanned) {
                LOGGER.info("Removed UUID {} from vanilla ban list", playerUUID);
            } else {
                LOGGER.info("Requested removal from vanilla ban list for UUID {} (profile cache miss or name changed)", playerUUID);
            }
        }
    }
    
    public static boolean isBanned(UUID playerUUID) {
        return customBans.containsKey(playerUUID);
    }
    
    public static BanInfo getBanInfo(UUID playerUUID) {
        return customBans.get(playerUUID);
    }
    
    public static List<BanInfo> getAllBans() {
        return new ArrayList<>(customBans.values());
    }
    
    public static void pardonPlayer(UUID playerUUID, boolean revertToSurvival) {
        BanInfo banInfo = customBans.get(playerUUID);
        
        // Remove from our custom ban list
        removeBan(playerUUID);
        
        LOGGER.info("PlayerEntity {} has been pardoned by an operator", 
            banInfo != null ? banInfo.playerName : playerUUID.toString());
        
        // Note: No need for complex gamemode reversion logic since InstaBan now
        // automatically prevents non-operators from being in spectator mode
    }
    
    public static void handlePlayerJoin(ServerPlayerEntity player) {
        // This method is kept for compatibility but no longer needs to handle gamemode reversion
        // since the InstaBan system now automatically prevents non-operators from being in spectator mode
    }
    
    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }
        
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            
            customBans.clear();
            if (json.has("bans")) {
                Type banMapType = new TypeToken<Map<String, BanInfo>>(){}.getType();
                Map<String, BanInfo> loadedBans = GSON.fromJson(json.get("bans"), banMapType);
                
                for (Map.Entry<String, BanInfo> entry : loadedBans.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        customBans.put(uuid, entry.getValue());
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("Invalid UUID in ban data: {}", entry.getKey());
                    }
                }
            }
            
            LOGGER.info("Loaded {} custom bans", customBans.size());
        } catch (IOException e) {
            LOGGER.error("Failed to load ban data", e);
        }
    }
    
    public static void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            
            JsonObject json = new JsonObject();
            
            // Convert UUID keys to strings for JSON
            Map<String, BanInfo> stringKeyMap = new HashMap<>();
            for (Map.Entry<UUID, BanInfo> entry : customBans.entrySet()) {
                stringKeyMap.put(entry.getKey().toString(), entry.getValue());
            }
            
            json.add("bans", GSON.toJsonTree(stringKeyMap));
            
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save ban data", e);
        }
    }
    
    public static class BanInfo {
        public final UUID playerUUID;
        public final String playerName;
        public final String reason;
        public final String bannedBy;
        public final String banDate;
        public final boolean isHardcoreDeath;
        public boolean revertToSurvival = false;
        public boolean isVanillaBan = false;
        
        public BanInfo(UUID playerUUID, String playerName, String reason, String bannedBy, 
                      String banDate, boolean isHardcoreDeath) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.reason = reason;
            this.bannedBy = bannedBy;
            this.banDate = banDate;
            this.isHardcoreDeath = isHardcoreDeath;
        }
        
        public String getFormattedBanDate() {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(banDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e) {
                return banDate;
            }
        }
    }
} 