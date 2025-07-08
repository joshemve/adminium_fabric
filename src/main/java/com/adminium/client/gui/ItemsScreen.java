package com.adminium.client.gui;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.GuiGraphics;
import java.util.List;
import java.util.stream.Collectors;

public class ItemsScreen extends Screen {
    private ItemList itemList;
    private List<Item> allItems;
    private EditBox searchBox;

    public ItemsScreen() {
        super(Component.literal("Item Management"));
        this.allItems = BuiltInRegistries.ITEM.stream().collect(Collectors.toList());
    }

    @Override
    protected void init() {
        super.init();
        this.itemList = new ItemList(this.minecraft, this.width, this.height, 48, 36);
        this.searchBox = new EditBox(this.font, this.width / 2 - 100, 22, 200, 20, Component.literal("Search..."));

        this.addWidget(this.itemList);
        this.addWidget(this.searchBox);

        this.searchBox.setResponder(this::onSearch);
        this.onSearch("");
    }

    private void onSearch(String query) {
        this.itemList.setItems(this.allItems.stream()
                .filter(item -> item.getDescription().getString().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.itemList.render(graphics, mouseX, mouseY, partialTicks);
        this.searchBox.render(graphics, mouseX, mouseY, partialTicks);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
    }
} 