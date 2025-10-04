package com.adminium.mod.command;

import com.adminium.mod.manager.WarpManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

public class WarpCommand {
    private static final SuggestionProvider<ServerCommandSource> WARP_SUGGESTIONS = 
        (context, builder) -> SharedSuggestionProvider.suggest(WarpManager.getWarpNames(), builder);
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("warp")
            .requires(source -> source.hasPermission(2))
            .executes(context -> listWarps(context))
            .then(Commands.argument("name", StringArgumentType.string())
                .suggests(WARP_SUGGESTIONS)
                .executes(context -> teleportToWarp(context))
            )
        );
    }
    
    private static int listWarps(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        
        if (WarpManager.getWarpNames().isEmpty()) {
            player.sendSystemMessage(Text.literal("§eNo warps have been created yet!"));
            return 0;
        }
        
        player.sendSystemMessage(Text.literal("§6Available warps:"));
        for (String warpName : WarpManager.getWarpNames()) {
            WarpManager.WarpLocation warp = WarpManager.getWarp(warpName);
            if (warp != null) {
                player.sendSystemMessage(Text.literal("§7- " + warpName + " §8(" + 
                    warp.pos.getX() + ", " + warp.pos.getY() + ", " + warp.pos.getZ() + 
                    " in " + warp.getDimensionName() + ")"));
            }
        }
        
        return 1;
    }
    
    private static int teleportToWarp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        String warpName = StringArgumentType.getString(context, "name");
        
        if (!WarpManager.warpExists(warpName)) {
            player.sendSystemMessage(Text.literal("§cWarp '" + warpName + "' does not exist!"));
            player.sendSystemMessage(Text.literal("§7Use /warp to see available warps."));
            return 0;
        }
        
        WarpManager.WarpLocation warp = WarpManager.getWarp(warpName);
        if (warp == null) {
            player.sendSystemMessage(Text.literal("§cError: Warp data is corrupted!"));
            return 0;
        }
        
        boolean success = WarpManager.teleportToWarp(warpName, player);
        if (success) {
            player.sendSystemMessage(Text.literal("§aTeleported to warp '" + warpName + "'!"));
            player.sendSystemMessage(Text.literal("§7Location: " + warp.pos.getX() + ", " + warp.pos.getY() + ", " + warp.pos.getZ() + " in " + warp.getDimensionName()));
        } else {
            player.sendSystemMessage(Text.literal("§cFailed to teleport to warp '" + warpName + "'!"));
            player.sendSystemMessage(Text.literal("§7The dimension may no longer exist."));
        }
        
        return success ? 1 : 0;
    }
} 