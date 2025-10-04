package com.adminium.mod.command;

import com.adminium.mod.network.ModNetworking;
import com.adminium.mod.network.S2COpenRolesScreenPacket;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

public class RolesCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("roles")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
                    ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2COpenRolesScreenPacket());
                    return 1;
                }
                context.getSource().sendFailure(Text.literal("This command can only be used by players"));
                return 0;
            })
        );
    }
} 