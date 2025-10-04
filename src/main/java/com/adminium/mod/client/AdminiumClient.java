package com.adminium.mod.client;

import com.adminium.mod.Adminium;
import com.adminium.mod.client.gui.*;
import com.adminium.mod.network.ModNetworking;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;

public class AdminiumClient implements ClientModInitializer {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitializeClient() {
        LOGGER.info("Adminium Client initializing...");

        // Register client-side networking
        ModNetworking.registerClient();

        // Register client-side event handlers
        registerClientEventHandlers();

        // Register screen handlers for custom GUIs
        registerScreenHandlers();

        LOGGER.info("Adminium Client initialized!");
    }

    private void registerClientEventHandlers() {
        // Register HUD rendering
        HudRenderCallback.EVENT.register((matrixStack, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && !client.options.hudHidden) {
                // Render role effect overlay
                RoleEffectOverlay.render(matrixStack, client);
            }
        });

        // Register world render events for pod highlighting
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            PodHighlightRenderer.render(context);
        });

        // Register client tick events
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Handle client-side tick events if needed
            ClientBarrelHandler.tick(client);
        });
    }

    private void registerScreenHandlers() {
        // Screen handlers will be registered here when needed
        // These are typically registered through the networking handlers
        // when the server sends packets to open custom screens
    }
}