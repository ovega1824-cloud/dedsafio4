package com.desafio4.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Pantalla generica con cajas de texto y botones que mandan una accion al servidor. */
public class InputScreen extends Screen {
    public record Btn(String label, String[] template) {
    }

    private final String[] fieldLabels;
    private final String[] defaults;
    private final List<Btn> buttons;
    private final String info;
    private final List<EditBox> boxes = new ArrayList<>();

    public InputScreen(String title, String info, String[] fieldLabels, String[] defaults, List<Btn> buttons) {
        super(Component.literal(title));
        this.info = info;
        this.fieldLabels = fieldLabels;
        this.defaults = defaults;
        this.buttons = buttons;
    }

    @Override
    protected void init() {
        boxes.clear();
        int cx = this.width / 2;
        int top = this.height / 2 - 50;
        for (int i = 0; i < fieldLabels.length; i++) {
            EditBox b = new EditBox(this.font, cx - 110, top + i * 36 + 12, 220, 20, Component.literal(fieldLabels[i]));
            b.setMaxLength(120);
            b.setValue(defaults[i]);
            addRenderableWidget(b);
            boxes.add(b);
        }
        if (!boxes.isEmpty()) setInitialFocus(boxes.get(0));

        int by = top + fieldLabels.length * 36 + 18;
        int n = buttons.size();
        int bw = Math.min(110, 440 / Math.max(1, n));
        int bx = cx - (n * bw + (n - 1) * 4) / 2;
        for (int i = 0; i < n; i++) {
            Btn btn = buttons.get(i);
            addRenderableWidget(Button.builder(Component.literal(btn.label()), x -> {
                if (btn.template() == null) {
                    onClose();
                    return;
                }
                String[] parts = new String[btn.template().length];
                for (int k = 0; k < parts.length; k++) {
                    String p = btn.template()[k];
                    for (int f = 0; f < boxes.size(); f++) p = p.replace("{" + f + "}", boxes.get(f).getValue());
                    parts[k] = p;
                }
                Cmd.send(parts);
            }).bounds(bx + i * (bw + 4), by, bw, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int cx = this.width / 2;
        int top = this.height / 2 - 50;
        g.drawCenteredString(this.font, this.title, cx, top - 28, 0xFFD84A);
        if (!info.isEmpty()) g.drawCenteredString(this.font, info, cx, top - 14, 0xAAAAAA);
        for (int i = 0; i < fieldLabels.length; i++) {
            g.drawString(this.font, fieldLabels[i], cx - 110, top + i * 36, 0xFFFFFF);
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------ fabricas

    public static InputScreen layer(boolean up, CompoundTag t) {
        String side = up ? "up" : "down";
        List<Btn> b = new ArrayList<>();
        b.add(new Btn("Activar", new String[]{"layer", side, "on", "{0}"}));
        b.add(new Btn("Desactivar", new String[]{"layer", side, "off", "{0}"}));
        b.add(new Btn("Cerrar", null));
        String info = up ? "Si estás por debajo de esa capa pierdes vida poco a poco"
                : "Si estás por encima de esa capa el gas rojo te hace daño";
        info += t.getBoolean("on") ? " (ACTIVA)" : " (apagada)";
        return new InputScreen(up ? "Capa para SUBIR" : "Capa para BAJAR", info,
                new String[]{"Altura de la capa (Y)"}, new String[]{String.valueOf(t.getInt("y"))}, b);
    }

    public static InputScreen craftLimit(CompoundTag t) {
        List<Btn> b = new ArrayList<>();
        b.add(new Btn("Guardar", new String[]{"cfg", "craftset", "{0}"}));
        b.add(new Btn("Cerrar", null));
        return new InputScreen("Corazones de la resurrección",
                "Ya crafteados: " + t.getInt("crafted") + "   (elige un límite de 1 a 10)",
                new String[]{"Cuántos se pueden craftear"}, new String[]{String.valueOf(t.getInt("craft"))}, b);
    }

    public static InputScreen moment() {
        List<Btn> b = new ArrayList<>();
        b.add(new Btn("Revelación", new String[]{"moment", "revelacion", "{0}", "{1}"}));
        b.add(new Btn("Épico", new String[]{"moment", "epico", "{0}", "{1}"}));
        b.add(new Btn("Terror", new String[]{"moment", "terror", "{0}", "{1}"}));
        b.add(new Btn("Victoria", new String[]{"moment", "victoria", "{0}", "{1}"}));
        b.add(new Btn("Cerrar", null));
        return new InputScreen("Momento", "Escribe el texto y elige el tipo de momento",
                new String[]{"Título", "Subtítulo"}, new String[]{"EL MOMENTO", ""}, b);
    }

    public static InputScreen box() {
        List<Btn> b = new ArrayList<>();
        b.add(new Btn("Mostrar en rectángulo", new String[]{"box", "{0}"}));
        b.add(new Btn("Cerrar", null));
        return new InputScreen("Mensaje en rectángulo", "Aparece en rojo y negrita para todos los jugadores",
                new String[]{"Mensaje"}, new String[]{""}, b);
    }
}
