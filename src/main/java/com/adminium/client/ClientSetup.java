package com.adminium.client;

import com.adminium.event.ClientModEvents;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

public class ClientSetup {
    public static void onClientSetup(final FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.register(ClientModEvents.class);
    }
} 