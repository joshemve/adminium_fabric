package com.adminium.mod.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class S2CSyncDisabledItemsPacket {
    private final Set<Identifier> disabledItems;

    public S2CSyncDisabledItemsPacket(Set<Identifier> disabledItems) {
        this.disabledItems = new HashSet<>(disabledItems);
    }

    public static void encode(S2CSyncDisabledItemsPacket packet, PacketByteBuf buffer) {
        buffer.writeInt(packet.disabledItems.size());
        for (Identifier item : packet.disabledItems) {
            buffer.writeResourceLocation(item);
        }
    }

    public static S2CSyncDisabledItemsPacket decode(PacketByteBuf buffer) {
        int size = buffer.readInt();
        Set<Identifier> items = new HashSet<>();
        for (int i = 0; i < size; i++) {
            items.add(buffer.readResourceLocation());
        }
        return new S2CSyncDisabledItemsPacket(items);
    }

    public static void handle(S2CSyncDisabledItemsPacket packet, Object ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                try {
                    Class<?> clientDataClass = Class.forName("com.adminium.mod.client.ClientDisabledItemsData");
                    clientDataClass.getMethod("setDisabledItems", Set.class).invoke(null, packet.disabledItems);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
} 