package com.adminium.mod.event;

import com.adminium.mod.manager.BarrelLootManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Random;

public class BarrelLootHandler {
    private static final Random RANDOM = new Random();

    // TODO: Convert to Fabric event
        public static void onBarrelInteract(PlayerInteractEvent.RightClickBlock event) {
        World level = event.getLevel();
        if (level.isClientSide()) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        PlayerEntity player = event.getEntity();

        if (state.getBlock() instanceof BarrelBlock) {
            // Cancel the normal barrel opening
            event.setCanceled(true);
            event.setUseBlock(Event.Result.DENY);
            event.setUseItem(Event.Result.DENY);

            // Check if barrel has been looted
            if (!BarrelLootManager.isBarrelLooted(pos)) {
                // Generate loot
                List<ItemStack> loot = BarrelLootManager.generateLoot();

                if (!loot.isEmpty()) {
                    // Drop items
                    for (ItemStack stack : loot) {
                        if (!stack.isEmpty()) {
                            dropItem(level, pos, stack, player);
                        }
                    }

                    // Play effects
                    playBarrelOpenEffects((ServerWorld) level, pos);

                    // Mark as looted
                    BarrelLootManager.markBarrelLooted(pos);

                    // Set barrel to open state
                    if (state.hasProperty(BlockStateProperties.OPEN)) {
                        BlockState openState = state.setValue(BlockStateProperties.OPEN, Boolean.TRUE);
                        level.setBlock(pos, openState, 3);
                    }
                } else {
                    // No loot generated, play empty sound but still open
                    level.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.5f, 1.2f);

                    // Mark as looted anyway
                    BarrelLootManager.markBarrelLooted(pos);

                    // Set barrel to open state
                    if (state.hasProperty(BlockStateProperties.OPEN)) {
                        BlockState openState = state.setValue(BlockStateProperties.OPEN, Boolean.TRUE);
                        level.setBlock(pos, openState, 3);
                    }
                }
            } else {
                // Already looted, just play empty sound
                level.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.5f, 1.5f);
            }
        }
    }

    private static void dropItem(World level, BlockPos pos, ItemStack stack, PlayerEntity player) {
        // Calculate spawn position (slightly above the barrel)
        double x = pos.getX() + 0.5 + (RANDOM.nextDouble() - 0.5) * 0.3;
        double y = pos.getY() + 1.1;
        double z = pos.getZ() + 0.5 + (RANDOM.nextDouble() - 0.5) * 0.3;

        // Create item entity
        ItemEntity itemEntity = new ItemEntity(level, x, y, z, stack);

        // Add some upward velocity for a nice drop effect
        double vx = (RANDOM.nextDouble() - 0.5) * 0.1;
        double vy = RANDOM.nextDouble() * 0.2 + 0.2;
        double vz = (RANDOM.nextDouble() - 0.5) * 0.1;
        itemEntity.setDeltaMovement(vx, vy, vz);

        // Set pickup delay
        itemEntity.setPickUpDelay(10);

        // Spawn the item
        level.addFreshEntity(itemEntity);
    }

    private static void playBarrelOpenEffects(ServerWorld level, BlockPos pos) {
        // Play barrel open sound
        level.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 1.0f, 0.9f);

        // Spawn particles
        Vec3d particlePos = Vec3d.atCenterOf(pos).add(0, 0.5, 0);

        // Happy villager particles to indicate loot
        for (int i = 0; i < 10; i++) {
            double px = particlePos.x + (RANDOM.nextDouble() - 0.5);
            double py = particlePos.y + RANDOM.nextDouble() * 0.5;
            double pz = particlePos.z + (RANDOM.nextDouble() - 0.5);

            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                px, py, pz,
                1, 0, 0, 0, 0);
        }

        // Some poof particles
        level.sendParticles(ParticleTypes.POOF,
            particlePos.x, particlePos.y, particlePos.z,
            5, 0.2, 0.2, 0.2, 0.02);
    }
}