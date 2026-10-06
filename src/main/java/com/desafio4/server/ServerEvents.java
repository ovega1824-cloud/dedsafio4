package com.desafio4.server;

import com.desafio4.Desafio4;
import com.desafio4.data.D4Data;
import com.desafio4.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Desafio4.MODID)
public class ServerEvents {
    private static final UUID HP_ID = UUID.fromString("5f0c2d52-3a1e-4d2b-9b0a-0d4a4a4a0001");
    private static final UUID SPEED_ID = UUID.fromString("5f0c2d52-3a1e-4d2b-9b0a-0d4a4a4a0002");

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        ModCommands.register(e.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent e) {
        MinecraftServer s = e.getServer();
        D4Data.get(s);
        Game.applyRules(s);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s != null) Game.tick(s);
    }

    // ---------------------------------------------------------------- muerte y vidas

    private static ItemStack findTotem(ServerPlayer p) {
        Item t = ModItems.TOTEM_LIFE.get();
        if (p.getMainHandItem().is(t)) return p.getMainHandItem();
        if (p.getOffhandItem().is(t)) return p.getOffhandItem();
        for (ItemStack st : p.getInventory().items) if (st.is(t)) return st;
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity().level().isClientSide) return;
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s == null) return;

        // misiones: matar mobs / jugadores
        Entity killer = e.getSource().getEntity();
        if (killer instanceof ServerPlayer) {
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(e.getEntity().getType());
            if (key != null) Missions.progress(s, "KILL", key.getPath(), 1);
        }

        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        D4Data d = D4Data.get(s);

        // Totem de vida: te salva y no pierdes la vida
        ItemStack totem = findTotem(p);
        if (!totem.isEmpty()) {
            e.setCanceled(true);
            totem.shrink(1);
            p.setHealth(8f);
            p.removeAllEffects();
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1));
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
            p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0));
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1f, 1f);
            Game.fxTo(p, "totem", "totem_life");
            return;
        }

        int left = Math.max(0, d.getLives(p.getUUID()) - 1);
        d.setLives(p.getUUID(), left);

        // corazon dorado en PvP
        if (killer instanceof ServerPlayer k && k != p && d.pvpDrop) {
            ItemEntity ie = new ItemEntity(p.level(), p.getX(), p.getY() + 0.5, p.getZ(), new ItemStack(ModItems.GOLD_HEART.get()));
            ie.setGlowingTag(true);
            ie.setPickUpDelay(20);
            p.level().addFreshEntity(ie);
        }

        String json = Component.Serializer.toJson(e.getSource().getLocalizedDeathMessage(p));
        Game.fx("death", p.getName().getString() + Game.SEP + json + Game.SEP + p.getUUID());

        if (left <= 0) {
            d.dead.put(p.getUUID(), p.getName().getString());
            d.setDirty();
            Game.broadcast(s, Component.literal(p.getName().getString() + " se quedó sin vidas").withStyle(net.minecraft.ChatFormatting.DARK_RED));
        }
        Game.syncAll(s);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        D4Data d = D4Data.get(p.server);
        if (d.getLives(p.getUUID()) <= 0) {
            Game.makeSpectator(p);
            Game.say(p, "Sin vidas: eres espectador hasta que te revivan");
        }
        Game.applyFly(p);
        Game.sync(p);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        MinecraftServer s = p.server;
        D4Data d = D4Data.get(s);
        if (d.pending.containsKey(p.getUUID())) {
            var bp = d.pending.remove(p.getUUID());
            d.setDirty();
            Game.startRevive(s, p, s.overworld(), net.minecraft.world.phys.Vec3.atBottomCenterOf(bp));
        } else if (d.getLives(p.getUUID()) <= 0) {
            d.dead.put(p.getUUID(), p.getName().getString());
            d.setDirty();
            Game.makeSpectator(p);
        }
        Game.applyFly(p);
        Game.sync(p);
        if (d.eclipse) Game.fxTo(p, "eclipse", "1");
        int tc = s.getTickCount();
        if (tc < Game.smokeUntil) Game.fxTo(p, "smoke", String.valueOf((Game.smokeUntil - tc) / 20));
        if (tc < Game.acidUntil) Game.fxTo(p, "acid", String.valueOf((Game.acidUntil - tc) / 20));
    }

    // ---------------------------------------------------------------- dificultad

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        if (!(e.getEntity() instanceof Monster m)) return;
        MinecraftServer s = e.getLevel().getServer();
        if (s == null) return;
        D4Data d = D4Data.get(s);

        AttributeInstance hp = m.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && d.mobHealth != 1f && hp.getModifier(HP_ID) == null) {
            hp.addTransientModifier(new AttributeModifier(HP_ID, "desafio4_hp", d.mobHealth - 1.0, AttributeModifier.Operation.MULTIPLY_BASE));
            m.setHealth(m.getMaxHealth());
        }
        AttributeInstance sp = m.getAttribute(Attributes.MOVEMENT_SPEED);
        if (sp != null && d.mobSpeed != 1f && sp.getModifier(SPEED_ID) == null) {
            sp.addTransientModifier(new AttributeModifier(SPEED_ID, "desafio4_speed", d.mobSpeed - 1.0, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer)) return;
        if (!(e.getSource().getEntity() instanceof Monster)) return;
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s == null) return;
        float mult = D4Data.get(s).mobDamage;
        if (mult != 1f) e.setAmount(e.getAmount() * mult);
    }

    // ---------------------------------------------------------------- misiones y crafteo

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer p)) return;
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(e.getState().getBlock());
        if (key != null) Missions.progress(p.server, "MINE", key.getPath(), 1);
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ItemStack out = e.getCrafting();
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(out.getItem());
        if (key != null) Missions.progress(p.server, "CRAFT", key.getPath(), out.getCount());
        if (out.is(ModItems.RESURRECTION_HEART.get())) {
            D4Data d = D4Data.get(p.server);
            d.crafted += out.getCount();
            d.setDirty();
            D4Data.refresh(d);
            Game.say(p, "Corazones de resurrección crafteados: " + d.crafted + "/" + d.craftLimit);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Missions.progress(p.server, "DIM", e.getTo().location().getPath(), 1);
    }
}
