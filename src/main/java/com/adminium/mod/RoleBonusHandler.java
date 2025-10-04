package com.adminium.mod;

import com.adminium.mod.roles.Role;
import com.adminium.mod.roles.RoleManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.ItemStack;
import net.minecraft.block.CropBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.block.Block;
import net.minecraft.inventory.Inventories;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.item.PickaxeItem;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;

import java.util.Random;
import java.util.List;

public class RoleBonusHandler {
    private static final Random RANDOM = new Random();

    private static final java.util.UUID FIGHTER_DAMAGE_UUID = java.util.UUID.fromString("0e02ef0a-b3cf-4d6c-bfac-29a2e0b600aa");
    private static final java.util.UUID MINER_WEAKNESS_UUID = java.util.UUID.fromString("8eede7c0-fc83-4eed-9f3a-f0ad720f8b9d");

    /**
     * Apply role benefits immediately to a player
     */
    public static void applyRoleBenefits(ServerPlayerEntity player, Role role) {
        var attrInstance = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attrInstance == null) return;

        // Apply role mob effect visuals (duration 210 ticks)
        if (role == Role.FIGHTER) {
            ensureEffect(player, ModEffects.FIGHTER);
            player.removeStatusEffect(ModEffects.MINER);
            player.removeStatusEffect(ModEffects.FARMER);
        } else if (role == Role.MINER) {
            ensureEffect(player, ModEffects.MINER);
            player.removeStatusEffect(ModEffects.FIGHTER);
            player.removeStatusEffect(ModEffects.FARMER);
        } else if (role == Role.FARMER) {
            ensureEffect(player, ModEffects.FARMER);
            player.removeStatusEffect(ModEffects.FIGHTER);
            player.removeStatusEffect(ModEffects.MINER);
        } else {
            player.removeStatusEffect(ModEffects.FIGHTER);
            player.removeStatusEffect(ModEffects.MINER);
            player.removeStatusEffect(ModEffects.FARMER);
        }

        boolean hasMod = attrInstance.getModifier(FIGHTER_DAMAGE_UUID) != null;
        if (role == Role.FIGHTER) {
            if (!hasMod) {
                EntityAttributeModifier mod = new EntityAttributeModifier(FIGHTER_DAMAGE_UUID, "Fighter bonus", 2.0, EntityAttributeModifier.Operation.ADDITION);
                attrInstance.addPersistentModifier(mod);
            }
        } else {
            if (hasMod) {
                attrInstance.removeModifier(FIGHTER_DAMAGE_UUID);
            }
        }

