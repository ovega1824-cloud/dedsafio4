#!/usr/bin/env python3
"""Genera texturas, modelos, traducciones, recetas y loot tables del mod Desafio 4."""
import json
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "src", "main", "resources")
A = os.path.join(ROOT, "assets", "desafio4")
D = os.path.join(ROOT, "data", "desafio4")
S = 32
OUT = (18, 10, 14, 255)


def mk(*p):
    path = os.path.join(*p)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    return path


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def outline(img, color=OUT):
    a = img.split()[3]
    dil = a.filter(ImageFilter.MaxFilter(3))
    out = img.copy()
    px, ap, dp = out.load(), a.load(), dil.load()
    for y in range(img.height):
        for x in range(img.width):
            if ap[x, y] < 40 and dp[x, y] >= 40:
                px[x, y] = color
    return out


def new(size=S):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3])


# ---------------------------------------------------------------- corazones
def heart_mask(size, scale=0.34):
    cx, cy, sc = size / 2, size / 2 + size * 0.03, size * scale
    m = {}
    for y in range(size):
        for x in range(size):
            X, Y = (x + 0.5 - cx) / sc, -(y + 0.5 - cy) / sc
            if (X * X + Y * Y - 1) ** 3 - X * X * Y ** 3 <= 0:
                m[(x, y)] = True
    return m, cx, cy, sc


def heart_img(size, light, dark, scale=0.34, spec=True):
    img = new(size)
    px = img.load()
    m, cx, cy, sc = heart_mask(size, scale)
    for (x, y) in m:
        t = min(1.0, max(0.0, (y / size) * 0.8 + (x / size) * 0.25 - 0.12))
        px[x, y] = lerp(light, dark, t) + (255,)
    # facetas tipo cristal / alien
    for (x, y) in m:
        if (x - 2 * y) % 9 == 0 and x < cx:
            px[x, y] = lerp(px[x, y][:3], (255, 255, 255), 0.18) + (255,)
        if (x + y) % 11 == 0 and x >= cx:
            px[x, y] = lerp(px[x, y][:3], (0, 0, 0), 0.12) + (255,)
    d = ImageDraw.Draw(img)
    if spec:
        hx, hy = cx - sc * 0.58, cy - sc * 0.55
        d.ellipse((hx - 2.2, hy - 1.6, hx + 2.2, hy + 1.6), fill=(255, 255, 255, 235))
        d.point((hx + 4, hy + 3), fill=(255, 255, 255, 200))
    return img


def sparkle(d, x, y, r=2, col=(255, 255, 255, 255)):
    d.line((x - r, y, x + r, y), fill=col)
    d.line((x, y - r, x, y + r), fill=col)


# ---------------------------------------------------------------- items
def roulette(color):
    img = new()
    px = img.load()
    cx = cy = 15.5
    dark = shade(color, 0.62)
    for y in range(S):
        for x in range(S):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            if r > 13.4:
                continue
            ang = (math.atan2(dx, -dy) + 2 * math.pi) % (2 * math.pi)
            seg = int(ang / (math.pi / 4))
            frac = (ang % (math.pi / 4)) / (math.pi / 4)
            c = color if seg % 2 == 0 else dark
            if r > 12.2:
                c = (225, 225, 235)
            elif frac < 0.04 or frac > 0.96:
                c = (20, 20, 24)
            elif r < 3.6:
                c = (30, 30, 36)
            px[x, y] = c + (255,)
    d = ImageDraw.Draw(img)
    d.ellipse((13, 13, 18, 18), fill=(235, 235, 240, 255))
    d.polygon([(13, 0), (19, 0), (16, 5)], fill=(255, 255, 255, 255))
    return outline(img)


def sphere(img, cx, cy, r, light, dark):
    px = img.load()
    for y in range(S):
        for x in range(S):
            dx, dy = x - cx, y - cy
            if dx * dx + dy * dy <= r * r:
                t = min(1.0, max(0.0, ((dx + dy) / (r * 1.6)) * 0.5 + 0.45))
                px[x, y] = lerp(light, dark, t) + (255,)


