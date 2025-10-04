package com.adminium.mod.command;

import com.adminium.mod.manager.NicknameManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.EntityArgument;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import com.mojang.brigadier.arguments.StringArgumentType;

public class NickCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(Commands.literal("nick")
            .requires(src -> src.hasPermission(2))
            // /nick clear <player>
            .then(Commands.literal("clear")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(NickCommand::clearNick)))
            // /nick <player> [nickname|clear]
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.literal("clear").executes(NickCommand::clearNick))
                .then(Commands.argument("nickname", StringArgumentType.greedyString())
                    .executes(NickCommand::setNick))
                .executes(NickCommand::clearNick))
        );
    }

    private static int setNick(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgument.getPlayer(ctx, "player");
        String nick = StringArgumentType.getString(ctx, "nickname");
        NicknameManager.setNickname(target.getUUID(), nick);
        refreshDisplay(target);
        ctx.getSource().sendSuccess(() -> Text.literal("Set nickname for " + target.getGameProfile().getName() + " to " + nick), true);
        return 1;
    }

    private static int clearNick(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgument.getPlayer(ctx, "player");
        NicknameManager.clearNickname(target.getUUID());
        refreshDisplay(target);
        ctx.getSource().sendSuccess(() -> Text.literal("Cleared nickname for " + target.getGameProfile().getName()), true);
        return 1;
    }

    private static void refreshDisplay(ServerPlayerEntity player) {
        // Refresh only this player's display name to avoid full rebuild.
        com.adminium.mod.Adminium.refreshPlayerDisplayName(player);

        // Update the visible nametag above the player's head.
        String nick = com.adminium.mod.manager.NicknameManager.getNickname(player.getUUID());
        if (nick != null) {
            // Build name with role + team prefix (same logic as tab list) but include nickname.
            net.minecraft.network.chat.MutableText nameComp = net.minecraft.network.chat.Text.empty();
            var roleIcon = com.adminium.mod.roles.RoleManager.getPlayerRolePrefixComponent(player.getUUID());
            if (roleIcon != null && !roleIcon.getString().isEmpty()) {
                nameComp.append(roleIcon).append(" ");
            }
            String teamPrefix = com.adminium.mod.team.TeamManager.getPlayerTeamPrefix(player.getUUID());
            if (teamPrefix != null && !teamPrefix.isEmpty()) {
                nameComp.append(net.minecraft.network.chat.Text.literal(teamPrefix + " "));
            }
            nameComp.append(net.minecraft.network.chat.Text.literal(nick));

            player.setCustomName(nameComp);
            player.setCustomNameVisible(true);
        } else {
            // Clear custom name so default (team prefix + username) shows.
            player.setCustomName(null);
            player.setCustomNameVisible(false);
        }
    }
} 