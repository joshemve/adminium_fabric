package com.adminium.mod.command;

import com.adminium.mod.manager.BarrelProtectionManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;

public class BarrelBreakableCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> root = Commands.literal("barrel_breakable")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                boolean enabled = BarrelProtectionManager.isBarrelBreakingEnabled();
                context.getSource().sendSuccess(() -> Text.literal("Barrel breaking is currently " + (enabled ? "enabled" : "disabled")), false);
                return enabled ? 1 : 0;
            })
            .then(Commands.literal("on")
                .executes(context -> {
                    BarrelProtectionManager.setBarrelBreakingEnabled(true);
                    context.getSource().sendSuccess(() -> Text.literal("§aBarrel breaking has been enabled"), true);
                    return 1;
                })
            )
            .then(Commands.literal("off")
                .executes(context -> {
                    BarrelProtectionManager.setBarrelBreakingEnabled(false);
                    context.getSource().sendSuccess(() -> Text.literal("§cBarrel breaking has been disabled"), true);
                    return 1;
                })
            )
            .then(Commands.literal("toggle")
                .executes(context -> {
                    boolean enabled = BarrelProtectionManager.toggleBarrelBreaking();
                    context.getSource().sendSuccess(() -> Text.literal("Barrel breaking has been " + (enabled ? "§aenabled" : "§cdisabled")), true);
                    return enabled ? 1 : 0;
                })
            )
            .then(Commands.literal("status")
                .executes(context -> {
                    boolean enabled = BarrelProtectionManager.isBarrelBreakingEnabled();
                    String status = enabled ? "§aenabled" : "§cdisabled";
                    context.getSource().sendSuccess(() -> Text.literal("Barrel breaking is " + status), false);
                    return enabled ? 1 : 0;
                })
            );

        dispatcher.register(root);
    }
}
