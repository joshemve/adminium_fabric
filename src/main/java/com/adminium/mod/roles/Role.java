package com.adminium.mod.roles;

import net.minecraft.util.Formatting;
import net.minecraft.text.Text;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;

public enum Role {
    FIGHTER("Fighter", Formatting.RED, Items.IRON_SWORD),
    FARMER("Farmer", Formatting.GREEN, Items.WHEAT),
    MINER("Miner", Formatting.GOLD, Items.IRON_PICKAXE);
    
    private final String displayName;
    private final Formatting color;
    private Item iconItem;
    
    Role(String displayName, Formatting color, Item defaultIcon) {
        this.displayName = displayName;
        this.color = color;
        this.iconItem = defaultIcon;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public Formatting getColor() {
        return color;
    }
    
    public Item getIconItem() {
        return iconItem;
    }
    
    public void setIconItem(Item item) {
        this.iconItem = item;
    }
    
    public String getPrefix() {
        return color + "[" + displayName + "]" + Formatting.RESET;
    }
    
    public Text getIconPrefix() {
        // Create item stack for hover display
        ItemStack iconStack = new ItemStack(iconItem);
        
        // Create the icon component with hover text
        MutableText iconComponent = Text.literal("[")
            .withStyle(color)
            .append(iconStack.getDisplayName().copy().withStyle(color))
            .append("]")
            .withStyle(style -> style.withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_ITEM, 
                    new HoverEvent.ItemStackInfo(iconStack))));
        
        return iconComponent;
    }
    
    public Text getIconOnly() {
        // Just the item name without brackets, but with hover
        ItemStack iconStack = new ItemStack(iconItem);
        return iconStack.getDisplayName().copy()
            .withStyle(color)
            .withStyle(style -> style.withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_ITEM, 
                    new HoverEvent.ItemStackInfo(iconStack))));
    }
    
    public Text getItemIconComponent() {
        // Create an item stack for the hover tooltip
        ItemStack iconStack = new ItemStack(iconItem);
        
        // Get a Unicode symbol based on the item type
        String symbol = getItemSymbol(iconItem);
        
        // Create a component with just the symbol (no item name)
        MutableText component = Text.literal(symbol)
            .withStyle(color)
            .withStyle(style -> style
                .withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_ITEM, 
                    new HoverEvent.ItemStackInfo(iconStack)))
                .withItalic(false));
        
        return component;
    }
    
    private String getItemSymbol(Item item) {
        String itemName = item.toString().toLowerCase();
        
        // Tools - order matters! Check more specific items first
        if (itemName.contains("sword")) return "⚔";
        if (itemName.contains("pickaxe")) return "⛏";  // Check pickaxe before axe
        if (itemName.contains("axe")) return "🪓";
        if (itemName.contains("shovel")) return "🔨";
        if (itemName.contains("hoe")) return "🌾";
        if (itemName.contains("bow")) return "🏹";
        
        // Armor
        if (itemName.contains("helmet")) return "⛑";
        if (itemName.contains("chestplate")) return "🛡";
        if (itemName.contains("leggings")) return "👖";
        if (itemName.contains("boots")) return "👢";
        
        // Food
        if (itemName.contains("apple")) return "🍎";
        if (itemName.contains("bread")) return "🍞";
        if (itemName.contains("carrot")) return "🥕";
        if (itemName.contains("potato")) return "🥔";
        if (itemName.contains("beef") || itemName.contains("pork") || itemName.contains("chicken")) return "🍖";
        
        // Materials
        if (itemName.contains("diamond")) return "��";
        if (itemName.contains("emerald")) return "💚";
        if (itemName.contains("gold")) return "🪙";
        if (itemName.contains("iron")) return "⚙";
        if (itemName.contains("coal")) return "◆";
        
        // Blocks
        if (itemName.contains("stone")) return "🪨";
        if (itemName.contains("wood") || itemName.contains("log")) return "🪵";
        if (itemName.contains("dirt")) return "🟫";
        if (itemName.contains("grass")) return "🌱";
        
        // Default symbols based on role
        switch (this) {
            case FIGHTER:
                return "⚔";
            case FARMER:
                return "🌾";
            case MINER:
                return "⛏";
            default:
                return "◆";
        }
    }
} 