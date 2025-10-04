package com.adminium.mod.command;

import com.adminium.mod.ModItems;
import com.adminium.mod.manager.PodManager;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;
import net.minecraft.command.argument.EntityArgument;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class PodCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("pods")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("wand")
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayerOrException();
                    player.getInventory().add(ModItems.POD_WAND.get().getDefaultInstance());
                    ctx.getSource().sendSuccess(() -> Text.literal("Given Pod-Selection Wand."), true);
                    return 1;
                }))
            .then(Commands.literal("list")
                .executes(ctx -> {
                    int total = PodManager.getTotalPodCount();
                    ctx.getSource().sendSuccess(() -> Text.literal("There are " + total + " pod spawn blocks registered."), false);
                    return 1;
                }))
            .then(Commands.literal("clear")
                .executes(ctx -> {
                    PodManager.clear();
                    ctx.getSource().sendSuccess(() -> Text.literal("Cleared all pod locations."), false);
                    return 1;
                }))
            .then(Commands.literal("exclude")
                .then(Commands.literal("add")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayerEntity target = EntityArgument.getPlayer(ctx, "player");
                            PodManager.addExcluded(target.getUUID());
                            ctx.getSource().sendSuccess(() -> Text.literal("Excluded " + target.getName().getString() + " from pod teleport."), false);
                            return 1;
                        })))
                .then(Commands.literal("remove")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayerEntity target = EntityArgument.getPlayer(ctx, "player");
                            PodManager.removeExcluded(target.getUUID());
                            ctx.getSource().sendSuccess(() -> Text.literal("Removed exclusion for " + target.getName().getString()), false);
                            return 1;
                        })))
                .then(Commands.literal("list")
                    .executes(ctx -> {
                        var list = PodManager.getExcludedPlayers();
                        ctx.getSource().sendSuccess(() -> Text.literal("Excluded players: " + (list.isEmpty() ? "<none>" : list.size())), false);
                        return 1;
                    })))
            .then(Commands.literal("start")
                .executes(ctx -> {
                    List<ServerPlayerEntity> players = Lists.newArrayList(ctx.getSource().getServer().getPlayerList().getPlayers());
                    players.removeIf(p -> PodManager.isExcluded(p.getUUID()));
                    var assignments = PodManager.assignPodsRandomly(players, ctx.getSource().getLevel().dimension());

                    int teleportedCount = 0;
                    for (var entry : assignments.entrySet()) {
                        ServerPlayerEntity p = entry.getKey();
                        var pos = entry.getValue();
                        p.teleportTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                        teleportedCount++;
                    }
                    int skipped = players.size() - teleportedCount;
                    String resultMsg = "Teleported " + teleportedCount + " player(s)." + (skipped > 0 ? " Skipped " + skipped + " due to insufficient pods." : "");
                    ctx.getSource().sendSuccess(() -> Text.literal(resultMsg), false);
                    return 1;
                }))
        );
    }
} 