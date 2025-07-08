package com.adminium.event;

import com.adminium.command.ClientCommands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = com.adminium.Adminium.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        ClientCommands.register(event);
    }
} 