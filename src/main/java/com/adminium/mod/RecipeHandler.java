package com.adminium.mod;

import com.adminium.mod.manager.DisabledItemsManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import java.util.stream.Collectors;

public class RecipeHandler {

    // TODO: Convert to Fabric event
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            updateRecipesForPlayer((ServerPlayerEntity) event.getEntity());
        }
    }

    public static void updateRecipesForPlayer(ServerPlayerEntity player) {
        RecipeManager recipeManager = player.getServer().getRecipeManager();
        player.connection.send(new ClientboundUpdateRecipesPacket(
            recipeManager.getRecipes().stream()
                .filter(recipe -> !DisabledItemsManager.isItemDisabled(recipe.getResultItem(player.level().registryAccess()).getItem()))
                .collect(Collectors.toList())
        ));
    }
} 