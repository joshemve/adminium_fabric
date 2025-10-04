package com.adminium.mod.client;

import com.adminium.mod.client.gui.BanManagementScreen;
import com.adminium.mod.client.gui.ItemsScreen;
import com.adminium.mod.client.gui.RolesScreen;
import com.adminium.mod.roles.Role;
import com.adminium.mod.roles.RoleManager;
import com.adminium.mod.team.Team;
import com.adminium.mod.team.TeamManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

public class ClientPacketHandler {
    
    public static void openBanManagementScreen() {
        MinecraftClient.getInstance().setScreen(new BanManagementScreen());
    }
    
    public static void openItemsScreen() {
        MinecraftClient.getInstance().setScreen(new ItemsScreen());
    }
    
    public static void openRolesScreen() {
        MinecraftClient.getInstance().setScreen(new RolesScreen());
    }
    
    // TODO: Convert to Fabric event
        public static void onRenderNameTag(RenderNameTagEvent event) {
        if (event.getEntity() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getEntity();
            
            // Get role and team information
            Role playerRole = RoleManager.getPlayerRole(player.getUUID());
            Team playerTeam = TeamManager.getPlayerTeam(player.getUUID());
            
            if (playerRole != null || playerTeam != null) {
                MutableText displayName = Text.empty();
                
                // Add role icon if present
                if (playerRole != null) {
                    Text roleIcon = RoleManager.getPlayerRolePrefixComponent(player.getUUID());
                    if (roleIcon != null) {
                        displayName.append(roleIcon).append(" ");
                    }
                }
                
                // Add team prefix if present
                if (playerTeam != null) {
                    String teamPrefix = TeamManager.getPlayerTeamPrefix(player.getUUID());
                    if (!teamPrefix.isEmpty()) {
                        displayName.append(Text.literal(teamPrefix + " "));
                    }
                }
                
                // Add player name
                displayName.append(player.getName());
                
                // Set the modified name
                event.setContent(displayName);
            }
        }
    }
} 