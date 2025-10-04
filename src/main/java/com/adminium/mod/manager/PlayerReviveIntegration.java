package com.adminium.mod.manager;

import com.mojang.logging.LogUtils;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;

import java.lang.reflect.Method;

public class PlayerReviveIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean playerReviveModLoaded = false;
    private static boolean integrationInitialized = false;
    
    public static void initialize() {
        if (integrationInitialized) {
            return;
        }
        
        integrationInitialized = true;
        
        // Check if PlayerRevive mod is loaded
        playerReviveModLoaded = ModList.get().isLoaded("playerrevive");
        
        if (playerReviveModLoaded) {
            LOGGER.info("PlayerRevive mod detected - InstaBan will integrate with bleed out system");
            
            // Register our event handler using reflection
            try {
                MinecraftForge.EVENT_BUS.register(new PlayerReviveEventHandler());
                LOGGER.info("PlayerRevive integration initialized successfully");
            } catch (Exception e) {
                LOGGER.error("Failed to initialize PlayerRevive integration", e);
                playerReviveModLoaded = false;
            }
        } else {
            LOGGER.info("PlayerRevive mod not detected - InstaBan will use normal death events");
        }
    }
    
    public static boolean isPlayerReviveModLoaded() {
        return playerReviveModLoaded;
    }
    
    public static class PlayerReviveEventHandler {
        
        // TODO: Convert to Fabric event
            public void onGenericEvent(net.minecraftforge.eventbus.api.Event event) {
            // Use reflection to check if this is a PlayerBleedOutEvent
            if (event.getClass().getName().equals("team.creative.playerrevive.api.event.PlayerBleedOutEvent")) {
                handlePlayerBleedOut(event);
            }
        }
        
        private void handlePlayerBleedOut(net.minecraftforge.eventbus.api.Event event) {
            try {
                // Use reflection to get the player from the event
                Method getEntityMethod = event.getClass().getMethod("getEntity");
                Object entity = getEntityMethod.invoke(event);
                
                if (entity instanceof ServerPlayerEntity) {
                    ServerPlayerEntity player = (ServerPlayerEntity) entity;
                    
                    // Handle InstaBan for actual deaths (bleed out) in hardcore mode
                    if (InstaBanManager.isInstaBanEnabled() && player.getServer().isHardcore() && !player.hasPermissions(2)) {
                        LOGGER.info("PlayerEntity {} bled out in hardcore mode with InstaBan enabled, banning player", player.getName().getString());
                        
                        // Ban the player immediately
                        BanManager.addBan(
                            player.getUUID(),
                            player.getName().getString(),
                            "Died in hardcore mode (bled out)",
                            "InstaBan System",
                            true
                        );
                        
                        LOGGER.info("PlayerEntity {} has been banned for bleeding out in hardcore mode", player.getName().getString());
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error handling PlayerBleedOutEvent", e);
            }
        }
    }
} 