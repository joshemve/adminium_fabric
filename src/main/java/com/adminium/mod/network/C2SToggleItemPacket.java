package com.adminium.mod.network;

import com.adminium.mod.manager.DisabledItemsManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.function.Supplier;

public class C2SToggleItemPacket {
    private final Identifier itemId;
    private final boolean disable;

    public C2SToggleItemPacket(Identifier itemId, boolean disable) {
        this.itemId = itemId;
        this.disable = disable;
    }

    public static void encode(C2SToggleItemPacket packet, PacketByteBuf buffer) {
        buffer.writeResourceLocation(packet.itemId);
        buffer.writeBoolean(packet.disable);
    }

    public static C2SToggleItemPacket decode(PacketByteBuf buffer) {
        return new C2SToggleItemPacket(buffer.readResourceLocation(), buffer.readBoolean());
    }

    public boolean handle(Object supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayerEntity player = ctx.getSender();
            if (player != null && player.hasPermissions(2)) {
                if (disable) {
                    DisabledItemsManager.disableItem(itemId);
                } else {
                    DisabledItemsManager.enableItem(itemId);
                }
                
                // Sync the updated list to all players
                DisabledItemsManager.syncToAllPlayers(player.server);
            }
        });
        return true;
    }
} 