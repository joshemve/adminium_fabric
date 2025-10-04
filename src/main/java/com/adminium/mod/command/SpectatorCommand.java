package com.adminium.mod.command;

import com.adminium.mod.manager.SpectatorManager;
import com.adminium.mod.util.CommandFeedbackHelper;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public class SpectatorCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("s")
            .requires(src -> src.hasPermission(2))
            .executes(ctx -> toggle(ctx.getSource()))
            .then(Commands.literal("on").executes(ctx -> set(ctx.getSource(), true)))
            .then(Commands.literal("off").executes(ctx -> set(ctx.getSource(), false))));
    }

    private static int toggle(ServerCommandSource src) throws CommandSyntaxException {
        ServerPlayerEntity p = src.getPlayerOrException();
        SpectatorManager.toggle(p);
        CommandFeedbackHelper.sendSuccess(src, Text.literal(SpectatorManager.isSpectating(p.getUUID()) ? "Entered custom spectator mode." : "Returned to previous gamemode."), false);
        return 1;
    }

    private static int set(ServerCommandSource src, boolean enable) throws CommandSyntaxException {
        ServerPlayerEntity p = src.getPlayerOrException();
        if (enable) {
            if (!SpectatorManager.isSpectating(p.getUUID())) SpectatorManager.enableSpectator(p);
        } else {
            if (SpectatorManager.isSpectating(p.getUUID())) SpectatorManager.disableSpectator(p);
        }
        CommandFeedbackHelper.sendSuccess(src, Text.literal(enable ? "Entered custom spectator mode." : "Returned to previous gamemode."), false);
        return 1;
    }
} 