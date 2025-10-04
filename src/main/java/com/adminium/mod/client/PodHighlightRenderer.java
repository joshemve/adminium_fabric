package com.adminium.mod.client;

import com.adminium.mod.ModItems;
import com.adminium.mod.manager.PodManager;
import com.adminium.mod.item.PodWandItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.math.Vec3d;

@OnlyIn(Dist.CLIENT)
public class PodHighlightRenderer {
    // TODO: Convert to Fabric event
        public static void onRenderWorld(RenderLevelStageEvent event) {
        // Draw only once at the end of entity rendering to avoid duplicate/offset boxes
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.level == null) return;

        ItemStack held = mc.player.getMainHandItem();
        if (!(held.getItem() instanceof PodWandItem)) {
            // Also allow if in offhand
            if (!(mc.player.getOffhandItem().getItem() instanceof PodWandItem)) return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3d cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        for (BlockPos pos : PodManager.getPods(mc.level.dimension())) {
            AABB box = new AABB(pos).inflate(0.01); // slightly inflate for visibility
            LevelRenderer.renderLineBox(poseStack, buffer.getBuffer(RenderType.lines()),
                box.move(-cameraPos.x, -cameraPos.y, -cameraPos.z),
                0f, 1f, 0f, 0.75f);
        }
        buffer.endBatch(RenderType.lines());
    }
} 