def smoke_bomb():
    img = new()
    sphere(img, 16, 19, 10, (120, 120, 130), (28, 28, 34))
    px = img.load()
    for y in range(17, 21):
        for x in range(S):
            if px[x, y][3] > 0:
                px[x, y] = lerp((255, 70, 60), (150, 10, 10), (y - 17) / 3) + (255,)
    d = ImageDraw.Draw(img)
    d.rectangle((15, 7, 17, 10), fill=(110, 70, 40, 255))
    d.point((16, 5), fill=(255, 230, 80, 255))
    d.point((15, 4), fill=(255, 150, 40, 255))
    d.point((17, 4), fill=(255, 150, 40, 255))
    d.point((16, 3), fill=(255, 255, 200, 255))
    d.ellipse((22, 5, 28, 10), fill=(190, 190, 195, 170))
    d.ellipse((6, 4, 10, 8), fill=(170, 170, 175, 150))
    d.ellipse((11, 14, 14, 16), fill=(255, 255, 255, 120))
    return outline(img)


def black_circle():
    img = new()
    px = img.load()
    for y in range(S):
        for x in range(S):
            r = math.hypot(x - 15.5, y - 15.5)
            if r <= 11:
                t = r / 11
                px[x, y] = lerp((0, 0, 0), (22, 8, 10), t * 0.6) + (255,)
            elif r <= 14.5:
                t = (r - 11) / 3.5
                px[x, y] = lerp((255, 200, 70), (200, 40, 10), t) + (int(255 * (1 - t * 0.55)),)
    d = ImageDraw.Draw(img)
    d.arc((8, 7, 17, 16), 200, 280, fill=(80, 80, 90, 255))
    return outline(img)


def layer_ball(up):
    img = new()
    sphere(img, 15.5, 15.5, 13, (255, 110, 100), (140, 10, 20))
    d = ImageDraw.Draw(img)
    if up:
        head = [(16, 6), (8, 16), (24, 16)]
        shaft = (13, 16, 19, 25)
    else:
        head = [(16, 26), (8, 16), (24, 16)]
        shaft = (13, 7, 19, 16)
    d.polygon(head, fill=(255, 255, 255, 255), outline=(60, 10, 10, 255))
    d.rectangle(shaft, fill=(255, 255, 255, 255), outline=(60, 10, 10, 255))
    d.polygon(head, fill=(255, 255, 255, 255))
    d.rectangle((shaft[0] + 1, shaft[1] + (0 if up else 1), shaft[2] - 1, shaft[3] - (1 if up else 0)), fill=(255, 255, 255, 255))
    d.ellipse((7, 6, 11, 9), fill=(255, 255, 255, 120))
    return outline(img)


def acid_drop():
    img = new()
    px = img.load()
    for y in range(S):
        for x in range(S):
            dx, dy = x - 16, y - 20
            inside = dx * dx + dy * dy <= 81
            if y < 20:
                half = (y - 3) / 17 * 8.6
                inside = inside or (y >= 3 and abs(dx) <= half)
            if inside:
                t = min(1.0, max(0.0, (x - 8) / 20 * 0.6 + (y - 3) / 28 * 0.5))
                px[x, y] = lerp((190, 255, 80), (20, 140, 30), t) + (255,)
    d = ImageDraw.Draw(img)
    d.ellipse((11, 17, 14, 22), fill=(255, 255, 255, 190))
    d.point((19, 24), fill=(220, 255, 160, 255))
    d.point((18, 22), fill=(220, 255, 160, 255))
    d.ellipse((22, 10, 25, 13), outline=(120, 230, 60, 200))
    return outline(img)


def feather():
    img = new()
    d = ImageDraw.Draw(img)
    p0, p1 = (5.0, 27.0), (27.0, 5.0)
    nx, ny = 0.7071, 0.7071
    left, right = [], []
    n = 40
    for i in range(n + 1):
        t = i / n
        cx = p0[0] + (p1[0] - p0[0]) * t
        cy = p0[1] + (p1[1] - p0[1]) * t
        w = 6.2 * (math.sin(math.pi * min(1, t * 1.05)) ** 0.75)
        left.append((cx + nx * w, cy + ny * w))
        right.append((cx - nx * w, cy - ny * w))
    d.polygon(left + right[::-1], fill=(246, 246, 252, 255))
    d.polygon(right + [p1, p0], fill=(214, 220, 238, 255))
    d.line((p0[0] - 3, p0[1] + 3, p1[0], p1[1]), fill=(150, 156, 180, 255), width=1)
    for i in range(4, 38, 3):
        t = i / n
        cx = p0[0] + (p1[0] - p0[0]) * t
        cy = p0[1] + (p1[1] - p0[1]) * t
        d.point((cx + 2, cy + 2), fill=(200, 206, 226, 255))
        d.point((cx - 2, cy - 2), fill=(255, 255, 255, 255))
    return outline(img)


