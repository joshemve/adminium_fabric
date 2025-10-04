package com.adminium.mod.command;

import com.adminium.mod.manager.PortalManager;
import com.adminium.mod.util.CommandFeedbackHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;

public class EndCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("end")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("on")
                .executes(context -> {
                    PortalManager.setEndEnabled(true);
                    CommandFeedbackHelper.sendSuccess(context.getSource(), Text.literal("End portals are now enabled"), true);
                    return 1;
                }))
            .then(Commands.literal("off")
                .executes(context -> {
                    PortalManager.setEndEnabled(false);
                    CommandFeedbackHelper.sendSuccess(context.getSource(), Text.literal("End portals are now disabled"), true);
                    return 1;
                }))
        );
    }
} 