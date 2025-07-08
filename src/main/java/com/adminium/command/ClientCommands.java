package com.adminium.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

public class ClientCommands {

    public static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        // Client-side commands are registered here.
    }
}