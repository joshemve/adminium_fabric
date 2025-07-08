package com.adminium.command;

import com.adminium.client.gui.ItemsScreen;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ClientCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("items")
                .executes(context -> {
                    Minecraft.getInstance().execute(() -> {
                        Minecraft.getInstance().setScreen(new ItemsScreen());
                    });
                    return 1;
                }));
    }
} 