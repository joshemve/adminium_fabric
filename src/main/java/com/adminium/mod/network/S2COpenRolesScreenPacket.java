package com.adminium.mod.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;

import java.util.function.Supplier;

public class S2COpenRolesScreenPacket {
    
    public S2COpenRolesScreenPacket() {
    }
    
    public S2COpenRolesScreenPacket(PacketByteBuf buf) {
    }
    
    public void encode(PacketByteBuf buf) {
    }
    
    public static S2COpenRolesScreenPacket decode(PacketByteBuf buf) {
        return new S2COpenRolesScreenPacket();
    }
    
    public static void handle(S2COpenRolesScreenPacket packet, Object ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                try {
                    // Use reflection to avoid loading client classes on server
                    Class<?> rolesScreenClass = Class.forName("com.adminium.mod.client.gui.RolesScreen");
                    Object rolesScreen = rolesScreenClass.getDeclaredConstructor().newInstance();
                    MinecraftClient.getInstance().setScreen((net.minecraft.client.gui.screens.Screen) rolesScreen);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
} 