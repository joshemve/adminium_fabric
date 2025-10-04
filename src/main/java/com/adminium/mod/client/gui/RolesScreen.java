package com.adminium.mod.client.gui;

import com.adminium.mod.network.ModNetworking;
import com.adminium.mod.network.RoleActionPacket;
import com.adminium.mod.roles.Role;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.text.Text;

public class RolesScreen extends Screen {
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 200; // Increased from 166 to 200
    private int leftPos;
    private int topPos;
    
    public RolesScreen() {
        super(Text.literal("Role Management"));
    }
    
    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;
        
        // Clear any existing widgets
        this.clearWidgets();
        
        // Role sections - vertical spacing
        int sectionStartY = topPos + 40;
        int sectionSpacing = 45;
        int buttonWidth = 80; // Reduced from 120
        int buttonHeight = 20;
        
        // Fighter Section
        addRenderableWidget(Button.builder(
            Text.literal("Edit Icon"),
            button -> openItemSelector(Role.FIGHTER))
            .pos(leftPos + GUI_WIDTH - buttonWidth - 10, sectionStartY) // Changed from -20 to -10
            .size(buttonWidth, buttonHeight)
            .build());
        
        // Farmer Section
        addRenderableWidget(Button.builder(
            Text.literal("Edit Icon"),
            button -> openItemSelector(Role.FARMER))
            .pos(leftPos + GUI_WIDTH - buttonWidth - 10, sectionStartY + sectionSpacing) // Changed from -20 to -10
            .size(buttonWidth, buttonHeight)
            .build());
        
        // Miner Section
        addRenderableWidget(Button.builder(
            Text.literal("Edit Icon"),
            button -> openItemSelector(Role.MINER))
            .pos(leftPos + GUI_WIDTH - buttonWidth - 10, sectionStartY + sectionSpacing * 2) // Changed from -20 to -10
            .size(buttonWidth, buttonHeight)
            .build());
        
        // Action buttons at the bottom
        int actionButtonY = topPos + GUI_HEIGHT - 40; // Changed from -35 to -40 for better spacing
        int actionButtonWidth = 90; // Reduced from 100 to 90 for better fit
        int buttonGap = 8; // Reduced from 10 to 8 for better fit
        
        // Calculate positions for three buttons in a row
        int totalButtonWidth = (actionButtonWidth * 3) + (buttonGap * 2);
        int actionButtonsStartX = leftPos + (GUI_WIDTH - totalButtonWidth) / 2;
        
        // Remove All Roles button
        addRenderableWidget(Button.builder(
            Text.literal("Remove All"),
            button -> {
                ModNetworking.sendToServer(new RoleActionPacket(RoleActionPacket.Action.REMOVE_ALL));
                minecraft.player.sendSystemMessage(Text.literal("Removing all roles..."));
            })
            .pos(actionButtonsStartX, actionButtonY)
            .size(actionButtonWidth, buttonHeight)
            .build());
        
        // Auto-assign button
        this.addRenderableWidget(Button.builder(
                Text.literal("Auto-Assign"),
                button -> {
                    ModNetworking.sendToServer(new RoleActionPacket(RoleActionPacket.Action.ASSIGN_ALL));
                    this.minecraft.setScreen(null);
                })
                .pos(actionButtonsStartX + actionButtonWidth + buttonGap, actionButtonY)
                .size(actionButtonWidth, buttonHeight)
                .build());
        
        // Manage Players button
        this.addRenderableWidget(Button.builder(
                Text.literal("Manage Players"),
                button -> {
                    this.minecraft.setScreen(new PlayerManagementScreen(this));
                })
                .pos(actionButtonsStartX + (actionButtonWidth + buttonGap) * 2, actionButtonY)
                .size(actionButtonWidth, buttonHeight)
                .build());
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Dark background
        this.renderBackground(guiGraphics);
        
        // Main panel background
        guiGraphics.fill(leftPos - 2, topPos - 2, leftPos + GUI_WIDTH + 2, topPos + GUI_HEIGHT + 2, 0xFF000000);
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xFF2B2B2B);
        
        // Title background bar
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + 25, 0xFF000000);
        
        // Render title
        guiGraphics.drawCenteredString(font, title, leftPos + GUI_WIDTH / 2, topPos + 8, 0xFFFFFF);
        
        // Role sections
        int sectionStartY = topPos + 40;
        int sectionSpacing = 45;
        int labelX = leftPos + 20;
        int iconX = leftPos + 90;
        
        // Section backgrounds
        for (int i = 0; i < 3; i++) {
            int y = sectionStartY + (i * sectionSpacing) - 5;
            guiGraphics.fill(leftPos + 10, y, leftPos + GUI_WIDTH - 10, y + 30, 0xFF1A1A1A);
        }
        
        // Fighter Section
        guiGraphics.drawString(font, "Fighter", labelX, sectionStartY + 6, Role.FIGHTER.getColor().getColor());
        guiGraphics.renderItem(Role.FIGHTER.getIconItem().getDefaultInstance(), iconX, sectionStartY);
        String fighterItemName = Role.FIGHTER.getIconItem().getDescription().getString();
        int maxTextWidth = GUI_WIDTH - 120 - (iconX + 25 - leftPos); // Leave space for button
        if (font.width(fighterItemName) > maxTextWidth) {
            fighterItemName = font.plainSubstrByWidth(fighterItemName, maxTextWidth - font.width("...")) + "...";
        }
        guiGraphics.drawString(font, fighterItemName, iconX + 25, sectionStartY + 6, 0x808080);
        
        // Farmer Section
        guiGraphics.drawString(font, "Farmer", labelX, sectionStartY + sectionSpacing + 6, Role.FARMER.getColor().getColor());
        guiGraphics.renderItem(Role.FARMER.getIconItem().getDefaultInstance(), iconX, sectionStartY + sectionSpacing);
        String farmerItemName = Role.FARMER.getIconItem().getDescription().getString();
        if (font.width(farmerItemName) > maxTextWidth) {
            farmerItemName = font.plainSubstrByWidth(farmerItemName, maxTextWidth - font.width("...")) + "...";
        }
        guiGraphics.drawString(font, farmerItemName, iconX + 25, sectionStartY + sectionSpacing + 6, 0x808080);
        
        // Miner Section
        guiGraphics.drawString(font, "Miner", labelX, sectionStartY + sectionSpacing * 2 + 6, Role.MINER.getColor().getColor());
        guiGraphics.renderItem(Role.MINER.getIconItem().getDefaultInstance(), iconX, sectionStartY + sectionSpacing * 2);
        String minerItemName = Role.MINER.getIconItem().getDescription().getString();
        if (font.width(minerItemName) > maxTextWidth) {
            minerItemName = font.plainSubstrByWidth(minerItemName, maxTextWidth - font.width("...")) + "...";
        }
        guiGraphics.drawString(font, minerItemName, iconX + 25, sectionStartY + sectionSpacing * 2 + 6, 0x808080);
        
        // Bottom separator line
        guiGraphics.fill(leftPos + 10, topPos + GUI_HEIGHT - 55, leftPos + GUI_WIDTH - 10, topPos + GUI_HEIGHT - 54, 0xFF404040); // Changed from -45 to -55
        
        // Render buttons and other widgets
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    private void openItemSelector(Role role) {
        minecraft.setScreen(new ItemSelectorScreen(role, this));
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
} 