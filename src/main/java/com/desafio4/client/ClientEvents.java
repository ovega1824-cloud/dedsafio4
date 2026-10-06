package com.desafio4.client;

import com.desafio4.Desafio4;
import com.desafio4.net.Net;
import com.desafio4.net.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Desafio4.MODID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        while (ClientModEvents.KEY_PANEL.consumeClick()) {
            Net.CH.sendToServer(new Packets.Act("open\u0001panel"));
        }
        while (ClientModEvents.KEY_MISSIONS.consumeClick()) {
            Net.CH.sendToServer(new Packets.Act("open\u0001mission"));
        }
    }

    /** Niebla: roja con humo, verde con acido, negra al revivir. */
    @SubscribeEvent
    public static void onFog(ViewportEvent.ComputeFogColor e) {
        float smoke = FxRenderer.smokeAmount();
        float acid = FxRenderer.acidAmount();
        float dark = FxRenderer.reviveDarkness();
        if (smoke > 0f) {
            e.setRed(lerp(e.getRed(), 0.75f, smoke));
            e.setGreen(lerp(e.getGreen(), 0.04f, smoke));
            e.setBlue(lerp(e.getBlue(), 0.04f, smoke));
        }
        if (acid > 0f) {
            e.setRed(lerp(e.getRed(), 0.25f, acid * 0.6f));
            e.setGreen(lerp(e.getGreen(), 0.75f, acid * 0.6f));
            e.setBlue(lerp(e.getBlue(), 0.12f, acid * 0.6f));
        }
        if (dark > 0f) {
            e.setRed(lerp(e.getRed(), 0f, dark));
            e.setGreen(lerp(e.getGreen(), 0f, dark));
            e.setBlue(lerp(e.getBlue(), 0.03f, dark));
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * Cualquier mensaje del sistema (por ejemplo /tellraw) cuyo texto sea rojo y en negrita
     * se muestra dentro de un rectangulo en el centro de la pantalla.
     */
    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent e) {
        Component msg = e.getMessage();
        if (msg == null) return;
        Style s = msg.getStyle();
        if (s.isBold() && s.getColor() != null && s.getColor().getValue() == 0xFF5555) {
            FxRenderer.BOXES.add(new FxRenderer.Box(msg, System.currentTimeMillis()));
            e.setCanceled(true);
        }
    }
}
