package com.adminium.command;

import com.adminium.manager.FreezeManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class FreezeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> freezeCommand = Commands.literal("freeze")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("on")
                        .executes(context -> {
                            FreezeManager.setFrozen(true);
                            context.getSource().getServer().getPlayerList().broadcastSystemMessage(
                                    Component.literal("Server has been frozen!").withStyle(style -> style.withColor(0x00FFFF).withBold(true)),
                                    false
                            );
                            return 1;
                        })
                )
                .then(Commands.literal("off")
                        .executes(context -> {
                            FreezeManager.setFrozen(false);
                            context.getSource().getServer().getPlayerList().broadcastSystemMessage(
                                    Component.literal("Server has been unfrozen!").withStyle(style -> style.withColor(0x00FF00).withBold(true)),
                                    false
                            );
                            return 1;
                        })
                );

        dispatcher.register(freezeCommand);
    }
} 