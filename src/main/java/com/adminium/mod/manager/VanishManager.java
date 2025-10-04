package com.adminium.mod.manager;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.List;

public class VanishManager {
    private static final Set<UUID> vanishedPlayers = new HashSet<>();
    private static MinecraftServer server;
    
    public static void setServer(MinecraftServer minecraftServer) {
        server = minecraftServer;
    }
    
    public static boolean isVanished(PlayerEntity player) {
        return vanishedPlayers.contains(player.getUUID());
    }
    
    public static void setVanished(ServerPlayerEntity player, boolean vanished) {
        UUID playerUUID = player.getUUID();
        
        if (vanished) {
            if (!vanishedPlayers.contains(playerUUID)) {
                vanishedPlayers.add(playerUUID);
                hidePlayerFromOthers(player);
            }
        } else {
            if (vanishedPlayers.contains(playerUUID)) {
                vanishedPlayers.remove(playerUUID);
                
                // Use a simple delayed execution to ensure proper unvanish
                server.execute(() -> {
                    showPlayerToOthers(player);

                    // Additional delay to ensure all packets are processed
                    server.execute(() -> {
                        // Final resync to ensure visibility
                        finalizePlayerVisibility(player);

                        // Rebuild scoreboard prefixes just in case
                        com.adminium.mod.Adminium.refreshAllPlayersDisplayNames(server);
                    });
                });
            }
        }
    }
    
