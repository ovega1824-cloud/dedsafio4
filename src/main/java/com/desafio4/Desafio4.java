package com.desafio4;

import com.desafio4.block.ModBlocks;
import com.desafio4.item.ModItems;
import com.desafio4.net.Net;
import com.desafio4.recipe.ModRecipes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(Desafio4.MODID)
public class Desafio4 {
    public static final String MODID = "desafio4";

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.desafio4"))
            .icon(() -> new ItemStack(ModItems.GOLD_HEART.get()))
            .displayItems((params, out) -> ModItems.ITEMS.getEntries().forEach(i -> out.accept(i.get())))
            .build());

    public Desafio4() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModRecipes.SERIALIZERS.register(bus);
        TABS.register(bus);
        Net.init();
    }
}
