package com.desafio4.block;

import com.desafio4.Desafio4;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Desafio4.MODID);

    public static final RegistryObject<Block> RESURRECTION_CAMPFIRE = BLOCKS.register("resurrection_campfire",
            ResurrectionCampfire::new);
}
