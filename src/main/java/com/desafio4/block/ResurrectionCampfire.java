package com.desafio4.block;

import com.desafio4.item.ModItems;
import com.desafio4.server.Game;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ResurrectionCampfire extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

    public ResurrectionCampfire() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .strength(2.0f)
                .sound(SoundType.WOOD)
                .lightLevel(s -> 15)
                .noOcclusion());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            if (sp.getItemInHand(hand).is(ModItems.RESURRECTION_HEART.get())) {
                Game.open(sp, "revive", pos);
            } else {
                Game.say(sp, "Necesitas un Corazón de la Resurrección en la mano");
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.6, z = pos.getZ() + 0.5;
        for (int i = 0; i < 3; i++) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    x + (r.nextDouble() - 0.5) * 1.6, y + r.nextDouble() * 0.6, z + (r.nextDouble() - 0.5) * 1.6,
                    0, 0.04 + r.nextDouble() * 0.04, 0);
        }
        if (r.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, x, y + 1.2, z, 0, 0.05, 0);
        }
        if (r.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.END_ROD, x + (r.nextDouble() - 0.5) * 2, y + 0.5, z + (r.nextDouble() - 0.5) * 2, 0, 0.06, 0);
        }
    }
}
