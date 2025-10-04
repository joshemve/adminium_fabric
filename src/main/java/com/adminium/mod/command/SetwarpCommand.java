package com.adminium.mod.command;

import com.adminium.mod.manager.WarpManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

public class SetwarpCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("setwarp")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("name", StringArgumentType.string())
                .executes(context -> setWarp(context))
            )
        );
    }
    
    private static int setWarp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        String warpName = StringArgumentType.getString(context, "name");
        
        // Validate warp name
        if (warpName.length() > 32) {
            player.sendSystemMessage(Text.literal("§cWarp name cannot be longer than 32 characters!"));
            return 0;
        }
        
        if (!warpName.matches("[a-zA-Z0-9_-]+")) {
            player.sendSystemMessage(Text.literal("§cWarp name can only contain letters, numbers, underscores, and hyphens!"));
            return 0;
        }
        
        boolean warpExists = WarpManager.warpExists(warpName);
        WarpManager.setWarp(warpName, player);
        
        if (warpExists) {
            player.sendSystemMessage(Text.literal("§aWarp '" + warpName + "' has been updated!"));
        } else {
            player.sendSystemMessage(Text.literal("§aWarp '" + warpName + "' has been created!"));
        }
        
        // Show location info
        WarpManager.WarpLocation warp = WarpManager.getWarp(warpName);
        if (warp != null) {
            player.sendSystemMessage(Text.literal("§7Location: " + warp.pos.getX() + ", " + warp.pos.getY() + ", " + warp.pos.getZ() + " in " + warp.getDimensionName()));
        }
        
        return 1;
    }
} 