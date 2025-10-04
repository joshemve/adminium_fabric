package com.adminium.mod.command;

import com.adminium.mod.manager.SafeManager;
import com.adminium.mod.util.CommandFeedbackHelper;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.EntityArgument;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

public class SafeCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("safe")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("on")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayerOrException();
                    SafeManager.setSafeMode(player.getUUID(), true);
                    CommandFeedbackHelper.sendSuccess(context.getSource(), Text.literal("Safe mode enabled!"), false);
                    return 1;
                })
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayerEntity target = EntityArgument.getPlayer(context, "player");
                        SafeManager.setSafeMode(target.getUUID(), true);
                        target.sendSystemMessage(Text.literal("Safe mode enabled!"));
                        CommandFeedbackHelper.sendSuccess(context.getSource(), Text.literal("Enabled safe mode for " + target.getName().getString()), false);
                        return 1;
                    })))
            .then(Commands.literal("off")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayerOrException();
                    SafeManager.setSafeMode(player.getUUID(), false);
                    CommandFeedbackHelper.sendSuccess(context.getSource(), Text.literal("Safe mode disabled!"), false);
                    return 1;
                })
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayerEntity target = EntityArgument.getPlayer(context, "player");
                        SafeManager.setSafeMode(target.getUUID(), false);
                        target.sendSystemMessage(Text.literal("Safe mode disabled!"));
                        CommandFeedbackHelper.sendSuccess(context.getSource(), Text.literal("Disabled safe mode for " + target.getName().getString()), false);
                        return 1;
                    })))
        );
    }
} 