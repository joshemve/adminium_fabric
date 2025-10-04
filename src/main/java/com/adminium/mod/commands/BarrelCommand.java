package com.adminium.mod.commands;

import com.adminium.mod.manager.BarrelLootManager;
import com.adminium.mod.network.ModNetworking;
import com.adminium.mod.network.S2COpenBarrelConfigPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

public class BarrelCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> barrelCommand = Commands.literal("barrel")
            .requires(source -> source.hasPermission(2)) // Require admin permission
            .executes(context -> {
                ServerCommandSource source = context.getSource();

                if (source.getEntity() instanceof ServerPlayerEntity player) {
                    // Send packet to open barrel config UI
                    ModNetworking.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new S2COpenBarrelConfigPacket(BarrelLootManager.getLootTable())
                    );

                    source.sendSuccess(() -> Text.literal("Opening barrel loot configuration..."), false);
                } else {
                    source.sendFailure(Text.literal("This command can only be used by players"));
                }

                return 1;
            });

        dispatcher.register(barrelCommand);
    }
}