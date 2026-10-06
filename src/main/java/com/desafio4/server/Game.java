package com.desafio4.server;

import com.desafio4.block.ModBlocks;
import com.desafio4.data.D4Data;
import com.desafio4.data.Mission;
import com.desafio4.item.ModItems;
import com.desafio4.net.Net;
import com.desafio4.net.Packets;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** Toda la logica del lado del servidor: efectos, vidas, menus, acciones. */
public class Game {
    public static final String SEP = "\u0001";
    private static final Random RNG = new Random();

    public static int smokeUntil = 0;
    public static int acidUntil = 0;

    private static final DustParticleOptions RING = new DustParticleOptions(new Vector3f(0.15f, 1.0f, 0.35f), 1.4f);
    private static final DustParticleOptions ACID = new DustParticleOptions(new Vector3f(0.35f, 1.0f, 0.1f), 1.2f);
    private static final DustParticleOptions GAS = new DustParticleOptions(new Vector3f(1.0f, 0.1f, 0.1f), 2.0f);

    private static class Revive {
        final UUID id;
        int ticks;

        Revive(UUID id, int ticks) {
            this.id = id;
            this.ticks = ticks;
        }
    }

    private static final List<Revive> REVIVES = new ArrayList<>();

    // ------------------------------------------------------------------ utilidades

    public static D4Data data(MinecraftServer s) {
        return D4Data.get(s);
    }

    public static void fx(String type, String data) {
        Net.toAll(new Packets.Fx(type, data));
    }

    public static void fxTo(ServerPlayer p, String type, String data) {
        Net.to(p, new Packets.Fx(type, data));
    }

    public static boolean isAdmin(ServerPlayer p) {
        return p.hasPermissions(2) || p.server.isSingleplayerOwner(p.getGameProfile());
    }

    public static boolean holds(ServerPlayer p, Item item) {
        return p.getMainHandItem().is(item) || p.getOffhandItem().is(item);
    }

    public static void say(ServerPlayer p, String msg) {
        p.displayClientMessage(Component.literal(msg), true);
    }

    public static void broadcast(MinecraftServer s, Component c) {
        s.getPlayerList().broadcastSystemMessage(c, false);
    }

    private static void consumeHeld(ServerPlayer p, Item item) {
        if (p.getAbilities().instabuild) return;
        if (p.getMainHandItem().is(item)) p.getMainHandItem().shrink(1);
        else if (p.getOffhandItem().is(item)) p.getOffhandItem().shrink(1);
    }

    public static void sync(ServerPlayer p) {
        D4Data d = data(p.server);
        Net.to(p, new Packets.State(d.getLives(p.getUUID()), d.startLives, d.maxLives, d.difficulty));
    }

    public static void syncAll(MinecraftServer s) {
        for (ServerPlayer p : s.getPlayerList().getPlayers()) sync(p);
    }

    // ------------------------------------------------------------------ efectos

    public static void roulette(MinecraftServer s, int idx) {
        if (idx < 0 || idx > 7) idx = RNG.nextInt(8);
        fx("roulette", String.valueOf(idx));
    }

    public static void smoke(MinecraftServer s, int seconds) {
        smokeUntil = s.getTickCount() + seconds * 20;
        fx("smoke", String.valueOf(seconds));
    }

    public static void acid(MinecraftServer s, int seconds) {
        acidUntil = s.getTickCount() + seconds * 20;
        fx("acid", String.valueOf(seconds));
    }

    public static void setEclipse(MinecraftServer s, boolean on) {
        D4Data d = data(s);
        d.eclipse = on;
        d.setDirty();
        fx("eclipse", on ? "1" : "0");
        if (on) moment(s, "terror", "ECLIPSE", "Dos miradas al cielo y todo termina");
    }

    public static void setLayer(MinecraftServer s, boolean up, boolean on, int y) {
        D4Data d = data(s);
        if (up) {
            d.layerUpOn = on;
            d.layerUp = y;
        } else {
            d.layerDownOn = on;
            d.layerDown = y;
        }
        d.setDirty();
        String t = up ? "CAPA SUPERIOR" : "CAPA INFERIOR";
        if (on) {
            moment(s, "terror", t, (up ? "Sube por encima de Y=" : "Baja por debajo de Y=") + y);
        } else {
            broadcast(s, Component.literal(t + " desactivada").withStyle(ChatFormatting.GRAY));
        }
    }

