package com.desafio4.client;

import com.desafio4.Desafio4;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Desafio4.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    public static final KeyMapping KEY_PANEL = new KeyMapping("key.desafio4.panel", InputConstants.KEY_K, "key.categories.desafio4");
    public static final KeyMapping KEY_MISSIONS = new KeyMapping("key.desafio4.missions", InputConstants.KEY_J, "key.categories.desafio4");

    @SubscribeEvent
    public static void onKeys(RegisterKeyMappingsEvent e) {
        e.register(KEY_PANEL);
        e.register(KEY_MISSIONS);
    }

    @SubscribeEvent
    public static void onOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("d4_hud", (gui, g, partialTick, w, h) -> FxRenderer.renderHud(g, w, h));
        e.registerAboveAll("d4_fx", (gui, g, partialTick, w, h) -> FxRenderer.render(g, w, h));
    }
}
