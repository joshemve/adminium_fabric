package com.adminium.mod.command;

import com.adminium.mod.manager.EnchantmentTableManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

public class EnchantBanCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("enchantban")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("max")
                .then(Commands.argument("level", IntegerArgumentType.integer(0, 255))
                    .executes(context -> setMaxLevel(context, IntegerArgumentType.getInteger(context, "level")))))
            .then(Commands.literal("status")
                .executes(EnchantBanCommand::showStatus))
            .then(Commands.literal("disable")
                .executes(context -> setMaxLevel(context, 0))));
    }

    private static int setMaxLevel(CommandContext<ServerCommandSource> context, int maxLevel) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            
            if (maxLevel == 0) {
                EnchantmentTableManager.setMaxEnchantmentLevel(0);
                player.sendSystemMessage(Text.literal("§aEnchantment table level restrictions disabled."));
            } else {
                EnchantmentTableManager.setMaxEnchantmentLevel(maxLevel);
                player.sendSystemMessage(Text.literal("§aEnchantment table max level set to " + maxLevel + "."));
                player.sendSystemMessage(Text.literal("§7Players can only get enchantments up to level " + maxLevel + " from enchantment tables."));
            }
            
            return 1;
        } catch (Exception e) {
            // Console execution
            if (maxLevel == 0) {
                EnchantmentTableManager.setMaxEnchantmentLevel(0);
                context.getSource().sendSystemMessage(Text.literal("Enchantment table level restrictions disabled."));
            } else {
                EnchantmentTableManager.setMaxEnchantmentLevel(maxLevel);
                context.getSource().sendSystemMessage(Text.literal("Enchantment table max level set to " + maxLevel + "."));
            }
            return 1;
        }
    }

    private static int showStatus(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            int maxLevel = EnchantmentTableManager.getMaxEnchantmentLevel();
            
            if (maxLevel == 0) {
                player.sendSystemMessage(Text.literal("§7Enchantment table level restrictions are §cdisabled§7."));
                player.sendSystemMessage(Text.literal("§7Players can get any enchantment level from enchantment tables."));
            } else {
                player.sendSystemMessage(Text.literal("§7Enchantment table max level: §c" + maxLevel));
                player.sendSystemMessage(Text.literal("§7Players cannot get enchantments above level " + maxLevel + " from enchantment tables."));
            }
            
            return 1;
        } catch (Exception e) {
            // Console execution
            int maxLevel = EnchantmentTableManager.getMaxEnchantmentLevel();
            
            if (maxLevel == 0) {
                context.getSource().sendSystemMessage(Text.literal("Enchantment table level restrictions are disabled."));
            } else {
                context.getSource().sendSystemMessage(Text.literal("Enchantment table max level: " + maxLevel));
            }
            return 1;
        }
    }
}
