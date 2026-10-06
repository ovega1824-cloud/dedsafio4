package com.desafio4.net;

import com.desafio4.client.ClientHooks;
import com.desafio4.server.Game;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class Packets {

    /** Servidor -> cliente: dispara un efecto visual/sonoro. */
    public static class Fx {
        public final String type;
        public final String data;

        public Fx(String type, String data) {
            this.type = type;
            this.data = data;
        }

        public static void encode(Fx m, FriendlyByteBuf b) {
            b.writeUtf(m.type);
            b.writeUtf(m.data);
        }

        public static Fx decode(FriendlyByteBuf b) {
            return new Fx(b.readUtf(), b.readUtf());
        }

        public static void handle(Fx m, Supplier<NetworkEvent.Context> c) {
            NetworkEvent.Context ctx = c.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.fx(m.type, m.data)));
            ctx.setPacketHandled(true);
        }
    }

    /** Servidor -> cliente: estado de vidas para el HUD. */
    public static class State {
        public final int lives, start, max, difficulty;

        public State(int lives, int start, int max, int difficulty) {
            this.lives = lives;
            this.start = start;
            this.max = max;
            this.difficulty = difficulty;
        }

        public static void encode(State m, FriendlyByteBuf b) {
            b.writeInt(m.lives);
            b.writeInt(m.start);
            b.writeInt(m.max);
            b.writeInt(m.difficulty);
        }

        public static State decode(FriendlyByteBuf b) {
            return new State(b.readInt(), b.readInt(), b.readInt(), b.readInt());
        }

        public static void handle(State m, Supplier<NetworkEvent.Context> c) {
            NetworkEvent.Context ctx = c.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.state(m.lives, m.start, m.max, m.difficulty)));
            ctx.setPacketHandled(true);
        }
    }

    /** Servidor -> cliente: abre una pantalla de menu. */
    public static class Open {
        public final String screen;
        public final CompoundTag data;

        public Open(String screen, CompoundTag data) {
            this.screen = screen;
            this.data = data;
        }

        public static void encode(Open m, FriendlyByteBuf b) {
            b.writeUtf(m.screen);
            b.writeNbt(m.data);
        }

        public static Open decode(FriendlyByteBuf b) {
            String s = b.readUtf();
            CompoundTag t = b.readNbt();
            return new Open(s, t == null ? new CompoundTag() : t);
        }

        public static void handle(Open m, Supplier<NetworkEvent.Context> c) {
            NetworkEvent.Context ctx = c.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.open(m.screen, m.data)));
            ctx.setPacketHandled(true);
        }
    }

    /** Cliente -> servidor: accion pedida desde un menu o una tecla. Campos separados por \u0001. */
    public static class Act {
        public final String cmd;

        public Act(String cmd) {
            this.cmd = cmd;
        }

        public static void encode(Act m, FriendlyByteBuf b) {
            b.writeUtf(m.cmd);
        }

        public static Act decode(FriendlyByteBuf b) {
            return new Act(b.readUtf());
        }

        public static void handle(Act m, Supplier<NetworkEvent.Context> c) {
            NetworkEvent.Context ctx = c.get();
            ctx.enqueueWork(() -> {
                ServerPlayer p = ctx.getSender();
                if (p != null) Game.handleAct(p, m.cmd);
            });
            ctx.setPacketHandled(true);
        }
    }
}
