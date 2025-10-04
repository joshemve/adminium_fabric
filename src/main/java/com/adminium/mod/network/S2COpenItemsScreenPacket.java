package com.adminium.mod.network;

import com.adminium.mod.client.ClientPacketHandler;
import net.minecraft.network.PacketByteBuf;

import java.util.function.Supplier;

public class S2COpenItemsScreenPacket {
    
    public S2COpenItemsScreenPacket() {
    }

    public static void encode(S2COpenItemsScreenPacket packet, PacketByteBuf buffer) {
        // No data to encode
    }

    public static S2COpenItemsScreenPacket decode(PacketByteBuf buffer) {
        return new S2COpenItemsScreenPacket();
    }

    public static void handle(S2COpenItemsScreenPacket packet, Object ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> 
                ClientPacketHandler.openItemsScreen()
            );
        });
        ctx.get().setPacketHandled(true);
    }
} 