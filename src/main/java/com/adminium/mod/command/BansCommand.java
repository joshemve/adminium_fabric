package com.adminium.mod.command;

import com.adminium.mod.network.ModNetworking;
import com.adminium.mod.network.S2COpenBanManagementScreenPacket;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

public class BansCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("bans")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                ServerCommandSource source = context.getSource();
                if (source.getEntity() instanceof ServerPlayerEntity player) {
                    // Send packet to open ban management GUI on client
                    ModNetworking.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new S2COpenBanManagementScreenPacket()
                    );
                    return 1;
                } else {
                    source.sendFailure(Text.literal("This command can only be used by players!"));
                    return 0;
                }
            })
        );
    }
} 