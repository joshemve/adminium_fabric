package com.adminium.mod.command;

import com.adminium.mod.manager.DisabledItemsManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;

public class DisabledItemsCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> root = Commands.literal("disableditems")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                boolean enabled = DisabledItemsManager.isInventoryRemovalEnabled();
                context.getSource().sendSuccess(() -> Text.literal("Disabled item inventory removal is currently " + (enabled ? "enabled" : "disabled")), false);
                return enabled ? 1 : 0;
            })
            .then(Commands.literal("inventoryremoval")
                .executes(context -> {
                    boolean enabled = DisabledItemsManager.isInventoryRemovalEnabled();
                    context.getSource().sendSuccess(() -> Text.literal("Disabled item inventory removal is currently " + (enabled ? "enabled" : "disabled")), false);
                    return enabled ? 1 : 0;
                })
                .then(Commands.literal("toggle").executes(context -> {
                    boolean enabled = DisabledItemsManager.toggleInventoryRemovalEnabled();
                    context.getSource().sendSuccess(() -> Text.literal("Disabled item inventory removal has been " + (enabled ? "enabled" : "disabled")), true);
                    return enabled ? 1 : 0;
                }))
                .then(Commands.literal("set")
                    .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean enabled = BoolArgumentType.getBool(context, "value");
                            DisabledItemsManager.setInventoryRemovalEnabled(enabled);
                            context.getSource().sendSuccess(() -> Text.literal("Disabled item inventory removal has been " + (enabled ? "enabled" : "disabled")), true);
                            return enabled ? 1 : 0;
                        })
                    )
                )
            );

        dispatcher.register(root);
    }
}
