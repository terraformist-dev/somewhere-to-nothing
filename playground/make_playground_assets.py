"""Nothing Playground assets for Somewhere to Nothing.

- somewhere-to-nothing-preview.png: 512x512 still of the Glyph Matrix.
- somewhere-to-nothing.json: 5 s, 30 fps Lottie of the Glyph Toy.

Matrix drawing follows MatrixArt.kt; layout follows GlyphMatrixEditor's Phone (3) Lottie
export (black disc, square pixels, unlit #1C1C1C) so it sits alongside other Playground toys.
Usage: python3 make_playground_assets.py <out dir>
"""
import json
import math
import sys

from PIL import Image, ImageDraw

OUT = sys.argv[1]
N = 25
C = 12.0
FPS = 30
FRAMES = 150
CANVAS = 512
PAD = 36.0
PITCH = 17.7896
PIX = 36 / (36 / 13.05)  # 13.05: pixel square in 512-unit space
OFF = 0x1C

INDENT = [9, 7, 5, 4, 3, 2, 2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 2, 2, 3, 4, 5, 7, 9]


def inside(x, y):
    return INDENT[y] <= x < N - INDENT[y]


SMALL = {
    "1": [".#.", "##.", ".#.", ".#.", "###"],
    "2": ["##.", "..#", ".#.", "#..", "###"],
    ".": [".", ".", ".", ".", "#"],
    "K": ["#.#", "#.#", "##.", "#.#", "#.#"],
    "M": ["#.#", "###", "###", "#.#", "#.#"],
}


def text_width(s):
    return sum(len(SMALL[c][0]) + 1 for c in s) - 1


def stamp(grid, s, left, top, level):
    x = left
    for ch in s:
        g = SMALL[ch]
        for gy, row in enumerate(g):
            for gx, c in enumerate(row):
                if c == "#":
                    grid[top + gy][x + gx] = max(grid[top + gy][x + gx], level)
        x += len(g[0]) + 1


def pulse(t_ms, period):
    phase = (t_ms % period) / period
    return 0.3 + 0.7 * (0.5 + 0.5 * math.cos(2 * math.pi * phase))


def empty():
    return [[0] * N for _ in range(N)]


def pointer(angle, tip_level, close=True):
    g = empty()
    a = math.radians(angle)
    dx_, dy_ = math.sin(a), -math.cos(a)
    tx, ty = C + 10.4 * dx_, C + 10.4 * dy_
    for y in range(N):
        for x in range(N):
            if math.hypot(x - tx, y - ty) <= 1.3:
                g[y][x] = max(g[y][x], tip_level)
            if close:
                dx, dy = x - C, y - C
                along = dx * dx_ + dy * dy_
                across = dx * dy_ - dy * dx_
                if 1.5 <= along <= 8.8 and abs(across) <= 0.6:
                    g[y][x] = max(g[y][x], int(70 + 130 * along / 8.8))
    g[12][12] = 255 if close else 110
    return g


