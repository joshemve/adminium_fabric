package com.adminium.mod.client.gui;

import com.adminium.mod.network.ModNetworking;
import com.adminium.mod.network.RequestPlayersPacket;
import com.adminium.mod.network.SetPlayerRolePacket;
import com.adminium.mod.roles.Role;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.*;

public class PlayerManagementScreen extends Screen {
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 200;
    
    private final Screen previousScreen;
    private final List<PlayerEntry> players = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int PLAYERS_PER_PAGE = 6;
    private static final int ENTRY_HEIGHT = 24;

    // Flag to track if there are unsaved changes
    private boolean hasUnsavedChanges = false;
    
    public PlayerManagementScreen(Screen previousScreen) {
        super(Text.literal("PlayerEntity Role Management"));
        this.previousScreen = previousScreen;
    }
    
    @Override
    protected void init() {
        // Request player list from server
        ModNetworking.sendToServer(new RequestPlayersPacket());
        
        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;
        
        // Back button
        this.addRenderableWidget(Button.builder(
                Text.literal("Back"),
                button -> this.minecraft.setScreen(previousScreen))
                .bounds(guiLeft + 10, guiTop + GUI_HEIGHT - 30, 60, 20)
                .build());

        // Save / Apply button
        this.addRenderableWidget(Button.builder(
                Text.literal("Save"),
                button -> {
                    // Send packets for all changed entries
                    for (PlayerEntry entry : players) {
                        if (entry.currentRole != entry.originalRole) {
                            ModNetworking.sendToServer(new SetPlayerRolePacket(entry.profile.getId(), entry.currentRole));
                            entry.originalRole = entry.currentRole; // mark as saved
                        }
                    }

                    this.hasUnsavedChanges = false;
                    if (this.minecraft.player != null) {
                        this.minecraft.player.sendSystemMessage(Text.literal("§aRoles updated!"));
                    }
                })
                .bounds(guiLeft + 80, guiTop + GUI_HEIGHT - 30, 60, 20)
                .build());
        
        // Scroll up button
        this.addRenderableWidget(Button.builder(
                Text.literal("▲"),
                button -> {
                    if (scrollOffset > 0) scrollOffset--;
                })
                .bounds(guiLeft + GUI_WIDTH - 30, guiTop + 30, 20, 20)
                .build());
        
        // Scroll down button
        this.addRenderableWidget(Button.builder(
                Text.literal("▼"),
                button -> {
                    int maxScroll = Math.max(0, players.size() - PLAYERS_PER_PAGE);
                    if (scrollOffset < maxScroll) scrollOffset++;
                })
                .bounds(guiLeft + GUI_WIDTH - 30, guiTop + GUI_HEIGHT - 50, 20, 20)
                .build());
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        
        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;
        
        // Draw background
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xFF000000);
        graphics.fill(guiLeft + 1, guiTop + 1, guiLeft + GUI_WIDTH - 1, guiTop + GUI_HEIGHT - 1, 0xFF444444);
        
        // Draw title area
        graphics.fill(guiLeft + 1, guiTop + 1, guiLeft + GUI_WIDTH - 1, guiTop + 25, 0xFF222222);
        
        // Title
        graphics.drawCenteredString(this.font, this.title, this.width / 2, guiTop + 10, 0xFFFFFF);
        
        // Render player entries
        int y = guiTop + 35;
        for (int i = scrollOffset; i < Math.min(scrollOffset + PLAYERS_PER_PAGE, players.size()); i++) {
            renderPlayerEntry(graphics, players.get(i), guiLeft + 10, y, mouseX, mouseY);
            y += ENTRY_HEIGHT;
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private void renderPlayerEntry(GuiGraphics graphics, PlayerEntry entry, int x, int y, int mouseX, int mouseY) {
        // Draw player head
        renderPlayerHead(graphics, entry.profile, x, y, 20);
        
        // Draw player name
        graphics.drawString(this.font, entry.profile.getName(), x + 25, y + 6, 0xFFFFFF);
        
        // Draw current role
        if (entry.currentRole != null) {
            Text roleText = Text.literal(entry.currentRole.getDisplayName()).withStyle(entry.currentRole.getColor());
            graphics.drawString(this.font, roleText, x + 100, y + 6, entry.currentRole.getColor().getColor());
        }
        
        // Role selection buttons - moved left to avoid scroll buttons
        int buttonX = x + 150; // Moved from 180 to 150
        for (Role role : Role.values()) {
            boolean isSelected = entry.currentRole == role;
            renderRoleButton(graphics, role, buttonX, y, 16, 16, isSelected, mouseX, mouseY);
            buttonX += 18; // Reduced spacing from 20 to 18
        }
    }
    
    private void renderRoleButton(GuiGraphics graphics, Role role, int x, int y, int width, int height, boolean selected, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        
        // Draw button background
        int color = selected ? 0xFF88FF88 : (hovered ? 0xFF666666 : 0xFF444444);
        graphics.fill(x, y, x + width, y + height, color);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF000000);
        
        // Draw role icon
        ItemStack icon = new ItemStack(role.getIconItem());
        graphics.renderItem(icon, x, y);
        
        if (hovered) {
            graphics.renderTooltip(this.font, Text.literal(role.getDisplayName()), mouseX, mouseY);
        }
    }
    
    private void renderPlayerHead(GuiGraphics graphics, GameProfile profile, int x, int y, int size) {
        // For now, render a placeholder steve head
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        graphics.renderItem(head, x, y);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;
        
        // Check role button clicks
        int y = guiTop + 35;
        for (int i = scrollOffset; i < Math.min(scrollOffset + PLAYERS_PER_PAGE, players.size()); i++) {
            PlayerEntry entry = players.get(i);
            int buttonX = guiLeft + 10 + 150; // Updated to match render position
            
            for (Role role : Role.values()) {
                if (mouseX >= buttonX && mouseX < buttonX + 16 && mouseY >= y && mouseY < y + 16) {
                    // Select role locally; sending happens when user clicks Save
                    entry.currentRole = role;
                    this.hasUnsavedChanges = true;
                    return true;
                }
                buttonX += 18; // Updated to match render spacing
            }
            y += ENTRY_HEIGHT;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    public void updatePlayerList(List<PlayerEntry> newPlayers) {
        this.players.clear();
        this.players.addAll(newPlayers);
        this.players.sort(Comparator.comparing(e -> e.profile.getName()));
    }
    
    public static class PlayerEntry {
        public final GameProfile profile;
        public Role currentRole;
        public Role originalRole;
        
        public PlayerEntry(GameProfile profile, Role currentRole) {
            this.profile = profile;
            this.currentRole = currentRole;
            this.originalRole = currentRole;
        }
    }
} 