        // Ensure any legacy miner weakness modifier is removed
        if (attrInstance.getModifier(MINER_WEAKNESS_UUID) != null) {
            attrInstance.removeModifier(MINER_WEAKNESS_UUID);
        }
    }

    /**
     * Handle player tick events for role benefits
     * This should be called from the main mod's tick event handler
     */
    public static void handlePlayerTick(ServerPlayerEntity player) {
        // Run this handler once every 20 ticks per player (~1 second) to reduce load.
        if ((player.age % 20) != 0) {
            return;
        }

        var attrInstance = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attrInstance == null) return;

        Role role = RoleManager.getPlayerRole(player.getUuid());

        // maintain role mob effect visuals (duration 210 ticks)
        if (role == Role.FIGHTER) {
            ensureEffect(player, ModEffects.FIGHTER);
            player.removeStatusEffect(ModEffects.MINER);
            player.removeStatusEffect(ModEffects.FARMER);
        } else if (role == Role.MINER) {
            ensureEffect(player, ModEffects.MINER);
            player.removeStatusEffect(ModEffects.FIGHTER);
            player.removeStatusEffect(ModEffects.FARMER);
        } else if (role == Role.FARMER) {
            ensureEffect(player, ModEffects.FARMER);
            player.removeStatusEffect(ModEffects.FIGHTER);
            player.removeStatusEffect(ModEffects.MINER);
        } else {
            player.removeStatusEffect(ModEffects.FIGHTER);
            player.removeStatusEffect(ModEffects.MINER);
            player.removeStatusEffect(ModEffects.FARMER);
        }

        boolean hasMod = attrInstance.getModifier(FIGHTER_DAMAGE_UUID) != null;
        if (role == Role.FIGHTER) {
            if (!hasMod) {
                EntityAttributeModifier mod = new EntityAttributeModifier(FIGHTER_DAMAGE_UUID, "Fighter bonus", 2.0, EntityAttributeModifier.Operation.ADDITION);
                attrInstance.addPersistentModifier(mod);
            }

            // Fighter debuff: steady exhaustion (extra hunger)
            player.addExhaustion(0.01f); // once per second (matches handler interval)
        } else {
            if (hasMod) {
                attrInstance.removeModifier(FIGHTER_DAMAGE_UUID);
            }
        }

        // Miner weakness attribute has been removed in favour of damage scaling
        // Ensure any legacy modifier is removed so miners can still damage entities.
        if (attrInstance.getModifier(MINER_WEAKNESS_UUID) != null) {
            attrInstance.removeModifier(MINER_WEAKNESS_UUID);
        }
    }

    private static void ensureEffect(ServerPlayerEntity player, StatusEffect effect) {
        int desired = 210;
        StatusEffectInstance inst = player.getStatusEffect(effect);
        if (inst == null || inst.getDuration() <= 40) {
            // ambient = false, showParticles = false, showIcon = false
            // This prevents the swirling effect particles from rendering on the player as well as the small icon in the HUD.
            player.addStatusEffect(new StatusEffectInstance(effect, desired, 0, false, false, false));
        }
    }

    /**
     * Get looting level for a player (Fighters get +2)
     */
    public static int getLootingLevel(ServerPlayerEntity player, int baseLevel) {
        if (RoleManager.getPlayerRole(player.getUuid()) == Role.FIGHTER) {
            return baseLevel + 2;
        }
        return baseLevel;
    }

    /**
     * Handle block breaking for role bonuses
     * Returns true if the block break should be allowed
     */
    public static boolean handleBlockBreak(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state) {
        // Barrel protection - prevent breaking barrels unless in creative mode or protection is disabled
        if (state.getBlock() == Blocks.BARREL) {
            if (!com.adminium.mod.manager.BarrelProtectionManager.isBarrelBreakingEnabled() && !player.isCreative()) {
                return false;
            }
        }

        Role role = RoleManager.getPlayerRole(player.getUuid());
        if (role == null) return true;

        if (role == Role.MINER) {
            // Simple fortune-like bonus: 50% chance to drop an extra item stack (ores mostly)
            if (!state.isAir() && RANDOM.nextFloat() < 0.5f) {
                List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player, player.getMainHandStack());
                for (ItemStack stack : drops) {
                    if (!stack.isEmpty()) {
                        Block.dropStack(world, pos, stack);
                    }
                }
            }
        } else if (role == Role.FARMER && state.getBlock() instanceof CropBlock cropBlock) {
            // Farmers get double drops from fully grown crops
            if (cropBlock.isMature(state)) {
                List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player, player.getMainHandStack());
                for (ItemStack stack : drops) {
                    if (!stack.isEmpty()) {
                        Block.dropStack(world, pos, stack);
                    }
                }
            }
        }

        return true;
    }

    /**
     * Apply damage modifiers based on attacker role
     * Returns the modified damage amount
     */
    public static float modifyDamage(ServerPlayerEntity attacker, PlayerEntity target, float amount) {
        Role role = RoleManager.getPlayerRole(attacker.getUuid());
        if (role == null) return amount;

        // Only reduce damage when the target is another player (no impact on mobs)
        if (role == Role.FARMER) {
            return amount * 0.9f; // -10% damage to players
        } else if (role == Role.MINER) {
            return amount * 0.8f; // -20% damage to players
        }

        return amount;
    }
}