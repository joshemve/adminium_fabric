package com.adminium.mod.command;

import com.adminium.mod.manager.WaterDamageManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class WaterDamageCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> waterDamageCommand = Commands.literal("water_damage")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("on")
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    WaterDamageManager.setWaterDamageEnabled(true);

                    source.sendSuccess(() -> Text.literal("Water damage has been §aenabled§r (interval: " +
                        WaterDamageManager.getDamageIntervalTicks() + " ticks, damage: " +
                        WaterDamageManager.getDamageAmount() + " HP)"), true);
                    return 1;
                })
            )
            .then(Commands.literal("off")
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    WaterDamageManager.setWaterDamageEnabled(false);

                    source.sendSuccess(() -> Text.literal("Water damage has been §cdisabled"), true);
                    return 1;
                })
            )
            .then(Commands.literal("interval")
                .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 1200))
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        int ticks = IntegerArgumentType.getInteger(context, "ticks");
                        WaterDamageManager.setDamageIntervalTicks(ticks);

                        float seconds = ticks / 20.0f;
                        source.sendSuccess(() -> Text.literal("Water damage interval set to §e" + ticks +
                            " ticks§r (" + String.format("%.1f", seconds) + " seconds)"), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("damage")
                .then(Commands.argument("amount", FloatArgumentType.floatArg(0.5f, 20.0f))
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        float amount = FloatArgumentType.getFloat(context, "amount");
                        WaterDamageManager.setDamageAmount(amount);

                        source.sendSuccess(() -> Text.literal("Water damage amount set to §e" + amount + " HP"), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("status")
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    boolean enabled = WaterDamageManager.isWaterDamageEnabled();
                    int ticks = WaterDamageManager.getDamageIntervalTicks();
                    float amount = WaterDamageManager.getDamageAmount();
                    float seconds = ticks / 20.0f;

                    String status = enabled ? "§aenabled" : "§cdisabled";
                    source.sendSuccess(() -> Text.literal("Water damage is " + status + "\n" +
                        "§7Interval: §f" + ticks + " ticks (" + String.format("%.1f", seconds) + " seconds)\n" +
                        "§7Damage: §f" + amount + " HP"), false);
                    return 1;
                })
            )
            .then(Commands.literal("debug")
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    if (source.getEntity() instanceof ServerPlayerEntity player) {
                        boolean inWater = player.isInWater();
                        boolean underWater = player.isUnderWater();
                        boolean inWaterOrBubble = player.isInWaterOrBubble();
                        BlockPos pos = player.blockPosition();
                        FluidState fluidState = player.level().getFluidState(pos);
                        boolean inWaterBlock = fluidState.is(Fluids.WATER) || fluidState.is(Fluids.FLOWING_WATER);

                        source.sendSuccess(() -> Text.literal("§eDebug Info:\n" +
                            "§7isInWater: §f" + inWater + "\n" +
                            "§7isUnderWater: §f" + underWater + "\n" +
                            "§7isInWaterOrBubble: §f" + inWaterOrBubble + "\n" +
                            "§7Water block at pos: §f" + inWaterBlock + "\n" +
                            "§7Water damage enabled: §f" + WaterDamageManager.isWaterDamageEnabled()), false);
                    }
                    return 1;
                })
            );

        dispatcher.register(waterDamageCommand);
    }
}