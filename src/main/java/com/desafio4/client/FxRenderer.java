package com.desafio4.client;

import com.desafio4.Desafio4;
import com.desafio4.data.D4Data;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Dibuja todos los efectos en pantalla: ruleta, muerte, revivir, humo, acido, eclipse, momentos... */
public class FxRenderer {
    // ------------------------------------------------------------ datos de cada efecto
    public static class Roulette {
        final int chosen;
        final long start;
        int lastSeg = -1;
        boolean belled = false;

        public Roulette(int chosen, long start) {
            this.chosen = chosen;
            this.start = start;
        }
    }

    public static class Death {
        final String name;
        final Component cause;
        final boolean self;
        final long start;

        public Death(String name, Component cause, boolean self, long start) {
            this.name = name;
            this.cause = cause;
            this.self = self;
            this.start = start;
        }
    }

    public static class Revive {
        final String name;
        final long start;
        boolean ended = false;

        public Revive(String name, long start) {
            this.name = name;
            this.start = start;
        }
    }

    public static class Smoke {
        final long start, dur;

        public Smoke(long start, long dur) {
            this.start = start;
            this.dur = dur;
        }
    }

    public static class Acid {
        final long start, dur;

        public Acid(long start, long dur) {
            this.start = start;
            this.dur = dur;
        }
    }

    public static class Moment {
        final String type, title, sub;
        final long start;

        public Moment(String type, String title, String sub, long start) {
            this.type = type;
            this.title = title;
            this.sub = sub;
            this.start = start;
        }
    }

    public static class Box {
        final Component msg;
        final long start;

        public Box(Component msg, long start) {
            this.msg = msg;
            this.start = start;
        }
    }

    public static class Gold {
        final long start;

        public Gold(long start) {
            this.start = start;
        }
    }

    public static final List<Roulette> ROULETTES = new ArrayList<>();
    public static final List<Death> DEATHS = new ArrayList<>();
    public static final List<Revive> REVIVES = new ArrayList<>();
    public static final List<Smoke> SMOKES = new ArrayList<>();
    public static final List<Acid> ACIDS = new ArrayList<>();
    public static final List<Moment> MOMENTS = new ArrayList<>();
    public static final List<Box> BOXES = new ArrayList<>();
    public static final List<Gold> GOLDS = new ArrayList<>();

    public static void clearAll() {
        ROULETTES.clear();
        DEATHS.clear();
        REVIVES.clear();
        SMOKES.clear();
        ACIDS.clear();
        MOMENTS.clear();
        BOXES.clear();
        GOLDS.clear();
    }

    // ------------------------------------------------------------ constantes
    private static final ResourceLocation SKULL = new ResourceLocation(Desafio4.MODID, "textures/gui/skull.png");
    private static final ResourceLocation HEART_Y = new ResourceLocation(Desafio4.MODID, "textures/gui/heart_yellow.png");
    private static final ResourceLocation HEART_G = new ResourceLocation(Desafio4.MODID, "textures/gui/heart_gray.png");
    private static final ResourceLocation GOLD_HEART = new ResourceLocation(Desafio4.MODID, "textures/item/gold_heart.png");

    public static final int[] ROULETTE_COLORS = {0xFF2B2B, 0xFF8A1F, 0xFFE02B, 0x2BD94A, 0x2BCFFF, 0x2B5BFF, 0xA52BFF, 0xFF5FB8};
    public static final String[] ROULETTE_NAMES = {"ROJO", "NARANJA", "AMARILLO", "VERDE", "CELESTE", "AZUL", "MORADO", "ROSA"};

    // ------------------------------------------------------------ ayudas
    static float c01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    static int col(float a, int rgb) {
        int ai = (int) (c01(a) * 255f);
        return (ai << 24) | (rgb & 0xFFFFFF);
    }

    /** Color para texto (el alfa minimo evita que Minecraft lo vuelva opaco). */
    static int tcol(float a, int rgb) {
        int ai = Math.max(6, (int) (c01(a) * 255f));
        return (ai << 24) | (rgb & 0xFFFFFF);
    }

    static float hash(int i) {
        return new Random(i * 7919L + 13).nextFloat();
    }

