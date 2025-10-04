package com.adminium.mod.client.gui;

import com.adminium.mod.manager.BanManager;
import com.adminium.mod.network.BanActionPacket;
import com.adminium.mod.network.ModNetworking;
import com.adminium.mod.network.RequestBanListPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class BanManagementScreen extends Screen {
    private final List<BanEntry> banEntries = new ArrayList<>();
    private EditBox searchBox;
    private int scrollOffset = 0;
    private static final int ENTRIES_PER_PAGE = 8;
    private static final int ENTRY_HEIGHT = 30;
    
    private int leftPos;
    private int topPos;
    private final int imageWidth = 320;
    private final int imageHeight = 240;
    
    public BanManagementScreen() {
        super(Text.literal("Ban Management"));
    }
    
    @Override
    protected void init() {
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        
        // Search box
        this.searchBox = new EditBox(this.font, leftPos + 10, topPos + 10, 200, 20, Text.literal("Search"));
        this.searchBox.setHint(Text.literal("Search banned players..."));
        this.addWidget(this.searchBox);
        
        // Refresh button
        this.addRenderableWidget(Button.builder(Text.literal("Refresh"), button -> {
            requestBanList();
        }).bounds(leftPos + 220, topPos + 10, 60, 20).build());
        
        // Close button
        this.addRenderableWidget(Button.builder(Text.literal("Close"), button -> {
            this.onClose();
        }).bounds(leftPos + imageWidth - 70, topPos + imageHeight - 30, 60, 20).build());
        
        // Scroll buttons (moved down to avoid overlap with revert buttons)
        this.addRenderableWidget(Button.builder(Text.literal("↑"), button -> {
            if (scrollOffset > 0) {
                scrollOffset--;
            }
        }).bounds(leftPos + imageWidth - 30, topPos + 100, 20, 20).build());
        
        this.addRenderableWidget(Button.builder(Text.literal("↓"), button -> {
            if (scrollOffset < Math.max(0, banEntries.size() - ENTRIES_PER_PAGE)) {
                scrollOffset++;
            }
        }).bounds(leftPos + imageWidth - 30, topPos + 130, 20, 20).build());
        
        // Request initial ban list
        requestBanList();
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        // Draw background
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0x88000000);
        guiGraphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0x88222222);
        
        // Title
        guiGraphics.drawCenteredString(this.font, "Ban Management", leftPos + imageWidth / 2, topPos + 5, 0xFFFFFF);
        
        // Draw ban entries
        String searchText = searchBox.getValue().toLowerCase();
        List<BanEntry> filteredEntries = banEntries.stream()
            .filter(entry -> entry.playerName.toLowerCase().contains(searchText) || 
                           entry.reason.toLowerCase().contains(searchText))
            .toList();
        
        int startIndex = scrollOffset;
        int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, filteredEntries.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            BanEntry entry = filteredEntries.get(i);
            int yPos = topPos + 40 + (i - startIndex) * ENTRY_HEIGHT;
            
            // Entry background
            guiGraphics.fill(leftPos + 10, yPos, leftPos + imageWidth - 50, yPos + ENTRY_HEIGHT - 2, 0x44FFFFFF);
            
            // PlayerEntity name
            guiGraphics.drawString(this.font, entry.playerName, leftPos + 15, yPos + 3, 0xFFFFFF);
            
            // Reason (truncated)
            String reason = entry.reason.length() > 30 ? entry.reason.substring(0, 27) + "..." : entry.reason;
            guiGraphics.drawString(this.font, "Reason: " + reason, leftPos + 15, yPos + 13, 0xCCCCCC);
            
            // Ban date and type
            String banInfo = entry.banDate + (entry.isHardcoreDeath ? " (Hardcore)" : "");
            guiGraphics.drawString(this.font, banInfo, leftPos + 15, yPos + 23, 0x888888);
            
            // Pardon button (InstaBan automatically handles survival mode)
            Button pardonButton = Button.builder(Text.literal("Pardon"), button -> {
                pardonPlayer(entry.playerUUID);
            }).bounds(leftPos + imageWidth - 100, yPos + 2, 60, 18).build();
            pardonButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        
        // Show total count
        guiGraphics.drawString(this.font, "Total bans: " + filteredEntries.size(), 
            leftPos + 10, topPos + imageHeight - 20, 0xFFFFFF);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle button clicks for dynamically rendered buttons
        String searchText = searchBox.getValue().toLowerCase();
        List<BanEntry> filteredEntries = banEntries.stream()
            .filter(entry -> entry.playerName.toLowerCase().contains(searchText) || 
                           entry.reason.toLowerCase().contains(searchText))
            .toList();
        
        int startIndex = scrollOffset;
        int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, filteredEntries.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            BanEntry entry = filteredEntries.get(i);
            int yPos = topPos + 40 + (i - startIndex) * ENTRY_HEIGHT;
            
            // Check pardon button
            if (mouseX >= leftPos + imageWidth - 100 && mouseX <= leftPos + imageWidth - 40 &&
                mouseY >= yPos + 2 && mouseY <= yPos + 20) {
                pardonPlayer(entry.playerUUID);
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void requestBanList() {
        ModNetworking.CHANNEL.sendToServer(new RequestBanListPacket());
    }
    
    private void pardonPlayer(java.util.UUID playerUUID) {
        ModNetworking.CHANNEL.sendToServer(new BanActionPacket(playerUUID));
        // Refresh the list after a short delay
        requestBanList();
    }
    
    public void updateBanList(List<BanManager.BanInfo> bans) {
        banEntries.clear();
        for (BanManager.BanInfo ban : bans) {
            banEntries.add(new BanEntry(ban.playerUUID, ban.playerName, ban.reason, 
                ban.bannedBy, ban.getFormattedBanDate(), ban.isHardcoreDeath));
        }
        scrollOffset = 0; // Reset scroll when updating
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    private static class BanEntry {
        public final java.util.UUID playerUUID;
        public final String playerName;
        public final String reason;
        public final String bannedBy;
        public final String banDate;
        public final boolean isHardcoreDeath;
        
        public BanEntry(java.util.UUID playerUUID, String playerName, String reason, 
                       String bannedBy, String banDate, boolean isHardcoreDeath) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.reason = reason;
            this.bannedBy = bannedBy;
            this.banDate = banDate;
            this.isHardcoreDeath = isHardcoreDeath;
        }
    }
} 