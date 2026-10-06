package com.desafio4.data;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public class Mission {
    public String id;
    public String text;
    /** KILL, MINE, CRAFT, DIM, TIME, MANUAL */
    public String type;
    public String target;
    public int amount;
    public int progress;
    public boolean done;

    public Mission(String id, String text, String type, String target, int amount) {
        this.id = id;
        this.text = text;
        this.type = type;
        this.target = target;
        this.amount = amount;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("id", id);
        t.putString("text", text);
        t.putString("type", type);
        t.putString("target", target);
        t.putInt("amount", amount);
        t.putInt("progress", progress);
        t.putBoolean("done", done);
        return t;
    }

    public static Mission load(CompoundTag t) {
        Mission m = new Mission(t.getString("id"), t.getString("text"), t.getString("type"),
                t.getString("target"), t.getInt("amount"));
        m.progress = t.getInt("progress");
        m.done = t.getBoolean("done");
        return m;
    }

    public static List<Mission> defaults() {
        List<Mission> l = new ArrayList<>();
        l.add(new Mission("m1", "Minero de diamantes: mina 10 diamantes", "MINE", "diamond_ore", 10));
        l.add(new Mission("m2", "Cazador de zombis: mata 25 zombis", "KILL", "zombie", 25));
        l.add(new Mission("m3", "Huesos rotos: mata 25 esqueletos", "KILL", "skeleton", 25));
        l.add(new Mission("m4", "Sin chispas: mata 15 creepers", "KILL", "creeper", 15));
        l.add(new Mission("m5", "Armadura de diamante: craftea un peto de diamante", "CRAFT", "diamond_chestplate", 1));
        l.add(new Mission("m6", "Viaje al infierno: entra al Nether", "DIM", "the_nether", 1));
        l.add(new Mission("m7", "Superviviente: aguanta 30 minutos", "TIME", "", 30));
        l.add(new Mission("m8", "Primera sangre: gana un combate PvP", "KILL", "player", 1));
        l.add(new Mission("m9", "Derrota al Wither", "KILL", "wither", 1));
        l.add(new Mission("m10", "Derrota al Dragón del End", "KILL", "ender_dragon", 1));
        return l;
    }
}