def distance(number, unit):
    g = empty()
    stamp(g, number, (N - text_width(number)) // 2, 6, 255)
    stamp(g, unit, (N - text_width(unit)) // 2, 14, 140)
    return g


def arrived(level):
    g = empty()
    for y in range(N):
        for x in range(N):
            if 10.6 <= math.hypot(x - C, y - C) <= 12.6:
                g[y][x] = level
    g[12][12] = 255
    return g


def smooth(t):
    return t * t * (3 - 2 * t)


def scale(g, k):
    return [[int(v * k) for v in row] for row in g]


INTRO_MS, ALL_ON_MS, DISSOLVE_MS, BEAT_MS, TIP = 1050, 120, 570, 720, 10.4


def switch_off_at(x, y):
    outside = min(1.0, math.hypot(x - C, y - C) / 12.7)
    scatter = ((x * 37 + y * 61) % 11 - 5) * 14
    span = (DISSOLVE_MS - ALL_ON_MS) * 0.85
    return max(ALL_ON_MS, ALL_ON_MS + int(span * (1 - outside)) + scatter)


def intro(angle, t):
    """Same as MatrixArt.intro: everything dissolves to nothing, then shoots out to somewhere."""
    g = empty()
    if t < DISSOLVE_MS:
        for y in range(N):
            for x in range(N):
                if inside(x, y) and t < switch_off_at(x, y):
                    g[y][x] = 255
    elif t >= BEAT_MS:
        p = smooth(min(1.0, (t - BEAT_MS) / (INTRO_MS - BEAT_MS)))
        a = math.radians(angle)
        for back, level in ((0, 255), (1, 140), (2, 60)):
            d = p * TIP - back
            if d < 0:
                continue
            # Kotlin's Math.round rounds halves up.
            x, y = math.floor(C + d * math.sin(a) + 0.5), math.floor(C - d * math.cos(a) + 0.5)
            g[y][x] = max(g[y][x], level)
    g[12][12] = 255
    return g


def frame(f):
    """The story, 5 s: switch on, long-press for the distance, close in, arrive."""
    ms = f * 1000 / FPS
    if ms < INTRO_MS:
        return intro(35, ms)
    if f < 45:
        return pointer(35, int(255 * pulse(ms - INTRO_MS, 1200)))
    if f < 85:
        # Long-press: the distance, as on the toy.
        return distance("1.2", "KM")
    if f < 118:
        # Walking closer: same direction, faster pulse.
        wobble = 3 * math.sin((f - 85) / 33 * 2 * math.pi)
        return pointer(35 + wobble, int(255 * pulse(ms - 85 * 1000 / FPS, 600)))
    # Within 50 m: the outer ring pulses.
    level = int(255 * pulse(ms - 118 * 1000 / FPS, 700))
    g = arrived(level)
    return g


def gray(level):
    if level <= 0:
        return OFF
    return round(OFF + (255 - OFF) * level / 255)


frames = [frame(f) for f in range(FRAMES)]


def px_xy(x, y):
    return PAD + x * PITCH, PAD + y * PITCH


# ---------- Still preview: needle pointing at the store, tip fully lit ----------

def draw_still(path, g, size=512):
    k = size / CANVAS
    ss = 4  # supersample for clean edges on the disc
    img = Image.new("RGBA", (size * ss, size * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([0, 0, size * ss - 1, size * ss - 1], fill=(0, 0, 0, 255))
    for y in range(N):
        for x in range(N):
            if not inside(x, y):
                continue
            v = gray(g[y][x])
            X, Y = px_xy(x, y)
            d.rectangle([X * k * ss, Y * k * ss, (X + PIX) * k * ss - 1, (Y + PIX) * k * ss - 1], fill=(v, v, v, 255))
    img.resize((size, size), Image.LANCZOS).save(path)


draw_still(f"{OUT}/somewhere-to-nothing-preview.png", pointer(35, 255))

# ---------- Lottie ----------

def col(v):
    c = round(v / 255, 4)
    return [c, c, c, 1]


groups = []
for y in range(N):
    for x in range(N):
        if not inside(x, y):
            continue
        seq = [gray(fr[y][x]) for fr in frames]
        X, Y = px_xy(x, y)
        if all(v == seq[0] for v in seq):
            color = {"a": 0, "k": col(seq[0])}
        else:
            keys = []
            for t, v in enumerate(seq):
                prev_diff = t > 0 and v != seq[t - 1]
                next_diff = t + 1 < FRAMES and seq[t + 1] != v
                if t == 0 or t == FRAMES - 1 or prev_diff or next_diff:
                    keys.append({"t": t, "s": col(v), "i": {"x": [1], "y": [1]}, "o": {"x": [0], "y": [0]}})
            del keys[-1]["i"], keys[-1]["o"]
            color = {"a": 1, "k": keys}
        groups.append({
            "ty": "gr",
            "nm": f"px {y}-{x}",
            "it": [
                {"ty": "rc", "d": 1, "s": {"a": 0, "k": [round(PIX, 3), round(PIX, 3)]},
                 "p": {"a": 0, "k": [round(X + PIX / 2, 3), round(Y + PIX / 2, 3)]}, "r": {"a": 0, "k": 0}},
                {"ty": "fl", "c": color, "o": {"a": 0, "k": 100}, "r": 1},
                {"ty": "tr", "p": {"a": 0, "k": [0, 0]}, "a": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [100, 100]},
                 "r": {"a": 0, "k": 0}, "o": {"a": 0, "k": 100}},
            ],
        })

transform = {"o": {"a": 0, "k": 100}, "r": {"a": 0, "k": 0}, "p": {"a": 0, "k": [0, 0, 0]},
             "a": {"a": 0, "k": [0, 0, 0]}, "s": {"a": 0, "k": [100, 100, 100]}}

disc = {
    "ddd": 0, "ind": 2, "ty": 4, "nm": "Disc", "sr": 1, "ks": transform, "ao": 0,
    "shapes": [
        {"ty": "el", "d": 1, "s": {"a": 0, "k": [CANVAS, CANVAS]}, "p": {"a": 0, "k": [CANVAS / 2, CANVAS / 2]}},
        {"ty": "fl", "c": {"a": 0, "k": [0, 0, 0, 1]}, "o": {"a": 0, "k": 100}, "r": 1},
    ],
    "ip": 0, "op": FRAMES, "st": 0, "bm": 0,
}
matrix = {
    "ddd": 0, "ind": 1, "ty": 4, "nm": "Glyph Matrix", "sr": 1, "ks": transform, "ao": 0,
    "shapes": groups, "ip": 0, "op": FRAMES, "st": 0, "bm": 0,
}
lottie = {
    "v": "5.7.4", "fr": FPS, "ip": 0, "op": FRAMES, "w": CANVAS, "h": CANVAS,
    "nm": "Somewhere to Nothing", "ddd": 0, "assets": [], "layers": [matrix, disc],
}
with open(f"{OUT}/somewhere-to-nothing.json", "w") as fh:
    json.dump(lottie, fh, separators=(",", ":"))

# Plain-pixel copies of every frame, for checking against the Lottie render.
with open(f"{OUT}/frames.json", "w") as fh:
    json.dump([[[gray(v) if inside(x, y) else -1 for x, v in enumerate(row)] for y, row in enumerate(fr)] for fr in frames], fh)
print("ok")
