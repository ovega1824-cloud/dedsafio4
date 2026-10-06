package com.desafio4.client.screen;

import com.desafio4.client.FxRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/** Panel principal: todo el mod desde un solo menu. */
public class PanelScreen extends Screen {
    private final boolean admin;

    public PanelScreen(CompoundTag t) {
        super(Component.literal("Panel Desafío 4"));
        this.admin = t.getBoolean("admin");
    }

    private void add(int col, int row, int left, int top, String label, Runnable r) {
        int w = 140, h = 20;
        addRenderableWidget(Button.builder(Component.literal(label), b -> r.run())
                .bounds(left + col * (w + 6), top + row * 24, w, h).build());
    }

    @Override
    protected void init() {
        int left = (this.width - 432) / 2;
        int top = Math.max(30, this.height / 2 - 118);

        add(0, 0, left, top, "Configuración y dificultad", () -> Cmd.send("open", "config"));
        add(1, 0, left, top, "Misiones", () -> Cmd.send("open", "mission"));
        add(2, 0, left, top, "Revivir jugadores", () -> Cmd.send("open", "revive"));

        add(0, 1, left, top, "Ruleta aleatoria", () -> Cmd.send("roulette", "-1"));
        add(1, 1, left, top, "Momento personalizado", () -> Cmd.send("open", "moment"));
        add(2, 1, left, top, "Caja de mensaje", () -> Minecraft.getInstance().setScreen(InputScreen.box()));

        add(0, 2, left, top, "Humo rojo", () -> Cmd.send("smoke"));
        add(1, 2, left, top, "Lluvia ácida", () -> Cmd.send("acid"));
        add(2, 2, left, top, "Eclipse (on/off)", () -> Cmd.send("eclipse"));

        add(0, 3, left, top, "Mi vuelo (on/off)", () -> Cmd.send("fly"));
        add(1, 3, left, top, "Capa: subir", () -> Cmd.send("open", "layer_up"));
        add(2, 3, left, top, "Capa: bajar", () -> Cmd.send("open", "layer_down"));

        add(0, 4, left, top, "Probar animación muerte", () -> Cmd.send("testdeath"));
        add(1, 4, left, top, "Probar animación revivir", () -> Cmd.send("testrevive"));
        add(2, 4, left, top, "Apagar todos los efectos", () -> Cmd.send("stopall"));

        // fila de colores de la ruleta
        String[] names = {"Rojo", "Naranja", "Amarillo", "Verde", "Celeste", "Azul", "Morado", "Rosa"};
        int bw = 50;
        for (int i = 0; i < 8; i++) {
            final int idx = i;
            addRenderableWidget(Button.builder(
                            Component.literal(names[i]).withStyle(Style.EMPTY.withColor(FxRenderer.ROULETTE_COLORS[i])),
                            b -> Cmd.send("roulette", String.valueOf(idx)))
                    .bounds(left + i * (bw + 4), top + 5 * 24 + 14, bw, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(this.width / 2 - 50, top + 5 * 24 + 42, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int top = Math.max(30, this.height / 2 - 118);
        g.drawCenteredString(this.font, this.title, this.width / 2, top - 22, 0xFFD84A);
        if (!admin) {
            g.drawCenteredString(this.font, "Solo los operadores pueden usar este panel", this.width / 2, top - 10, 0xFF5555);
        }
        g.drawCenteredString(this.font, "Ruleta por color", this.width / 2, top + 5 * 24 + 3, 0xCCCCCC);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
