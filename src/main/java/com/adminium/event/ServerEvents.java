package com.adminium.event;

import com.adminium.command.FreezeCommand;
import com.adminium.command.PvpCommand;
import com.adminium.command.SafeCommand;
import com.adminium.command.TeamCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = com.adminium.Adminium.MODID)
public class ServerEvents {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        PvpCommand.register(event.getDispatcher());
        TeamCommand.register(event.getDispatcher());
        FreezeCommand.register(event.getDispatcher());
        SafeCommand.register(event.getDispatcher());
    }
} 