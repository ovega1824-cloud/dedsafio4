package com.desafio4.client.screen;

import com.desafio4.Desafio4;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.gui.PlayerFaceRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Lista de jugadores muertos con su cara y un boton de angel para revivirlos. */
public class ReviveScreen extends Screen {
    private static final ResourceLocation ANGEL = new ResourceLocation(Desafio4.MODID, "textures/gui/angel.png");
    private final ListTag dead;
    private final String pos;

    public ReviveScreen(CompoundTag t) {
        super(Component.literal("Jugadores caídos"));
        this.dead = t.getList("dead", Tag.TAG_COMPOUND);
        this.pos = t.getBoolean("hasPos") ? t.getInt("x") + "," + t.getInt("y") + "," + t.getInt("z") : "";
    }

    private int top() {
        return Math.max(46, this.height / 2 - Math.min(dead.size(), 6) * 15 - 20);
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int top = top();
        int shown = Math.min(dead.size(), 6);
        for (int i = 0; i < shown; i++) {
            String uuid = dead.getCompound(i).getString("uuid");
            addRenderableWidget(Button.builder(Component.literal("     Revivir"), b -> {
                Cmd.send("revive", uuid, pos);
                onClose();
            }).bounds(cx + 40, top + i * 30, 90, 22).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(cx - 50, top + shown * 30 + 8, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int cx = this.width / 2;
        int top = top();
        g.drawCenteredString(this.font, "Elige a quién revivir", cx, top - 24, 0x7DFFB0);
        g.drawCenteredString(this.font, pos.isEmpty() ? "Reaparecerá a tu lado" : "Se elevará desde la fogata", cx, top - 12, 0xAAAAAA);
        super.render(g, mx, my, pt);

        Minecraft mc = Minecraft.getInstance();
        int shown = Math.min(dead.size(), 6);
        for (int i = 0; i < shown; i++) {
            CompoundTag e = dead.getCompound(i);
            int y = top + i * 30;
            ResourceLocation skin;
            PlayerInfo info = null;
            UUID id = null;
            try {
                id = UUID.fromString(e.getString("uuid"));
                if (mc.getConnection() != null) info = mc.getConnection().getPlayerInfo(id);
            } catch (Exception ignored) {
            }
            skin = info != null ? info.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(id != null ? id : new UUID(0, 0));
            g.fill(cx - 130, y - 2, cx + 134, y + 24, 0x88101010);
            PlayerFaceRenderer.draw(g, skin, cx - 124, y, 22);
            g.drawString(this.font, e.getString("name"), cx - 96, y + 7, 0xFFFFFF);
            g.blit(ANGEL, cx + 44, y + 3, 16, 16, 0f, 0f, 16, 16, 16, 16);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
