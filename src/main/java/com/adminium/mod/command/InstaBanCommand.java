package com.adminium.mod.command;

import com.adminium.mod.manager.InstaBanManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

public class InstaBanCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("instaban")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("on")
                .executes(context -> setInstaBan(context, true))
            )
            .then(Commands.literal("off")
                .executes(context -> setInstaBan(context, false))
            )
            .executes(context -> showStatus(context))
        );
    }
    
    private static int setInstaBan(CommandContext<ServerCommandSource> context, boolean enabled) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        
        InstaBanManager.setInstaBanEnabled(enabled);
        
        if (enabled) {
            // Check if the world is actually in hardcore mode
            if (player.getServer().isHardcore()) {
                player.sendSystemMessage(Text.literal("§aInstaBan has been enabled!"));
                player.sendSystemMessage(Text.literal("§7Players who die will be instantly banned instead of going to spectator mode."));
                
                // Broadcast to all operators
                context.getSource().getServer().getPlayerList().broadcastSystemMessage(
                    Text.literal("§6[InstaBan] §aEnabled by " + player.getName().getString() + 
                        " - Players will be banned on death in hardcore mode"), false);
            } else {
                player.sendSystemMessage(Text.literal("§aInstaBan has been enabled!"));
                player.sendSystemMessage(Text.literal("§eNote: This world is not in hardcore mode, so InstaBan will have no effect."));
            }
        } else {
            player.sendSystemMessage(Text.literal("§cInstaBan has been disabled!"));
            player.sendSystemMessage(Text.literal("§7Players who die will use normal hardcore behavior (spectator mode)."));
            
            // Broadcast to all operators
            context.getSource().getServer().getPlayerList().broadcastSystemMessage(
                Text.literal("§6[InstaBan] §cDisabled by " + player.getName().getString() + 
                    " - Normal hardcore behavior restored"), false);
        }
        
        return 1;
    }
    
    private static int showStatus(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        
        boolean enabled = InstaBanManager.isInstaBanEnabled();
        boolean isHardcore = player.getServer().isHardcore();
        
        player.sendSystemMessage(Text.literal("§6=== InstaBan Status ==="));
        player.sendSystemMessage(Text.literal("§7InstaBan: " + (enabled ? "§aEnabled" : "§cDisabled")));
        player.sendSystemMessage(Text.literal("§7World Mode: " + (isHardcore ? "§6Hardcore" : "§bNormal")));
        
        if (enabled && isHardcore) {
            player.sendSystemMessage(Text.literal("§a✓ InstaBan is active - players will be banned on death"));
        } else if (enabled && !isHardcore) {
            player.sendSystemMessage(Text.literal("§e⚠ InstaBan is enabled but world is not hardcore"));
        } else {
            player.sendSystemMessage(Text.literal("§7○ InstaBan is disabled - normal hardcore behavior"));
        }
        
        player.sendSystemMessage(Text.literal("§7Use /instaban <on|off> to toggle"));
        
        return 1;
    }
} 