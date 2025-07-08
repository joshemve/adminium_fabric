package com.adminium.client.gui;

import com.adminium.manager.DisabledItemsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.GuiGraphics;
import java.util.List;

public class ItemList extends ObjectSelectionList<ItemList.ItemEntry> {

    public ItemList(Minecraft mc, int width, int height, int y, int itemHeight) {
        super(mc, width, height, y, itemHeight);
    }

    public void setItems(List<Item> items) {
        this.clearEntries();
        items.forEach(item -> this.addEntry(new ItemEntry(item)));
    }

    @Override
    public int getRowWidth() {
        return 300;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getX() + this.width;
    }

    public class ItemEntry extends ObjectSelectionList.Entry<ItemEntry> {
        private final Item item;
        private final Button toggleButton;

        public ItemEntry(Item item) {
            this.item = item;
            this.toggleButton = Button.builder(getButtonText(), this::toggle)
                    .bounds(0, 0, 50, 20)
                    .build();
        }

        private Component getButtonText() {
            boolean isDisabled = DisabledItemsManager.isItemDisabled(item);
            return Component.literal(isDisabled ? "Enable" : "Disable").withStyle(isDisabled ? ChatFormatting.GREEN : ChatFormatting.RED);
        }

        private void toggle(Button button) {
            if (DisabledItemsManager.isItemDisabled(item)) {
                DisabledItemsManager.enableItem(item);
            } else {
                DisabledItemsManager.disableItem(item);
            }
            this.toggleButton.setMessage(getButtonText());
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTicks) {
            ItemStack stack = new ItemStack(item);
            graphics.renderFakeItem(stack, left + 4, top + 8);
            graphics.drawString(Minecraft.getInstance().font, stack.getHoverName(), left + 28, top + 13, 0xFFFFFF);
            this.toggleButton.setX(left + width - this.toggleButton.getWidth() - 4);
            this.toggleButton.setY(top + 8);
            this.toggleButton.render(graphics, mouseX, mouseY, partialTicks);
        }
        
        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", this.item.getDescription().getString());
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.toggleButton.isMouseOver(mouseX, mouseY)) {
                return this.toggleButton.mouseClicked(mouseX, mouseY, button);
            }
            return false;
        }
    }
} 