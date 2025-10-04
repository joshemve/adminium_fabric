#!/usr/bin/env python3
"""
Script to convert Forge imports to Fabric imports in Java files.
"""

import os
import re
import sys

# Mapping of Forge imports to Fabric imports
IMPORT_MAPPINGS = {
    # Commands
    'net.minecraft.commands.CommandSourceStack': 'net.minecraft.server.command.ServerCommandSource',
    'net.minecraft.commands.Commands': 'com.mojang.brigadier.CommandDispatcher',
    'net.minecraft.commands.arguments': 'net.minecraft.command.argument',

    # Network/Chat
    'net.minecraft.network.chat.Component': 'net.minecraft.text.Text',
    'net.minecraft.network.chat.MutableComponent': 'net.minecraft.text.MutableText',
    'net.minecraft.network.chat.TextColor': 'net.minecraft.text.TextColor',
    'net.minecraft.network.chat.Style': 'net.minecraft.text.Style',
    'net.minecraft.ChatFormatting': 'net.minecraft.util.Formatting',

    # Server/Player
    'net.minecraft.server.level.ServerPlayer': 'net.minecraft.server.network.ServerPlayerEntity',
    'net.minecraft.server.level.ServerLevel': 'net.minecraft.server.world.ServerWorld',
    'net.minecraft.world.entity.player.Player': 'net.minecraft.entity.player.PlayerEntity',
    'net.minecraft.world.entity.LivingEntity': 'net.minecraft.entity.LivingEntity',
    'net.minecraft.world.entity.Entity': 'net.minecraft.entity.Entity',

    # World/Level
    'net.minecraft.world.level.Level': 'net.minecraft.world.World',
    'net.minecraft.world.level.GameRules': 'net.minecraft.world.GameRules',
    'net.minecraft.world.level.block': 'net.minecraft.block',
    'net.minecraft.core.BlockPos': 'net.minecraft.util.math.BlockPos',
    'net.minecraft.core.Direction': 'net.minecraft.util.math.Direction',
    'net.minecraft.core.Vec3i': 'net.minecraft.util.math.Vec3i',

    # Items
    'net.minecraft.world.item.ItemStack': 'net.minecraft.item.ItemStack',
    'net.minecraft.world.item.Item': 'net.minecraft.item.Item',
    'net.minecraft.world.item.Items': 'net.minecraft.item.Items',
    'net.minecraft.world.item.Rarity': 'net.minecraft.util.Rarity',

    # Effects
    'net.minecraft.world.effect.MobEffect': 'net.minecraft.entity.effect.StatusEffect',
    'net.minecraft.world.effect.MobEffectCategory': 'net.minecraft.entity.effect.StatusEffectCategory',
    'net.minecraft.world.effect.MobEffectInstance': 'net.minecraft.entity.effect.StatusEffectInstance',
    'net.minecraft.world.effect.MobEffects': 'net.minecraft.entity.effect.StatusEffects',

    # Damage
    'net.minecraft.world.damagesource.DamageSource': 'net.minecraft.entity.damage.DamageSource',

    # Resources
    'net.minecraft.resources.ResourceLocation': 'net.minecraft.util.Identifier',
    'net.minecraft.resources.ResourceKey': 'net.minecraft.registry.RegistryKey',

    # Container/Inventory
    'net.minecraft.world.Container': 'net.minecraft.inventory.Inventory',
    'net.minecraft.world.inventory.AbstractContainerMenu': 'net.minecraft.screen.ScreenHandler',

    # NBT
    'net.minecraft.nbt.CompoundTag': 'net.minecraft.nbt.NbtCompound',
    'net.minecraft.nbt.ListTag': 'net.minecraft.nbt.NbtList',
    'net.minecraft.nbt.Tag': 'net.minecraft.nbt.NbtElement',

    # Sounds
    'net.minecraft.sounds.SoundEvent': 'net.minecraft.sound.SoundEvent',
    'net.minecraft.sounds.SoundEvents': 'net.minecraft.sound.SoundEvents',
    'net.minecraft.sounds.SoundSource': 'net.minecraft.sound.SoundCategory',

    # GameProfile
    'com.mojang.authlib.GameProfile': 'com.mojang.authlib.GameProfile',

    # Math
    'net.minecraft.core.Vec3': 'net.minecraft.util.math.Vec3d',

    # Hand
    'net.minecraft.world.InteractionHand': 'net.minecraft.util.Hand',
    'net.minecraft.world.InteractionResult': 'net.minecraft.util.ActionResult',

    # Registries
    'net.minecraft.core.registries.BuiltInRegistries': 'net.minecraft.registry.Registries',

    # Remove Forge-specific imports
    'net.minecraftforge': None,
}

