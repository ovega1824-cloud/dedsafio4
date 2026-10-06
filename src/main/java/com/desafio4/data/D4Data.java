package com.desafio4.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class D4Data extends SavedData {
    public static final String[] PRESET_NAMES = {"Fácil", "Normal", "Difícil", "Hardcore", "Pesadilla"};
    public static final String[] PRESET_IDS = {"facil", "normal", "dificil", "hardcore", "pesadilla"};
    /** dano, vida, velocidad, hambre, regen (1 = si) */
    public static final float[][] PRESETS = {
            {0.75f, 0.75f, 1.0f, 0.75f, 1},
            {1.0f, 1.0f, 1.0f, 1.0f, 1},
            {1.5f, 1.5f, 1.0f, 1.25f, 1},
            {2.0f, 2.0f, 1.1f, 1.5f, 0},
            {3.0f, 3.0f, 1.25f, 2.0f, 0}
    };

    /** Se actualiza en el servidor: si todavia se pueden craftear corazones de resurreccion. */
    public static volatile boolean heartsCraftable = true;

    public final Map<UUID, Integer> lives = new HashMap<>();
    public final Map<UUID, String> dead = new LinkedHashMap<>();
    public final Map<UUID, BlockPos> pending = new HashMap<>();
    public final Set<UUID> flyers = new HashSet<>();
    public final List<Mission> missions = new ArrayList<>();

    public int startLives = 4;
    public int maxLives = 6;
    public int difficulty = 1;
    public int craftLimit = 3;
    public int crafted = 0;
    public float mobDamage = 1f, mobHealth = 1f, mobSpeed = 1f, hunger = 1f;
    public boolean regen = true;
    public boolean pvpDrop = true;

    public boolean eclipse = false;
    public boolean layerUpOn = false, layerDownOn = false;
    public int layerUp = 64, layerDown = 0;

    public D4Data() {
        missions.addAll(Mission.defaults());
    }

    public static D4Data get(MinecraftServer server) {
        D4Data d = server.overworld().getDataStorage().computeIfAbsent(D4Data::load, D4Data::new, "desafio4");
        refresh(d);
        return d;
    }

    public static void refresh(D4Data d) {
        heartsCraftable = d.crafted < d.craftLimit;
    }

    public int getLives(UUID id) {
        return lives.getOrDefault(id, startLives);
    }

    public void setLives(UUID id, int n) {
        lives.put(id, Math.max(0, Math.min(maxLives, n)));
        setDirty();
    }

    public void applyPreset(int idx) {
        idx = Math.max(0, Math.min(PRESETS.length - 1, idx));
        difficulty = idx;
        float[] p = PRESETS[idx];
        mobDamage = p[0];
        mobHealth = p[1];
        mobSpeed = p[2];
        hunger = p[3];
        regen = p[4] > 0;
        setDirty();
    }

    public Mission mission(String id) {
        for (Mission m : missions) if (m.id.equals(id)) return m;
        return null;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        t.putInt("startLives", startLives);
        t.putInt("maxLives", maxLives);
        t.putInt("difficulty", difficulty);
        t.putInt("craftLimit", craftLimit);
        t.putInt("crafted", crafted);
        t.putFloat("mobDamage", mobDamage);
        t.putFloat("mobHealth", mobHealth);
        t.putFloat("mobSpeed", mobSpeed);
        t.putFloat("hunger", hunger);
        t.putBoolean("regen", regen);
        t.putBoolean("pvpDrop", pvpDrop);
        t.putBoolean("eclipse", eclipse);
        t.putBoolean("layerUpOn", layerUpOn);
        t.putBoolean("layerDownOn", layerDownOn);
        t.putInt("layerUp", layerUp);
        t.putInt("layerDown", layerDown);

        CompoundTag lv = new CompoundTag();
        lives.forEach((k, v) -> lv.putInt(k.toString(), v));
        t.put("lives", lv);

        CompoundTag dd = new CompoundTag();
        dead.forEach((k, v) -> dd.putString(k.toString(), v));
        t.put("dead", dd);

        CompoundTag pe = new CompoundTag();
        pending.forEach((k, v) -> pe.putLong(k.toString(), v.asLong()));
        t.put("pending", pe);

        ListTag fl = new ListTag();
        for (UUID u : flyers) fl.add(StringTag.valueOf(u.toString()));
        t.put("flyers", fl);

        ListTag ml = new ListTag();
        for (Mission m : missions) ml.add(m.save());
        t.put("missions", ml);
        return t;
    }

    public static D4Data load(CompoundTag t) {
        D4Data d = new D4Data();
        d.startLives = t.getInt("startLives");
        d.maxLives = t.getInt("maxLives");
        d.difficulty = t.getInt("difficulty");
        d.craftLimit = t.getInt("craftLimit");
        d.crafted = t.getInt("crafted");
        d.mobDamage = t.getFloat("mobDamage");
        d.mobHealth = t.getFloat("mobHealth");
        d.mobSpeed = t.getFloat("mobSpeed");
        d.hunger = t.getFloat("hunger");
        d.regen = t.getBoolean("regen");
        d.pvpDrop = t.getBoolean("pvpDrop");
        d.eclipse = t.getBoolean("eclipse");
        d.layerUpOn = t.getBoolean("layerUpOn");
        d.layerDownOn = t.getBoolean("layerDownOn");
        d.layerUp = t.getInt("layerUp");
        d.layerDown = t.getInt("layerDown");

        CompoundTag lv = t.getCompound("lives");
        for (String k : lv.getAllKeys()) d.lives.put(UUID.fromString(k), lv.getInt(k));
        CompoundTag dd = t.getCompound("dead");
        for (String k : dd.getAllKeys()) d.dead.put(UUID.fromString(k), dd.getString(k));
        CompoundTag pe = t.getCompound("pending");
        for (String k : pe.getAllKeys()) d.pending.put(UUID.fromString(k), BlockPos.of(pe.getLong(k)));
        ListTag fl = t.getList("flyers", Tag.TAG_STRING);
        for (int i = 0; i < fl.size(); i++) d.flyers.add(UUID.fromString(fl.getString(i)));

        d.missions.clear();
        ListTag ml = t.getList("missions", Tag.TAG_COMPOUND);
        for (int i = 0; i < ml.size(); i++) d.missions.add(Mission.load(ml.getCompound(i)));
        if (d.missions.isEmpty()) d.missions.addAll(Mission.defaults());
        return d;
    }
}
