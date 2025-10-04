package com.adminium.mod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.EntityArgument;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.SimpleContainer;

public class EndchestCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("endchest")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> openPlayerEnderChest(context))
            )
        );
    }
    
    private static int openPlayerEnderChest(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity executor = context.getSource().getPlayerOrException();
        ServerPlayerEntity targetPlayer = EntityArgument.getPlayer(context, "player");
        
        if (targetPlayer == executor) {
            executor.sendSystemMessage(Text.literal("§cYou cannot view your own ender chest with this command!"));
            return 0;
        }
        
        // Create a container that mirrors the target player's ender chest
        SimpleContainer enderChestContainer = new SimpleContainer(27);
        
        // Copy ender chest inventory (27 slots)
        for (int i = 0; i < 27; i++) {
            enderChestContainer.setItem(i, targetPlayer.getEnderChestInventory().getItem(i).copy());
        }
        
        // Create a menu provider for the ender chest view
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            public Text getDisplayName() {
                return Text.literal(targetPlayer.getName().getString() + "'s Ender Chest");
            }
            
            @Override
            public ScreenHandler createMenu(int containerId, Inventory playerInventory, PlayerEntity player) {
                return new ChestMenu(MenuType.GENERIC_9x3, containerId, playerInventory, enderChestContainer, 3);
            }
        };
        
        executor.openMenu(menuProvider);
        executor.sendSystemMessage(Text.literal("§aOpened " + targetPlayer.getName().getString() + "'s ender chest"));
        
        return 1;
    }
} 