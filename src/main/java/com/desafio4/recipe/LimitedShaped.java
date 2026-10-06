package com.desafio4.recipe;

import com.desafio4.data.D4Data;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/** Receta con forma que solo funciona mientras no se alcance el limite de corazones de resurreccion. */
public class LimitedShaped extends ShapedRecipe {
    public LimitedShaped(ShapedRecipe r) {
        super(r.getId(), r.getGroup(), r.category(), r.getWidth(), r.getHeight(), r.getIngredients(),
                r.getResultItem(RegistryAccess.EMPTY));
    }

    @Override
    public boolean matches(CraftingContainer c, Level level) {
        return D4Data.heartsCraftable && super.matches(c, level);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.LIMITED.get();
    }

    public static class Serializer implements RecipeSerializer<LimitedShaped> {
        @Override
        public LimitedShaped fromJson(ResourceLocation id, JsonObject json) {
            return new LimitedShaped(RecipeSerializer.SHAPED_RECIPE.fromJson(id, json));
        }

        @Override
        public LimitedShaped fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new LimitedShaped(RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, LimitedShaped recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe);
        }
    }
}
