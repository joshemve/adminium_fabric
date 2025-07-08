package com.adminium;

import com.adminium.client.ClientSetup;
import com.adminium.manager.DisabledItemsManager;
import com.adminium.manager.FreezeManager;
import com.adminium.manager.PvpManager;
import com.adminium.team.TeamManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@Mod(Adminium.MODID)
public class Adminium {
    public static final String MODID = "adminium";

    public Adminium(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(ForgeEvents.class);
        modEventBus.addListener(ClientSetup::onClientSetup);
    }

    public static class ForgeEvents {
        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            DisabledItemsManager.load();
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
        public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
            TeamManager.getPlayerTeam(event.getEntity().getUUID()).ifPresent(team -> {
                MutableComponent teamPrefix = Component.literal("[" + team.getName() + "] ").withStyle(team.getColor());
                event.setDisplayName(Component.empty().append(teamPrefix).append(event.getDisplayName()));
            });
        }

        @SubscribeEvent
        public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
            if (DisabledItemsManager.isItemDisabled(event.getCrafting().getItem())) {
                event.getEntity().sendSystemMessage(Component.literal("This item is disabled and cannot be crafted.").withStyle(ChatFormatting.RED));
                for (int i = 0; i < event.getInventory().getContainerSize(); ++i) {
                    ItemStack ingredient = event.getInventory().getItem(i);
                    if (!ingredient.isEmpty()) {
                        event.getEntity().getInventory().placeItemBackInInventory(ingredient.copy());
                    }
                }
                event.getCrafting().setCount(0);
            }
        }

        @SubscribeEvent
        public static void onAttackEntity(AttackEntityEvent event) {
            // Check freeze state first
            if (FreezeManager.isFrozen() && !event.getEntity().hasPermissions(2)) {
                event.setCanceled(true);
                return;
            }
            
            // Check PvP state
            if (!PvpManager.isPvpEnabled()) {
                if (event.getEntity() instanceof Player && event.getTarget() instanceof Player) {
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent
        public static void onBlockBreak(BlockEvent.BreakEvent event) {
            if (FreezeManager.isFrozen() && !event.getPlayer().hasPermissions(2)) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
            if (FreezeManager.isFrozen() && event.getEntity() instanceof Player player && !player.hasPermissions(2)) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
            if (FreezeManager.isFrozen() && !event.getEntity().hasPermissions(2)) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
            if (FreezeManager.isFrozen() && !event.getEntity().hasPermissions(2)) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onEntityTick(EntityTickEvent.Pre event) {
            if (FreezeManager.isFrozen()) {
                // Freeze movement for non-operator players
                if (event.getEntity() instanceof Player player && !player.hasPermissions(2)) {
                    event.getEntity().setDeltaMovement(0, 0, 0);
                }
                // Freeze all non-player entities
                else if (!(event.getEntity() instanceof Player)) {
                    event.getEntity().setDeltaMovement(0, 0, 0);
                }
            }
        }
    }
} 