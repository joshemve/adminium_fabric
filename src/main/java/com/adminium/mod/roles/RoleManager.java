package com.adminium.mod.roles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.Item;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class RoleManager {
    private static final Map<UUID, Role> playerRoles = new HashMap<>();
    private static final Map<Role, String> roleIcons = new HashMap<>();
    private static final Map<Role, net.minecraft.text.Text> ROLE_PREFIX_CACHE = new HashMap<>();
    private static final Path CONFIG_FILE = Paths.get("config", "adminium_roles.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // batching
    private static boolean DIRTY = false;
    private static int dirtyTicks = 0;
    private static MinecraftServer server;
    
    static {
        // Initialize default icons
        roleIcons.put(Role.FIGHTER, "minecraft:iron_sword");
        roleIcons.put(Role.FARMER, "minecraft:wheat");
        roleIcons.put(Role.MINER, "minecraft:iron_pickaxe");
    }
    
    public static void setServer(MinecraftServer server) {
        RoleManager.server = server;
    }
    
    public static void load() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                String json = Files.readString(CONFIG_FILE);
                RoleData data = GSON.fromJson(json, RoleData.class);
                if (data != null) {
                    playerRoles.clear();
                    if (data.playerRoles != null) {
                        for (Map.Entry<String, String> entry : data.playerRoles.entrySet()) {
                            try {
                                UUID uuid = UUID.fromString(entry.getKey());
                                Role role = Role.valueOf(entry.getValue());
                                playerRoles.put(uuid, role);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    
                    if (data.roleIcons != null) {
                        for (Map.Entry<String, String> entry : data.roleIcons.entrySet()) {
                            try {
                                Role role = Role.valueOf(entry.getKey());
                                roleIcons.put(role, entry.getValue());
                                
                                // Update the role's icon item
                                Identifier itemId = Identifier.tryParse(entry.getValue());
                                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                                if (item != null) {
                                    role.setIconItem(item);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            RoleData data = new RoleData();
            data.playerRoles = new HashMap<>();
            for (Map.Entry<UUID, Role> entry : playerRoles.entrySet()) {
                data.playerRoles.put(entry.getKey().toString(), entry.getValue().name());
            }
            
            data.roleIcons = new HashMap<>();
            for (Map.Entry<Role, String> entry : roleIcons.entrySet()) {
                data.roleIcons.put(entry.getKey().name(), entry.getValue());
            }
            
            String json = GSON.toJson(data);
            Files.writeString(CONFIG_FILE, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void assignRole(UUID playerId, Role role) {
        playerRoles.put(playerId, role);
        markDirty();
        
        // Apply role benefits immediately
        if (server != null) {
            ServerPlayerEntity player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                applyRoleBenefitsImmediately(player, role);
            }
            
            // Refresh display names for all players
            com.adminium.mod.Adminium.refreshAllPlayersDisplayNames(server);
        }
    }
    
    public static void removeRole(UUID playerId) {
        playerRoles.remove(playerId);
        markDirty();
        
        // Refresh display names for all players
        if (server != null) {
            com.adminium.mod.Adminium.refreshAllPlayersDisplayNames(server);
        }
    }
    
    public static Role getPlayerRole(UUID playerId) {
        return playerRoles.get(playerId);
    }
    
    public static void setRoleIcon(Role role, Item item) {
        role.setIconItem(item);
        Identifier itemId = ForgeRegistries.ITEMS.getKey(item);
        if (itemId != null) {
            roleIcons.put(role, itemId.toString());
            ROLE_PREFIX_CACHE.remove(role); // invalidate cache so new icon is reflected
            markDirty();
            
            // Refresh display names for all players
            if (server != null) {
                com.adminium.mod.Adminium.refreshAllPlayersDisplayNames(server);
            }
        }
    }
    
    public static void removeAllRoles() {
        // Clean up role effects and attributes from all online players first
        if (server != null) {
            for (ServerPlayerEntity player : server.getPlayerList().getPlayers()) {
                cleanupPlayerRoleEffects(player);
            }
        }
        
        playerRoles.clear();
        ROLE_PREFIX_CACHE.clear();
        markDirty();
        
        // Refresh display names for all players
        if (server != null) {
            com.adminium.mod.Adminium.refreshAllPlayersDisplayNames(server);
        }
    }
    
    public static void assignRolesRandomly() {
        if (server == null) return;
        
        List<ServerPlayerEntity> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        
        // Clean up existing role effects and clear roles
        for (ServerPlayerEntity player : players) {
            cleanupPlayerRoleEffects(player);
        }
        playerRoles.clear();
        
        // Calculate counts for each role
        int fightersCount = (int) Math.ceil(players.size() * 0.30);
        int farmersCount = (int) Math.ceil(players.size() * 0.35);
        // Miners get the rest
        
        List<UUID> availablePlayers = new ArrayList<>(players.stream().map(ServerPlayerEntity::getUUID).collect(Collectors.toList()));
        
        // Shuffle players for random assignment
        Collections.shuffle(availablePlayers);
        
        int index = 0;
        
        // Assign fighters
        for (int i = 0; i < fightersCount && index < availablePlayers.size(); i++, index++) {
            assignRole(availablePlayers.get(index), Role.FIGHTER);
        }
        
        // Assign farmers
        for (int i = 0; i < farmersCount && index < availablePlayers.size(); i++, index++) {
            assignRole(availablePlayers.get(index), Role.FARMER);
        }
        
        // Assign remaining as miners
        while (index < availablePlayers.size()) {
            assignRole(availablePlayers.get(index), Role.MINER);
            index++;
        }
        
        save();
        
        // Refresh display names for all players
        com.adminium.mod.Adminium.refreshAllPlayersDisplayNames(server);
    }
    
    public static Map<UUID, Role> getAllPlayerRoles() {
        return new HashMap<>(playerRoles);
    }
    
    public static String getPlayerRolePrefix(UUID playerId) {
        Role role = playerRoles.get(playerId);
        return role != null ? role.getPrefix() : "";
    }
    
    public static net.minecraft.text.Text getPlayerRolePrefixComponent(UUID playerId) {
        Role role = playerRoles.get(playerId);
        if (role == null) return net.minecraft.text.Text.empty();

        return ROLE_PREFIX_CACHE.computeIfAbsent(role, r -> r.getItemIconComponent());
    }
    
    public static void setPlayerRole(UUID playerId, Role role) {
        playerRoles.put(playerId, role);
        markDirty();
        
        // Apply role benefits immediately
        if (server != null) {
            ServerPlayerEntity player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                applyRoleBenefitsImmediately(player, role);
            }
            
            com.adminium.mod.Adminium.refreshAllPlayersDisplayNames(server);
        }
    }
    
    public static Set<UUID> getAllPlayersWithRoles() {
        return new HashSet<>(playerRoles.keySet());
    }
    
    /**
     * Clean up all role-related effects and attributes from a player
     */
    private static void cleanupPlayerRoleEffects(ServerPlayerEntity player) {
        // Remove role visual effects
        player.removeEffect(com.adminium.mod.ModEffects.FIGHTER.get());
        player.removeEffect(com.adminium.mod.ModEffects.MINER.get());
        player.removeEffect(com.adminium.mod.ModEffects.FARMER.get());
        
        // Remove fighter damage bonus attribute modifier
        var attrInstance = player.getAttribute(net.minecraft.world.entity.ai.attributes.EntityAttributes.ATTACK_DAMAGE);
        if (attrInstance != null) {
            // Use the same UUID as RoleBonusHandler
            java.util.UUID FIGHTER_DAMAGE_UUID = java.util.UUID.fromString("0e02ef0a-b3cf-4d6c-bfac-29a2e0b600aa");
            if (attrInstance.getModifier(FIGHTER_DAMAGE_UUID) != null) {
                attrInstance.removeModifier(FIGHTER_DAMAGE_UUID);
            }
        }
        
        // Remove any potentially stuck vanilla effects that could interfere with mining
        player.removeEffect(net.minecraft.world.effect.StatusEffects.DIG_SLOWDOWN); // Mining Fatigue
        player.removeEffect(net.minecraft.world.effect.StatusEffects.MOVEMENT_SLOWDOWN); // Slowness
        player.removeEffect(net.minecraft.world.effect.StatusEffects.BLINDNESS); // Blindness
    }
    
    /**
     * Immediately apply role benefits to a player without waiting for the tick handler
     */
    private static void applyRoleBenefitsImmediately(ServerPlayerEntity player, Role role) {
        // Force the RoleBonusHandler logic to run immediately
        com.adminium.mod.RoleBonusHandler.applyRoleBenefits(player, role);
    }
    private static void markDirty() {
        DIRTY = true;
        dirtyTicks = 0;
    }

    // This should be called from the main mod's tick handler
    public static void tickHandler() {
        if (!DIRTY) return;
        dirtyTicks++;
        if (dirtyTicks >= 100) { // flush every 5s
            save();
            DIRTY = false;
            dirtyTicks = 0;
        }
    }
    
    private static class RoleData {
        Map<String, String> playerRoles;
        Map<String, String> roleIcons;
    }
} 