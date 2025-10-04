#!/usr/bin/env python3
"""
Enhanced script to convert Forge imports to Fabric imports in Java files.
"""

import os
import re

# Additional mappings for the second pass
ADDITIONAL_MAPPINGS = {
    # Block states
    'net.minecraft.block.state.BlockState': 'net.minecraft.block.BlockState',
    'net.minecraft.world.level.block.state.BlockState': 'net.minecraft.block.BlockState',

    # Network
    'net.minecraft.network.FriendlyByteBuf': 'net.minecraft.network.PacketByteBuf',

    # Attributes
    'net.minecraft.world.entity.ai.attributes.AttributeModifier': 'net.minecraft.entity.attribute.EntityAttributeModifier',
    'net.minecraft.world.entity.ai.attributes.Attributes': 'net.minecraft.entity.attribute.EntityAttributes',

    # Items
    'net.minecraft.world.item.PickaxeItem': 'net.minecraft.item.PickaxeItem',
    'net.minecraft.world.food.FoodData': 'net.minecraft.entity.player.HungerManager',

    # Client
    'net.minecraft.client.Minecraft': 'net.minecraft.client.MinecraftClient',

    # Inventory
    'net.minecraft.inventory.Inventorys': 'net.minecraft.inventory.Inventories',

    # Hover events
    'net.minecraft.network.chat.HoverEvent': 'net.minecraft.text.HoverEvent',

    # Registry/Identifier
    'net.minecraft.resources.Identifier': 'net.minecraft.util.Identifier',

    # Entity
    'net.minecraft.world.entity.player.PlayerEntity': 'net.minecraft.entity.player.PlayerEntity',
}

TYPE_REPLACEMENTS = {
    'FriendlyByteBuf': 'PacketByteBuf',
    'AttributeModifier': 'EntityAttributeModifier',
    'Attributes': 'EntityAttributes',
    'FoodData': 'HungerManager',
    'Minecraft': 'MinecraftClient',
    'Inventorys': 'Inventories',
}

def fix_file(filepath):
    """Fix additional import and type issues."""
    with open(filepath, 'r') as f:
        content = f.read()

    original = content

    # Fix imports
    for forge, fabric in ADDITIONAL_MAPPINGS.items():
        content = content.replace(f'import {forge};', f'import {fabric};')

    # Fix type names
    for forge, fabric in TYPE_REPLACEMENTS.items():
        content = re.sub(r'\b' + re.escape(forge) + r'\b', fabric, content)

    # Remove any remaining Forge event annotations
    content = re.sub(r'@Mod\.EventBusSubscriber\([^)]*\)\s*\n', '', content)
    content = re.sub(r'@SubscribeEvent\s*\n', '// TODO: Convert to Fabric event\n    ', content)

    # Remove Forge NetworkEvent references
    content = re.sub(r'Supplier<NetworkEvent\.Context>', 'Object', content)

    # Fix TickEvent references
    content = re.sub(r'TickEvent\.PlayerTickEvent', 'Object /* TODO: Convert to Fabric event */', content)
    content = re.sub(r'TickEvent\.Phase\.\w+', '/* TODO: Check phase */', content)

    # Fix other event references
    content = re.sub(r'LootingLevelEvent', 'Object /* TODO: Custom event needed */', content)
    content = re.sub(r'BlockEvent\.BreakEvent', 'Object /* TODO: Convert to Fabric event */', content)
    content = re.sub(r'LivingHurtEvent', 'Object /* TODO: Convert to Fabric event */', content)

    if content != original:
        with open(filepath, 'w') as f:
            f.write(content)
        return True
    return False

def process_directory(directory):
    """Process all Java files in a directory."""
    fixed_files = []

    for root, dirs, files in os.walk(directory):
        if 'build' in root or '.gradle' in root:
            continue

        for file in files:
            if file.endswith('.java'):
                filepath = os.path.join(root, file)
                if fix_file(filepath):
                    fixed_files.append(filepath)

    return fixed_files

if __name__ == '__main__':
    src_dir = '/Users/joshlaptop/Documents/adminium/src/main/java/com/adminium/mod'

    print("Applying additional Fabric conversions...")
    fixed = process_directory(src_dir)

    if fixed:
        print(f"\nFixed {len(fixed)} files:")
        for f in fixed[:10]:
            print(f"  - {os.path.basename(f)}")
        if len(fixed) > 10:
            print(f"  ... and {len(fixed) - 10} more")
    else:
        print("No additional fixes needed.")