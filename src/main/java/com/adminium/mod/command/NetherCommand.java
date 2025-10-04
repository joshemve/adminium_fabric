package com.adminium.mod.command;

import com.adminium.mod.manager.PortalManager;
import com.adminium.mod.util.CommandFeedbackHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;

public class NetherCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("nether")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("on")
                .executes(context -> {
                    PortalManager.setNetherEnabled(true);
                    CommandFeedbackHelper.sendSuccess(context.getSource(), Text.literal("Nether portals are now enabled"), true);
                    return 1;
                }))
            .then(Commands.literal("off")
                .executes(context -> {
                    PortalManager.setNetherEnabled(false);
                    CommandFeedbackHelper.sendSuccess(context.getSource(), Text.literal("Nether portals are now disabled"), true);
                    return 1;
                }))
        );
    }
} 