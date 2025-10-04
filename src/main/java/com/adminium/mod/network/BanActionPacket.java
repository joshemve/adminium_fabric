package com.adminium.mod.network;

import com.adminium.mod.manager.BanManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.function.Supplier;

public class BanActionPacket {
    private final UUID playerUUID;
    
    public BanActionPacket(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }
    
    public BanActionPacket(PacketByteBuf buf) {
        this.playerUUID = buf.readUUID();
    }
    
    public void encode(PacketByteBuf buf) {
        buf.writeUUID(playerUUID);
    }
    
    public static BanActionPacket decode(PacketByteBuf buf) {
        return new BanActionPacket(buf);
    }
    
    public static void handle(BanActionPacket packet, Object ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null && player.hasPermissions(2)) {
                BanManager.BanInfo banInfo = BanManager.getBanInfo(packet.playerUUID);
                String playerName = banInfo != null ? banInfo.playerName : packet.playerUUID.toString();
                
                BanManager.pardonPlayer(packet.playerUUID, false);
                
                // Send feedback to the operator
                if (banInfo != null && banInfo.isHardcoreDeath) {
                    player.sendSystemMessage(net.minecraft.network.chat.Text.literal(
                        "§aPlayer " + playerName + " has been pardoned! They will automatically be in survival mode when InstaBan is enabled."));
                } else {
                    player.sendSystemMessage(net.minecraft.network.chat.Text.literal(
                        "§aPlayer " + playerName + " has been pardoned!"));
                }

                // Push updated ban list to the operator's GUI
                ModNetworking.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new BanListPacket(BanManager.getAllBans())
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }
}