def nut(d, cx, cy, r=6):
    pts = [(cx + r * math.cos(math.radians(60 * i + 30)), cy + r * math.sin(math.radians(60 * i + 30))) for i in range(6)]
    d.polygon(pts, fill=(160, 166, 178, 255), outline=(40, 40, 50, 255))
    d.ellipse((cx - 2.6, cy - 2.6, cx + 2.6, cy + 2.6), fill=(40, 40, 50, 255))
    d.ellipse((cx - 1.4, cy - 1.4, cx + 1.4, cy + 1.4), fill=(0, 0, 0, 0))
    d.line((cx - r + 1, cy - 2, cx - 1, cy - r + 2), fill=(230, 232, 240, 255))


def config_heart():
    img = heart_img(S, (255, 238, 90), (230, 160, 10))
    d = ImageDraw.Draw(img)
    nut(d, 23, 23)
    return outline(img)


def mini_table(d, x0, y0, s=13):
    d.rectangle((x0, y0, x0 + s, y0 + s), fill=(176, 130, 80, 255), outline=(70, 40, 20, 255))
    step = s // 3
    for i in range(1, 3):
        d.line((x0 + i * step, y0 + 1, x0 + i * step, y0 + s - 1), fill=(70, 40, 20, 255))
        d.line((x0 + 1, y0 + i * step, x0 + s - 1, y0 + i * step), fill=(70, 40, 20, 255))
    d.rectangle((x0 + 1, y0 + 1, x0 + step - 1, y0 + step - 1), fill=(210, 170, 120, 255))


def craft_limit_heart():
    img = heart_img(S, (255, 238, 90), (230, 160, 10))
    d = ImageDraw.Draw(img)
    mini_table(d, 17, 17)
    return outline(img)


def gold_heart():
    img = heart_img(S, (255, 246, 150), (214, 140, 10), scale=0.36)
    d = ImageDraw.Draw(img)
    sparkle(d, 5, 5, 3, (255, 255, 230, 255))
    sparkle(d, 27, 8, 2, (255, 255, 200, 255))
    sparkle(d, 25, 27, 3, (255, 255, 220, 255))
    sparkle(d, 4, 22, 2, (255, 240, 170, 255))
    return outline(img, (90, 50, 0, 255))


def resurrection_heart():
    glow = new()
    gd = ImageDraw.Draw(glow)
    gd.ellipse((2, 2, 29, 29), fill=(255, 120, 150, 150))
    glow = glow.filter(ImageFilter.GaussianBlur(3))
    img = heart_img(S, (255, 100, 100), (160, 8, 40), scale=0.33)
    out = Image.alpha_composite(glow, img)
    d = ImageDraw.Draw(out)
    sparkle(d, 4, 6, 2, (255, 220, 230, 255))
    sparkle(d, 27, 25, 2, (255, 220, 230, 255))
    return outline(out, (60, 0, 20, 255))


def mission_book():
    img = new()
    d = ImageDraw.Draw(img)
    d.rectangle((6, 4, 26, 28), fill=(124, 72, 38, 255))
    d.rectangle((6, 4, 9, 28), fill=(84, 44, 22, 255))
    d.rectangle((25, 6, 27, 27), fill=(244, 238, 220, 255))
    d.line((10, 5, 24, 5), fill=(170, 110, 64, 255))
    d.line((10, 27, 24, 27), fill=(78, 40, 20, 255))
    cx, cy = 17, 16
    pts = []
    for i in range(10):
        r = 6.5 if i % 2 == 0 else 2.8
        a = math.radians(-90 + i * 36)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    d.polygon(pts, fill=(255, 214, 60, 255), outline=(150, 100, 10, 255))
    return outline(img)


