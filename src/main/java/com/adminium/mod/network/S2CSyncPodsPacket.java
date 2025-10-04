package com.adminium.mod.network;

import com.adminium.mod.manager.PodManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import java.util.*;
import java.util.function.Supplier;

/**
 * Server → Client packet: sends the complete list of pod locations for every dimension.
 */
public class S2CSyncPodsPacket {
    private final Map<RegistryKey<World>, List<BlockPos>> pods;

    public S2CSyncPodsPacket(Map<RegistryKey<World>, List<BlockPos>> pods) {
        this.pods = pods;
    }

    public static void encode(S2CSyncPodsPacket msg, PacketByteBuf buf) {
        buf.writeVarInt(msg.pods.size());
        for (Map.Entry<RegistryKey<World>, List<BlockPos>> entry : msg.pods.entrySet()) {
            buf.writeResourceLocation(entry.getKey().location());
            List<BlockPos> list = entry.getValue();
            buf.writeVarInt(list.size());
            for (BlockPos pos : list) {
                buf.writeInt(pos.getX());
                buf.writeInt(pos.getY());
                buf.writeInt(pos.getZ());
            }
        }
    }

    public static S2CSyncPodsPacket decode(PacketByteBuf buf) {
        int dimCount = buf.readVarInt();
        Map<RegistryKey<World>, List<BlockPos>> map = new HashMap<>();
        for (int i = 0; i < dimCount; i++) {
            Identifier dimId = buf.readResourceLocation();
            RegistryKey<World> dimension = RegistryKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimId);
            int listSize = buf.readVarInt();
            List<BlockPos> list = new ArrayList<>(listSize);
            for (int j = 0; j < listSize; j++) {
                int x = buf.readInt();
                int y = buf.readInt();
                int z = buf.readInt();
                list.add(new BlockPos(x, y, z));
            }
            map.put(dimension, list);
        }
        return new S2CSyncPodsPacket(map);
    }

    public static void handle(S2CSyncPodsPacket msg, Object ctx) {
        ctx.get().enqueueWork(() -> PodManager.clientReplacePods(msg.pods));
        ctx.get().setPacketHandled(true);
    }
} 