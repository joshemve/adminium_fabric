package com.adminium.mod.command;

import com.adminium.mod.manager.VanishManager;
import com.adminium.mod.util.CommandFeedbackHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.EntityArgument;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

public class VanishCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("vanish")
            .requires(source -> source.hasPermission(2))
            .executes(context -> toggleVanish(context, null))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> toggleVanish(context, EntityArgument.getPlayer(context, "player")))
            )
        );
    }
    
    private static int toggleVanish(CommandContext<ServerCommandSource> context, ServerPlayerEntity targetPlayer) throws CommandSyntaxException {
        ServerPlayerEntity executor = context.getSource().getPlayerOrException();
        ServerPlayerEntity playerToVanish = targetPlayer != null ? targetPlayer : executor;
        
        boolean isCurrentlyVanished = VanishManager.isVanished(playerToVanish);
        VanishManager.setVanished(playerToVanish, !isCurrentlyVanished);
        
        if (playerToVanish == executor) {
            // PlayerEntity is vanishing/unvanishing themselves
            if (!isCurrentlyVanished) {
                CommandFeedbackHelper.sendSuccess(context.getSource(), Text.literal("§aYou are now vanished!"), false);
            } else {
                CommandFeedbackHelper.sendSuccess(context.getSource(), Text.literal("§eYou are no longer vanished!"), false);
            }
        } else {
            // PlayerEntity is vanishing/unvanishing someone else
            if (!isCurrentlyVanished) {
                CommandFeedbackHelper.sendSuccess(context.getSource(), Text.literal("§a" + playerToVanish.getName().getString() + " is now vanished!"), false);
                playerToVanish.sendSystemMessage(Text.literal("§aYou have been vanished by " + executor.getName().getString()));
            } else {
                CommandFeedbackHelper.sendSuccess(context.getSource(), Text.literal("§e" + playerToVanish.getName().getString() + " is no longer vanished!"), false);
                playerToVanish.sendSystemMessage(Text.literal("§eYou have been unvanished by " + executor.getName().getString()));
            }
        }
        
        return 1;
    }
} 