    static void fan(GuiGraphics g, float cx, float cy, float r, float a0, float a1, int rgb, float alpha) {
        if (alpha <= 0.01f) return;
        Matrix4f m = g.pose().last().pose();
        float rr = ((rgb >> 16) & 255) / 255f, gg = ((rgb >> 8) & 255) / 255f, bb = (rgb & 255) / 255f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        b.vertex(m, cx, cy, 0).color(rr, gg, bb, alpha).endVertex();
        int steps = Math.max(2, (int) ((a1 - a0) / 4f) + 1);
        for (int i = 0; i <= steps; i++) {
            double a = Math.toRadians(a0 + (a1 - a0) * i / steps - 90.0);
            b.vertex(m, (float) (cx + Math.cos(a) * r), (float) (cy + Math.sin(a) * r), 0).color(rr, gg, bb, alpha).endVertex();
        }
        BufferUploader.drawWithShader(b.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    static void circle(GuiGraphics g, float cx, float cy, float r, int rgb, float alpha) {
        fan(g, cx, cy, r, 0, 360, rgb, alpha);
    }

    static void triangle(GuiGraphics g, float x1, float y1, float x2, float y2, float x3, float y3, int rgb, float alpha) {
        Matrix4f m = g.pose().last().pose();
        float rr = ((rgb >> 16) & 255) / 255f, gg = ((rgb >> 8) & 255) / 255f, bb = (rgb & 255) / 255f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        b.vertex(m, x1, y1, 0).color(rr, gg, bb, alpha).endVertex();
        b.vertex(m, x2, y2, 0).color(rr, gg, bb, alpha).endVertex();
        b.vertex(m, x3, y3, 0).color(rr, gg, bb, alpha).endVertex();
        BufferUploader.drawWithShader(b.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    static void textCentered(GuiGraphics g, Font f, Component text, float cx, float y, float scale, int color, boolean shadow) {
        g.pose().pushPose();
        g.pose().translate(cx, y, 0);
        g.pose().scale(scale, scale, 1f);
        FormattedCharSequence seq = text.getVisualOrderText();
        g.drawString(f, seq, -f.width(seq) / 2, 0, color, shadow);
        g.pose().popPose();
    }

    // ------------------------------------------------------------ HUD (corazones)
    public static void renderHud(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        int slots = Math.max(1, Math.max(ClientState.start, ClientState.lives));
        int size = 16, gap = 2;
        int total = slots * (size + gap) - gap;
        int x0 = w / 2 - total / 2;
        int y = 4;
        RenderSystem.enableBlend();
        for (int i = 0; i < slots; i++) {
            ResourceLocation tex = i < ClientState.lives ? HEART_Y : HEART_G;
            g.blit(tex, x0 + i * (size + gap), y, size, size, 0f, 0f, 32, 32, 32, 32);
        }
        String dn = D4Data.PRESET_NAMES[Math.max(0, Math.min(4, ClientState.difficulty))];
        g.pose().pushPose();
        g.pose().translate(w / 2f, y + size + 3, 0);
        g.pose().scale(0.75f, 0.75f, 1f);
        g.drawCenteredString(mc.font, "Dificultad: " + dn, 0, 0, 0xCCCCCC);
        g.pose().popPose();
        RenderSystem.disableBlend();
    }

    // ------------------------------------------------------------ render principal de efectos
    public static void render(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        long now = System.currentTimeMillis();

        SMOKES.removeIf(s -> now - s.start > s.dur);
        for (Smoke s : SMOKES) drawSmoke(g, w, h, s, now);

        ACIDS.removeIf(a -> now - a.start > a.dur);
        for (Acid a : ACIDS) drawAcid(g, w, h, a, now);

        if (ClientState.eclipse) drawEclipse(g, w, h, now, mc);

        REVIVES.removeIf(r -> now - r.start > 9000);
        for (Revive r : REVIVES) drawRevive(g, w, h, r, now, mc);

        ROULETTES.removeIf(r -> now - r.start > 7800);
        for (Roulette r : ROULETTES) drawRoulette(g, w, h, r, now, mc);

        DEATHS.removeIf(d -> now - d.start > 4200);
        for (Death d : DEATHS) drawDeath(g, w, h, d, now, mc);

        MOMENTS.removeIf(m -> now - m.start > 6500);
        for (Moment m : MOMENTS) drawMoment(g, w, h, m, now, mc);

        GOLDS.removeIf(o -> now - o.start > 2600);
        for (Gold o : GOLDS) drawGold(g, w, h, o, now);

        BOXES.removeIf(b -> now - b.start > 6000);
        for (Box b : BOXES) drawBox(g, w, h, b, now, mc);
    }

    /** Intensidad actual del humo rojo (0..1), usada tambien para el color de la niebla. */
    public static float smokeAmount() {
        long now = System.currentTimeMillis();
        float best = 0;
        for (Smoke s : SMOKES) {
            float t = (now - s.start) / 1000f, dur = s.dur / 1000f;
            best = Math.max(best, c01(t / 3f) * c01((dur - t) / 2f));
        }
        return best;
    }

    public static float acidAmount() {
        long now = System.currentTimeMillis();
        float best = 0;
        for (Acid a : ACIDS) {
            float t = (now - a.start) / 1000f, dur = a.dur / 1000f;
            best = Math.max(best, c01(t / 2f) * c01((dur - t) / 2f));
        }
        return best;
    }

    public static float reviveDarkness() {
        long now = System.currentTimeMillis();
        float best = 0;
        for (Revive r : REVIVES) {
            float t = (now - r.start) / 1000f;
            best = Math.max(best, Math.min(c01(t / 0.8f), c01((9f - t) / 1.5f)));
        }
        return best;
    }

    // ------------------------------------------------------------ humo rojo
    private static void drawSmoke(GuiGraphics g, int w, int h, Smoke s, long now) {
        float t = (now - s.start) / 1000f, dur = s.dur / 1000f;
        float sweep = c01(t / 3f);
        float fade = c01((dur - t) / 2f);
        float flick = 0.85f + 0.15f * (float) Math.sin(t * 9f);
        int y = (int) (h * sweep);
        if (y > 0) {
            g.fillGradient(0, 0, w, y, col(0.55f * fade, 0xB00000), col(0.30f * fade * flick, 0xFF2020));
        }
        if (sweep < 1f) {
            g.fill(0, y - 2, w, y + 2, col(0.8f * fade, 0xFF6060));
        }
    }

    // ------------------------------------------------------------ lluvia acida
    private static void drawAcid(GuiGraphics g, int w, int h, Acid a, long now) {
        float amt = acidAmount();
        g.fill(0, 0, w, h, col(0.10f * amt, 0x40FF40));
        for (int i = 0; i < 70; i++) {
            float x = hash(i) * w;
            float y = ((now / 4f) + hash(i + 100) * h * 2f) % h;
            g.fill((int) x, (int) y, (int) x + 1, (int) y + 10, col(0.45f * amt, 0x60FF70));
        }
    }

    // ------------------------------------------------------------ eclipse (circulo negro)
    private static void drawEclipse(GuiGraphics g, int w, int h, long now, Minecraft mc) {
        float pitch = mc.player.getXRot();
        float amt = c01((-pitch - 15f) / 45f);
        if (amt <= 0) return;
        float cx = w / 2f, cy = h * 0.35f, r = h * 0.14f;
        g.fill(0, 0, w, h, col(0.35f * amt, 0x000000));
        float pulse = 1f + 0.05f * (float) Math.sin(now / 250.0);
        for (int i = 6; i >= 1; i--) {
            circle(g, cx, cy, r * (1f + i * 0.12f) * pulse, i % 2 == 0 ? 0xFF4A10 : 0xFF9A30, 0.07f * amt * (7 - i) / 6f);
        }
        circle(g, cx, cy, r, 0x000000, 1f * amt);
    }

    // ------------------------------------------------------------ revivir (cielo negro + aurora)
    private static void drawRevive(GuiGraphics g, int w, int h, Revive r, long now, Minecraft mc) {
        float t = (now - r.start) / 1000f;
        float fadeIn = c01(t / 0.8f), fadeOut = c01((9f - t) / 1.5f);
        float a = Math.min(fadeIn, fadeOut);
        g.fillGradient(0, 0, w, h, col(0.93f * a, 0x000008), col(0.80f * a, 0x02051A));

        for (int i = 0; i < 90; i++) {
            int x = (int) (hash(i * 2) * w), y = (int) (hash(i * 2 + 1) * h * 0.75f);
            float tw = 0.4f + 0.6f * (float) Math.sin(t * 3f + i);
            g.fill(x, y, x + 2, y + 2, col(a * c01(tw), 0xFFFFFF));
        }

        int[] cols = {0x2BFF88, 0x2BE0FF, 0x9B4BFF};
        for (int L = 0; L < 3; L++) {
            for (int x = 0; x < w; x += 4) {
                float base = h * (0.16f + L * 0.07f) + (float) Math.sin(x * 0.012f + t * 1.1f + L) * h * 0.05f
                        + (float) Math.sin(x * 0.031f - t * 0.7f) * h * 0.02f;
                float height = h * 0.20f + (float) Math.sin(x * 0.02f + t * 0.9f + L * 2f) * h * 0.06f;
                int top = (int) (base - height), bot = (int) base;
                g.fillGradient(x, top, x + 4, bot, col(0f, cols[L]), col(0.50f * a, cols[L]));
            }
        }

        if (t > 2.2f) {
            float ta = c01((t - 2.2f) / 0.8f) * fadeOut;
            textCentered(g, mc.font, Component.literal(r.name.toUpperCase()).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                    w / 2f, h * 0.60f, 3f, tcol(ta, 0x55FF99), true);
            textCentered(g, mc.font, Component.literal("HA REVIVIDO").withStyle(ChatFormatting.WHITE),
                    w / 2f, h * 0.60f + 40f, 2f, tcol(ta, 0xFFFFFF), true);
        }
        if (t > 8.3f && !r.ended) {
            r.ended = true;
            ClientHooks.play(SoundEvents.PLAYER_LEVELUP, 1f, 0.8f);
        }
    }

    // ------------------------------------------------------------ ruleta
    private static void wheel(GuiGraphics g, float cx, float cy, float R, float rot, int[] colors, float alpha, int tint) {
        for (int i = 0; i < 8; i++) {
            float c = i * 45f + rot;
            int rgb = tint >= 0 ? tint : colors[i];
            fan(g, cx, cy, R, c - 22.5f + 0.8f, c + 22.5f - 0.8f, rgb, alpha);
        }
    }

    private static void drawRoulette(GuiGraphics g, int w, int h, Roulette r, long now, Minecraft mc) {
        float t = (now - r.start) / 1000f;
        float inEnd = 0.6f, spinEnd = 5.0f, holdEnd = 7.2f, total = 7.8f;
        Random rnd = new Random(now / 70);
        float alpha = 1f;
        boolean glitch = false;
        if (t < inEnd) {
            glitch = true;
            alpha = rnd.nextFloat() < 0.55f ? c01(t / inEnd) : 0f;
        } else if (t > holdEnd) {
            glitch = true;
            alpha = rnd.nextFloat() < 0.55f ? c01((total - t) / (total - holdEnd)) : 0f;
        }
        boolean result = t >= spinEnd;
        float rotEnd = 360f * 6 + ((360f - r.chosen * 45f) % 360f);
        float p = c01((t - 0.3f) / (spinEnd - 0.3f));
        float ease = 1f - (float) Math.pow(1f - p, 3);
        float rot = rotEnd * ease;

        int seg = Math.floorMod(Math.round(-rot / 45f), 8);
        if (!result && seg != r.lastSeg) {
            if (r.lastSeg != -1) ClientHooks.play(SoundEvents.COMPARATOR_CLICK, 1.2f + 0.8f * (1f - ease), 0.7f);
            r.lastSeg = seg;
        }
        if (result && !r.belled) {
            r.belled = true;
            ClientHooks.play(SoundEvents.BELL_BLOCK, 1f, 1f);
            ClientHooks.play(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2f, 0.6f);
        }

        float R = Math.min(w, h) * 0.28f;
        float cx = w / 2f, cy = h / 2f;
        g.fill(0, 0, w, h, col(0.35f * alpha, 0x000000));

        int[] colors = ROULETTE_COLORS.clone();
        if (result) {
            for (int i = 0; i < 8; i++) colors[i] = ROULETTE_COLORS[r.chosen];
        }
        float flash = result ? c01(1f - (t - spinEnd) / 0.4f) : 0f;

        if (glitch && alpha > 0f) {
            float off = 6f + rnd.nextFloat() * 8f;
            g.pose().pushPose();
            g.pose().translate(-off, 0, 0);
            wheel(g, cx, cy, R, rot, colors, alpha * 0.5f, 0xFF0033);
            g.pose().popPose();
            g.pose().pushPose();
            g.pose().translate(off, 0, 0);
            wheel(g, cx, cy, R, rot, colors, alpha * 0.5f, 0x00FFFF);
            g.pose().popPose();
        }

        circle(g, cx, cy, R + 8f, 0x101010, alpha);
        circle(g, cx, cy, R + 4f, 0xF0F0F0, alpha);
        circle(g, cx, cy, R + 2f, 0x101010, alpha);
        wheel(g, cx, cy, R, rot, colors, alpha, -1);
        circle(g, cx, cy, R * 0.18f, 0x151515, alpha);
        circle(g, cx, cy, R * 0.10f, 0xE8E8E8, alpha);
        triangle(g, cx - 12, cy - R - 18, cx + 12, cy - R - 18, cx, cy - R + 6, 0xFFFFFF, alpha);

        if (flash > 0f) circle(g, cx, cy, R + 8f, 0xFFFFFF, 0.7f * flash * alpha);

        if (glitch && alpha > 0f) {
            for (int i = 0; i < 4; i++) {
                int y = (int) (cy - R + rnd.nextFloat() * R * 2f);
                g.fill((int) (cx - R - 20), y, (int) (cx + R + 20), y + 2 + rnd.nextInt(5), col(0.85f * alpha, 0x000000));
            }
        }

        if (result) {
            float ta = c01((t - spinEnd) / 0.3f) * (t > holdEnd ? alpha : 1f);
            textCentered(g, mc.font, Component.literal(ROULETTE_NAMES[r.chosen]).withStyle(ChatFormatting.BOLD),
                    cx, cy - R - 54f, 3f, tcol(ta, ROULETTE_COLORS[r.chosen]), true);
        }
    }

    // ------------------------------------------------------------ muerte (calavera + UNA VIDA PERDIDA)
    private static void drawDeath(GuiGraphics g, int w, int h, Death d, long now, Minecraft mc) {
        float t = (now - d.start) / 1000f, total = 4.2f;
        Random rnd = new Random(now / 70);
        boolean glitch = false;
        float alpha = 1f;
        if (t < 0.5f) {
            glitch = true;
            alpha = rnd.nextFloat() < 0.55f ? c01(t / 0.5f) : 0f;
        } else if (t > 3.5f) {
            glitch = true;
            alpha = rnd.nextFloat() < 0.55f ? c01((total - t) / 0.7f) : 0f;
        } else if (t > 0.9f && t < 1.25f) {
            glitch = true;
        }
        if (alpha <= 0.01f) return;

        float size = d.self ? h * 0.36f : h * 0.12f;
        float cx = w / 2f;
        float cy = d.self ? h * 0.34f : 44f + size / 2f;
        float rot = (float) Math.sin(t * 2.2f) * 1.4f;
        float dx = (float) Math.sin(t * 1.7f) * size * 0.01f;
        float u = size / 128f;

        RenderSystem.enableBlend();
        if (glitch) {
            for (int k = 0; k < 2; k++) {
                g.pose().pushPose();
                g.pose().translate(cx + dx + (k == 0 ? -5f : 5f) * u * 2f, cy, 0);
                g.pose().mulPose(Axis.ZP.rotationDegrees(rot));
                if (k == 0) RenderSystem.setShaderColor(1f, 0.15f, 0.15f, alpha * 0.5f);
                else RenderSystem.setShaderColor(0.15f, 1f, 1f, alpha * 0.5f);
                g.blit(SKULL, (int) (-size / 2), (int) (-size / 2), (int) size, (int) size, 0f, 0f, 128, 128, 128, 128);
                g.pose().popPose();
            }
        }
        g.pose().pushPose();
        g.pose().translate(cx + dx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(rot));
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        g.blit(SKULL, (int) (-size / 2), (int) (-size / 2), (int) size, (int) size, 0f, 0f, 128, 128, 128, 128);
        g.pose().popPose();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();

        float ts = d.self ? 2.4f : 0.9f;
        float y = cy + size / 2f + (d.self ? 12f : 4f);
        textCentered(g, mc.font, Component.literal(d.name).withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                cx, y, ts, tcol(alpha, 0xFF3030), true);
        y += 10f * ts + 4f;
        float causeScale = d.self ? 1.4f : 0.7f;
        int maxW = (int) ((w * 0.8f) / causeScale);
        for (FormattedCharSequence line : mc.font.split(Component.empty().append(d.cause).withStyle(ChatFormatting.RED), maxW)) {
            g.pose().pushPose();
            g.pose().translate(cx, y, 0);
            g.pose().scale(causeScale, causeScale, 1f);
            g.drawString(mc.font, line, -mc.font.width(line) / 2, 0, tcol(alpha, 0xFF4040), true);
            g.pose().popPose();
            y += 10f * causeScale;
        }

        if (t > 0.9f) {
            float titleScale = d.self ? 4.2f : 1.5f;
            Component title = Component.literal("UNA VIDA PERDIDA").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            float maxScale = (w * 0.92f) / mc.font.width(title);
            titleScale = Math.min(titleScale, maxScale);
            float ty = y + (d.self ? 14f : 6f);
            float jx = glitch ? (rnd.nextFloat() - 0.5f) * 10f * titleScale / 2f : 0f;
            if (glitch) {
                textCentered(g, mc.font, title, cx + jx - 4f, ty, titleScale, tcol(alpha * 0.6f, 0x00FFFF), false);
                textCentered(g, mc.font, title, cx + jx + 4f, ty, titleScale, tcol(alpha * 0.6f, 0xFF0044), false);
            }
            textCentered(g, mc.font, title, cx + jx, ty, titleScale, tcol(alpha, 0xFF1A1A), true);
        }
    }

    // ------------------------------------------------------------ momentos
    private static int momentColor(String type) {
        return switch (type) {
            case "terror" -> 0xFF2A2A;
            case "epico" -> 0xFF9A2B;
            case "victoria" -> 0x6BFF8A;
            default -> 0xFFD84A;
        };
    }

    private static void drawMoment(GuiGraphics g, int w, int h, Moment m, long now, Minecraft mc) {
        float t = (now - m.start) / 1000f, total = 6.5f;
        float inA = c01(t / 0.6f), outA = c01((total - t) / 0.7f);
        float a = Math.min(inA, outA);
        int color = momentColor(m.type);
        Random rnd = new Random(now / 60);

        int bar = (int) (h * 0.13f * a);
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);
        g.fillGradient(0, bar, w, bar + h / 5, col(0.55f * a, 0x000000), col(0f, 0x000000));
        g.fillGradient(0, h - bar - h / 5, w, h - bar, col(0f, 0x000000), col(0.55f * a, 0x000000));
        g.fill(0, 0, w, h, col(0.25f * a, m.type.equals("terror") ? 0x300000 : 0x000010));

        if (m.type.equals("revelacion") || m.type.equals("epico")) {
            float flash = c01(1f - t / 0.35f);
            if (flash > 0) g.fill(0, 0, w, h, col(flash, 0xFFFFFF));
        }

        if (t > 0.5f) {
            float ta = c01((t - 0.5f) / 0.4f) * outA;
            boolean glitch = t < 1.0f || (m.type.equals("terror") && rnd.nextInt(8) == 0);
            Component title = Component.literal(m.title.toUpperCase()).withStyle(ChatFormatting.BOLD);
            float sc = Math.min(4f, (w * 0.85f) / Math.max(1, mc.font.width(title)));
            float jx = glitch ? (rnd.nextFloat() - 0.5f) * 14f : 0f;
            float ty = h * 0.42f;
            if (glitch) {
                textCentered(g, mc.font, title, w / 2f + jx - 4f, ty, sc, tcol(ta * 0.6f, 0x00FFFF), false);
                textCentered(g, mc.font, title, w / 2f + jx + 4f, ty, sc, tcol(ta * 0.6f, 0xFF0044), false);
            }
            textCentered(g, mc.font, title, w / 2f + jx, ty, sc, tcol(ta, color), true);
            int lw = (int) (mc.font.width(title) * sc * 0.5f * ta);
            g.fill(w / 2 - lw, (int) (ty + 10 * sc + 4), w / 2 + lw, (int) (ty + 10 * sc + 6), col(ta, color));
        }
        if (t > 1.0f && !m.sub.isEmpty()) {
            float sa = c01((t - 1.0f) / 0.5f) * outA;
            Component sub = Component.literal(m.sub);
            float sc = Math.min(1.8f, (w * 0.8f) / Math.max(1, mc.font.width(sub)));
            textCentered(g, mc.font, sub, w / 2f, h * 0.42f + 10 * 4f + 20f, sc, tcol(sa, 0xFFFFFF), true);
        }
    }

    // ------------------------------------------------------------ caja de mensaje (/tellraw rojo en negrita)
    private static void drawBox(GuiGraphics g, int w, int h, Box b, long now, Minecraft mc) {
        float t = (now - b.start) / 1000f, total = 6f;
        float a = Math.min(c01(t / 0.3f), c01((total - t) / 0.5f));
        float scale = 2f;
        int boxW = (int) Math.min(w * 0.75f, 460f);
        int wrap = (int) ((boxW - 40) / scale);
        List<FormattedCharSequence> lines = mc.font.split(b.msg, wrap);
        int lh = (int) (10 * scale);
        int boxH = lines.size() * lh + 36;
        int x0 = w / 2 - boxW / 2, y0 = h / 2 - boxH / 2 - h / 10;

        g.fill(x0, y0, x0 + boxW, y0 + boxH, col(0.88f * a, 0x0A0000));
        float pulse = 0.65f + 0.35f * (float) Math.sin(t * 6f);
        int bc = col(a * pulse, 0xFF2020);
        g.fill(x0, y0, x0 + boxW, y0 + 3, bc);
        g.fill(x0, y0 + boxH - 3, x0 + boxW, y0 + boxH, bc);
        g.fill(x0, y0, x0 + 3, y0 + boxH, bc);
        g.fill(x0 + boxW - 3, y0, x0 + boxW, y0 + boxH, bc);

        int y = y0 + 18;
        for (FormattedCharSequence line : lines) {
            g.pose().pushPose();
            g.pose().translate(w / 2f, y, 0);
            g.pose().scale(scale, scale, 1f);
            g.drawString(mc.font, line, -mc.font.width(line) / 2, 0, tcol(a, 0xFF4040), true);
            g.pose().popPose();
            y += lh;
        }
    }

    // ------------------------------------------------------------ corazon dorado girando
    private static void drawGold(GuiGraphics g, int w, int h, Gold o, long now) {
        float t = (now - o.start) / 1000f, total = 2.6f;
        float a = Math.min(c01(t / 0.25f), c01((total - t) / 0.5f));
        float size = h * 0.30f;
        float cx = w / 2f, cy = h * 0.40f;
        float scaleX = Math.max(0.06f, Math.abs((float) Math.cos(t * Math.PI * 2.5)));
        float pop = 0.6f + 0.4f * c01(t / 0.4f);

        for (int i = 0; i < 26; i++) {
            float ang = hash(i) * 6.2831f + t * (0.8f + hash(i + 50));
            float rad = size * (0.55f + 0.5f * hash(i + 90)) * (0.7f + 0.3f * (float) Math.sin(t * 4f + i));
            int sx = (int) (cx + Math.cos(ang) * rad), sy = (int) (cy + Math.sin(ang) * rad);
            float tw = c01((float) Math.sin(t * 8f + i * 1.7f) * 0.5f + 0.5f);
            g.fill(sx, sy, sx + 3, sy + 3, col(a * tw, 0xFFE866));
        }
        circle(g, cx, cy, size * 0.75f, 0xFFD030, 0.18f * a);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, a);
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(scaleX * pop, pop, 1f);
        g.blit(GOLD_HEART, (int) (-size / 2), (int) (-size / 2), (int) size, (int) size, 0f, 0f, 32, 32, 32, 32);
        g.pose().popPose();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
}
