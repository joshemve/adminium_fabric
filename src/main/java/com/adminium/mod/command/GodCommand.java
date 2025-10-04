package com.adminium.mod.command;

import com.adminium.mod.manager.GodManager;
import com.adminium.mod.util.CommandFeedbackHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.EntityArgument;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

public class GodCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("god")
            .requires(source -> source.hasPermission(2))
            .executes(context -> toggleGodSelf(context))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> toggleGodPlayer(context, EntityArgument.getPlayer(context, "player")))
            )
        );
    }
    
    private static int toggleGodSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        ServerCommandSource source = context.getSource();
        
        boolean newGodMode = !GodManager.isInGodMode(player.getUUID());
        GodManager.setGodMode(player.getUUID(), newGodMode);
        
        if (newGodMode) {
            CommandFeedbackHelper.sendSuccess(source, Text.literal("§aGod mode enabled! You are now invulnerable to damage."), false);
        } else {
            CommandFeedbackHelper.sendSuccess(source, Text.literal("§cGod mode disabled! You can now take damage."), false);
        }
        
        return 1;
    }
    
    private static int toggleGodPlayer(CommandContext<ServerCommandSource> context, ServerPlayerEntity targetPlayer) throws CommandSyntaxException {
        ServerPlayerEntity executor = context.getSource().getPlayerOrException();
        ServerCommandSource source = context.getSource();
        
        boolean newGodMode = !GodManager.isInGodMode(targetPlayer.getUUID());
        GodManager.setGodMode(targetPlayer.getUUID(), newGodMode);
        
        if (targetPlayer == executor) {
            if (newGodMode) {
                CommandFeedbackHelper.sendSuccess(source, Text.literal("§aGod mode enabled! You are now invulnerable to damage."), false);
            } else {
                CommandFeedbackHelper.sendSuccess(source, Text.literal("§cGod mode disabled! You can now take damage."), false);
            }
        } else {
            if (newGodMode) {
                CommandFeedbackHelper.sendSuccess(source, Text.literal("§aEnabled god mode for " + targetPlayer.getName().getString() + "!"), false);
                targetPlayer.sendSystemMessage(Text.literal("§aGod mode enabled by " + executor.getName().getString() + "! You are now invulnerable to damage."));
            } else {
                CommandFeedbackHelper.sendSuccess(source, Text.literal("§cDisabled god mode for " + targetPlayer.getName().getString() + "!"), false);
                targetPlayer.sendSystemMessage(Text.literal("§cGod mode disabled by " + executor.getName().getString() + "! You can now take damage."));
            }
        }
        
        return 1;
    }
} 