    private static void hidePlayerFromOthers(ServerPlayerEntity vanishedPlayer) {
        List<ServerPlayerEntity> allPlayers = server.getPlayerList().getPlayers();
        
        for (ServerPlayerEntity otherPlayer : allPlayers) {
            if (otherPlayer != vanishedPlayer) {
                // Remove from tab list
                otherPlayer.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(vanishedPlayer.getUUID())));
                
                // Remove entity from world
                otherPlayer.connection.send(new ClientboundRemoveEntitiesPacket(vanishedPlayer.getId()));
                
                // Clear all equipment (held items, armor, etc.) for this player
                clearPlayerEquipment(otherPlayer, vanishedPlayer);
            }
        }
    }
    
    private static void showPlayerToOthers(ServerPlayerEntity unvanishedPlayer) {
        List<ServerPlayerEntity> allPlayers = server.getPlayerList().getPlayers();
        
        for (ServerPlayerEntity otherPlayer : allPlayers) {
            if (otherPlayer != unvanishedPlayer) {
                // Add back to tab list
                otherPlayer.connection.send(new ClientboundPlayerInfoUpdatePacket(
                    ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, unvanishedPlayer));

                // Ensure listed flag is restored
                otherPlayer.connection.send(new ClientboundPlayerInfoUpdatePacket(
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, unvanishedPlayer));

                // Ensure the custom display name (scoreboard prefix) is applied
                otherPlayer.connection.send(new ClientboundPlayerInfoUpdatePacket(
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, unvanishedPlayer));
                
                // Re-add the player entity
                otherPlayer.connection.send(new ClientboundAddPlayerPacket(unvanishedPlayer));
                
                // Send player's current position and rotation
                otherPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket(unvanishedPlayer));
                otherPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundRotateHeadPacket(
                    unvanishedPlayer, (byte) (unvanishedPlayer.getYHeadRot() * 256.0F / 360.0F)));
                
                // Send entity data
                otherPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(
                    unvanishedPlayer.getId(), unvanishedPlayer.getEntityData().getNonDefaultValues()));
                
                // Restore equipment
                restorePlayerEquipment(otherPlayer, unvanishedPlayer);
            }
        }

        // Also broadcast to everyone (including the unvanished player) to guarantee tab-list re-add
        server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(
            ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, unvanishedPlayer));
        server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, unvanishedPlayer));
        server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, unvanishedPlayer));
    }
    
    public static void onPlayerJoin(ServerPlayerEntity joiningPlayer) {
        // Hide all vanished players from the joining player (if they're not an op)
        for (UUID vanishedUUID : vanishedPlayers) {
            ServerPlayerEntity vanishedPlayer = server.getPlayerList().getPlayer(vanishedUUID);
            if (vanishedPlayer != null && joiningPlayer != vanishedPlayer) {
                joiningPlayer.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(vanishedUUID)));
                joiningPlayer.connection.send(new ClientboundRemoveEntitiesPacket(vanishedPlayer.getId()));
                clearPlayerEquipment(joiningPlayer, vanishedPlayer);
            }
        }
    }
    
    public static void onPlayerLeave(ServerPlayerEntity leavingPlayer) {
        // Clean up if the leaving player was vanished
        vanishedPlayers.remove(leavingPlayer.getUUID());
    }
    
    public static Set<UUID> getVanishedPlayers() {
        return new HashSet<>(vanishedPlayers);
    }
    
    public static void onPlayerEquipmentChange(ServerPlayerEntity player) {
        // If the player is vanished, prevent equipment updates from being sent to non-ops
        if (isVanished(player)) {
            List<ServerPlayerEntity> allPlayers = server.getPlayerList().getPlayers();
            
            for (ServerPlayerEntity otherPlayer : allPlayers) {
                if (otherPlayer != player) {
                    // Keep sending empty equipment to maintain invisibility
                    clearPlayerEquipment(otherPlayer, player);
                }
            }
        }
    }
    
    private static void clearPlayerEquipment(ServerPlayerEntity viewer, ServerPlayerEntity vanishedPlayer) {
        // Send empty equipment packets to hide all items/armor
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            viewer.connection.send(new ClientboundSetEquipmentPacket(
                vanishedPlayer.getId(), 
                List.of(com.mojang.datafixers.util.Pair.of(slot, ItemStack.EMPTY))
            ));
        }
    }
    
    private static void restorePlayerEquipment(ServerPlayerEntity viewer, ServerPlayerEntity unvanishedPlayer) {
        // Send actual equipment packets to restore visibility
        java.util.List<com.mojang.datafixers.util.Pair<EquipmentSlot, ItemStack>> equipment = 
            new java.util.ArrayList<>();
        
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack item = unvanishedPlayer.getItemBySlot(slot);
            if (!item.isEmpty()) {
                equipment.add(com.mojang.datafixers.util.Pair.of(slot, item));
            }
        }
        
        if (!equipment.isEmpty()) {
            viewer.connection.send(new ClientboundSetEquipmentPacket(
                unvanishedPlayer.getId(), 
                equipment
            ));
        }
    }
    
    /**
     * Final step to ensure player visibility is completely restored
     */
    private static void finalizePlayerVisibility(ServerPlayerEntity player) {
        // Refresh display name and tab list
        player.refreshDisplayName();
        server.getPlayerList().broadcastAll(
            new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(
                net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                player
            )
        );
        
        // Send a final position update to ensure the player is properly synced
        List<ServerPlayerEntity> allPlayers = server.getPlayerList().getPlayers();
        for (ServerPlayerEntity otherPlayer : allPlayers) {
            if (otherPlayer != player) {
                // Final position sync
                otherPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket(player));
                
                // Final equipment sync
                restorePlayerEquipment(otherPlayer, player);
            }
        }
    }

    /**
     * Force a complete player resync - useful for fixing visibility issues
     */
    public static void forcePlayerResync(ServerPlayerEntity player) {
        List<ServerPlayerEntity> allPlayers = server.getPlayerList().getPlayers();
        
        for (ServerPlayerEntity otherPlayer : allPlayers) {
            if (otherPlayer != player) {
                // Remove and re-add to tab list
                otherPlayer.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID())));
                otherPlayer.connection.send(new ClientboundPlayerInfoUpdatePacket(
                    ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, player));
                
                // Remove and re-add entity
                otherPlayer.connection.send(new ClientboundRemoveEntitiesPacket(player.getId()));
                otherPlayer.connection.send(new ClientboundAddPlayerPacket(player));
                
                // Send position and rotation
                otherPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket(player));
                otherPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundRotateHeadPacket(
                    player, (byte) (player.getYHeadRot() * 256.0F / 360.0F)));
                
                // Send entity data
                otherPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(
                    player.getId(), player.getEntityData().getNonDefaultValues()));
                
                // Restore equipment
                restorePlayerEquipment(otherPlayer, player);
            }
        }
    }
} 