package com.adminium.mod.network;

import com.adminium.mod.manager.BanManager;
import net.minecraft.network.PacketByteBuf;

import java.util.*;

public class BanListPacket {
    private final Map<UUID, BanManager.BanInfo> bans;

    public BanListPacket(Map<UUID, BanManager.BanInfo> bans) {
        this.bans = bans;
    }

    public BanListPacket(PacketByteBuf buf) {
        int size = buf.readVarInt();
        this.bans = new HashMap<>();
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUuid();
            String name = buf.readString();
            String reason = buf.readString();
            String source = buf.readString();
            Date created = buf.readBoolean() ? new Date(buf.readLong()) : null;
            Date expires = buf.readBoolean() ? new Date(buf.readLong()) : null;

            BanManager.BanInfo info = new BanManager.BanInfo(name, reason, source, created, expires);
            this.bans.put(uuid, info);
        }
    }

    public void encode(PacketByteBuf buf) {
        buf.writeVarInt(bans.size());
        for (Map.Entry<UUID, BanManager.BanInfo> entry : bans.entrySet()) {
            buf.writeUuid(entry.getKey());
            BanManager.BanInfo info = entry.getValue();
            buf.writeString(info.playerName != null ? info.playerName : "");
            buf.writeString(info.reason != null ? info.reason : "");
            buf.writeString(info.source != null ? info.source : "");

            buf.writeBoolean(info.created != null);
            if (info.created != null) {
                buf.writeLong(info.created.getTime());
            }

            buf.writeBoolean(info.expires != null);
            if (info.expires != null) {
                buf.writeLong(info.expires.getTime());
            }
        }
    }

    public static BanListPacket decode(PacketByteBuf buf) {
        return new BanListPacket(buf);
    }

    public Map<UUID, BanManager.BanInfo> getBans() {
        return bans;
    }
}