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

public class DelwarpCommand {
    private static final SuggestionProvider<ServerCommandSource> WARP_SUGGESTIONS = 
        (context, builder) -> SharedSuggestionProvider.suggest(WarpManager.getWarpNames(), builder);
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("delwarp")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("name", StringArgumentType.string())
                .suggests(WARP_SUGGESTIONS)
                .executes(context -> deleteWarp(context))
            )
        );
    }
    
    private static int deleteWarp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        String warpName = StringArgumentType.getString(context, "name");
        
        if (!WarpManager.warpExists(warpName)) {
            player.sendSystemMessage(Text.literal("§cWarp '" + warpName + "' does not exist!"));
            return 0;
        }
        
        boolean success = WarpManager.deleteWarp(warpName);
        if (success) {
            player.sendSystemMessage(Text.literal("§aWarp '" + warpName + "' has been deleted!"));
        } else {
            player.sendSystemMessage(Text.literal("§cFailed to delete warp '" + warpName + "'!"));
        }
        
        return success ? 1 : 0;
    }
} 