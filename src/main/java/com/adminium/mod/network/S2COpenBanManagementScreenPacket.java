package com.adminium.mod.network;

import net.minecraft.network.PacketByteBuf;

public class S2COpenBanManagementScreenPacket {

    public S2COpenBanManagementScreenPacket() {
    }

    public S2COpenBanManagementScreenPacket(PacketByteBuf buf) {
    }

    public void encode(PacketByteBuf buf) {
    }

    public static S2COpenBanManagementScreenPacket decode(PacketByteBuf buf) {
        return new S2COpenBanManagementScreenPacket();
    }

    // Client-side handling will be done through Fabric's networking API
    // This needs to be registered in the client mod initializer
}