package com.desafio4.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Item generico: al hacer clic derecho ejecuta una accion en el servidor. */
public class ActionItem extends Item {
    public interface Use {
        /** @return true si la accion se ejecuto (y por tanto se puede consumir el item). */
        boolean run(ServerPlayer player, InteractionHand hand);
    }

    private final Use use;
    private final boolean consume;
    private final boolean foil;
    private final int cooldown;

    public ActionItem(Properties props, Use use, boolean consume, boolean foil, int cooldown) {
        super(props);
        this.use = use;
        this.consume = consume;
        this.foil = foil;
        this.cooldown = cooldown;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            boolean ok = use.run(sp, hand);
            if (ok) {
                if (consume && !sp.getAbilities().instabuild) stack.shrink(1);
                if (cooldown > 0) sp.getCooldowns().addCooldown(this, cooldown);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return foil || super.isFoil(stack);
    }
}