# Type name mappings (for code, not imports)
TYPE_MAPPINGS = {
    'CommandSourceStack': 'ServerCommandSource',
    'Component': 'Text',
    'MutableComponent': 'MutableText',
    'ServerPlayer': 'ServerPlayerEntity',
    'ServerLevel': 'ServerWorld',
    'Player': 'PlayerEntity',
    'Level': 'World',
    'BlockPos': 'BlockPos',
    'ItemStack': 'ItemStack',
    'MobEffect': 'StatusEffect',
    'MobEffectCategory': 'StatusEffectCategory',
    'MobEffectInstance': 'StatusEffectInstance',
    'MobEffects': 'StatusEffects',
    'DamageSource': 'DamageSource',
    'ResourceLocation': 'Identifier',
    'ResourceKey': 'RegistryKey',
    'AbstractContainerMenu': 'ScreenHandler',
    'CompoundTag': 'NbtCompound',
    'ListTag': 'NbtList',
    'InteractionHand': 'Hand',
    'InteractionResult': 'ActionResult',
    'ChatFormatting': 'Formatting',
}

def convert_file(filepath):
    """Convert a single Java file from Forge to Fabric imports."""
    with open(filepath, 'r') as f:
        content = f.read()

    original_content = content

    # Convert imports
    for forge_import, fabric_import in IMPORT_MAPPINGS.items():
        if fabric_import is None:
            # Remove Forge-specific imports
            content = re.sub(f'import {re.escape(forge_import)}[^;]*;\\n?', '', content)
        else:
            # Replace with Fabric equivalent
            content = content.replace(f'import {forge_import}', f'import {fabric_import}')

    # Convert type names in code
    for forge_type, fabric_type in TYPE_MAPPINGS.items():
        # Match whole words only to avoid partial replacements
        content = re.sub(r'\b' + re.escape(forge_type) + r'\b', fabric_type, content)

    # Remove @Mod annotations
    content = re.sub(r'@Mod\([^)]*\)\\n?', '', content)
    content = re.sub(r'@Mod\.EventBusSubscriber\([^)]*\)\\n?', '', content)

    # Remove @SubscribeEvent annotations (will need manual conversion to Fabric events)
    content = re.sub(r'@SubscribeEvent\\n?', '// TODO: Convert to Fabric event\\n', content)

    if content != original_content:
        with open(filepath, 'w') as f:
            f.write(content)
        return True
    return False

def process_directory(directory):
    """Process all Java files in a directory recursively."""
    converted_files = []

    for root, dirs, files in os.walk(directory):
        # Skip build directories
        if 'build' in root or '.gradle' in root:
            continue

        for file in files:
            if file.endswith('.java'):
                filepath = os.path.join(root, file)
                if convert_file(filepath):
                    converted_files.append(filepath)

    return converted_files

if __name__ == '__main__':
    src_dir = '/Users/joshlaptop/Documents/adminium/src/main/java/com/adminium/mod'

    print("Converting Forge imports to Fabric...")
    converted = process_directory(src_dir)

    if converted:
        print(f"\\nConverted {len(converted)} files:")
        for f in converted[:10]:  # Show first 10
            print(f"  - {os.path.basename(f)}")
        if len(converted) > 10:
            print(f"  ... and {len(converted) - 10} more")
    else:
        print("No files needed conversion.")

    print("\\nNote: Manual review is required for:")
    print("  - Event handlers (marked with TODO comments)")
    print("  - Networking code")
    print("  - Registry code")
    print("  - Any Forge-specific functionality")