    public static void toggleFly(ServerPlayer p) {
        D4Data d = data(p.server);
        boolean on = !d.flyers.contains(p.getUUID());
        if (on) d.flyers.add(p.getUUID());
        else d.flyers.remove(p.getUUID());
        d.setDirty();
        applyFly(p);
        say(p, on ? "Vuelo activado" : "Vuelo desactivado");
    }

    public static void applyFly(ServerPlayer p) {
        if (p.isCreative() || p.isSpectator()) return;
        boolean on = data(p.server).flyers.contains(p.getUUID());
        Abilities a = p.getAbilities();
        if (a.mayfly != on) {
            a.mayfly = on;
            if (!on) a.flying = false;
            p.onUpdateAbilities();
        }
    }

    public static void moment(MinecraftServer s, String type, String title, String sub) {
        fx("moment", type + SEP + title + SEP + sub);
    }

    public static void box(MinecraftServer s, String text) {
        Component c = Component.literal(text).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        fx("box", Component.Serializer.toJson(c));
    }

    public static void chaos(MinecraftServer s) {
        switch (RNG.nextInt(4)) {
            case 0 -> roulette(s, -1);
            case 1 -> smoke(s, 15);
            case 2 -> acid(s, 30);
            default -> moment(s, "terror", "CAOS", "Algo ha cambiado...");
        }
    }

