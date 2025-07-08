package com.adminium;

import com.adminium.client.ClientSetup;
import com.adminium.command.PvpCommand;
import com.adminium.command.TeamCommand;
import com.adminium.manager.DisabledItemsManager;
import com.adminium.manager.PvpManager;
import com.adminium.team.TeamManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.minecraft.server.level.ServerPlayer;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;

@Mod(Adminium.MODID)
public class Adminium {
    public static final String MODID = "adminium";

    public Adminium(IEventBus modEventBus) {
        // Register our static event handler class to the FORGE bus
        NeoForge.EVENT_BUS.register(ForgeEvents.class);

        // Register client setup
        modEventBus.addListener(ClientSetup::onClientSetup);
    }

    // A static inner class for holding event handlers
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            DisabledItemsManager.load();
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            PvpCommand.register(event.getDispatcher());
            TeamCommand.register(event.getDispatcher());
        }

        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                var recipesToRemove = player.getServer().getRecipeManager().getRecipes().stream()
                        .filter(r -> DisabledItemsManager.isItemDisabled(r.value().getResultItem(player.level().registryAccess()).getItem()))
                        .toList();
                recipesToRemove.forEach(player.getRecipeBook()::remove);
            }
        }

        @SubscribeEvent
        public static void onPlayerAttack(AttackEntityEvent event) {
            if (!PvpManager.isPvpEnabled()) {
                if (event.getEntity() instanceof Player && event.getTarget() instanceof Player) {
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent
        public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
            TeamManager.getPlayerTeam(event.getEntity().getUUID()).ifPresent(team -> {
                MutableComponent teamPrefix = Component.literal("[" + team.getName() + "] ").withStyle(team.getColor());
                event.setDisplayName(Component.empty().append(teamPrefix).append(event.getDisplayName()));
            });
        }

        @SubscribeEvent
        public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
            if (DisabledItemsManager.isItemDisabled(event.getCrafting().getItem())) {
                // Send a message to the player
                event.getEntity().sendSystemMessage(Component.literal("This item is disabled and cannot be crafted.").withStyle(ChatFormatting.RED));

                // Return the ingredients to the player's inventory before they are consumed
                for (int i = 0; i < event.getInventory().getContainerSize(); ++i) {
                    ItemStack ingredient = event.getInventory().getItem(i);
                    if (!ingredient.isEmpty()) {
                        event.getEntity().getInventory().placeItemBackInInventory(ingredient.copy());
                    }
                }
                
                // By setting the crafted item's count to 0, the player receives nothing.
                event.getCrafting().setCount(0);
            }
        }
    }
} 