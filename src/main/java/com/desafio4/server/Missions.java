package com.desafio4.server;

import com.desafio4.data.D4Data;
import com.desafio4.data.Mission;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public class Missions {

    public static void progress(MinecraftServer s, String type, String key, int n) {
        D4Data d = D4Data.get(s);
        for (Mission m : d.missions) {
            if (m.done || !m.type.equals(type)) continue;
            boolean match = switch (type) {
                case "MINE" -> key.contains(m.target);
                case "TIME" -> true;
                default -> key.equals(m.target);
            };
            if (match) add(s, d, m, n);
        }
    }

    public static void add(MinecraftServer s, D4Data d, Mission m, int n) {
        if (m.done) return;
        m.progress = Math.min(m.amount, m.progress + n);
        d.setDirty();
        if (m.progress >= m.amount) complete(s, d, m);
    }

    public static void complete(MinecraftServer s, D4Data d, Mission m) {
        m.done = true;
        m.progress = m.amount;
        d.setDirty();
        Game.broadcast(s, Component.literal("★ Misión completada: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(m.text).withStyle(ChatFormatting.YELLOW)));
        Game.moment(s, "epico", "MISIÓN COMPLETADA", m.text);
    }
}
