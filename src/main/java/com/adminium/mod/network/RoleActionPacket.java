package com.adminium.mod.network;

import com.adminium.mod.roles.RoleManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.function.Supplier;

public class RoleActionPacket {
    public enum Action {
        REMOVE_ALL,
        ASSIGN_ALL
    }
    
    private final Action action;
    
    public RoleActionPacket(Action action) {
        this.action = action;
    }
    
    public RoleActionPacket(PacketByteBuf buf) {
        this.action = buf.readEnum(Action.class);
    }
    
    public void toBytes(PacketByteBuf buf) {
        buf.writeEnum(action);
    }
    
    public boolean handle(Object ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null && player.hasPermissions(2)) {
                switch (action) {
                    case REMOVE_ALL:
                        RoleManager.removeAllRoles();
                        player.sendSystemMessage(net.minecraft.network.chat.Text.literal("§aAll roles have been removed!"));
                        break;
                    case ASSIGN_ALL:
                        RoleManager.assignRolesRandomly();
                        player.sendSystemMessage(net.minecraft.network.chat.Text.literal("§aRoles have been automatically assigned to all players!"));
                        break;
                }
                
                // Refresh all online players' display names
                for (ServerPlayerEntity onlinePlayer : player.getServer().getPlayerList().getPlayers()) {
                    onlinePlayer.refreshDisplayName();
                    
                    // Update tab list
                    player.getServer().getPlayerList().broadcastAll(
                        new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(
                            net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                            onlinePlayer
                        )
                    );
                }
            }
        });
        return true;
    }
} 