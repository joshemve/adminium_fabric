package com.adminium.mod.network;

import com.adminium.mod.manager.BarrelLootManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class S2COpenBarrelConfigPacket {
    private final Map<Identifier, BarrelLootManager.BarrelLootEntry> lootTable;

    public S2COpenBarrelConfigPacket(Map<Identifier, BarrelLootManager.BarrelLootEntry> lootTable) {
        this.lootTable = new HashMap<>(lootTable);
    }

    public static void encode(S2COpenBarrelConfigPacket packet, PacketByteBuf buffer) {
        buffer.writeInt(packet.lootTable.size());
        for (Map.Entry<Identifier, BarrelLootManager.BarrelLootEntry> entry : packet.lootTable.entrySet()) {
            buffer.writeResourceLocation(entry.getKey());
            BarrelLootManager.BarrelLootEntry lootEntry = entry.getValue();
            buffer.writeDouble(lootEntry.getDropChance());
            buffer.writeInt(lootEntry.getMinCount());
            buffer.writeInt(lootEntry.getMaxCount());
        }
    }

    public static S2COpenBarrelConfigPacket decode(PacketByteBuf buffer) {
        int size = buffer.readInt();
        Map<Identifier, BarrelLootManager.BarrelLootEntry> lootTable = new HashMap<>();
        for (int i = 0; i < size; i++) {
            Identifier itemId = buffer.readResourceLocation();
            double chance = buffer.readDouble();
            int minCount = buffer.readInt();
            int maxCount = buffer.readInt();
            lootTable.put(itemId, new BarrelLootManager.BarrelLootEntry(itemId, chance, minCount, maxCount));
        }
        return new S2COpenBarrelConfigPacket(lootTable);
    }

    public static void handle(S2COpenBarrelConfigPacket packet, Object ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                try {
                    Class<?> clientClass = Class.forName("com.adminium.mod.client.ClientBarrelHandler");
                    clientClass.getMethod("openBarrelConfig", Map.class).invoke(null, packet.lootTable);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}