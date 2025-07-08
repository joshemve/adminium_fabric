package com.adminium.command;

import com.adminium.manager.SafeManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SafeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> safeCommand = Commands.literal("safe")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("on")
                        .executes(context -> setSafe(context.getSource(), context.getSource().getPlayerOrException(), true))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> setSafe(context.getSource(), EntityArgument.getPlayer(context, "player"), true))
                        )
                )
                .then(Commands.literal("off")
                        .executes(context -> setSafe(context.getSource(), context.getSource().getPlayerOrException(), false))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> setSafe(context.getSource(), EntityArgument.getPlayer(context, "player"), false))
                        )
                );
        dispatcher.register(safeCommand);
    }

    private static int setSafe(CommandSourceStack source, ServerPlayer player, boolean isSafe) {
        SafeManager.setSafe(player.getUUID(), isSafe);
        if (isSafe) {
            source.sendSuccess(() -> Component.literal("Safe mode enabled for " + player.getName().getString()), true);
            player.sendSystemMessage(Component.literal("Safe mode has been enabled for you. You will not die from damage."));
        } else {
            source.sendSuccess(() -> Component.literal("Safe mode disabled for " + player.getName().getString()), true);
            player.sendSystemMessage(Component.literal("Safe mode has been disabled for you."));
        }
        return 1;
    }
} 