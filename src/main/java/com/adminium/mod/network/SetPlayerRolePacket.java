package com.adminium.mod.network;

import com.adminium.mod.roles.Role;
import com.adminium.mod.roles.RoleManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.function.Supplier;

public class SetPlayerRolePacket {
    private final UUID playerId;
    private final Role role;
    
    public SetPlayerRolePacket(UUID playerId, Role role) {
        this.playerId = playerId;
        this.role = role;
    }
    
    public SetPlayerRolePacket(PacketByteBuf buf) {
        this.playerId = buf.readUUID();
        this.role = buf.readEnum(Role.class);
    }
    
    public void toBytes(PacketByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeEnum(role);
    }
    
    public boolean handle(Object supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayerEntity sender = ctx.getSender();
            if (sender != null && sender.hasPermissions(2)) {
                // Set role and trigger full refresh (RoleManager already handles saving & refresh)
                RoleManager.setPlayerRole(playerId, role);
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }
} 