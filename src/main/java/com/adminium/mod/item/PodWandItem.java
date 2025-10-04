package com.adminium.mod.item;

import com.adminium.mod.manager.PodManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.World;
import net.minecraft.text.Text;

public class PodWandItem extends Item {
    public PodWandItem(Properties props) {
        super(props);
    }

    // Called when right-clicking a block
    @Override
    public ActionResult useOn(UseOnContext ctx) {
        World level = ctx.getLevel();
        if (level.isClientSide) {
            return ActionResult.SUCCESS;
        }

        ServerWorld serverLevel = (ServerWorld) level;
        ServerPlayerEntity player = (ServerPlayerEntity) ctx.getPlayer();
        if (player == null) return ActionResult.SUCCESS;

        BlockPos clickedPos = ctx.getClickedPos().immutable();
        // Use top-center of the block as reference spawn point
        boolean isSneaking = player.isShiftKeyDown();

        if (isSneaking) {
            boolean removed = PodManager.removePod(serverLevel.dimension(), clickedPos);
            if (removed) {
                player.sendSystemMessage(Text.literal("Removed pod at " + formatPos(clickedPos)));
            } else {
                player.sendSystemMessage(Text.literal("No pod at that block to remove."));
            }
        } else {
            if (PodManager.getPods(serverLevel.dimension()).contains(clickedPos)) {
                player.sendSystemMessage(Text.literal("That block is already registered as a pod."));
            } else {
                PodManager.addPod(serverLevel.dimension(), clickedPos);
                player.sendSystemMessage(Text.literal("Added pod at " + formatPos(clickedPos)));
            }
        }
        return ActionResult.SUCCESS;
    }

    private static String formatPos(BlockPos pos) {
        return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
    }
} 