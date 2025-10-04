package com.adminium.mod.network;

import com.adminium.mod.roles.Role;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class PlayerListPacket {
    private final List<PlayerInfo> players;
    
    public PlayerListPacket(List<PlayerInfo> players) {
        this.players = players;
    }
    
    public PlayerListPacket(PacketByteBuf buf) {
        int size = buf.readInt();
        this.players = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUUID();
            String name = buf.readUtf();
            Role role = null;
            if (buf.readBoolean()) {
                role = buf.readEnum(Role.class);
            }
            players.add(new PlayerInfo(uuid, name, role));
        }
    }
    
    public void toBytes(PacketByteBuf buf) {
        buf.writeInt(players.size());
        for (PlayerInfo info : players) {
            buf.writeUUID(info.uuid);
            buf.writeUtf(info.name);
            buf.writeBoolean(info.role != null);
            if (info.role != null) {
                buf.writeEnum(info.role);
            }
        }
    }
    
    public static void handle(PlayerListPacket packet, Object supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                try {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    Class<?> playerManagementScreenClass = Class.forName("com.adminium.mod.client.gui.PlayerManagementScreen");
                    
                    if (playerManagementScreenClass.isInstance(mc.screen)) {
                        Object screen = mc.screen;
                        List<Object> entries = new ArrayList<>();
                        
                        Class<?> playerEntryClass = Class.forName("com.adminium.mod.client.gui.PlayerManagementScreen$PlayerEntry");
                        
                        for (PlayerInfo info : packet.players) {
                            GameProfile profile = new GameProfile(info.uuid, info.name);
                            Object entry = playerEntryClass.getDeclaredConstructor(GameProfile.class, Role.class).newInstance(profile, info.role);
                            entries.add(entry);
                        }
                        
                        // Call updatePlayerList method via reflection
                        playerManagementScreenClass.getMethod("updatePlayerList", List.class).invoke(screen, entries);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
        ctx.setPacketHandled(true);
    }
    
    public static class PlayerInfo {
        public final UUID uuid;
        public final String name;
        public final Role role;
        
        public PlayerInfo(UUID uuid, String name, Role role) {
            this.uuid = uuid;
            this.name = name;
            this.role = role;
        }
    }
} 