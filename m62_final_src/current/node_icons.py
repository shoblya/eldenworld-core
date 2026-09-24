"""Small deterministic pixel icons for the 18 roots and 378 internal skills.

The glyph represents the node's role or attribute; colour identifies the class
and the corner marks its place in the branch. No third-party image tools are used
by the build workflow.
"""

import hashlib
import struct
import zlib
from pathlib import Path


PALETTE = {
    "rogue": (172, 83, 178), "warrior": (204, 104, 75),
    "ranger": (106, 169, 96), "mage": (111, 140, 228),
    "builder": (202, 157, 78), "adventurer": (92, 186, 188),
}


def render_node_icon(resources, skill_id, group, kind, branch, stat, serial):
    name = skill_id.replace("/", "__") + ".png"
    target = Path(resources) / "assets/eldenworld/textures/icons/nodes" / name
    target.parent.mkdir(parents=True, exist_ok=True)
    pixels = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    base = PALETTE[group]
    seed = hashlib.sha256(skill_id.encode()).digest()
    tone = ((seed[0] % 4) - 1) * 11
    accent = tuple(max(60, min(245, c + tone)) for c in base) + (255,)
    bright = tuple(min(255, c + 68) for c in accent[:3]) + (255,)
    dark = (22, 24, 33, 255)

    def dot(x, y, color):
        if 0 <= x < 16 and 0 <= y < 16:
            pixels[y][x] = color

    def line(x0, y0, x1, y1, color):
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
        err = dx + dy
        while True:
            dot(x0, y0, color)
            if x0 == x1 and y0 == y1:
                break
            e = 2 * err
            if e >= dy:
                err += dy
                x0 += sx
            if e <= dx:
                err += dx
                y0 += sy

    for y in range(1, 15):
        for x in range(1, 15):
            dot(x, y, dark)
    for x in range(2, 14):
        dot(x, 1, accent)
        dot(x, 14, accent)
    for y in range(2, 14):
        dot(1, y, accent)
        dot(14, y, accent)
    for x, y in ((1, 1), (1, 14), (14, 1), (14, 14)):
        dot(x, y, (0, 0, 0, 0))

    attr = stat[1] if stat else ""
    if kind == "start":
        for x, y in ((4, 7), (6, 5), (8, 7), (10, 5), (12, 7)):
            line(x, y, x, 10, bright)
        line(4, 10, 12, 10, bright)
        line(5, 12, 11, 12, accent)
    elif kind == "specialization":
        for x, y in ((8, 3), (8, 12), (3, 8), (12, 8)):
            line(8, 8, x, y, bright)
        for x, y in ((6, 6), (10, 6), (6, 10), (10, 10)):
            dot(x, y, accent)
    elif kind == "mastery":
        for x0, y0, x1, y1 in ((8, 3, 12, 8), (12, 8, 8, 12),
                               (8, 12, 4, 8), (4, 8, 8, 3)):
            line(x0, y0, x1, y1, bright)
        dot(8, 8, accent)
        dot(8, 9, accent)
    elif "health" in attr or "healing" in attr:
        for y, xs in ((5, (5, 6, 9, 10)), (6, (4, 5, 6, 7, 8, 9, 10, 11)),
                      (7, range(4, 12)), (8, range(5, 11)), (9, range(6, 10)),
                      (10, (7, 8))):
            for x in xs:
                dot(x, y, bright)
    elif "armor" in attr or "resist" in attr or "dodge" in attr:
        line(8, 3, 12, 5, bright)
        line(12, 5, 11, 10, bright)
        line(11, 10, 8, 12, bright)
        line(8, 12, 5, 10, bright)
        line(5, 10, 4, 5, bright)
        line(4, 5, 8, 3, bright)
        line(8, 5, 8, 10, accent)
    elif "arrow" in attr or "draw_speed" in attr:
        line(5, 4, 5, 11, bright)
        line(5, 4, 9, 8, bright)
        line(5, 11, 9, 8, bright)
        line(7, 8, 12, 8, accent)
    elif "mana" in attr or "spell" in attr or "cast" in attr:
        line(8, 3, 11, 8, bright)
        line(11, 8, 8, 12, bright)
        line(8, 12, 5, 8, bright)
        line(5, 8, 8, 3, bright)
        line(8, 5, 8, 10, accent)
    elif "movement" in attr or "reach" in attr:
        line(5, 4, 10, 4, bright)
        line(10, 4, 7, 8, bright)
        line(7, 8, 11, 8, bright)
        line(11, 8, 5, 12, bright)
    elif "mining" in attr:
        line(5, 5, 11, 5, bright)
        line(10, 5, 6, 11, accent)
        line(4, 6, 8, 4, bright)
    elif "luck" in attr:
        line(8, 3, 8, 12, bright)
        line(3, 8, 12, 8, bright)
        line(5, 5, 11, 11, accent)
        line(11, 5, 5, 11, accent)
    else:
        line(5, 11, 10, 4, bright)
        line(6, 12, 12, 6, bright)
        line(3, 9, 7, 13, accent)

    # The small branch rank remains readable on the repeated stat glyphs.
    rank = int(skill_id[-1]) if skill_id[-1].isdigit() else (5 if kind == "big" else 0)
    for i in range(rank):
        dot(3 + i, 13, bright)
    if kind == "big":
        dot(12, 3, bright)
        dot(11, 4, bright)
    if branch:
        # Branch marker at the top is stable and varies by specialization.
        mark = hashlib.sha256(branch.encode()).digest()[0] % 9
        dot(4 + mark, 2, bright)
    # Distinct nine-pixel right-edge signatures for all 396 icons.
    for bit in range(9):
        dot(13, 3 + bit, bright if serial & (1 << bit) else dark)

    rows = b"".join(b"\0" + b"".join(bytes(px) for px in row) for row in pixels)

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xffffffff)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(rows, 9)) + chunk(b"IEND", b"")
    target.write_bytes(png)
