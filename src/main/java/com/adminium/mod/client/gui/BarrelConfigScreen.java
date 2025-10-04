package com.adminium.mod.client.gui;

import com.adminium.mod.manager.BarrelLootManager;
import com.adminium.mod.network.C2SUpdateBarrelLootPacket;
import com.adminium.mod.network.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class BarrelConfigScreen extends Screen {
    private static final int ITEM_SIZE = 18;
    private static final int ITEM_SPACING = 20;
    private static final int ITEMS_PER_ROW = 9;
    private static final int ROWS_PER_PAGE = 4;
    private static final int ITEMS_PER_PAGE = ITEMS_PER_ROW * ROWS_PER_PAGE;

    private final Map<Identifier, BarrelLootManager.BarrelLootEntry> lootTable;
    private List<Item> allItems;
    private List<Item> filteredItems;
    private List<ConfiguredLootEntry> configuredItems;

    private EditBox searchBox;
    private EditBox chanceBox;
    private EditBox minCountBox;
    private EditBox maxCountBox;
    private EditBox configuredSearchBox;

    private Button addButton;
    private Button clearAllButton;
    private Button sortByNameButton;
    private Button sortByChanceButton;
    private Button doneButton;

    private Item selectedItem;
    private int currentPage = 0;
    private int configuredScrollOffset = 0;
    private SortMode sortMode = SortMode.NAME;

    private enum SortMode {
        NAME,
        CHANCE,
        COUNT
    }

    private static class ConfiguredLootEntry {
        final Identifier itemId;
        final Item item;
        final BarrelLootManager.BarrelLootEntry entry;

        ConfiguredLootEntry(Identifier itemId, Item item, BarrelLootManager.BarrelLootEntry entry) {
            this.itemId = itemId;
            this.item = item;
            this.entry = entry;
        }
    }

    public BarrelConfigScreen(Map<Identifier, BarrelLootManager.BarrelLootEntry> lootTable) {
        super(Text.literal("Barrel Loot Configuration"));
        this.lootTable = new HashMap<>(lootTable);
        this.allItems = new ArrayList<>();
        this.filteredItems = new ArrayList<>();
        this.configuredItems = new ArrayList<>();
    }

    @Override
    protected void init() {
        super.init();

        // Load all items
        allItems.clear();
        for (Item item : ForgeRegistries.ITEMS) {
            if (item != Items.AIR) {
                allItems.add(item);
            }
        }
        filteredItems = new ArrayList<>(allItems);
        updateConfiguredItemsList();

        int leftPanelWidth = (this.width * 2) / 3;
        int rightPanelX = leftPanelWidth + 10;
        int rightPanelWidth = this.width - rightPanelX - 10;

        // LEFT PANEL - Item Selection
        int leftX = 10;
        int topY = 30;

        // Search box for all items
        this.searchBox = new EditBox(this.font, leftX, topY, 200, 20, Text.literal("Search Items"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setHint(Text.literal("Search items..."));
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);

        int gridY = topY + 25;

        // Input fields below item grid
        int inputY = gridY + (ROWS_PER_PAGE * ITEM_SPACING) + 10;

        this.chanceBox = new EditBox(this.font, leftX + 75, inputY, 40, 20, Text.literal("Chance"));
        this.chanceBox.setMaxLength(6);
        this.chanceBox.setValue("10.0");
        this.addRenderableWidget(this.chanceBox);

        this.minCountBox = new EditBox(this.font, leftX + 140, inputY, 30, 20, Text.literal("Min"));
        this.minCountBox.setMaxLength(3);
        this.minCountBox.setValue("1");
        this.addRenderableWidget(this.minCountBox);

        this.maxCountBox = new EditBox(this.font, leftX + 200, inputY, 30, 20, Text.literal("Max"));
        this.maxCountBox.setMaxLength(3);
        this.maxCountBox.setValue("1");
        this.addRenderableWidget(this.maxCountBox);

        // Add/Update button
        this.addButton = Button.builder(Text.literal("Add Block"), button -> this.addOrUpdateItem())
            .pos(leftX, this.height - 50)
            .size(80, 20)
            .build();
        this.addRenderableWidget(this.addButton);


        // RIGHT PANEL - Configured Items List
        // Title
        int rightTopY = topY;

        // Search box for configured items
        this.configuredSearchBox = new EditBox(this.font, rightPanelX, rightTopY, rightPanelWidth - 10, 20, Text.literal("Filter Configured"));
        this.configuredSearchBox.setMaxLength(50);
        this.configuredSearchBox.setHint(Text.literal("Filter items..."));
        this.configuredSearchBox.setResponder(text -> updateConfiguredItemsList());
        this.addRenderableWidget(this.configuredSearchBox);

        // Sort buttons - calculate sizes to fit within panel
        int sortY = rightTopY + 25;
        int buttonWidth = Math.max(60, (rightPanelWidth - 15) / 2); // Ensure minimum width but fit two buttons
        
        this.sortByNameButton = Button.builder(Text.literal("Name"), button -> {
                this.sortMode = SortMode.NAME;
                updateConfiguredItemsList();
                updateSortButtonText();
            })
            .pos(rightPanelX, sortY)
            .size(buttonWidth, 20)
            .build();
        this.addRenderableWidget(this.sortByNameButton);

        this.sortByChanceButton = Button.builder(Text.literal("Chance"), button -> {
                this.sortMode = SortMode.CHANCE;
                updateConfiguredItemsList();
                updateSortButtonText();
            })
            .pos(rightPanelX + buttonWidth + 5, sortY)
            .size(buttonWidth, 20)
            .build();
        this.addRenderableWidget(this.sortByChanceButton);

        // Clear all button
        this.clearAllButton = Button.builder(Text.literal("Clear All"), button -> this.clearAll())
            .pos(rightPanelX, this.height - 50)
            .size(80, 20)
            .build();
        this.addRenderableWidget(this.clearAllButton);

        // Done button
        this.doneButton = Button.builder(Text.literal("Done"), button -> this.onClose())
            .pos(this.width / 2 - 40, this.height - 25)
            .size(80, 20)
            .build();
        this.addRenderableWidget(this.doneButton);

        updateButtonStates();
        updateSortButtonText();
    }

    private void updateSortButtonText() {
        if (sortByNameButton != null) {
            sortByNameButton.setMessage(Text.literal(sortMode == SortMode.NAME ? "▼Name" : "Name"));
        }
        if (sortByChanceButton != null) {
            sortByChanceButton.setMessage(Text.literal(sortMode == SortMode.CHANCE ? "▼Chance" : "Chance"));
        }
    }

    private void updateConfiguredItemsList() {
        configuredItems.clear();
        String filterText = configuredSearchBox != null ? configuredSearchBox.getValue().toLowerCase() : "";

        for (Map.Entry<Identifier, BarrelLootManager.BarrelLootEntry> entry : lootTable.entrySet()) {
            Item item = ForgeRegistries.ITEMS.getValue(entry.getKey());
            if (item != null && item != Items.AIR) {
                String itemName = item.getDescription().getString().toLowerCase();
                String itemId = entry.getKey().toString().toLowerCase();

                if (filterText.isEmpty() || itemName.contains(filterText) || itemId.contains(filterText)) {
                    configuredItems.add(new ConfiguredLootEntry(entry.getKey(), item, entry.getValue()));
                }
            }
        }

        // Sort the list
        switch (sortMode) {
            case NAME:
                configuredItems.sort(Comparator.comparing(e -> e.item.getDescription().getString()));
                break;
            case CHANCE:
                configuredItems.sort(Comparator.comparingDouble((ConfiguredLootEntry e) -> e.entry.getDropChance()).reversed());
                break;
            case COUNT:
                configuredItems.sort(Comparator.comparingInt((ConfiguredLootEntry e) -> e.entry.getMaxCount()).reversed());
                break;
        }

        configuredScrollOffset = 0;
    }

    private void onSearchChanged(String text) {
        filteredItems.clear();
        String searchLower = text.toLowerCase();

        for (Item item : allItems) {
            String itemName = item.getDescription().getString().toLowerCase();
            Identifier itemId = ForgeRegistries.ITEMS.getKey(item);
            String itemIdString = itemId != null ? itemId.toString().toLowerCase() : "";

            if (itemName.contains(searchLower) || itemIdString.contains(searchLower)) {
                filteredItems.add(item);
            }
        }

        currentPage = 0;
        updateButtonStates();
    }

    private void addOrUpdateItem() {
        if (selectedItem == null) return;

        try {
            double chance = Double.parseDouble(chanceBox.getValue());
            int minCount = Integer.parseInt(minCountBox.getValue());
            int maxCount = Integer.parseInt(maxCountBox.getValue());

            if (chance < 0 || chance > 100) {
                chance = Math.max(0, Math.min(100, chance));
            }

            if (minCount < 1) minCount = 1;
            if (maxCount < minCount) maxCount = minCount;

            Identifier itemId = ForgeRegistries.ITEMS.getKey(selectedItem);
            if (itemId != null) {
                lootTable.put(itemId, new BarrelLootManager.BarrelLootEntry(itemId, chance, minCount, maxCount));

                ModNetworking.CHANNEL.sendToServer(
                    new C2SUpdateBarrelLootPacket(C2SUpdateBarrelLootPacket.Action.ADD, itemId, chance, minCount, maxCount)
                );

                updateConfiguredItemsList();
            }
        } catch (NumberFormatException e) {
            // Invalid input, ignore
        }
    }

    private void removeConfiguredItem(ConfiguredLootEntry entry) {
        lootTable.remove(entry.itemId);

        ModNetworking.CHANNEL.sendToServer(
            new C2SUpdateBarrelLootPacket(C2SUpdateBarrelLootPacket.Action.REMOVE, entry.itemId, 0, 1, 1)
        );

        updateConfiguredItemsList();
    }

    private void editConfiguredItem(ConfiguredLootEntry entry) {
        selectedItem = entry.item;
        chanceBox.setValue(String.valueOf(entry.entry.getDropChance()));
        minCountBox.setValue(String.valueOf(entry.entry.getMinCount()));
        maxCountBox.setValue(String.valueOf(entry.entry.getMaxCount()));
    }

    private void clearAll() {
        lootTable.clear();

        ModNetworking.CHANNEL.sendToServer(
            new C2SUpdateBarrelLootPacket(C2SUpdateBarrelLootPacket.Action.CLEAR_ALL)
        );

        updateConfiguredItemsList();
    }


    private void updateButtonStates() {
        clearAllButton.active = !lootTable.isEmpty();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        // Title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        int leftPanelWidth = (this.width * 2) / 3;
        int rightPanelX = leftPanelWidth + 10;

        // Draw separator line
        guiGraphics.fill(leftPanelWidth, 25, leftPanelWidth + 2, this.height - 20, 0xFF404040);

        // LEFT PANEL - Render item grid
        int leftX = 10;
        int gridY = 55;

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            int relativeIndex = i - startIndex;
            int row = relativeIndex / ITEMS_PER_ROW;
            int col = relativeIndex % ITEMS_PER_ROW;

            int x = leftX + col * ITEM_SPACING;
            int y = gridY + row * ITEM_SPACING;

            Item item = filteredItems.get(i);
            ItemStack stack = new ItemStack(item);

            // Highlight if selected
            if (item == selectedItem) {
                guiGraphics.fill(x - 1, y - 1, x + ITEM_SIZE + 1, y + ITEM_SIZE + 1, 0xFF00FF00);
            }

            // Highlight if in loot table
            Identifier itemId = ForgeRegistries.ITEMS.getKey(item);
            if (itemId != null && lootTable.containsKey(itemId)) {
                guiGraphics.fill(x, y, x + ITEM_SIZE, y + ITEM_SIZE, 0x6000FF00);
            }

            guiGraphics.renderItem(stack, x + 1, y + 1);

            // Tooltip on hover
            if (mouseX >= x && mouseX < x + ITEM_SIZE && mouseY >= y && mouseY < y + ITEM_SIZE) {
                List<Text> tooltip = new ArrayList<>();
                tooltip.add(item.getDescription());

                if (itemId != null && lootTable.containsKey(itemId)) {
                    BarrelLootManager.BarrelLootEntry entry = lootTable.get(itemId);
                    tooltip.add(Text.literal("§aIn Loot Table:"));
                    tooltip.add(Text.literal("§7Chance: §f" + entry.getDropChance() + "%"));
                    tooltip.add(Text.literal("§7Count: §f" + entry.getMinCount() + "-" + entry.getMaxCount()));
                }

                guiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
            }
        }

        // Page indicator (place it below the navigation buttons to avoid overlap)
        int maxPage = Math.max(0, (filteredItems.size() - 1) / ITEMS_PER_PAGE);
        String pageText = "Page " + (currentPage + 1) + " / " + (maxPage + 1);
        int navTextY = gridY + (ROWS_PER_PAGE * ITEM_SPACING) + 60; // below nav buttons
        guiGraphics.drawCenteredString(this.font, pageText, leftX + 140, navTextY, 0xFFFFFF);

        // Input field labels
        int inputY = gridY + (ROWS_PER_PAGE * ITEM_SPACING) + 10;
        guiGraphics.drawString(this.font, "Chance %:", leftX, inputY + 6, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Min:", leftX + 120, inputY + 6, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Max:", leftX + 175, inputY + 6, 0xFFFFFF);

        // Selected item info
        if (selectedItem != null) {
            String itemName = selectedItem.getDescription().getString();
            guiGraphics.drawString(this.font, "Selected: " + itemName, leftX, gridY + (ROWS_PER_PAGE * ITEM_SPACING) + 55, 0xFFFFFF);
        }

        // RIGHT PANEL - Configured Items List
        guiGraphics.drawString(this.font, "Configured Loot (" + lootTable.size() + " items)", rightPanelX, 15, 0xFFFFFF);

        // Calculate visible area for configured items (leave extra room for total text and buttons)
        int listStartY = 75;
        int listEndY = this.height - 90;
        int itemHeight = 25;
        int visibleItems = (listEndY - listStartY) / itemHeight;

        // Render configured items list
        int yPos = listStartY;
        for (int i = configuredScrollOffset; i < Math.min(configuredScrollOffset + visibleItems, configuredItems.size()); i++) {
            ConfiguredLootEntry entry = configuredItems.get(i);

            // Background for item entry
            boolean isHovered = mouseX >= rightPanelX && mouseX < this.width - 10 &&
                               mouseY >= yPos && mouseY < yPos + itemHeight - 2;

            int bgColor = isHovered ? 0x40FFFFFF : (i % 2 == 0 ? 0x20000000 : 0x20303030);
            guiGraphics.fill(rightPanelX, yPos, this.width - 10, yPos + itemHeight - 2, bgColor);

            // Item icon
            guiGraphics.renderItem(new ItemStack(entry.item), rightPanelX + 2, yPos + 3);

            // Item name (truncate to avoid overlapping action buttons)
            String itemName = entry.item.getDescription().getString();
            int textX = rightPanelX + 25;
            int actionButtonX = this.width - 65;
            int maxNameWidth = Math.max(0, (actionButtonX - 8) - textX);
            String trimmedName = this.font.plainSubstrByWidth(itemName, maxNameWidth);
            guiGraphics.drawString(this.font, trimmedName, textX, yPos + 3, 0xFFFFFF);

            // Chance and count info (truncate to avoid overlapping action buttons)
            String info = String.format("%.1f%% [%d-%d]",
                entry.entry.getDropChance(),
                entry.entry.getMinCount(),
                entry.entry.getMaxCount());
            int maxInfoWidth = Math.max(0, (actionButtonX - 8) - textX);
            String trimmedInfo = this.font.plainSubstrByWidth(info, maxInfoWidth);
            guiGraphics.drawString(this.font, trimmedInfo, textX, yPos + 13, 0x8080FF);

            // Mini buttons for edit/remove
            int buttonX = actionButtonX;

            // Edit button
            boolean editHovered = mouseX >= buttonX && mouseX < buttonX + 25 &&
                                 mouseY >= yPos + 2 && mouseY < yPos + 20;
            guiGraphics.fill(buttonX, yPos + 2, buttonX + 25, yPos + 20, editHovered ? 0xFF4080FF : 0xFF303060);
            guiGraphics.drawCenteredString(this.font, "✎", buttonX + 12, yPos + 6, 0xFFFFFF);

            // Remove button
            boolean removeHovered = mouseX >= buttonX + 30 && mouseX < buttonX + 55 &&
                                   mouseY >= yPos + 2 && mouseY < yPos + 20;
            guiGraphics.fill(buttonX + 30, yPos + 2, buttonX + 55, yPos + 20, removeHovered ? 0xFFFF4040 : 0xFF803030);
            guiGraphics.drawCenteredString(this.font, "✕", buttonX + 42, yPos + 6, 0xFFFFFF);

            yPos += itemHeight;
        }

        // Total drop chance indicator
        double totalChance = lootTable.values().stream()
            .mapToDouble(BarrelLootManager.BarrelLootEntry::getDropChance)
            .sum();
        String totalText = String.format("Total Drop Chance: %.1f%%", totalChance);
        int totalColor = totalChance > 100 ? 0xFFFF4040 : 0xFF40FF40;
        guiGraphics.drawString(this.font, totalText, rightPanelX, this.height - 75, totalColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        int leftPanelWidth = (this.width * 2) / 3;
        int rightPanelX = leftPanelWidth + 10;

        // Check item grid clicks
        int leftX = 10;
        int gridY = 55;

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            int relativeIndex = i - startIndex;
            int row = relativeIndex / ITEMS_PER_ROW;
            int col = relativeIndex % ITEMS_PER_ROW;

            int x = leftX + col * ITEM_SPACING;
            int y = gridY + row * ITEM_SPACING;

            if (mouseX >= x && mouseX < x + ITEM_SIZE && mouseY >= y && mouseY < y + ITEM_SIZE) {
                selectedItem = filteredItems.get(i);

                Identifier itemId = ForgeRegistries.ITEMS.getKey(selectedItem);
                if (itemId != null && lootTable.containsKey(itemId)) {
                    BarrelLootManager.BarrelLootEntry entry = lootTable.get(itemId);
                    chanceBox.setValue(String.valueOf(entry.getDropChance()));
                    minCountBox.setValue(String.valueOf(entry.getMinCount()));
                    maxCountBox.setValue(String.valueOf(entry.getMaxCount()));
                }

                return true;
            }
        }

        // Check configured items list clicks
        int listStartY = 75;
        int itemHeight = 25;
        int listEndY = this.height - 90;
        int visibleItems = (listEndY - listStartY) / itemHeight;

        for (int i = 0; i < Math.min(visibleItems, configuredItems.size() - configuredScrollOffset); i++) {
            int yPos = listStartY + (i * itemHeight);
            ConfiguredLootEntry entry = configuredItems.get(i + configuredScrollOffset);

            int buttonX = this.width - 65;

            // Edit button click
            if (mouseX >= buttonX && mouseX < buttonX + 25 &&
                mouseY >= yPos + 2 && mouseY < yPos + 20) {
                editConfiguredItem(entry);
                return true;
            }

            // Remove button click
            if (mouseX >= buttonX + 30 && mouseX < buttonX + 55 &&
                mouseY >= yPos + 2 && mouseY < yPos + 20) {
                removeConfiguredItem(entry);
                return true;
            }

            // Click on item itself to select
            if (mouseX >= rightPanelX && mouseX < buttonX - 5 &&
                mouseY >= yPos && mouseY < yPos + itemHeight) {
                editConfiguredItem(entry);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int leftPanelWidth = (this.width * 2) / 3;
        int rightPanelX = leftPanelWidth + 10;

        // Scroll in right panel (configured items)
        if (mouseX >= rightPanelX) {
            int listStartY = 75;
            int listEndY = this.height - 90;
            int itemHeight = 25;
            int visibleItems = (listEndY - listStartY) / itemHeight;
            int maxScroll = Math.max(0, configuredItems.size() - visibleItems);

            if (delta > 0) {
                configuredScrollOffset = Math.max(0, configuredScrollOffset - 1);
            } else if (delta < 0) {
                configuredScrollOffset = Math.min(maxScroll, configuredScrollOffset + 1);
            }
            return true;
        }

        // Scroll in left panel (item grid)
        if (mouseX < leftPanelWidth) {
            int maxPage = (filteredItems.size() - 1) / ITEMS_PER_PAGE;

            if (delta > 0 && currentPage > 0) {
                currentPage--;
                updateButtonStates();
                return true;
            } else if (delta < 0 && currentPage < maxPage) {
                currentPage++;
                updateButtonStates();
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}