package com.adminium.mod.client.gui;

import com.adminium.mod.network.ModNetworking;
import com.adminium.mod.network.SetRoleIconPacket;
import com.adminium.mod.roles.Role;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.text.Text;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemSelectorScreen extends Screen {
    private final Role role;
    private final Screen previousScreen;
    private final List<Item> items = new ArrayList<>();
    private final List<Item> filteredItems = new ArrayList<>();
    
    private EditBox searchBox;
    private int scrollOffset = 0;
    private static final int ITEMS_PER_ROW = 9;
    private static final int ROWS_VISIBLE = 5;
    private static final int SLOT_SIZE = 20;
    private static final int SLOT_PADDING = 2;
    
    private int leftPos;
    private int topPos;
    private final int imageWidth = 220;
    private final int imageHeight = 220;
    
    public ItemSelectorScreen(Role role, Screen previousScreen) {
        super(Text.literal("Select Icon for " + role.getDisplayName()));
        this.role = role;
        this.previousScreen = previousScreen;
        
        // Collect all items including modded ones
        ForgeRegistries.ITEMS.forEach(item -> {
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                items.add(item);
            }
        });
        filteredItems.addAll(items);
    }
    
    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        
        // Search box - positioned after title with proper spacing
        this.searchBox = new EditBox(this.font, leftPos + 10, topPos + 35, imageWidth - 20, 20, Text.literal("Search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setResponder(this::onSearchChanged);
        this.searchBox.setHint(Text.literal("Search items..."));
        this.searchBox.setFocused(true);
        this.searchBox.setCanLoseFocus(true);
        this.addRenderableWidget(this.searchBox);
        
        // Back button - bottom left
        addRenderableWidget(Button.builder(
            Text.literal("Back"),
            button -> minecraft.setScreen(previousScreen))
            .pos(leftPos + 10, topPos + imageHeight - 30)
            .size(60, 20)
            .build());
            
        // Cancel button - bottom right
        addRenderableWidget(Button.builder(
            Text.literal("Cancel"),
            button -> minecraft.setScreen(previousScreen))
            .pos(leftPos + imageWidth - 70, topPos + imageHeight - 30)
            .size(60, 20)
            .build());
    }
    
    private void onSearchChanged(String search) {
        filteredItems.clear();
        scrollOffset = 0;
        
        if (search.isEmpty()) {
            filteredItems.addAll(items);
        } else {
            String lowerSearch = search.toLowerCase();
            for (Item item : items) {
                String itemName = item.getDescription().getString().toLowerCase();
                String registryName = ForgeRegistries.ITEMS.getKey(item).toString().toLowerCase();
                if (itemName.contains(lowerSearch) || registryName.contains(lowerSearch)) {
                    filteredItems.add(item);
                }
            }
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Dark background
        this.renderBackground(guiGraphics);
        
        // Main panel
        guiGraphics.fill(leftPos - 2, topPos - 2, leftPos + imageWidth + 2, topPos + imageHeight + 2, 0xFF000000);
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2B2B2B);
        
        // Title bar
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 25, 0xFF000000);
        guiGraphics.drawCenteredString(font, title, leftPos + imageWidth / 2, topPos + 8, 0xFFFFFF);
        
        // Current icon section - moved down to make room
        int currentSectionY = topPos + 65;
        guiGraphics.drawString(font, "Current:", leftPos + 10, currentSectionY, 0xFFFFFF);
        guiGraphics.renderItem(new ItemStack(role.getIconItem()), leftPos + 65, currentSectionY - 5);
        
        // Truncate long item names
        String currentItemName = role.getIconItem().getDescription().getString();
        int maxNameWidth = imageWidth - 90;
        if (font.width(currentItemName) > maxNameWidth) {
            currentItemName = font.plainSubstrByWidth(currentItemName, maxNameWidth - font.width("...")) + "...";
        }
        guiGraphics.drawString(font, currentItemName, leftPos + 85, currentSectionY, 0x808080);
        
        // Item grid background - adjusted position
        int gridX = leftPos + 10;
        int gridY = topPos + 90;
        int gridWidth = ITEMS_PER_ROW * (SLOT_SIZE + SLOT_PADDING) - SLOT_PADDING;
        int gridHeight = ROWS_VISIBLE * (SLOT_SIZE + SLOT_PADDING) - SLOT_PADDING;
        
        guiGraphics.fill(gridX - 2, gridY - 2, gridX + gridWidth + 2, gridY + gridHeight + 2, 0xFF000000);
        guiGraphics.fill(gridX - 1, gridY - 1, gridX + gridWidth + 1, gridY + gridHeight + 1, 0xFF1A1A1A);
        
        // Draw item grid
        for (int row = 0; row < ROWS_VISIBLE; row++) {
            for (int col = 0; col < ITEMS_PER_ROW; col++) {
                int index = (scrollOffset + row) * ITEMS_PER_ROW + col;
                if (index < filteredItems.size()) {
                    Item item = filteredItems.get(index);
                    int x = gridX + col * (SLOT_SIZE + SLOT_PADDING);
                    int y = gridY + row * (SLOT_SIZE + SLOT_PADDING);
                    
                    // Draw slot background
                    guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF3C3C3C);
                    guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF1E1E1E);
                    
                    // Draw item
                    guiGraphics.renderItem(new ItemStack(item), x + 2, y + 2);
                    
                    // Highlight on hover
                    if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                        guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0x80FFFFFF);
                        guiGraphics.renderTooltip(font, item.getDescription(), mouseX, mouseY);
                    }
                    
                    // Highlight if this is the current icon
                    if (item == role.getIconItem()) {
                        guiGraphics.fill(x, y, x + SLOT_SIZE, y + 1, 0xFFFFFF00);
                        guiGraphics.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, 0xFFFFFF00);
                        guiGraphics.fill(x, y, x + 1, y + SLOT_SIZE, 0xFFFFFF00);
                        guiGraphics.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFFFFFF00);
                    }
                }
            }
        }
        
        // Scroll indicator
        if (filteredItems.size() > ITEMS_PER_ROW * ROWS_VISIBLE) {
            int maxScroll = Math.max(0, (filteredItems.size() - 1) / ITEMS_PER_ROW - ROWS_VISIBLE + 1);
            int scrollBarHeight = 20;
            int scrollBarY = gridY + (int)((float)scrollOffset / maxScroll * (gridHeight - scrollBarHeight));
            
            guiGraphics.fill(gridX + gridWidth + 5, gridY, gridX + gridWidth + 8, gridY + gridHeight, 0xFF1A1A1A);
            guiGraphics.fill(gridX + gridWidth + 5, scrollBarY, gridX + gridWidth + 8, scrollBarY + scrollBarHeight, 0xFF808080);
        }
        
        // Bottom separator
        guiGraphics.fill(leftPos + 10, topPos + imageHeight - 40, leftPos + imageWidth - 10, topPos + imageHeight - 39, 0xFF404040);
        
        // Render widgets (search box and buttons) last so they appear on top
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Let the search box handle its own clicks first
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        // Check item grid clicks
        int gridX = leftPos + 10;
        int gridY = topPos + 90;
        
        for (int row = 0; row < ROWS_VISIBLE; row++) {
            for (int col = 0; col < ITEMS_PER_ROW; col++) {
                int index = (scrollOffset + row) * ITEMS_PER_ROW + col;
                if (index < filteredItems.size()) {
                    int x = gridX + col * (SLOT_SIZE + SLOT_PADDING);
                    int y = gridY + row * (SLOT_SIZE + SLOT_PADDING);
                    
                    if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                        Item selectedItem = filteredItems.get(index);
                        ModNetworking.sendToServer(new SetRoleIconPacket(role, selectedItem));
                        minecraft.player.sendSystemMessage(Text.literal("§aSet " + role.getDisplayName() + " icon to " + selectedItem.getDescription().getString()));
                        minecraft.setScreen(previousScreen);
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxScroll = Math.max(0, (filteredItems.size() - 1) / ITEMS_PER_ROW - ROWS_VISIBLE + 1);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)delta));
        return true;
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return super.charTyped(codePoint, modifiers);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Allow escape to close the screen
        if (keyCode == 256) { // ESC key
            minecraft.setScreen(previousScreen);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
} 