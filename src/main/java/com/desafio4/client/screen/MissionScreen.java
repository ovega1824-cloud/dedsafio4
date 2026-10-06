package com.desafio4.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

/** Menu de misiones con progreso. Los operadores pueden sumar progreso, completar o quitar. */
public class MissionScreen extends Screen {
    private static final int PER_PAGE = 7;
    private static int page = 0;

    private final ListTag list;
    private final boolean admin;

    public MissionScreen(CompoundTag t) {
        super(Component.literal("Misiones"));
        this.list = t.getList("missions", Tag.TAG_COMPOUND);
        this.admin = t.getBoolean("admin");
    }

    private int top() {
        return Math.max(40, this.height / 2 - 110);
    }

    @Override
    protected void init() {
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        if (page >= pages) page = pages - 1;
        int cx = this.width / 2;
        int top = top();

        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= list.size()) break;
            CompoundTag m = list.getCompound(idx);
            String id = m.getString("id");
            int y = top + i * 28;
            if (admin) {
                addRenderableWidget(Button.builder(Component.literal("+1"), b -> Cmd.send("mission", "plus", id))
                        .bounds(cx + 150, y, 24, 20).build());
                addRenderableWidget(Button.builder(Component.literal("OK"), b -> Cmd.send("mission", "done", id))
                        .bounds(cx + 176, y, 26, 20).build());
                addRenderableWidget(Button.builder(Component.literal("X"), b -> Cmd.send("mission", "remove", id))
                        .bounds(cx + 204, y, 20, 20).build());
            }
        }

        int by = top + PER_PAGE * 28 + 6;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            if (page > 0) page--;
            rebuildWidgets();
        }).bounds(cx - 110, by, 30, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            if (page < pages - 1) page++;
            rebuildWidgets();
        }).bounds(cx + 80, by, 30, 20).build());
        if (admin) {
            addRenderableWidget(Button.builder(Component.literal("Reiniciar"), b -> Cmd.send("mission", "reset", ""))
                    .bounds(cx - 74, by, 70, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(cx + (admin ? 2 : -34), by, 70, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int cx = this.width / 2;
        int top = top();
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        g.drawCenteredString(this.font, "MISIONES  (" + (page + 1) + "/" + pages + ")", cx, top - 24, 0xFFD84A);

        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= list.size()) break;
            CompoundTag m = list.getCompound(idx);
            int y = top + i * 28;
            boolean done = m.getBoolean("done");
            int amount = Math.max(1, m.getInt("amount"));
            int prog = m.getInt("progress");

            g.fill(cx - 230, y - 2, cx + (admin ? 146 : 230), y + 24, done ? 0x8828A028 : 0x88202020);
            String text = m.getString("text");
            int maxW = (admin ? 360 : 440);
            if (this.font.width(text) > maxW) text = this.font.plainSubstrByWidth(text, maxW - 10) + "...";
            g.drawString(this.font, (done ? "✔ " : "") + text, cx - 226, y + 1, done ? 0x77FF77 : 0xFFFFFF);

            int barX = cx - 226, barW = admin ? 300 : 380;
            g.fill(barX, y + 13, barX + barW, y + 20, 0xFF101010);
            g.fill(barX, y + 13, barX + (int) (barW * (prog / (float) amount)), y + 20, done ? 0xFF3CC03C : 0xFFE0B020);
            g.drawString(this.font, prog + "/" + amount, barX + barW + 6, y + 12, 0xCCCCCC);
        }
        if (list.isEmpty()) {
            g.drawCenteredString(this.font, "No hay misiones", cx, top + 20, 0xAAAAAA);
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
