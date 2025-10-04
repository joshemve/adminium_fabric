package com.adminium.mod.manager;

import com.mojang.logging.LogUtils;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import org.slf4j.Logger;

public class InstaBanManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean instaBanEnabled = false;
    
    public static void setInstaBanEnabled(boolean enabled) {
        instaBanEnabled = enabled;
        LOGGER.info("InstaBan mode has been " + (enabled ? "enabled" : "disabled"));
    }
    
    public static boolean isInstaBanEnabled() {
        return instaBanEnabled;
    }
    
    public static void handlePlayerDeath(ServerPlayerEntity player) {
        if (!instaBanEnabled) {
            return;
        }
        
        // Never ban operators
        if (player.hasPermissions(2)) {
            LOGGER.info("PlayerEntity {} died but is an operator, skipping InstaBan", player.getName().getString());
            return;
        }
        
        // Check if the world is in hardcore mode
        if (player.getServer().isHardcore()) {
            // With PlayerRevive mod, this is just a knockdown, not actual death
            // The actual death/ban happens in PlayerBleedOutEvent
            LOGGER.info("PlayerEntity {} was knocked down in hardcore mode with InstaBan enabled (can still be revived)", player.getName().getString());
            
            // Don't ban here - let the PlayerRevive mod handle the knockdown/revival process
            // The ban will happen in PlayerBleedOutEvent if they actually bleed out
        }
    }
    
    public static void handlePlayerRespawn(ServerPlayerEntity player) {
        if (!instaBanEnabled) {
            return;
        }
        
        // If InstaBan is enabled and this is a hardcore world, 
        // we shouldn't reach this point as the player should be banned on death
        if (player.getServer().isHardcore()) {
            // Double-check: if somehow a player is trying to respawn in hardcore with InstaBan on, ban them
            if (player.gameMode.getGameModeForPlayer() == GameMode.SPECTATOR) {
                LOGGER.warn("PlayerEntity {} somehow reached spectator mode in hardcore with InstaBan enabled, banning now", player.getName().getString());
                
                BanManager.addBan(
                    player.getUUID(),
                    player.getName().getString(),
                    "Attempted to respawn in hardcore mode",
                    "InstaBan System",
                    true
                );
                
                player.connection.disconnect(net.minecraft.network.chat.Text.literal(
                    "§cInstaBan is enabled in hardcore mode!\n§7You have been banned but can be pardoned by an operator."
                ));
            }
        }
    }
} 