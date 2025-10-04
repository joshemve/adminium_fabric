package com.adminium.mod.command;

import com.adminium.mod.manager.FreezeManager;
import com.adminium.mod.util.CommandFeedbackHelper;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;

public class FreezeCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("freeze")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("on")
                .executes(context -> {
                    FreezeManager.setFrozen(true);
                    CommandFeedbackHelper.sendSuccess(context.getSource(), Text.literal("Server has been frozen!"), true);
                    return 1;
                }))
            .then(Commands.literal("off")
                .executes(context -> {
                    FreezeManager.setFrozen(false);
                    CommandFeedbackHelper.sendSuccess(context.getSource(), Text.literal("Server has been unfrozen!"), true);
                    return 1;
                }))
        );
    }
} 