package com.adminium.mod.command;

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
import net.minecraft.entity.player.HungerManager;

public class HealCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("heal")
            .requires(source -> source.hasPermission(2))
            .executes(context -> healSelf(context))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> healPlayer(context, EntityArgument.getPlayer(context, "player")))
            )
        );
    }
    
    private static int healSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        
        // Restore health
        player.setHealth(player.getMaxHealth());
        
        // Restore hunger
        HungerManager foodData = player.getFoodData();
        foodData.setFoodLevel(20);
        foodData.setSaturation(20.0f);
        
        CommandFeedbackHelper.sendSuccess(context.getSource(), Text.literal("§aYou have been healed!"), false);
        return 1;
    }
    
    private static int healPlayer(CommandContext<ServerCommandSource> context, ServerPlayerEntity targetPlayer) throws CommandSyntaxException {
        ServerPlayerEntity executor = context.getSource().getPlayerOrException();
        
        // Restore health
        targetPlayer.setHealth(targetPlayer.getMaxHealth());
        
        // Restore hunger
        HungerManager foodData = targetPlayer.getFoodData();
        foodData.setFoodLevel(20);
        foodData.setSaturation(20.0f);
        
        if (targetPlayer == executor) {
            CommandFeedbackHelper.sendSuccess(context.getSource(), Text.literal("§aYou have been healed!"), false);
        } else {
            CommandFeedbackHelper.sendSuccess(context.getSource(), Text.literal("§aHealed " + targetPlayer.getName().getString() + "!"), false);
            targetPlayer.sendSystemMessage(Text.literal("§aYou have been healed by " + executor.getName().getString() + "!"));
        }
        
        return 1;
    }
} 