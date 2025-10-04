package com.adminium.mod;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEffects {
    public static StatusEffect FIGHTER;
    public static StatusEffect MINER;
    public static StatusEffect FARMER;

    public static void register() {
        FIGHTER = Registry.register(Registries.STATUS_EFFECT,
            new Identifier(Adminium.MODID, "fighter_role"),
            new SimpleRoleEffect());

        MINER = Registry.register(Registries.STATUS_EFFECT,
            new Identifier(Adminium.MODID, "miner_role"),
            new SimpleRoleEffect());

        FARMER = Registry.register(Registries.STATUS_EFFECT,
            new Identifier(Adminium.MODID, "farmer_role"),
            new SimpleRoleEffect());
    }

    // Simple status effect with no behavior
    private static class SimpleRoleEffect extends StatusEffect {
        protected SimpleRoleEffect() {
            super(StatusEffectCategory.NEUTRAL, 0xFFFFFF);
        }
    }
}