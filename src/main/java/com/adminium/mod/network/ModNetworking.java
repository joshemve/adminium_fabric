package com.adminium.mod.network;

import com.adminium.mod.Adminium;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;

public class ModNetworking {
    // Define packet identifiers
    public static final Identifier TOGGLE_ITEM_PACKET = new Identifier(Adminium.MODID, "toggle_item");
    public static final Identifier SYNC_DISABLED_ITEMS_PACKET = new Identifier(Adminium.MODID, "sync_disabled_items");
    public static final Identifier OPEN_ITEMS_SCREEN_PACKET = new Identifier(Adminium.MODID, "open_items_screen");
    public static final Identifier OPEN_ROLES_SCREEN_PACKET = new Identifier(Adminium.MODID, "open_roles_screen");
    public static final Identifier ROLE_ACTION_PACKET = new Identifier(Adminium.MODID, "role_action");
    public static final Identifier SET_ROLE_ICON_PACKET = new Identifier(Adminium.MODID, "set_role_icon");
    public static final Identifier REQUEST_PLAYERS_PACKET = new Identifier(Adminium.MODID, "request_players");
    public static final Identifier PLAYER_LIST_PACKET = new Identifier(Adminium.MODID, "player_list");
    public static final Identifier SET_PLAYER_ROLE_PACKET = new Identifier(Adminium.MODID, "set_player_role");
    public static final Identifier REQUEST_BAN_LIST_PACKET = new Identifier(Adminium.MODID, "request_ban_list");
    public static final Identifier BAN_LIST_PACKET = new Identifier(Adminium.MODID, "ban_list");
    public static final Identifier BAN_ACTION_PACKET = new Identifier(Adminium.MODID, "ban_action");
    public static final Identifier SYNC_PODS_PACKET = new Identifier(Adminium.MODID, "sync_pods");
    public static final Identifier OPEN_BAN_MANAGEMENT_SCREEN_PACKET = new Identifier(Adminium.MODID, "open_ban_screen");
    public static final Identifier OPEN_BARREL_CONFIG_PACKET = new Identifier(Adminium.MODID, "open_barrel_config");
    public static final Identifier UPDATE_BARREL_LOOT_PACKET = new Identifier(Adminium.MODID, "update_barrel_loot");

    public static void register() {
        // Server-side packet receivers will be registered here
        // Example:
        // ServerPlayNetworking.registerGlobalReceiver(TOGGLE_ITEM_PACKET, (server, player, handler, buf, responseSender) -> {
        //     // Handle packet
        // });
    }

    public static void registerClient() {
        // Client-side packet receivers will be registered here
        // This should be called from the client mod initializer
        // Example:
        // ClientPlayNetworking.registerGlobalReceiver(SYNC_DISABLED_ITEMS_PACKET, (client, handler, buf, responseSender) -> {
        //     // Handle packet
        // });
    }

    // Utility methods for sending packets
    public static void sendToPlayer(ServerPlayerEntity player, Identifier packetId, net.minecraft.network.PacketByteBuf buf) {
        ServerPlayNetworking.send(player, packetId, buf);
    }

    public static void sendToServer(Identifier packetId, net.minecraft.network.PacketByteBuf buf) {
        ClientPlayNetworking.send(packetId, buf);
    }
}