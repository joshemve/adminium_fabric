package com.adminium.mod.command;

import com.adminium.mod.team.Team;
import com.adminium.mod.team.TeamManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.util.Formatting;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.EntityArgument;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Arrays;
import java.util.UUID;

/**
 * Team management commands for Adminium mod.
 * Uses /ateam instead of /team to avoid conflicts with vanilla MinecraftClient team commands.
 * 
 * Commands:
 * - /ateam create <name> - Create a new team
 * - /ateam invite <player> - Invite a player to your team
 * - /ateam join <name> - Join a team you've been invited to
 * - /ateam leave - Leave your current team
 * - /ateam remove <player> - Remove a player from your team (op only)
 * - /ateam op <player> - Make a player a team operator (op only)
 * - /ateam color <color> - Change your team's color (op only)
 * - /ateam list - List all teams
 */
public class TeamCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("ateam")
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayerOrException();
                        String teamName = StringArgumentType.getString(context, "name");
                        
                        if (TeamManager.createTeam(teamName, player.getUUID())) {
                            Team team = TeamManager.getTeam(teamName);
                            player.sendSystemMessage(Text.literal("Team created: " + team.getPrefix()));
                            
                            // Refresh player's display name
                            player.refreshDisplayName();
                            
                            // Update tab list
                            player.getServer().getPlayerList().broadcastAll(
                                new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(
                                    net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                                    player
                                )
                            );
                            
                            return 1;
                        } else {
                            player.sendSystemMessage(Text.literal("Team name already exists!"));
                            return 0;
                        }
                    })))
            
            .then(Commands.literal("invite")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayerEntity inviter = context.getSource().getPlayerOrException();
                        ServerPlayerEntity invited = EntityArgument.getPlayer(context, "player");
                        Team team = TeamManager.getPlayerTeam(inviter.getUUID());
                        
                        if (team == null) {
                            inviter.sendSystemMessage(Text.literal("You are not in a team!"));
                            return 0;
                        }
                        
                        if (TeamManager.invitePlayer(team.getName(), inviter.getUUID(), invited.getUUID())) {
                            invited.sendSystemMessage(Text.literal("You have been invited to team '" + team.getName() + "'! Use /ateam join " + team.getName() + " to accept."));
                            inviter.sendSystemMessage(Text.literal("Invitation sent to " + invited.getName().getString()));
                            return 1;
                        } else {
                            inviter.sendSystemMessage(Text.literal("You don't have permission to invite players!"));
                            return 0;
                        }
                    })))
            
            .then(Commands.literal("join")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayerOrException();
                        String teamName = StringArgumentType.getString(context, "name");
                        
                        // Debug: Check if team exists
                        Team team = TeamManager.getTeam(teamName);
                        if (team == null) {
                            player.sendSystemMessage(Text.literal("Team '" + teamName + "' does not exist! Use the exact team name (case doesn't matter)."));
                            return 0;
                        }
                        
                        // Check for invitation
                        if (!TeamManager.getInvitations(player.getUUID()).contains(teamName.toLowerCase())) {
                            player.sendSystemMessage(Text.literal("You don't have an invitation to team '" + team.getName() + "'!"));
                            return 0;
                        }
                        
                        if (TeamManager.joinTeam(teamName, player.getUUID())) {
                            player.sendSystemMessage(Text.literal("Joined team: " + team.getPrefix()));
                            
                            // Refresh player's display name
                            player.refreshDisplayName();
                            
                            // Update tab list
                            player.getServer().getPlayerList().broadcastAll(
                                new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(
                                    net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                                    player
                                )
                            );
                            
                            return 1;
                        } else {
                            player.sendSystemMessage(Text.literal("Failed to join team!"));
                            return 0;
                        }
                    })))
            
            .then(Commands.literal("leave")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayerOrException();
                    Team team = TeamManager.getPlayerTeam(player.getUUID());
                    
                    if (team == null) {
                        player.sendSystemMessage(Text.literal("You are not in a team!"));
                        return 0;
                    }
                    
                    TeamManager.leaveTeam(player.getUUID());
                    player.sendSystemMessage(Text.literal("Left team '" + team.getName() + "'!"));
                    
                    // Refresh player's display name
                    player.refreshDisplayName();
                    
                    // Update tab list
                    player.getServer().getPlayerList().broadcastAll(
                        new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(
                            net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                            player
                        )
                    );
                    
                    return 1;
                }))
            
            .then(Commands.literal("remove")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayerEntity remover = context.getSource().getPlayerOrException();
                        ServerPlayerEntity toRemove = EntityArgument.getPlayer(context, "player");
                        Team team = TeamManager.getPlayerTeam(remover.getUUID());
                        
                        if (team == null) {
                            remover.sendSystemMessage(Text.literal("You are not in a team!"));
                            return 0;
                        }
                        
                        if (TeamManager.removeFromTeam(team.getName(), remover.getUUID(), toRemove.getUUID())) {
                            toRemove.sendSystemMessage(Text.literal("You have been removed from team '" + team.getName() + "'!"));
                            remover.sendSystemMessage(Text.literal("Removed " + toRemove.getName().getString() + " from the team!"));
                            
                            // Refresh removed player's display name
                            toRemove.refreshDisplayName();
                            
                            // Update tab list
                            remover.getServer().getPlayerList().broadcastAll(
                                new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(
                                    net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                                    toRemove
                                )
                            );
                            
                            return 1;
                        } else {
                            remover.sendSystemMessage(Text.literal("You don't have permission to remove players!"));
                            return 0;
                        }
                    })))
            
            .then(Commands.literal("op")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayerEntity promoter = context.getSource().getPlayerOrException();
                        ServerPlayerEntity toPromote = EntityArgument.getPlayer(context, "player");
                        Team team = TeamManager.getPlayerTeam(promoter.getUUID());
                        
                        if (team == null) {
                            promoter.sendSystemMessage(Text.literal("You are not in a team!"));
                            return 0;
                        }
                        
                        if (TeamManager.promoteToOperator(team.getName(), promoter.getUUID(), toPromote.getUUID())) {
                            toPromote.sendSystemMessage(Text.literal("You are now a team operator!"));
                            promoter.sendSystemMessage(Text.literal("Promoted " + toPromote.getName().getString() + " to team operator!"));
                            return 1;
                        } else {
                            promoter.sendSystemMessage(Text.literal("You don't have permission to promote players!"));
                            return 0;
                        }
                    })))
            
            .then(Commands.literal("color")
                .then(Commands.argument("color", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        Arrays.stream(Formatting.values())
                            .filter(Formatting::isColor)
                            .forEach(color -> builder.suggest(color.getName()));
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayerOrException();
                        String colorName = StringArgumentType.getString(context, "color");
                        Team team = TeamManager.getPlayerTeam(player.getUUID());
                        
                        if (team == null) {
                            player.sendSystemMessage(Text.literal("You are not in a team!"));
                            return 0;
                        }
                        
                        Formatting color = Formatting.getByName(colorName);
                        if (color == null || !color.isColor()) {
                            player.sendSystemMessage(Text.literal("Invalid color!"));
                            return 0;
                        }
                        
                        if (TeamManager.setTeamColor(team.getName(), player.getUUID(), color)) {
                            player.sendSystemMessage(Text.literal("Team color changed to " + color + color.getName() + Formatting.RESET + "!"));
                            
                            // Refresh display names for all team members
                            for (UUID memberId : team.getMembers()) {
                                ServerPlayerEntity member = player.getServer().getPlayerList().getPlayer(memberId);
                                if (member != null) {
                                    member.refreshDisplayName();
                                    
                                    // Update tab list for this member
                                    player.getServer().getPlayerList().broadcastAll(
                                        new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(
                                            net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                                            member
                                        )
                                    );
                                }
                            }
                            
                            return 1;
                        } else {
                            player.sendSystemMessage(Text.literal("You don't have permission to change team color!"));
                            return 0;
                        }
                    })))
            
            .then(Commands.literal("list")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayerOrException();
                    
                    if (TeamManager.getAllTeams().isEmpty()) {
                        player.sendSystemMessage(Text.literal("No teams exist!"));
                        return 0;
                    }
                    
                    player.sendSystemMessage(Text.literal("=== Teams ==="));
                    for (Team team : TeamManager.getAllTeams()) {
                        player.sendSystemMessage(Text.literal(team.getPrefix() + " - " + team.getMembers().size() + " members"));
                    }
                    return 1;
                }))
        );
    }
} 