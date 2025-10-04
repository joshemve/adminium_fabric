package com.adminium.mod.command;

import com.adminium.mod.manager.PvpManager;
import com.adminium.mod.util.CommandFeedbackHelper;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;

public class PvpCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("pvp")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("on")
                .executes(context -> {
                    PvpManager.setPvpEnabled(true);
                    CommandFeedbackHelper.sendSuccess(context.getSource(), Text.literal("PvP has been enabled!"), true);
                    return 1;
                }))
            .then(Commands.literal("off")
                .executes(context -> {
                    PvpManager.setPvpEnabled(false);
                    CommandFeedbackHelper.sendSuccess(context.getSource(), Text.literal("PvP has been disabled!"), true);
                    return 1;
                }))
        );
    }
} 