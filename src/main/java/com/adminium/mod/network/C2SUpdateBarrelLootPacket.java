package com.adminium.mod.network;

import com.adminium.mod.manager.BarrelLootManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.function.Supplier;

public class C2SUpdateBarrelLootPacket {
    public enum Action {
        ADD,
        UPDATE,
        REMOVE,
        CLEAR_ALL
    }

    private final Action action;
    private final Identifier itemId;
    private final double dropChance;
    private final int minCount;
    private final int maxCount;

    public C2SUpdateBarrelLootPacket(Action action, Identifier itemId, double dropChance, int minCount, int maxCount) {
        this.action = action;
        this.itemId = itemId;
        this.dropChance = dropChance;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }

    public C2SUpdateBarrelLootPacket(Action action) {
        this(action, null, 0, 1, 1);
    }

    public static void encode(C2SUpdateBarrelLootPacket packet, PacketByteBuf buffer) {
        buffer.writeEnum(packet.action);
        if (packet.action != Action.CLEAR_ALL && packet.itemId != null) {
            buffer.writeResourceLocation(packet.itemId);
            buffer.writeDouble(packet.dropChance);
            buffer.writeInt(packet.minCount);
            buffer.writeInt(packet.maxCount);
        }
    }

    public static C2SUpdateBarrelLootPacket decode(PacketByteBuf buffer) {
        Action action = buffer.readEnum(Action.class);
        if (action == Action.CLEAR_ALL) {
            return new C2SUpdateBarrelLootPacket(action);
        }
        Identifier itemId = buffer.readResourceLocation();
        double dropChance = buffer.readDouble();
        int minCount = buffer.readInt();
        int maxCount = buffer.readInt();
        return new C2SUpdateBarrelLootPacket(action, itemId, dropChance, minCount, maxCount);
    }

    public static void handle(C2SUpdateBarrelLootPacket packet, Object ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null && player.hasPermissions(2)) {
                switch (packet.action) {
                    case ADD:
                    case UPDATE:
                        if (packet.itemId != null) {
                            BarrelLootManager.addOrUpdateLootEntry(packet.itemId, packet.dropChance, packet.minCount, packet.maxCount);
                            player.sendSystemMessage(Text.literal("Updated barrel loot: " + packet.itemId));
                        }
                        break;
                    case REMOVE:
                        if (packet.itemId != null) {
                            BarrelLootManager.removeLootEntry(packet.itemId);
                            player.sendSystemMessage(Text.literal("Removed barrel loot: " + packet.itemId));
                        }
                        break;
                    case CLEAR_ALL:
                        BarrelLootManager.clearLootTable();
                        player.sendSystemMessage(Text.literal("Cleared all barrel loot entries"));
                        break;
                }

                // Send updated loot table back to client
                ModNetworking.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new S2COpenBarrelConfigPacket(BarrelLootManager.getLootTable())
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }
}