package com.adminium.mod.command;

import com.adminium.mod.manager.GoldenAppleLimitManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;

public class GoldenAppleLimitCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("goldenapple_limit")
            .requires(src -> src.hasPermission(2))
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Text.literal(
                    "Golden Apple cap is " + (GoldenAppleLimitManager.isEnabled() ? "§aON§r" : "§cOFF§r") +
                    ", limit=" + GoldenAppleLimitManager.getLimit()
                ), false);
                return 1;
            })
            .then(Commands.literal("on").executes(ctx -> {
                GoldenAppleLimitManager.setEnabled(true);
                ctx.getSource().sendSuccess(() -> Text.literal("§aGolden Apple cap enabled"), true);
                return 1;
            }))
            .then(Commands.literal("off").executes(ctx -> {
                GoldenAppleLimitManager.setEnabled(false);
                ctx.getSource().sendSuccess(() -> Text.literal("§cGolden Apple cap disabled"), true);
                return 1;
            }))
            .then(Commands.literal("toggle").executes(ctx -> {
                GoldenAppleLimitManager.toggle();
                boolean on = GoldenAppleLimitManager.isEnabled();
                ctx.getSource().sendSuccess(() -> Text.literal("Golden Apple cap is now " + (on ? "§aON" : "§cOFF")), true);
                return on ? 1 : 0;
            }))
            .then(Commands.literal("set").then(Commands.argument("limit", IntegerArgumentType.integer(0, 64))
                .executes(ctx -> {
                    int limit = IntegerArgumentType.getInteger(ctx, "limit");
                    GoldenAppleLimitManager.setLimit(limit);
                    ctx.getSource().sendSuccess(() -> Text.literal("Set Golden Apple cap to " + limit), true);
                    return 1;
                })
            ))
        );
    }
}
