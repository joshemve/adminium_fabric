package com.adminium.mod.network;

import com.adminium.mod.manager.BanManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.function.Supplier;

public class RequestBanListPacket {
    
    public RequestBanListPacket() {
    }
    
    public RequestBanListPacket(PacketByteBuf buf) {
    }
    
    public void encode(PacketByteBuf buf) {
    }
    
    public static RequestBanListPacket decode(PacketByteBuf buf) {
        return new RequestBanListPacket();
    }
    
    public static void handle(RequestBanListPacket packet, Object ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null && player.hasPermissions(2)) {
                // Send ban list to the requesting player
                ModNetworking.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new BanListPacket(BanManager.getAllBans())
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }
} 