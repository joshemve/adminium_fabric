package com.adminium.mod.network;

import com.adminium.mod.roles.Role;
import com.adminium.mod.roles.RoleManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.Item;

import java.util.function.Supplier;

public class SetRoleIconPacket {
    private final Role role;
    private final Identifier itemId;
    
    public SetRoleIconPacket(Role role, Item item) {
        this.role = role;
        this.itemId = ForgeRegistries.ITEMS.getKey(item);
    }
    
    public SetRoleIconPacket(PacketByteBuf buf) {
        this.role = buf.readEnum(Role.class);
        this.itemId = buf.readResourceLocation();
    }
    
    public void toBytes(PacketByteBuf buf) {
        buf.writeEnum(role);
        buf.writeResourceLocation(itemId);
    }
    
    public boolean handle(Object ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null && player.hasPermissions(2)) {
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                if (item != null) {
                    RoleManager.setRoleIcon(role, item);
                    player.sendSystemMessage(net.minecraft.network.chat.Text.literal(
                        "§aSet " + role.getDisplayName() + " icon to " + item.getDescription().getString()));
                }
            }
        });
        return true;
    }
} 