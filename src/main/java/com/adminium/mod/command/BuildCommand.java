package com.adminium.mod.command;

import com.adminium.mod.manager.BuildManager;
import com.adminium.mod.util.CommandFeedbackHelper;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;

/**
 * /build on|off
 * Allows operators to toggle whether regular players can build or engage in PvP.
 */
public class BuildCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("build")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("on")
                .executes(context -> {
                    BuildManager.setBuildEnabled(true);
                    CommandFeedbackHelper.sendSuccess(context.getSource(), Text.literal("Build mode enabled: players may break/place blocks and PvP."), true);
                    return 1;
                }))
            .then(Commands.literal("off")
                .executes(context -> {
                    BuildManager.setBuildEnabled(false);
                    CommandFeedbackHelper.sendSuccess(context.getSource(), Text.literal("Build mode disabled: players cannot break/place blocks or PvP."), true);
                    return 1;
                }))
        );
    }
} 