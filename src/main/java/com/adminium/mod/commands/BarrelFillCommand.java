package com.adminium.mod.commands;

import com.adminium.mod.manager.BarrelLootManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;

public class BarrelFillCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> barrelFillCommand = Commands.literal("barrel_fill")
            .requires(source -> source.hasPermission(2)) // Require admin permission
            .executes(context -> {
                ServerCommandSource source = context.getSource();

                int resetCount = BarrelLootManager.fillAllBarrels();

                if (resetCount > 0) {
                    source.sendSuccess(() -> Text.literal("Successfully reset " + resetCount + " barrels! They can now drop loot again."), true);
                } else {
                    source.sendSuccess(() -> Text.literal("All barrels are already ready to drop loot!"), true);
                }

                return resetCount;
            });

        dispatcher.register(barrelFillCommand);
    }
}