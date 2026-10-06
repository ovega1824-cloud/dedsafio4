package com.desafio4.client;

import com.desafio4.Desafio4;
import com.desafio4.client.screen.ConfigScreen;
import com.desafio4.client.screen.InputScreen;
import com.desafio4.client.screen.MissionScreen;
import com.desafio4.client.screen.PanelScreen;
import com.desafio4.client.screen.ReviveScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Recibe los paquetes del servidor y arranca los efectos/pantallas del cliente. */
public class ClientHooks {

    public static void play(SoundEvent se, float pitch, float vol) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(se, pitch, vol));
    }

    public static void state(int lives, int start, int max, int difficulty) {
        ClientState.lives = lives;
        ClientState.start = start;
        ClientState.max = max;
        ClientState.difficulty = difficulty;
    }

    public static void fx(String type, String data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        long now = System.currentTimeMillis();
        switch (type) {
            case "roulette" -> {
                int idx = 0;
                try {
                    idx = Integer.parseInt(data);
                } catch (Exception ignored) {
                }
                FxRenderer.ROULETTES.add(new FxRenderer.Roulette(idx, now));
                play(SoundEvents.ENCHANTMENT_TABLE_USE, 1.4f, 0.8f);
            }
            case "death" -> {
                String[] p = data.split("\u0001", -1);
                String name = p.length > 0 ? p[0] : "?";
                Component cause = Component.literal(name);
                try {
                    Component c = Component.Serializer.fromJson(p[1]);
                    if (c != null) cause = c;
                } catch (Exception ignored) {
                }
                boolean self = p.length > 2 && p[2].equals(mc.player.getUUID().toString());
                FxRenderer.DEATHS.add(new FxRenderer.Death(name, cause, self, now));
                float vol = self ? 0.9f : 0.35f;
                play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.55f, vol);
                play(SoundEvents.WARDEN_SONIC_BOOM, 0.85f, vol * 0.55f);
                play(SoundEvents.WARDEN_HEARTBEAT, 0.8f, vol);
            }
            case "revive" -> {
                FxRenderer.REVIVES.add(new FxRenderer.Revive(data, now));
                play(SoundEvents.BEACON_ACTIVATE, 0.9f, 1f);
                play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f, 1f);
            }
            case "smoke" -> {
                int s = 25;
                try {
                    s = Integer.parseInt(data);
                } catch (Exception ignored) {
                }
                FxRenderer.SMOKES.add(new FxRenderer.Smoke(now, s * 1000L));
                play(SoundEvents.WITHER_SPAWN, 0.6f, 1.5f);
                play(SoundEvents.ENDER_DRAGON_GROWL, 0.5f, 1.2f);
            }
            case "acid" -> {
                int s = 45;
                try {
                    s = Integer.parseInt(data);
                } catch (Exception ignored) {
                }
                FxRenderer.ACIDS.add(new FxRenderer.Acid(now, s * 1000L));
                play(SoundEvents.GENERIC_EXTINGUISH_FIRE, 0.6f, 1f);
            }
            case "eclipse" -> {
                ClientState.eclipse = data.equals("1");
                if (ClientState.eclipse) play(SoundEvents.ELDER_GUARDIAN_CURSE, 0.7f, 1f);
            }
            case "moment" -> {
                String[] p = data.split("\u0001", -1);
                String t = p.length > 0 ? p[0] : "revelacion";
                String title = p.length > 1 ? p[1] : "";
                String sub = p.length > 2 ? p[2] : "";
                FxRenderer.MOMENTS.add(new FxRenderer.Moment(t, title, sub, now));
                switch (t) {
                    case "terror" -> {
                        play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6f, 0.8f);
                        play(SoundEvents.ENDER_DRAGON_GROWL, 0.7f, 0.8f);
                    }
                    case "epico", "victoria" -> {
                        play(SoundEvents.TOTEM_USE, 1f, 0.8f);
                        play(SoundEvents.PLAYER_LEVELUP, 0.9f, 1f);
                    }
                    default -> {
                        play(SoundEvents.BEACON_POWER_SELECT, 1f, 1f);
                        play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1f);
                    }
                }
            }
            case "box" -> {
                try {
                    Component c = Component.Serializer.fromJson(data);
                    if (c != null) FxRenderer.BOXES.add(new FxRenderer.Box(c, now));
                } catch (Exception ignored) {
                }
            }
            case "goldheart" -> {
                FxRenderer.GOLDS.add(new FxRenderer.Gold(now));
                play(SoundEvents.PLAYER_LEVELUP, 1.3f, 1f);
                play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 1f);
            }
            case "totem" -> {
                Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(Desafio4.MODID, data));
                if (it != null) mc.gameRenderer.displayItemActivation(new ItemStack(it));
            }
            case "clear" -> {
                FxRenderer.clearAll();
                ClientState.eclipse = false;
            }
            default -> {
            }
        }
    }

    public static void open(String screen, CompoundTag t) {
        Minecraft mc = Minecraft.getInstance();
        switch (screen) {
            case "panel" -> mc.setScreen(new PanelScreen(t));
            case "config" -> mc.setScreen(new ConfigScreen(t));
            case "mission" -> mc.setScreen(new MissionScreen(t));
            case "revive" -> mc.setScreen(new ReviveScreen(t));
            case "layer_up" -> mc.setScreen(InputScreen.layer(true, t));
            case "layer_down" -> mc.setScreen(InputScreen.layer(false, t));
            case "craftlimit" -> mc.setScreen(InputScreen.craftLimit(t));
            case "moment" -> mc.setScreen(InputScreen.moment());
            case "box" -> mc.setScreen(InputScreen.box());
            default -> {
            }
        }
    }
}
