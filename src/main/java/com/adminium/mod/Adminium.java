package com.adminium.mod;

import com.adminium.mod.commands.*;
import com.adminium.mod.command.*;
import com.adminium.mod.manager.*;
import com.adminium.mod.network.ModNetworking;
import com.adminium.mod.roles.RoleManager;
import com.adminium.mod.team.TeamManager;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.slf4j.Logger;

public class Adminium implements ModInitializer {
    public static final String MODID = "adminium";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static MinecraftServer server;

    @Override
    public void onInitialize() {
        LOGGER.info("Adminium initializing...");

        // Register custom effects
        ModEffects.register();

        // Register custom items
        ModItems.register();

        // Initialize networking
        ModNetworking.register();

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            PvpCommand.register(dispatcher);
            FreezeCommand.register(dispatcher);
            SafeCommand.register(dispatcher);
            TeamCommand.register(dispatcher);
            TeamMsgCommand.register(dispatcher);
            ItemsCommand.register(dispatcher);
            NetherCommand.register(dispatcher);
            EndCommand.register(dispatcher);
            AnnounceCommand.register(dispatcher);
            RolesCommand.register(dispatcher);
            VanishCommand.register(dispatcher);
            InvseeCommand.register(dispatcher);
            EndchestCommand.register(dispatcher);
            SetwarpCommand.register(dispatcher);
            WarpCommand.register(dispatcher);
            DelwarpCommand.register(dispatcher);
            BansCommand.register(dispatcher);
            InstaBanCommand.register(dispatcher);
            HealCommand.register(dispatcher);
            GodCommand.register(dispatcher);
            HelpCommand.register(dispatcher);
            NickCommand.register(dispatcher);
            SpectatorCommand.register(dispatcher);
            PodCommand.register(dispatcher);
            FlyCommand.register(dispatcher);
            BuildCommand.register(dispatcher);
            BarrelCommand.register(dispatcher);
            BarrelFillCommand.register(dispatcher);
            WaterDamageCommand.register(dispatcher);
            DisabledItemsCommand.register(dispatcher);
            BarrelBreakableCommand.register(dispatcher);
            GoldenAppleLimitCommand.register(dispatcher);
            EnchantBanCommand.register(dispatcher);
        });

        // Server lifecycle events
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            Adminium.server = server;

            // Initialize managers with server reference
            RoleManager.setServer(server);
            TeamManager.setServer(server);
            VanishManager.setServer(server);
            WarpManager.setServer(server);
            BanManager.setServer(server);
            DisabledItemsManager.setServer(server);
            BarrelProtectionManager.load();
            BarrelLootManager.setServer(server);
            NicknameManager.load();
            PodManager.setServer(server);
            EnchantmentTableManager.setServer(server);

            // Load configuration for managers
            RoleManager.load();
            TeamManager.load();
            BanManager.load();
            WarpManager.load();
            GodManager.load();
            SpectatorManager.load();
            PodManager.load();
            BuildManager.load();
            BarrelLootManager.load();
            GoldenAppleLimitManager.load();
            EnchantmentTableManager.load();
            PlayerReviveIntegration.onServerStart(server);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            // Save all manager states
            RoleManager.save();
            TeamManager.save();
            BanManager.save();
            WarpManager.save();
            VanishManager.save();
            NicknameManager.save();
            BarrelProtectionManager.save();
            PodManager.save();
            BarrelLootManager.save();
            GoldenAppleLimitManager.save();
            EnchantmentTableManager.save();
        });

        // Register event handlers
        registerEventHandlers();

        LOGGER.info("Adminium initialized!");
    }

    private void registerEventHandlers() {
        // Server tick events
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            FreezeManager.tick(server);
            VanishManager.tick(server);
            FlyManager.tick(server);
            RoleManager.tickHandler();
            NicknameManager.tickHandler();

            // Handle player tick events for role bonuses
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                RoleBonusHandler.handlePlayerTick(player);
            }
        });

        // PlayerEntity connection events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            // Handle player join events
            BanManager.onPlayerJoin(player);
            RoleManager.onPlayerJoin(player);
            TeamManager.onPlayerJoin(player);
            VanishManager.onPlayerJoin(player);
            NicknameManager.onPlayerJoin(player);
            DisabledItemsManager.syncToPlayer(player);
            PodManager.syncToPlayer(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            // Handle player disconnect events
            VanishManager.onPlayerLeave(player);
            FlyManager.onPlayerDisconnect(player);
        });

        // PlayerEntity respawn event
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // Handle respawn events for managers
            GodManager.onPlayerRespawn(newPlayer);
            FlyManager.onPlayerRespawn(newPlayer);
        });

        // Attack entity event
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof LivingEntity livingEntity) {
                // Check PvP restrictions
                if (!PvpManager.canAttack(player, livingEntity)) {
                    return ActionResult.FAIL;
                }

                // Check freeze state
                if (FreezeManager.isGameFrozen() || FreezeManager.isFrozen(player)) {
                    return ActionResult.FAIL;
                }

                // Check safe mode
                if (SafeManager.isSafeMode()) {
                    return ActionResult.FAIL;
                }

                // Check build mode
                if (!BuildManager.canBuild(player)) {
                    return ActionResult.FAIL;
                }
            }

            return ActionResult.PASS;
        });

        // Block break event
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            // Check freeze state
            if (FreezeManager.isGameFrozen() || FreezeManager.isFrozen(player)) {
                return false;
            }

            // Check build permissions
            if (!BuildManager.canBuild(player)) {
                return false;
            }

            // Check barrel protection and role bonuses
            if (!RoleBonusHandler.handleBlockBreak(player, (ServerWorld)world, pos, state)) {
                return false;
            }

            return true;
        });

        // Block place event (using UseBlockCallback)
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            // Check freeze state
            if (FreezeManager.isGameFrozen() || FreezeManager.isFrozen(player)) {
                return ActionResult.FAIL;
            }

            // Check build permissions
            if (!BuildManager.canBuild(player)) {
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });

        // Item use event
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (DisabledItemsManager.isDisabled(player.getStackInHand(hand).getItem())) {
                return TypedActionResult.fail(player.getStackInHand(hand));
            }

            // Check golden apple limits
            if (!GoldenAppleLimitManager.canUseGoldenApple(player, player.getStackInHand(hand))) {
                return TypedActionResult.fail(player.getStackInHand(hand));
            }

            return TypedActionResult.pass(player.getStackInHand(hand));
        });

        // Chat message event
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            // Handle nickname display in chat
            return NicknameManager.processChat(sender, message);
        });

        // Living entity death event
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity player) {
                // Handle death events
                TeamManager.onPlayerDeath(player);
                PlayerReviveIntegration.onPlayerDeath(player);
            }
        });

        // Living entity hurt event
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                // Check god mode
                if (GodManager.isGodMode(player)) {
                    return false;
                }

                // Check water damage
                if (!WaterDamageManager.canTakeWaterDamage(player, source)) {
                    return false;
                }
            }

            return true;
        });

        // Dimension change event
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            // Check portal restrictions
            if (!PortalManager.canUseDimension(player, destination.getRegistryKey())) {
                // Teleport back if not allowed
                player.teleport(origin, player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
            }
        });
    }

    public static MinecraftServer getServer() {
        return server;
    }
}