    public static boolean goldHeart(ServerPlayer p) {
        D4Data d = data(p.server);
        int cur = d.getLives(p.getUUID());
        if (cur >= d.maxLives) {
            say(p, "Ya tienes el máximo de vidas (" + d.maxLives + ")");
            return false;
        }
        d.setLives(p.getUUID(), cur + 1);
        fxTo(p, "goldheart", "");
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.2f);
        sync(p);
        return true;
    }

    // ------------------------------------------------------------------ dificultad

    public static void preset(MinecraftServer s, int idx) {
        D4Data d = data(s);
        d.applyPreset(idx);
        applyRules(s);
        syncAll(s);
        moment(s, idx >= 3 ? "terror" : "revelacion", "DIFICULTAD: " + D4Data.PRESET_NAMES[d.difficulty].toUpperCase(),
                "Daño x" + d.mobDamage + "  Vida x" + d.mobHealth + "  Hambre x" + d.hunger);
    }

    public static void cycleDifficulty(MinecraftServer s) {
        preset(s, (data(s).difficulty + 1) % D4Data.PRESETS.length);
    }

    public static void applyRules(MinecraftServer s) {
        D4Data d = data(s);
        s.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(d.regen, s);
        Difficulty diff = d.difficulty <= 0 ? Difficulty.EASY : d.difficulty == 1 ? Difficulty.NORMAL : Difficulty.HARD;
        s.setDifficulty(diff, true);
    }

    // ------------------------------------------------------------------ vidas y reviver

    public static void makeSpectator(ServerPlayer p) {
        p.setGameMode(GameType.SPECTATOR);
    }

    public static void revive(MinecraftServer s, UUID id, ServerLevel lvl, Vec3 at) {
        D4Data d = data(s);
        String name = d.dead.remove(id);
        d.setLives(id, 1);
        d.setDirty();
        ServerPlayer t = s.getPlayerList().getPlayer(id);
        if (t == null) {
            d.pending.put(id, BlockPos.containing(at));
            fx("revive", name == null ? "Jugador" : name);
            return;
        }
        startRevive(s, t, lvl, at);
    }

    public static void startRevive(MinecraftServer s, ServerPlayer t, ServerLevel lvl, Vec3 at) {
        t.setGameMode(GameType.SURVIVAL);
        t.teleportTo(lvl, at.x, at.y, at.z, t.getYRot(), t.getXRot());
        t.setHealth(t.getMaxHealth());
        t.getFoodData().setFoodLevel(20);
        t.removeAllEffects();
        t.setInvulnerable(true);
        t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 0, false, false));
        t.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, false));
        REVIVES.removeIf(r -> r.id.equals(t.getUUID()));
        REVIVES.add(new Revive(t.getUUID(), 180));
        fx("revive", t.getName().getString());
        sync(t);
    }

    // ------------------------------------------------------------------ menus

    public static void open(ServerPlayer p, String screen) {
        open(p, screen, null);
    }

    public static void open(ServerPlayer p, String screen, BlockPos pos) {
        D4Data d = data(p.server);
        CompoundTag t = new CompoundTag();
        t.putBoolean("admin", isAdmin(p));
        switch (screen) {
            case "config" -> {
                t.putInt("start", d.startLives);
                t.putInt("max", d.maxLives);
                t.putInt("craft", d.craftLimit);
                t.putInt("crafted", d.crafted);
                t.putInt("difficulty", d.difficulty);
                t.putFloat("dmg", d.mobDamage);
                t.putFloat("hp", d.mobHealth);
                t.putFloat("speed", d.mobSpeed);
                t.putFloat("hunger", d.hunger);
                t.putBoolean("regen", d.regen);
                t.putBoolean("pvp", d.pvpDrop);
            }
            case "mission" -> {
                ListTag l = new ListTag();
                for (Mission m : d.missions) l.add(m.save());
                t.put("missions", l);
            }
            case "revive" -> {
                if (d.dead.isEmpty()) {
                    say(p, "No hay jugadores muertos para revivir");
                    return;
                }
                ListTag l = new ListTag();
                d.dead.forEach((id, name) -> {
                    CompoundTag e = new CompoundTag();
                    e.putString("uuid", id.toString());
                    e.putString("name", name);
                    l.add(e);
                });
                t.put("dead", l);
                if (pos != null) {
                    t.putBoolean("hasPos", true);
                    t.putInt("x", pos.getX());
                    t.putInt("y", pos.getY());
                    t.putInt("z", pos.getZ());
                }
            }
            case "layer_up" -> {
                t.putInt("y", d.layerUp);
                t.putBoolean("on", d.layerUpOn);
            }
            case "layer_down" -> {
                t.putInt("y", d.layerDown);
                t.putBoolean("on", d.layerDownOn);
            }
            case "craftlimit" -> {
                t.putInt("craft", d.craftLimit);
                t.putInt("crafted", d.crafted);
            }
            default -> {
            }
        }
        Net.to(p, new Packets.Open(screen, t));
    }

    private static boolean canOpen(ServerPlayer p, String screen) {
        if (isAdmin(p)) return true;
        return switch (screen) {
            case "mission" -> true;
            case "revive" -> holds(p, ModItems.TOTEM_REVIVE.get()) || holds(p, ModItems.RESURRECTION_HEART.get());
            case "layer_up" -> holds(p, ModItems.LAYER_UP.get());
            case "layer_down" -> holds(p, ModItems.LAYER_DOWN.get());
            case "craftlimit" -> holds(p, ModItems.CRAFT_LIMIT_HEART.get());
            case "moment" -> holds(p, ModItems.MOMENT_CRYSTAL.get());
            case "panel", "config" -> holds(p, ModItems.CONFIG_HEART.get());
            default -> false;
        };
    }

    // ------------------------------------------------------------------ acciones desde menus/teclas

    private static int num(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static float clampF(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static void handleAct(ServerPlayer p, String raw) {
        String[] a = raw.split(SEP, -1);
        MinecraftServer s = p.server;
        D4Data d = data(s);
        boolean admin = isAdmin(p);
        String cmd = a[0];
        String a1 = a.length > 1 ? a[1] : "";
        String a2 = a.length > 2 ? a[2] : "";
        String a3 = a.length > 3 ? a[3] : "";

        switch (cmd) {
            case "open" -> {
                if (canOpen(p, a1)) open(p, a1);
                else say(p, "Necesitas permisos de operador para abrir este menú");
            }
            case "cfg" -> {
                boolean craftItem = a1.equals("craftset") && holds(p, ModItems.CRAFT_LIMIT_HEART.get());
                if (!admin && !craftItem) {
                    say(p, "Solo un operador puede cambiar la configuración");
                    return;
                }
                int v = num(a2, 0);
                switch (a1) {
                    case "start" -> {
                        d.startLives = clamp(d.startLives + v, 1, 10);
                        if (d.maxLives < d.startLives) d.maxLives = d.startLives;
                    }
                    case "max" -> d.maxLives = clamp(d.maxLives + v, Math.max(1, d.startLives), 10);
                    case "craft" -> d.craftLimit = clamp(d.craftLimit + v, 1, 10);
                    case "craftset" -> d.craftLimit = clamp(v, 1, 10);
                    case "pvp" -> d.pvpDrop = !d.pvpDrop;
                    case "regen" -> d.regen = !d.regen;
                    case "preset" -> {
                        preset(s, v);
                    }
                    case "dmg" -> d.mobDamage = clampF(d.mobDamage + 0.25f * v, 0.25f, 10f);
                    case "hp" -> d.mobHealth = clampF(d.mobHealth + 0.25f * v, 0.25f, 10f);
                    case "speed" -> d.mobSpeed = clampF(d.mobSpeed + 0.05f * v, 0.5f, 3f);
                    case "hunger" -> d.hunger = clampF(d.hunger + 0.25f * v, 0.25f, 10f);
                    default -> {
                    }
                }
                d.setDirty();
                D4Data.refresh(d);
                applyRules(s);
                syncAll(s);
                if (craftItem && !admin) open(p, "craftlimit");
                else open(p, "config");
            }
            case "roulette" -> {
                if (admin) roulette(s, num(a1, -1));
            }
            case "moment" -> {
                if (admin || holds(p, ModItems.MOMENT_CRYSTAL.get())) {
                    moment(s, a1.isEmpty() ? "revelacion" : a1, a2.isEmpty() ? "MOMENTO" : a2, a3);
                }
            }
            case "box" -> {
                if (admin && !a1.isEmpty()) box(s, a1);
            }
            case "smoke" -> {
                if (admin) smoke(s, 25);
            }
            case "acid" -> {
                if (admin) acid(s, 45);
            }
            case "eclipse" -> {
                if (admin) setEclipse(s, !d.eclipse);
            }
            case "fly" -> {
                if (admin) toggleFly(p);
            }
            case "stopall" -> {
                if (admin) stopAll(s);
            }
            case "testdeath" -> {
                if (admin) fx("death", p.getName().getString() + SEP
                        + Component.Serializer.toJson(Component.literal(p.getName().getString() + " perdió una vida (prueba)")) + SEP
                        + p.getUUID());
            }
            case "testrevive" -> {
                if (admin) fx("revive", p.getName().getString());
            }
            case "layer" -> {
                boolean up = a1.equals("up");
                Item need = up ? ModItems.LAYER_UP.get() : ModItems.LAYER_DOWN.get();
                if (admin || holds(p, need)) {
                    boolean on = a2.equals("on");
                    int y = num(a3, up ? d.layerUp : d.layerDown);
                    setLayer(s, up, on, y);
                    open(p, up ? "layer_up" : "layer_down");
                }
            }
            case "mission" -> {
                if (!admin) {
                    say(p, "Solo un operador puede editar misiones");
                    return;
                }
                switch (a1) {
                    case "plus" -> {
                        Mission m = d.mission(a2);
                        if (m != null) Missions.add(s, d, m, 1);
                    }
                    case "done" -> {
                        Mission m = d.mission(a2);
                        if (m != null && !m.done) Missions.complete(s, d, m);
                    }
                    case "remove" -> d.missions.removeIf(m -> m.id.equals(a2));
                    case "reset" -> {
                        for (Mission m : d.missions) {
                            m.progress = 0;
                            m.done = false;
                        }
                    }
                    default -> {
                    }
                }
                d.setDirty();
                open(p, "mission");
            }
            case "revive" -> handleRevive(p, a1, a2);
            default -> {
            }
        }
    }

    private static void handleRevive(ServerPlayer p, String uuid, String pos) {
        MinecraftServer s = p.server;
        D4Data d = data(s);
        UUID id;
        try {
            id = UUID.fromString(uuid);
        } catch (Exception e) {
            return;
        }
        boolean admin = isAdmin(p);
        boolean viaHeart = !pos.isEmpty() && holds(p, ModItems.RESURRECTION_HEART.get());
        boolean viaTotem = pos.isEmpty() && holds(p, ModItems.TOTEM_REVIVE.get());
        if (!admin && !viaHeart && !viaTotem) {
            say(p, "Necesitas un Corazón de la Resurrección o un Tótem de Resurrección");
            return;
        }
        if (!d.dead.containsKey(id)) {
            say(p, "Ese jugador ya no está muerto");
            return;
        }
        Vec3 at = p.position();
        if (!pos.isEmpty()) {
            String[] c = pos.split(",");
            if (c.length == 3) {
                BlockPos bp = new BlockPos(num(c[0], 0), num(c[1], 0), num(c[2], 0));
                if (p.level().getBlockState(bp).is(ModBlocks.RESURRECTION_CAMPFIRE.get())
                        && p.distanceToSqr(Vec3.atCenterOf(bp)) < 100) {
                    at = Vec3.atBottomCenterOf(bp).add(0, 1.0, 0);
                } else if (!admin) {
                    say(p, "Debes estar cerca de la fogata");
                    return;
                }
            }
        }
        if (viaHeart) consumeHeld(p, ModItems.RESURRECTION_HEART.get());
        else if (viaTotem) consumeHeld(p, ModItems.TOTEM_REVIVE.get());
        revive(s, id, p.serverLevel(), at);
    }

    public static void stopAll(MinecraftServer s) {
        smokeUntil = 0;
        acidUntil = 0;
        D4Data d = data(s);
        d.eclipse = false;
        d.layerUpOn = false;
        d.layerDownOn = false;
        d.setDirty();
        fx("clear", "");
    }

    // ------------------------------------------------------------------ tick

    public static void tick(MinecraftServer s) {
        int tc = s.getTickCount();
        D4Data d = data(s);
        List<ServerPlayer> players = s.getPlayerList().getPlayers();

        // sesiones de revivir: anillos verdes girando
        if (!REVIVES.isEmpty()) {
            REVIVES.removeIf(r -> {
                ServerPlayer t = s.getPlayerList().getPlayer(r.id);
                if (t == null) return true;
                r.ticks--;
                int age = 180 - r.ticks;
                ServerLevel lv = t.serverLevel();
                for (int k = 0; k < 3; k++) {
                    double rad = 1.0 + 0.25 * k;
                    double yo = 0.2 + 0.8 * k + Math.sin(age * 0.1 + k) * 0.15;
                    double dir = (k % 2 == 0) ? 1 : -1;
                    for (int i = 0; i < 10; i++) {
                        double ang = age * 0.18 * dir + i * (Math.PI * 2 / 10);
                        lv.sendParticles(RING, t.getX() + Math.cos(ang) * rad, t.getY() + yo, t.getZ() + Math.sin(ang) * rad, 1, 0, 0, 0, 0);
                    }
                }
                if (age % 10 == 0) lv.sendParticles(ParticleTypes.END_ROD, t.getX(), t.getY() + 1, t.getZ(), 6, 0.4, 0.8, 0.4, 0.02);
                if (r.ticks <= 0) {
                    t.setInvulnerable(false);
                    return true;
                }
                return false;
            });
        }

        // eclipse: si 2 jugadores miran al cielo mueren y se desactiva
        if (d.eclipse && tc % 4 == 0) {
            List<ServerPlayer> lookers = new ArrayList<>();
            for (ServerPlayer p : players) {
                if (p.isCreative() || p.isSpectator() || !p.isAlive()) continue;
                if (p.getXRot() < -50 && p.serverLevel().canSeeSky(p.blockPosition())) lookers.add(p);
            }
            if (lookers.size() >= 2) {
                for (int i = 0; i < 2; i++) {
                    ServerPlayer p = lookers.get(i);
                    p.hurt(p.damageSources().genericKill(), Float.MAX_VALUE);
                }
                setEclipse(s, false);
            }
        }

        // lluvia acida: particulas
        boolean acidOn = tc < acidUntil;
        if (acidOn && tc % 4 == 0) {
            for (ServerPlayer p : players) {
                ServerLevel lv = p.serverLevel();
                for (int i = 0; i < 8; i++) {
                    double x = p.getX() + (RNG.nextDouble() - 0.5) * 12;
                    double z = p.getZ() + (RNG.nextDouble() - 0.5) * 12;
                    lv.sendParticles(ACID, x, p.getY() + 7 + RNG.nextDouble() * 3, z, 1, 0, -0.5, 0, 0.1);
                }
            }
        }

        // cada segundo
        if (tc % 20 == 0) {
            for (ServerPlayer p : players) {
                if (p.isCreative() || p.isSpectator() || !p.isAlive()) continue;
                ServerLevel lv = p.serverLevel();

                if (acidOn && lv.canSeeSky(p.blockPosition().above()) && tc % 40 == 0) {
                    p.hurt(p.damageSources().magic(), 1.5f);
                }
                if (d.layerUpOn && p.getY() < d.layerUp) {
                    say(p, "¡Sube por encima de la capa Y=" + d.layerUp + "!");
                    if (tc % 40 == 0) p.hurt(p.damageSources().magic(), 1.0f);
                }
                if (d.layerDownOn && p.getY() > d.layerDown) {
                    say(p, "¡Baja por debajo de la capa Y=" + d.layerDown + "! (gas rojo)");
                    lv.sendParticles(GAS, p.getX(), p.getY() + 1, p.getZ(), 12, 0.6, 0.8, 0.6, 0.01);
                    if (tc % 40 == 0) p.hurt(p.damageSources().wither(), 1.0f);
                }
                if (d.hunger > 1f) p.causeFoodExhaustion((d.hunger - 1f) * 0.4f);
            }
        }

        // misiones de tiempo: cada minuto
        if (tc % 1200 == 0 && !players.isEmpty()) {
            Missions.progress(s, "TIME", "", 1);
        }
    }
}
