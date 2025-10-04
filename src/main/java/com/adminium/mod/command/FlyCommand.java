package com.adminium.mod.command;

import com.adminium.mod.manager.FlyManager;
import com.adminium.mod.util.CommandFeedbackHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

public class FlyCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("fly")
            .requires(src -> src.hasPermission(2))
            .executes(ctx -> toggle(ctx.getSource()))
            .then(Commands.literal("on").executes(ctx -> set(ctx.getSource(), true)))
            .then(Commands.literal("off").executes(ctx -> set(ctx.getSource(), false))));
    }

    private static int toggle(ServerCommandSource src) throws CommandSyntaxException {
        ServerPlayerEntity player = src.getPlayerOrException();
        FlyManager.toggle(player);
        CommandFeedbackHelper.sendSuccess(src, Text.literal(FlyManager.canFly(player.getUUID()) ? "Flight enabled." : "Flight disabled."), false);
        return 1;
    }

    private static int set(ServerCommandSource src, boolean enable) throws CommandSyntaxException {
        ServerPlayerEntity player = src.getPlayerOrException();
        if (enable) {
            FlyManager.enable(player);
        } else {
            FlyManager.disable(player);
        }
        CommandFeedbackHelper.sendSuccess(src, Text.literal(enable ? "Flight enabled." : "Flight disabled."), false);
        return 1;
    }
} 