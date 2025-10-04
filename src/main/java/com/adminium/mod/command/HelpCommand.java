package com.adminium.mod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

public class HelpCommand {
    private static final String[] HELP_LINES = new String[]{
        "§6Adminium Commands:",
        "§e/vanish [player]§7 - Toggle invisibility",
        "§e/heal [player]§7 - Restore health",
        "§e/god [on|off] [player]§7 - Toggle god mode",
        "§e/safe [on|off] [player]§7 - Toggle safe mode (no death)",
        "§e/freeze <on|off>§7 - Freeze the server",        
        "§e/pvp <on|off>§7 - Enable or disable PvP",
        "§e/nether <on|off>§7 - Enable or disable Nether portals",
        "§e/end <on|off>§7 - Enable or disable End portals",
        "§e/instaban <on|off>§7 - Toggle InstaBan in hardcore",
        "§e/announce <message>§7 - Broadcast announcement",
        "§e/bans§7 - View or manage bans",
        "§e/roles§7 - Open role management GUI",
        "§e/ateam ...§7 - Team management commands",
        "§e/tm <message>§7 - Team chat",
        "§e/warp <name>§7 - Teleport to warp",
        "§e/setwarp <name>§7 - Create warp",
        "§e/delwarp <name>§7 - Delete warp",
        "§e/invsee <player>§7 - View a player's inventory",
        "§e/endchest [player]§7 - View ender chest",
        "§e/pods§7 - Open pod management GUI",
        "§e/goldenapple_limit§7 - View and set golden apple cap",
        "§e/enchantban§7 - Manage enchantment table level limits",
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // /adminium help
        dispatcher.register(Commands.literal("adminium")
            .then(Commands.literal("help")
                .executes(HelpCommand::execute)));

        // /ahelp shortcut
        dispatcher.register(Commands.literal("ahelp")
            .executes(HelpCommand::execute));
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player;
        try {
            player = context.getSource().getPlayerOrException();
        } catch (Exception e) {
            return 0; // Console or command block
        }
        for (String line : HELP_LINES) {
            player.sendSystemMessage(Text.literal(line));
        }
        return 1;
    }
} 