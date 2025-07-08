package com.adminium.command;

import com.adminium.manager.PvpManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class PvpCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("pvp")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("on")
                        .executes(context -> {
                            PvpManager.setPvpEnabled(true);
                            context.getSource().sendSuccess(() -> Component.literal("PVP is now enabled."), true);
                            return 1;
                        }))
                .then(Commands.literal("off")
                        .executes(context -> {
                            PvpManager.setPvpEnabled(false);
                            context.getSource().sendSuccess(() -> Component.literal("PVP is now disabled."), true);
                            return 1;
                        }));

        dispatcher.register(command);
    }
} 