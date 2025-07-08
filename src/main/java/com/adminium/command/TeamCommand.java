package com.adminium.command;

import com.adminium.team.TeamManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.ChatFormatting;

public class TeamCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("team")
                .then(createCreateCommand())
                .then(createInviteCommand())
                .then(createJoinCommand())
                .then(createRemoveCommand())
                .then(createOpCommand())
                .then(createColorCommand())
                .then(createLeaveCommand());

        dispatcher.register(command);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createCreateCommand() {
        return Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(context -> {
                            String teamName = StringArgumentType.getString(context, "name");
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            boolean success = TeamManager.createTeam(teamName, player.getUUID());
                            if (success) {
                                context.getSource().sendSuccess(() -> Component.literal("Team '" + teamName + "' created."), true);
                            } else {
                                context.getSource().sendFailure(Component.literal("A team with that name already exists."));
                            }
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createInviteCommand() {
        return Commands.literal("invite")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer inviter = context.getSource().getPlayerOrException();
                            ServerPlayer invited = EntityArgument.getPlayer(context, "player");

                            TeamManager.getPlayerTeam(inviter.getUUID()).ifPresentOrElse(team -> {
                                if (team.isOperator(inviter.getUUID())) {
                                    TeamManager.invitePlayer(invited.getUUID(), team.getName());
                                    context.getSource().sendSuccess(() -> Component.literal("Invited " + invited.getName().getString() + " to " + team.getName()), true);
                                    invited.sendSystemMessage(Component.literal("You have been invited to join team " + team.getName() + ". Type /team join " + team.getName() + " to accept."));
                                } else {
                                    context.getSource().sendFailure(Component.literal("You must be an operator of the team to invite players."));
                                }
                            }, () -> {
                                context.getSource().sendFailure(Component.literal("You are not in a team."));
                            });
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createJoinCommand() {
        return Commands.literal("join")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(context -> {
                            String teamName = StringArgumentType.getString(context, "name");
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            boolean success = TeamManager.acceptInvite(player.getUUID(), teamName);
                            if (success) {
                                context.getSource().sendSuccess(() -> Component.literal("You have joined team " + teamName + "."), true);
                            } else {
                                context.getSource().sendFailure(Component.literal("You have no pending invitation to that team."));
                            }
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createRemoveCommand() {
        return Commands.literal("remove")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer operator = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");

                            TeamManager.getPlayerTeam(operator.getUUID()).ifPresentOrElse(team -> {
                                if (team.isOperator(operator.getUUID())) {
                                    team.removeMember(target.getUUID());
                                    context.getSource().sendSuccess(() -> Component.literal("Removed " + target.getName().getString() + " from " + team.getName()), true);
                                    target.sendSystemMessage(Component.literal("You have been removed from team " + team.getName() + "."));
                                } else {
                                    context.getSource().sendFailure(Component.literal("You must be an operator of the team to remove players."));
                                }
                            }, () -> {
                                context.getSource().sendFailure(Component.literal("You are not in a team."));
                            });
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createOpCommand() {
        return Commands.literal("op")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer operator = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");

                            TeamManager.getPlayerTeam(operator.getUUID()).ifPresentOrElse(team -> {
                                if (team.isOperator(operator.getUUID())) {
                                    team.promote(target.getUUID());
                                    context.getSource().sendSuccess(() -> Component.literal("Promoted " + target.getName().getString() + " to operator in " + team.getName()), true);
                                    target.sendSystemMessage(Component.literal("You have been promoted to operator in team " + team.getName() + "."));
                                } else {
                                    context.getSource().sendFailure(Component.literal("You must be an operator of the team to promote players."));
                                }
                            }, () -> {
                                context.getSource().sendFailure(Component.literal("You are not in a team."));
                            });
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createColorCommand() {
        return Commands.literal("color")
                .then(Commands.argument("color", ColorArgument.color())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ChatFormatting color = ColorArgument.getColor(context, "color");

                            TeamManager.getPlayerTeam(player.getUUID()).ifPresentOrElse(team -> {
                                if (team.isOperator(player.getUUID())) {
                                    team.setColor(color);
                                    context.getSource().sendSuccess(() -> Component.literal("Team color changed to " + color.getName()), true);
                                } else {
                                    context.getSource().sendFailure(Component.literal("You must be an operator of the team to change the color."));
                                }
                            }, () -> {
                                context.getSource().sendFailure(Component.literal("You are not in a team."));
                            });
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createLeaveCommand() {
        return Commands.literal("leave")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    TeamManager.getPlayerTeam(player.getUUID()).ifPresentOrElse(team -> {
                        TeamManager.leaveTeam(player.getUUID());
                        context.getSource().sendSuccess(() -> Component.literal("You have left team " + team.getName()), true);
                    }, () -> {
                        context.getSource().sendFailure(Component.literal("You are not in a team."));
                    });
                    return 1;
                });
    }
} 