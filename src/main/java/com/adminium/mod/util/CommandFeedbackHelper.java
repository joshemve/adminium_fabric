package com.adminium.mod.util;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameRules;

public class CommandFeedbackHelper {

    /**
     * Sends a success message that respects the sendCommandFeedback gamerule
     */
    public static void sendSuccess(ServerCommandSource source, Text message, boolean broadcastToOps) {
        source.sendFeedback(() -> message, broadcastToOps);
    }

    /**
     * Sends a failure message (always shown regardless of gamerule)
     */
    public static void sendFailure(ServerCommandSource source, Text message) {
        source.sendError(message);
    }

    /**
     * Sends a message directly to a player (bypasses command feedback rules)
     * Use this for messages that should always be shown to the executing player
     */
    public static void sendDirectMessage(ServerPlayerEntity player, Text message) {
        if (player != null) {
            player.sendMessage(message, false);
        }
    }

    /**
     * Checks if command feedback should be shown based on gamerule
     */
    public static boolean shouldSendCommandFeedback(ServerCommandSource source) {
        return source.getWorld().getGameRules().getBoolean(GameRules.SEND_COMMAND_FEEDBACK);
    }

    /**
     * Sends a message only if sendCommandFeedback is true
     */
    public static void sendFeedback(ServerCommandSource source, Text message) {
        if (shouldSendCommandFeedback(source)) {
            source.sendMessage(message);
        }
    }
}