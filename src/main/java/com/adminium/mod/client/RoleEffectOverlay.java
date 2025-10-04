package com.adminium.mod.client;

import com.adminium.mod.roles.Role;
import com.adminium.mod.roles.RoleManager;
import com.adminium.mod.ModEffects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;

public class RoleEffectOverlay {
    // TODO: Convert to Fabric event
        public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        // Only inject in inventory screen overlay pass
        if (!(MinecraftClient.getInstance().screen instanceof InventoryScreen inv)) return;

        var player = MinecraftClient.getInstance().player;
        if (player == null) return;

        Role role = RoleManager.getPlayerRole(player.getUUID());
        if (role == null) return;

        // Determine the effect instance and y-position where vanilla rendered it
        StatusEffectInstance effectInst = null;
        if (role == Role.FIGHTER) effectInst = player.getEffect(ModEffects.FIGHTER.get());
        else if (role == Role.MINER) effectInst = player.getEffect(ModEffects.MINER.get());
        else if (role == Role.FARMER) effectInst = player.getEffect(ModEffects.FARMER.get());

        if (effectInst == null) return; // should not happen

        int index = 0;
        for (StatusEffectInstance inst : player.getActiveEffects()) {
            if (inst == effectInst) break;
            index++;
        }

        // Vanilla positions each entry starting at y=others? assume top = inv.topPos - 34
        int guiLeft = (inv.width - 176) / 2;
        int guiTop = (inv.height - 166) / 2;
        int x = guiLeft - 124; // vanilla effect list offset
        int y = guiTop + 9 + index * 26;

        // Draw the role's item icon
        ItemStack iconStack = new ItemStack(role.getIconItem());
        GuiGraphics gg = event.getGuiGraphics();
        gg.renderItem(iconStack, x + 6, y + 6);
    }
} 