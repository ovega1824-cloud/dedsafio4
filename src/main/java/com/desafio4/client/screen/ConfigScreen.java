package com.desafio4.client.screen;

import com.desafio4.data.D4Data;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Configuracion: vidas, limite de corazones, dificultad y multiplicadores. */
public class ConfigScreen extends Screen {
    private final CompoundTag t;
    private final List<String[]> labels = new ArrayList<>(); // x, y, texto

    public ConfigScreen(CompoundTag t) {
        super(Component.literal("Configuración de Desafío 4"));
        this.t = t;
    }

    private void stepper(int x, int y, String key, int step, String label, String value) {
        addRenderableWidget(Button.builder(Component.literal("-"), b -> Cmd.send("cfg", key, String.valueOf(-step)))
                .bounds(x, y, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> Cmd.send("cfg", key, String.valueOf(step)))
                .bounds(x + 214, y, 20, 20).build());
        labels.add(new String[]{String.valueOf(x + 117), String.valueOf(y + 6), label + ": " + value});
    }

    private void toggle(int x, int y, String key, String label, boolean on) {
        addRenderableWidget(Button.builder(Component.literal(label + ": " + (on ? "SÍ" : "NO")), b -> Cmd.send("cfg", key, "0"))
                .bounds(x, y, 234, 20).build());
    }

    private static String f(float v) {
        return String.format(Locale.ROOT, "x%.2f", v);
    }

    @Override
    protected void init() {
        labels.clear();
        int left = this.width / 2 - 244;
        int right = this.width / 2 + 10;
        int top = Math.max(34, this.height / 2 - 105);

        stepper(left, top, "start", 1, "Vidas iniciales", String.valueOf(t.getInt("start")));
        stepper(left, top + 24, "max", 1, "Vidas máximas", String.valueOf(t.getInt("max")));
        stepper(left, top + 48, "craft", 1, "Corazones de resurrección crafteables",
                t.getInt("crafted") + "/" + t.getInt("craft"));
        toggle(left, top + 72, "pvp", "Corazón dorado al morir en PvP", t.getBoolean("pvp"));
        toggle(left, top + 96, "regen", "Regeneración natural", t.getBoolean("regen"));

        stepper(right, top, "dmg", 1, "Daño de mobs", f(t.getFloat("dmg")));
        stepper(right, top + 24, "hp", 1, "Vida de mobs", f(t.getFloat("hp")));
        stepper(right, top + 48, "speed", 1, "Velocidad de mobs", f(t.getFloat("speed")));
        stepper(right, top + 72, "hunger", 1, "Hambre", f(t.getFloat("hunger")));

        int bw = 94;
        int px = this.width / 2 - (5 * bw + 4 * 4) / 2;
        for (int i = 0; i < D4Data.PRESET_NAMES.length; i++) {
            final int idx = i;
            String name = D4Data.PRESET_NAMES[i];
            if (i == t.getInt("difficulty")) name = "» " + name + " «";
            addRenderableWidget(Button.builder(Component.literal(name), b -> Cmd.send("cfg", "preset", String.valueOf(idx)))
                    .bounds(px + i * (bw + 4), top + 146, bw, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Volver al panel"), b -> Cmd.send("open", "panel"))
                .bounds(this.width / 2 - 105, top + 176, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(this.width / 2 + 5, top + 176, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int top = Math.max(34, this.height / 2 - 105);
        g.drawCenteredString(this.font, this.title, this.width / 2, top - 22, 0xFFD84A);
        g.drawCenteredString(this.font, "Niveles de dificultad (cambian daño, vida, velocidad, hambre y regeneración)",
                this.width / 2, top + 132, 0xAAAAAA);
        if (!t.getBoolean("admin")) {
            g.drawCenteredString(this.font, "Solo los operadores pueden cambiar estos valores", this.width / 2, top - 10, 0xFF5555);
        }
        super.render(g, mx, my, pt);
        for (String[] l : labels) {
            g.drawCenteredString(this.font, l[2], Integer.parseInt(l[0]), Integer.parseInt(l[1]), 0xFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