def moment_crystal():
    img = new()
    d = ImageDraw.Draw(img)
    top, bot, l, r, mid = (16, 2), (16, 30), (6, 12), (26, 12), (16, 12)
    d.polygon([top, l, mid], fill=(200, 150, 255, 255))
    d.polygon([top, mid, r], fill=(160, 100, 235, 255))
    d.polygon([l, mid, bot], fill=(110, 60, 200, 255))
    d.polygon([mid, r, bot], fill=(80, 40, 160, 255))
    d.line((16, 2, 16, 30), fill=(230, 200, 255, 120))
    sparkle(d, 12, 8, 2, (255, 255, 255, 255))
    sparkle(d, 24, 22, 2, (230, 210, 255, 255))
    return outline(img)


def difficulty_die():
    img = new()
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((4, 4, 27, 27), radius=4, fill=(240, 240, 244, 255))
    d.rounded_rectangle((4, 15, 27, 27), radius=4, fill=(206, 208, 218, 255))
    d.rectangle((6, 15, 25, 20), fill=(224, 226, 234, 255))
    for (x, y) in [(9, 9), (22, 9), (16, 16), (9, 23), (22, 23)]:
        d.ellipse((x - 2, y - 2, x + 2, y + 2), fill=(200, 20, 30, 255))
    return outline(img)


def totem(accent, body=(222, 178, 58)):
    img = new()
    d = ImageDraw.Draw(img)
    dark = shade(body, 0.7)
    d.rectangle((10, 3, 21, 13), fill=body + (255,))
    d.rectangle((10, 9, 21, 13), fill=dark + (255,))
    d.rectangle((4, 13, 27, 18), fill=body + (255,))
    d.rectangle((4, 16, 27, 18), fill=dark + (255,))
    d.rectangle((11, 18, 20, 29), fill=body + (255,))
    d.rectangle((11, 24, 20, 29), fill=dark + (255,))
    d.rectangle((4, 13, 7, 18), fill=accent + (255,))
    d.rectangle((24, 13, 27, 18), fill=accent + (255,))
    d.rectangle((15, 18, 16, 29), fill=accent + (255,))
    d.rectangle((12, 6, 13, 7), fill=(20, 20, 20, 255))
    d.rectangle((18, 6, 19, 7), fill=(20, 20, 20, 255))
    d.rectangle((14, 10, 17, 10), fill=accent + (255,))
    d.rectangle((10, 3, 21, 4), fill=lerp(body, (255, 255, 255), 0.35) + (255,))
    return outline(img)


def campfire_icon():
    img = new()
    d = ImageDraw.Draw(img)
    d.line((4, 29, 28, 21), fill=(98, 60, 30, 255), width=5)
    d.line((4, 21, 28, 29), fill=(120, 76, 38, 255), width=5)
    d.line((4, 28, 28, 20), fill=(140, 92, 46, 255), width=1)
    flame = [(16, 2), (22, 11), (25, 19), (16, 25), (7, 19), (10, 11)]
    d.polygon(flame, fill=(60, 220, 230, 255))
    d.polygon([(16, 8), (20, 14), (21, 20), (16, 23), (11, 20), (12, 14)], fill=(150, 255, 245, 255))
    d.polygon([(16, 14), (18, 18), (16, 22), (14, 18)], fill=(255, 255, 255, 255))
    return outline(img)


