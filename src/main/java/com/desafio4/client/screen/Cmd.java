package com.desafio4.client.screen;

import com.desafio4.net.Net;
import com.desafio4.net.Packets;

public class Cmd {
    public static void send(String... parts) {
        Net.CH.sendToServer(new Packets.Act(String.join("\u0001", parts)));
    }
}
