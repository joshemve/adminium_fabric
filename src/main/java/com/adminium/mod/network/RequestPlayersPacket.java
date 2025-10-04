package com.adminium.mod.network;

import com.adminium.mod.roles.RoleManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.players.PlayerList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class RequestPlayersPacket {
    
    public RequestPlayersPacket() {}
    
    public RequestPlayersPacket(PacketByteBuf buf) {}
    
    public void toBytes(PacketByteBuf buf) {}
    
    public boolean handle(Object supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayerEntity sender = ctx.getSender();
            if (sender != null && sender.hasPermissions(2)) {
                MinecraftServer server = sender.getServer();
                if (server != null) {
                    // Get all players who have joined the server
                    List<PlayerListPacket.PlayerInfo> playerInfos = new ArrayList<>();
                    
                    // Get online players
                    PlayerList playerList = server.getPlayerList();
                    for (ServerPlayerEntity player : playerList.getPlayers()) {
                        GameProfile profile = player.getGameProfile();
                        playerInfos.add(new PlayerListPacket.PlayerInfo(
                            profile.getId(),
                            profile.getName(),
                            RoleManager.getPlayerRole(profile.getId())
                        ));
                    }
                    
                    // Also include offline players from role data
                    for (UUID playerId : RoleManager.getAllPlayersWithRoles()) {
                        boolean alreadyAdded = playerInfos.stream()
                            .anyMatch(info -> info.uuid.equals(playerId));
                        
                        if (!alreadyAdded) {
                            // Try to get player name from server's user cache
                            GameProfile profile = server.getProfileCache().get(playerId).orElse(null);
                            if (profile != null) {
                                playerInfos.add(new PlayerListPacket.PlayerInfo(
                                    playerId,
                                    profile.getName(),
                                    RoleManager.getPlayerRole(playerId)
                                ));
                            }
                        }
                    }
                    
                    // Send the list back to the client
                    ModNetworking.sendToPlayer(new PlayerListPacket(playerInfos), sender);
                }
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }
} 