# ---------------------------------------------------------------- gui
def skull():
    W = 64
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    bone, bone_d, bone_dd = (238, 230, 207, 255), (192, 180, 152, 255), (120, 108, 90, 255)
    for (x0, y0, x1, y1) in [(8, 8, 56, 56), (56, 8, 8, 56)]:
        d.line((x0, y0, x1, y1), fill=bone_dd, width=7)
        d.line((x0, y0, x1, y1), fill=bone, width=5)
    for (x, y) in [(8, 8), (56, 8), (8, 56), (56, 56)]:
        d.ellipse((x - 5, y - 5, x + 5, y + 5), fill=bone, outline=bone_dd)
        d.ellipse((x - 2, y - 2, x + 2, y + 2), fill=bone_d)
    d.ellipse((15, 9, 49, 43), fill=bone_d)
    d.rounded_rectangle((22, 36, 42, 53), radius=4, fill=bone_d)
    d.ellipse((14, 8, 45, 40), fill=bone)
    d.rounded_rectangle((22, 36, 40, 51), radius=4, fill=bone)
    d.ellipse((20, 24, 30, 35), fill=(10, 4, 6, 255))
    d.ellipse((34, 24, 44, 35), fill=(10, 4, 6, 255))
    d.ellipse((23, 28, 27, 32), fill=(255, 50, 36, 255))
    d.ellipse((37, 28, 41, 32), fill=(255, 50, 36, 255))
    d.point((24, 29), fill=(255, 220, 160, 255))
    d.point((38, 29), fill=(255, 220, 160, 255))
    d.polygon([(30, 39), (34, 39), (32, 33)], fill=(34, 16, 16, 255))
    d.line((24, 45, 40, 45), fill=bone_dd)
    for x in (26, 29, 32, 35, 38):
        d.line((x, 45, x, 51), fill=bone_dd)
    d.line((41, 12, 38, 19), fill=bone_dd)
    d.line((38, 19, 40, 22), fill=bone_dd)
    d.line((19, 14, 22, 18), fill=bone_d)
    crown = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    cd = ImageDraw.Draw(crown)
    gold, gold_d = (246, 196, 44, 255), (176, 118, 14, 255)
    cd.polygon([(22, 10), (22, 1), (27, 6), (32, -1), (37, 6), (42, 1), (42, 10)], fill=gold, outline=gold_d)
    cd.rectangle((22, 7, 42, 11), fill=gold_d)
    cd.rectangle((22, 7, 42, 8), fill=(255, 226, 110, 255))
    cd.rectangle((31, 3, 33, 5), fill=(235, 30, 50, 255))
    cd.point((26, 9), fill=(60, 140, 255, 255))
    cd.point((38, 9), fill=(60, 220, 120, 255))
    crown = crown.rotate(-22, resample=Image.NEAREST, center=(32, 10))
    img = Image.alpha_composite(img, crown)
    img = outline(img, (26, 10, 12, 255))
    return img.resize((128, 128), Image.NEAREST)


def angel():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse((5, 0, 10, 2), outline=(255, 226, 90, 255))
    d.polygon([(7, 6), (0, 3), (1, 8), (5, 10)], fill=(245, 245, 255, 255))
    d.polygon([(8, 6), (15, 3), (14, 8), (10, 10)], fill=(245, 245, 255, 255))
    d.ellipse((5, 3, 10, 8), fill=(255, 222, 190, 255))
    d.rectangle((5, 8, 10, 14), fill=(255, 255, 255, 255))
    return outline(img, (60, 70, 100, 255))


# ---------------------------------------------------------------- bloque
def campfire_log():
    img = Image.new("RGBA", (16, 16), (96, 62, 32, 255))
    px = img.load()
    rnd = random.Random(4)
    for y in range(16):
        for x in range(16):
            v = 0.85 + 0.25 * math.sin(y * 1.7 + rnd.random()) + rnd.uniform(-0.08, 0.08)
            px[x, y] = shade((104, 66, 34), v) + (255,)
    return img


def campfire_fire():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(8, 0), (12, 5), (14, 11), (11, 15), (5, 15), (2, 11), (4, 5)], fill=(40, 190, 210, 235))
    d.polygon([(8, 4), (11, 8), (11, 13), (8, 15), (5, 13), (5, 8)], fill=(130, 245, 245, 245))
    d.polygon([(8, 9), (9, 12), (8, 15), (7, 12)], fill=(255, 255, 255, 255))
    return img


# ---------------------------------------------------------------- principal
ROUL = {
    "red": (255, 43, 43), "orange": (255, 138, 31), "yellow": (255, 224, 43), "green": (43, 217, 74),
    "cyan": (43, 207, 255), "blue": (43, 91, 255), "purple": (165, 43, 255), "pink": (255, 95, 184),
}

ITEMS = {}
for k, c in ROUL.items():
    ITEMS["roulette_" + k] = (lambda c=c: roulette(c))
ITEMS.update({
    "smoke_bomb": smoke_bomb,
    "black_circle": black_circle,
    "layer_up": lambda: layer_ball(True),
    "layer_down": lambda: layer_ball(False),
    "acid_drop": acid_drop,
    "flight_feather": feather,
    "config_heart": config_heart,
    "craft_limit_heart": craft_limit_heart,
    "gold_heart": gold_heart,
    "resurrection_heart": resurrection_heart,
    "mission_book": mission_book,
    "moment_crystal": moment_crystal,
    "difficulty_die": difficulty_die,
    "totem_life": lambda: totem((30, 190, 90)),
    "totem_revive": lambda: totem((70, 210, 240)),
    "totem_chaos": lambda: totem((200, 50, 210), (150, 120, 170)),
    "resurrection_campfire": campfire_icon,
})

