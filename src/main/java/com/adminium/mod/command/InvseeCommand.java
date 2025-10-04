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
import com.adminium.mod.container.InvseeContainer;

public class InvseeCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("invsee")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> openPlayerInventory(context))
            )
        );
    }
    
    private static int openPlayerInventory(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity executor = context.getSource().getPlayerOrException();
        ServerPlayerEntity targetPlayer = EntityArgument.getPlayer(context, "player");
        
        if (targetPlayer == executor) {
            executor.sendSystemMessage(Text.literal("§cYou cannot view your own inventory with this command!"));
            return 0;
        }
        
        // Wrapper container exposing 54 slots (6 rows)
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            public Text getDisplayName() {
                return Text.literal(targetPlayer.getName().getString() + "'s Inventory");
            }

            @Override
            public ScreenHandler createMenu(int containerId, Inventory playerInventory, PlayerEntity player) {
                return new ChestMenu(MenuType.GENERIC_9x6, containerId, playerInventory, new InvseeContainer(targetPlayer), 6);
            }
        };

        executor.openMenu(menuProvider);
        executor.sendSystemMessage(Text.literal("§aOpened " + targetPlayer.getName().getString() + "'s inventory"));
        
        return 1;
    }
} 