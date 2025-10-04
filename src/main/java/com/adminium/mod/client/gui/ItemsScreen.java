package com.adminium.mod.client.gui;

import com.adminium.mod.client.ClientDisabledItemsData;
import com.adminium.mod.network.C2SToggleItemPacket;
import com.adminium.mod.network.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class ItemsScreen extends Screen {
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 200;
    private static final int ITEMS_PER_ROW = 9;
    private static final int ROWS_PER_PAGE = 6;
    private static final int ITEMS_PER_PAGE = ITEMS_PER_ROW * ROWS_PER_PAGE;
    private static final int SLOT_SIZE = 18;  // Standard MinecraftClient slot size
    
    // Colors
    private static final int COLOR_BACKGROUND = 0xCC000000;
    private static final int COLOR_BORDER = 0xCC5555AA;
    private static final int COLOR_SLOT_BG = 0xFF8B8B8B;
    private static final int COLOR_SLOT_INNER = 0xFF373737;
    private static final int COLOR_SLOT_HIGHLIGHT = 0xFF555555;
    private static final int COLOR_DISABLED_OVERLAY = 0x80FF0000;
    private static final int COLOR_TITLE = 0xFFFFFF;
    private static final int COLOR_PAGE_INFO = 0xAAAAAA;
    
    private EditBox searchBox;
    private Button prevButton;
    private Button nextButton;
    private Button closeButton;
    private Button enableAllButton;
    private Button disableAllButton;
    
    private List<Item> allItems;
    private List<Item> filteredItems;
    private int currentPage = 0;
    private String searchText = "";
    
    // GUI positioning
    private int guiLeft;
    private int guiTop;
    private int inventoryLeft;
    private int inventoryTop;

    public ItemsScreen() {
        super(Text.literal("Item Management"));
    }

    @Override
    protected void init() {
        super.init();
        
        // Calculate GUI position
        guiLeft = (this.width - GUI_WIDTH) / 2;
        guiTop = (this.height - GUI_HEIGHT) / 2;
        
        // Calculate grid dimensions
        int gridWidth = ITEMS_PER_ROW * SLOT_SIZE;
        
        // Calculate starting positions to center the grid
        int startX = guiLeft + (GUI_WIDTH - gridWidth) / 2;
        int startY = guiTop + 40;
        
        // Center the inventory grid within the GUI
        inventoryLeft = startX;
        inventoryTop = startY;
        
        // Load all items and sort them
        allItems = new ArrayList<>(ForgeRegistries.ITEMS.getValues());
        allItems.sort(Comparator.comparing(item -> ForgeRegistries.ITEMS.getKey(item).toString()));
        filteredItems = new ArrayList<>(allItems);
        
        // Search box - centered
        int searchBoxWidth = 180;
        searchBox = new EditBox(this.font, guiLeft + (GUI_WIDTH - searchBoxWidth - 22) / 2, guiTop + 20, searchBoxWidth, 16, Text.literal("Search"));
        searchBox.setMaxLength(50);
        searchBox.setResponder(this::onSearchChanged);
        searchBox.setHint(Text.literal("Search items..."));
        this.addWidget(searchBox);
        
        // Clear search button - next to search box
        this.addRenderableWidget(Button.builder(Text.literal("✕"), button -> {
                searchBox.setValue("");
                onSearchChanged("");
            })
            .pos(searchBox.getX() + searchBoxWidth + 4, guiTop + 19)
            .size(18, 18)
            .build());
        
        // Navigation buttons - repositioned to avoid overlap
        int buttonY = inventoryTop + (ROWS_PER_PAGE * SLOT_SIZE) + 25;
        int totalButtonWidth = 20 + 5 + 60 + 5 + 60 + 5 + 50 + 5 + 20; // buttons + spacing
        int buttonStartX = guiLeft + (GUI_WIDTH - totalButtonWidth) / 2;
        
        prevButton = Button.builder(Text.literal("◀"), button -> previousPage())
            .pos(buttonStartX, buttonY)
            .size(20, 20)
            .build();
        this.addRenderableWidget(prevButton);
        
        // Action buttons - centered with proper spacing
        enableAllButton = Button.builder(Text.literal("Enable All"), button -> toggleAllItems(false))
            .pos(buttonStartX + 25, buttonY)
            .size(60, 20)
            .build();
        this.addRenderableWidget(enableAllButton);
        
        disableAllButton = Button.builder(Text.literal("Disable All"), button -> toggleAllItems(true))
            .pos(buttonStartX + 90, buttonY)
            .size(60, 20)
            .build();
        this.addRenderableWidget(disableAllButton);
        
        // Close button - moved to avoid overlap with navigation
        closeButton = Button.builder(Text.literal("Close"), button -> this.onClose())
            .pos(buttonStartX + 155, buttonY)
            .size(50, 20)
            .build();
        this.addRenderableWidget(closeButton);
        
        nextButton = Button.builder(Text.literal("▶"), button -> nextPage())
            .pos(buttonStartX + 210, buttonY)
            .size(20, 20)
            .build();
        this.addRenderableWidget(nextButton);
        
        updateButtons();
    }

    private void onSearchChanged(String text) {
        searchText = text.toLowerCase();
        currentPage = 0;
        updateItemList();
        updateButtons();
    }

    private void updateItemList() {
        if (searchText.isEmpty()) {
            filteredItems = new ArrayList<>(allItems);
        } else {
            filteredItems = allItems.stream()
                .filter(item -> {
                    Identifier id = ForgeRegistries.ITEMS.getKey(item);
                    String name = id.toString().toLowerCase();
                    String displayName = item.getDescription().getString().toLowerCase();
                    return name.contains(searchText) || displayName.contains(searchText);
                })
                .collect(Collectors.toList());
        }
    }

    private List<Item> getCurrentPageItems() {
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, filteredItems.size());
        return start < filteredItems.size() ? filteredItems.subList(start, end) : new ArrayList<>();
    }

    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            updateButtons();
        }
    }

    private void nextPage() {
        if ((currentPage + 1) * ITEMS_PER_PAGE < filteredItems.size()) {
            currentPage++;
            updateButtons();
        }
    }

    private void updateButtons() {
        prevButton.active = currentPage > 0;
        nextButton.active = (currentPage + 1) * ITEMS_PER_PAGE < filteredItems.size();
        
        // Update action buttons based on current page items
        List<Item> currentItems = getCurrentPageItems();
        enableAllButton.active = currentItems.stream()
            .anyMatch(item -> ClientDisabledItemsData.isItemDisabled(ForgeRegistries.ITEMS.getKey(item)));
        disableAllButton.active = currentItems.stream()
            .anyMatch(item -> !ClientDisabledItemsData.isItemDisabled(ForgeRegistries.ITEMS.getKey(item)));
    }

    private void toggleAllItems(boolean disable) {
        for (Item item : getCurrentPageItems()) {
            Identifier itemId = ForgeRegistries.ITEMS.getKey(item);
            if (ClientDisabledItemsData.isItemDisabled(itemId) != disable) {
                ModNetworking.CHANNEL.sendToServer(new C2SToggleItemPacket(itemId, disable));
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        
        // Draw main GUI background with border
        graphics.fill(guiLeft - 2, guiTop - 2, guiLeft + GUI_WIDTH + 2, guiTop + GUI_HEIGHT + 2, COLOR_BORDER);
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, COLOR_BACKGROUND);
        
        // Draw header background
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + 40, 0xCC222266);
        
        // Draw inventory area background
        int invAreaLeft = inventoryLeft - 2;
        int invAreaTop = inventoryTop - 2;
        int invAreaRight = inventoryLeft + (ITEMS_PER_ROW * SLOT_SIZE) + 2;
        int invAreaBottom = inventoryTop + (ROWS_PER_PAGE * SLOT_SIZE) + 2;
        
        graphics.fill(invAreaLeft, invAreaTop, invAreaRight, invAreaBottom, 0xFF000000);
        graphics.fill(invAreaLeft + 1, invAreaTop + 1, invAreaRight - 1, invAreaBottom - 1, 0xFF2B2B2B);
        
        // Title
        graphics.drawCenteredString(this.font, this.title, guiLeft + GUI_WIDTH / 2, guiTop + 6, COLOR_TITLE);
        
        // Page info - moved closer to the items
        int totalPages = Math.max(1, (filteredItems.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        String pageText = String.format("Page %d/%d (%d items)", currentPage + 1, totalPages, filteredItems.size());
        graphics.drawCenteredString(this.font, pageText, guiLeft + GUI_WIDTH / 2, inventoryTop + (ROWS_PER_PAGE * SLOT_SIZE) + 8, COLOR_PAGE_INFO);
        
        // Render search box
        searchBox.render(graphics, mouseX, mouseY, partialTick);
        
        // Render items
        renderItems(graphics, mouseX, mouseY);
        
        // Render tooltip if hovering over an item
        renderTooltip(graphics, mouseX, mouseY);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderItems(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Item> items = getCurrentPageItems();
        
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            int row = i / ITEMS_PER_ROW;
            int col = i % ITEMS_PER_ROW;
            
            int slotX = inventoryLeft + col * SLOT_SIZE;
            int slotY = inventoryTop + row * SLOT_SIZE;
            
            // Check if mouse is hovering over this slot
            boolean isHovered = mouseX >= slotX && mouseX < slotX + SLOT_SIZE && 
                               mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
            
            // Draw slot background (mimicking vanilla slot appearance)
            graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, COLOR_SLOT_BG);
            graphics.fill(slotX + 1, slotY + 1, slotX + SLOT_SIZE - 1, slotY + SLOT_SIZE - 1, 
                         isHovered ? COLOR_SLOT_HIGHLIGHT : COLOR_SLOT_INNER);
            
            // Draw item using standard positioning (1 pixel offset is standard for MinecraftClient GUIs)
            graphics.renderItem(new ItemStack(item), slotX + 1, slotY + 1);
            
            // Draw disabled overlay
            Identifier itemId = ForgeRegistries.ITEMS.getKey(item);
            if (ClientDisabledItemsData.isItemDisabled(itemId)) {
                graphics.fill(slotX + 1, slotY + 1, slotX + SLOT_SIZE - 1, slotY + SLOT_SIZE - 1, COLOR_DISABLED_OVERLAY);
                // Draw X mark centered
                String xMark = "✖";
                int xWidth = this.font.width(xMark);
                int xX = slotX + (SLOT_SIZE - xWidth) / 2;
                int xY = slotY + (SLOT_SIZE - this.font.lineHeight) / 2;
                graphics.drawString(this.font, xMark, xX, xY, 0xFFFFFF, true);
            }
        }
    }

    private void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Item> items = getCurrentPageItems();
        
        for (int i = 0; i < items.size(); i++) {
            int row = i / ITEMS_PER_ROW;
            int col = i % ITEMS_PER_ROW;
            
            int slotX = inventoryLeft + col * SLOT_SIZE;
            int slotY = inventoryTop + row * SLOT_SIZE;
            
            if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                Item item = items.get(i);
                Identifier itemId = ForgeRegistries.ITEMS.getKey(item);
                boolean isDisabled = ClientDisabledItemsData.isItemDisabled(itemId);
                
                List<Text> tooltip = new ArrayList<>();
                tooltip.add(item.getDescription());
                tooltip.add(Text.literal(itemId.toString()).withStyle(style -> style.withColor(0x808080)));
                tooltip.add(Text.empty());
                tooltip.add(Text.literal(isDisabled ? "Status: §cDisabled" : "Status: §aEnabled"));
                tooltip.add(Text.literal("§7Click to " + (isDisabled ? "enable" : "disable")));
                
                graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle item clicks
        if (button == 0) { // Left click
            List<Item> items = getCurrentPageItems();
            
            for (int i = 0; i < items.size(); i++) {
                int row = i / ITEMS_PER_ROW;
                int col = i % ITEMS_PER_ROW;
                
                int slotX = inventoryLeft + col * SLOT_SIZE;
                int slotY = inventoryTop + row * SLOT_SIZE;
                
                if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                    Item item = items.get(i);
                    Identifier itemId = ForgeRegistries.ITEMS.getKey(item);
                    boolean isDisabled = ClientDisabledItemsData.isItemDisabled(itemId);
                    
                    // Send toggle packet to server
                    ModNetworking.CHANNEL.sendToServer(new C2SToggleItemPacket(itemId, !isDisabled));
                    
                    // Update buttons
                    this.updateButtons();
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
} 