for name, fn in ITEMS.items():
    fn().save(mk(A, "textures", "item", name + ".png"))
    if name == "gold_heart":
        model = {"parent": "item/generated", "textures": {"layer0": "desafio4:item/gold_heart"},
                 "display": {"ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [1.6, 1.6, 1.6]}}}
    elif name == "resurrection_campfire":
        model = {"parent": "desafio4:block/resurrection_campfire"}
    else:
        model = {"parent": "item/generated", "textures": {"layer0": "desafio4:item/" + name}}
    with open(mk(A, "models", "item", name + ".json"), "w") as f:
        json.dump(model, f, indent=2)

outline(heart_img(32, (255, 238, 90), (230, 160, 10))).save(mk(A, "textures", "gui", "heart_yellow.png"))
outline(heart_img(32, (150, 150, 160), (64, 64, 76), spec=False)).save(mk(A, "textures", "gui", "heart_gray.png"))
skull().save(mk(A, "textures", "gui", "skull.png"))
angel().save(mk(A, "textures", "gui", "angel.png"))
campfire_log().save(mk(A, "textures", "block", "campfire_log.png"))
campfire_fire().save(mk(A, "textures", "block", "campfire_fire.png"))

# bloque: fogata muy grande (los elementos se salen del bloque a proposito)
logs = []
for frm, to in [([-6, 0, 2], [22, 5, 7]), ([-6, 0, 9], [22, 5, 14]), ([2, 5, -6], [7, 10, 22]), ([9, 5, -6], [14, 10, 22])]:
    logs.append({"from": frm, "to": to, "faces": {
        s: {"uv": [0, 0, 16, 5], "texture": "#log"} for s in ("north", "south", "east", "west", "up", "down")}})
fire = [
    {"from": [-2, 5, 8], "to": [18, 28, 8], "shade": False, "faces": {
        "north": {"uv": [0, 0, 16, 16], "texture": "#fire"}, "south": {"uv": [0, 0, 16, 16], "texture": "#fire"}}},
    {"from": [8, 5, -2], "to": [8, 28, 18], "shade": False, "faces": {
        "east": {"uv": [0, 0, 16, 16], "texture": "#fire"}, "west": {"uv": [0, 0, 16, 16], "texture": "#fire"}}},
]
block_model = {"render_type": "minecraft:cutout", "textures": {"log": "desafio4:block/campfire_log",
                                                               "fire": "desafio4:block/campfire_fire",
                                                               "particle": "desafio4:block/campfire_log"},
               "elements": logs + fire}
with open(mk(A, "models", "block", "resurrection_campfire.json"), "w") as f:
    json.dump(block_model, f, indent=2)
with open(mk(A, "blockstates", "resurrection_campfire.json"), "w") as f:
    json.dump({"variants": {"": {"model": "desafio4:block/resurrection_campfire"}}}, f, indent=2)

# ---------------------------------------------------------------- traducciones
ES = {
    "itemGroup.desafio4": "Desafío 4",
    "key.categories.desafio4": "Desafío 4",
    "key.desafio4.panel": "Abrir panel de Desafío 4",
    "key.desafio4.missions": "Abrir misiones",
    "item.desafio4.roulette_red": "Ruleta roja",
    "item.desafio4.roulette_orange": "Ruleta naranja",
    "item.desafio4.roulette_yellow": "Ruleta amarilla",
    "item.desafio4.roulette_green": "Ruleta verde",
    "item.desafio4.roulette_cyan": "Ruleta celeste",
    "item.desafio4.roulette_blue": "Ruleta azul",
    "item.desafio4.roulette_purple": "Ruleta morada",
    "item.desafio4.roulette_pink": "Ruleta rosa",
    "item.desafio4.smoke_bomb": "Bomba de humo",
    "item.desafio4.black_circle": "Círculo negro (Eclipse)",
    "item.desafio4.layer_up": "Bola roja: subir de capa",
    "item.desafio4.layer_down": "Bola roja: bajar de capa",
    "item.desafio4.acid_drop": "Gota de ácido",
    "item.desafio4.flight_feather": "Pluma de vuelo",
    "item.desafio4.config_heart": "Corazón de configuración",
    "item.desafio4.craft_limit_heart": "Corazón de límite de crafteo",
    "item.desafio4.gold_heart": "Corazón dorado",
    "item.desafio4.resurrection_heart": "Corazón de la resurrección",
    "item.desafio4.mission_book": "Libro de misiones",
    "item.desafio4.moment_crystal": "Cristal de momentos",
    "item.desafio4.difficulty_die": "Dado de dificultad",
    "item.desafio4.totem_life": "Tótem de vida",
    "item.desafio4.totem_revive": "Tótem de resurrección",
    "item.desafio4.totem_chaos": "Tótem del caos",
    "block.desafio4.resurrection_campfire": "Fogata de resurrección",
}
EN = {
    "itemGroup.desafio4": "Desafio 4",
    "key.categories.desafio4": "Desafio 4",
    "key.desafio4.panel": "Open Desafio 4 panel",
    "key.desafio4.missions": "Open missions",
    "item.desafio4.roulette_red": "Red Roulette",
    "item.desafio4.roulette_orange": "Orange Roulette",
    "item.desafio4.roulette_yellow": "Yellow Roulette",
    "item.desafio4.roulette_green": "Green Roulette",
    "item.desafio4.roulette_cyan": "Cyan Roulette",
    "item.desafio4.roulette_blue": "Blue Roulette",
    "item.desafio4.roulette_purple": "Purple Roulette",
    "item.desafio4.roulette_pink": "Pink Roulette",
    "item.desafio4.smoke_bomb": "Smoke Bomb",
    "item.desafio4.black_circle": "Black Circle (Eclipse)",
    "item.desafio4.layer_up": "Red Ball: Go Up a Layer",
    "item.desafio4.layer_down": "Red Ball: Go Down a Layer",
    "item.desafio4.acid_drop": "Acid Drop",
    "item.desafio4.flight_feather": "Flight Feather",
    "item.desafio4.config_heart": "Config Heart",
    "item.desafio4.craft_limit_heart": "Craft Limit Heart",
    "item.desafio4.gold_heart": "Golden Heart",
    "item.desafio4.resurrection_heart": "Resurrection Heart",
    "item.desafio4.mission_book": "Mission Book",
    "item.desafio4.moment_crystal": "Moment Crystal",
    "item.desafio4.difficulty_die": "Difficulty Die",
    "item.desafio4.totem_life": "Totem of Life",
    "item.desafio4.totem_revive": "Totem of Resurrection",
    "item.desafio4.totem_chaos": "Totem of Chaos",
    "block.desafio4.resurrection_campfire": "Resurrection Campfire",
}
for code, data in (("es_es", ES), ("es_mx", ES), ("en_us", EN)):
    with open(mk(A, "lang", code + ".json"), "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)

# ---------------------------------------------------------------- recetas y loot
recipes = {
    "resurrection_heart": {
        "type": "desafio4:limited_shaped", "category": "misc",
        "pattern": ["GRG", "RTR", "GRG"],
        "key": {"G": {"item": "minecraft:gold_ingot"}, "R": {"item": "minecraft:redstone_block"},
                "T": {"item": "minecraft:totem_of_undying"}},
        "result": {"item": "desafio4:resurrection_heart", "count": 1}},
    "resurrection_campfire": {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["LLL", "BFB", "OOO"],
        "key": {"L": {"tag": "minecraft:logs"}, "B": {"item": "minecraft:blaze_rod"},
                "F": {"item": "minecraft:soul_campfire"}, "O": {"item": "minecraft:obsidian"}},
        "result": {"item": "desafio4:resurrection_campfire", "count": 1}},
}
for name, r in recipes.items():
    with open(mk(D, "recipes", name + ".json"), "w") as f:
        json.dump(r, f, indent=2)

loot = {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": "desafio4:resurrection_campfire"}],
    "conditions": [{"condition": "minecraft:survives_explosion"}]}]}
with open(mk(D, "loot_tables", "blocks", "resurrection_campfire.json"), "w") as f:
    json.dump(loot, f, indent=2)

print("assets ok:", len(ITEMS), "items")
