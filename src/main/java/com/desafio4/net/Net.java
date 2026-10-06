package com.desafio4.net;

import com.desafio4.Desafio4;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class Net {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CH = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Desafio4.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void init() {
        int i = 0;
        CH.registerMessage(i++, Packets.Fx.class, Packets.Fx::encode, Packets.Fx::decode, Packets.Fx::handle);
        CH.registerMessage(i++, Packets.State.class, Packets.State::encode, Packets.State::decode, Packets.State::handle);
        CH.registerMessage(i++, Packets.Open.class, Packets.Open::encode, Packets.Open::decode, Packets.Open::handle);
        CH.registerMessage(i++, Packets.Act.class, Packets.Act::encode, Packets.Act::decode, Packets.Act::handle);
    }

    public static void toAll(Object msg) {
        CH.send(PacketDistributor.ALL.noArg(), msg);
    }

    public static void to(ServerPlayer p, Object msg) {
        CH.send(PacketDistributor.PLAYER.with(() -> p), msg);
    }
}
