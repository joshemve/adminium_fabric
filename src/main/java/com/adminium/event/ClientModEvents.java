package com.adminium.event;

import com.adminium.command.ClientCommands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

public class ClientModEvents {
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        ClientCommands.register(event.getDispatcher());
    }
} 