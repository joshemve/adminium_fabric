package com.adminium.mod.command;

import com.adminium.mod.team.Team;
import com.adminium.mod.team.TeamManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class TeamMsgCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("tm")
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(context -> sendTeamMessage(context))
            )
        );
    }
    
    private static int sendTeamMessage(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        String message = StringArgumentType.getString(context, "message");
        
        // Get the player's team
        Team team = TeamManager.getPlayerTeam(player.getUUID());
        if (team == null) {
            player.sendSystemMessage(Text.literal("§cYou must be on a team to message your team!"));
            return 0;
        }
        
        // Create the team message
        String teamPrefix = team.getPrefix();
        Text teamMessage = Text.literal("§8[TEAM] " + teamPrefix + " §7" + player.getName().getString() + "§8: §f" + message);
        
        // Send message to all team members
        int messagesSent = 0;
        for (UUID memberId : team.getMembers()) {
            ServerPlayerEntity member = player.getServer().getPlayerList().getPlayer(memberId);
            if (member != null) {
                member.sendSystemMessage(teamMessage);
                messagesSent++;
            }
        }
        
        // If no messages were sent (shouldn't happen since sender is a team member)
        if (messagesSent == 0) {
            player.sendSystemMessage(Text.literal("§cNo team members are online!"));
            return 0;
        }
        
        return 1;
    }
} 