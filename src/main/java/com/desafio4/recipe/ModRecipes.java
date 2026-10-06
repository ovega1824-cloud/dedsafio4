package com.desafio4.recipe;

import com.desafio4.Desafio4;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Desafio4.MODID);

    public static final RegistryObject<RecipeSerializer<?>> LIMITED =
            SERIALIZERS.register("limited_shaped", LimitedShaped.Serializer::new);
}
