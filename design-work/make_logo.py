"""
Logo for Create Walkable Tracks mod.

Designed at 64x64 native, scaled 4x nearest-neighbor to 256x256.
Composition:
  - Dark slate background with subtle vignette
  - Brass/copper inset border (Create-aesthetic)
  - Diagonal curved track segment (bottom-left to top-right) rendered top-down
    with two parallel rails on a dark wood base
  - Three footprint pairs walking along the curve in warm off-white
"""

from PIL import Image, ImageDraw
from pathlib import Path
import math

OUT_DIR = Path(__file__).parent
NATIVE = 64
SCALE = 4
FINAL = NATIVE * SCALE

# Palette (sampled from Create's standard_track.png + extended)
BG_DARK         = (20, 16, 14, 255)
BG_LIGHT        = (36, 28, 22, 255)
BORDER_BRASS    = (180, 130, 60, 255)
BORDER_BRASS_HI = (232, 180, 96, 255)
BORDER_BRASS_LO = (100, 70, 30, 255)
TIE_DARK        = (32, 24, 18, 255)
TIE_WOOD        = (96, 60, 36, 255)
TIE_WOOD_HI     = (132, 84, 50, 255)
RAIL_LO         = (110, 110, 116, 255)
RAIL            = (170, 170, 178, 255)
RAIL_HI         = (224, 224, 232, 255)
FOOT            = (255, 252, 240, 255)
FOOT_OUTLINE    = (12, 8, 6, 255)


def make_background(img: Image.Image, draw: ImageDraw.ImageDraw):
    """Vertical gradient for slight depth."""
    for y in range(NATIVE):
        t = y / (NATIVE - 1)
        r = int(BG_DARK[0] * (1 - t) + BG_LIGHT[0] * t)
        g = int(BG_DARK[1] * (1 - t) + BG_LIGHT[1] * t)
        b = int(BG_DARK[2] * (1 - t) + BG_LIGHT[2] * t)
        draw.line([(0, y), (NATIVE - 1, y)], fill=(r, g, b, 255))


def make_border(draw: ImageDraw.ImageDraw):
    """2px brass border with 1px highlight inside, 1px shadow outside-line."""
    # Outermost: 1px dark
    draw.rectangle([0, 0, NATIVE - 1, NATIVE - 1], outline=BORDER_BRASS_LO, width=1)
    # Inset 1px brass
    draw.rectangle([1, 1, NATIVE - 2, NATIVE - 2], outline=BORDER_BRASS, width=1)
    # Inset 2px highlight (subtle, only top + left to suggest bevel)
    for x in range(2, NATIVE - 2):
        draw.point((x, 2), fill=BORDER_BRASS_HI)
    for y in range(2, NATIVE - 2):
        draw.point((2, y), fill=BORDER_BRASS_HI)
    # Inset 2px shadow on bottom + right
    for x in range(2, NATIVE - 2):
        draw.point((x, NATIVE - 3), fill=BORDER_BRASS_LO)
    for y in range(2, NATIVE - 2):
        draw.point((NATIVE - 3, y), fill=BORDER_BRASS_LO)


def curve_point(t: float):
    """Quadratic Bezier curve from bottom-left to top-right with sag through middle.
    Returns (x, y) in native coords. The 'walkable area' lives within border (3..60)."""
    # Control points (chosen so curve sweeps nicely diagonally)
    p0 = (8, 54)
    p1 = (40, 48)   # slight downward sag in middle
    p2 = (56, 12)
    one_minus_t = 1 - t
    x = one_minus_t * one_minus_t * p0[0] + 2 * one_minus_t * t * p1[0] + t * t * p2[0]
    y = one_minus_t * one_minus_t * p0[1] + 2 * one_minus_t * t * p1[1] + t * t * p2[1]
    return x, y


def curve_tangent(t: float):
    p0 = (8, 54)
    p1 = (40, 48)
    p2 = (56, 12)
    dx = 2 * (1 - t) * (p1[0] - p0[0]) + 2 * t * (p2[0] - p1[0])
    dy = 2 * (1 - t) * (p1[1] - p0[1]) + 2 * t * (p2[1] - p1[1])
    length = math.sqrt(dx * dx + dy * dy)
    if length < 1e-6:
        return 1.0, 0.0
    return dx / length, dy / length


def draw_track(img: Image.Image):
    """Stamp a track cross-section along the bezier.
    Layout perpendicular to curve (from -half_base to +half_base):
        edge | wood | rail | wood gap | rail | wood | edge
        -4     -3    -2    -1 0 1     2     3     4
    """
    px = img.load()
    samples = 480
    half_base = 4
    # First pass: lay down the wood base + rails (centerline of curve)
    for i in range(samples + 1):
        t = i / samples
        cx, cy = curve_point(t)
        tx, ty = curve_tangent(t)
        px_perp, py_perp = -ty, tx
        for offset in range(-half_base, half_base + 1):
            sx = int(round(cx + px_perp * offset))
            sy = int(round(cy + py_perp * offset))
            if not (3 <= sx < NATIVE - 3 and 3 <= sy < NATIVE - 3):
                continue
            abs_off = abs(offset)
            if abs_off == half_base:
                color = TIE_DARK
            elif abs_off == 2:
                color = RAIL
            elif abs_off > 2:
                color = TIE_WOOD
            else:
                color = TIE_WOOD_HI if abs_off == 0 else TIE_WOOD

            px[sx, sy] = color

    # Second pass: perpendicular dark tie bars every N samples
    tie_spacing = 40
    for i in range(tie_spacing // 2, samples + 1, tie_spacing):
        t = i / samples
        cx, cy = curve_point(t)
        tx, ty = curve_tangent(t)
        px_perp, py_perp = -ty, tx
        # Tie bar: 1px wide perpendicular to curve, spans entire base
        for offset in range(-half_base + 1, half_base):
            for along in (-0.4, 0.4):  # 1-pixel-wide tie
                sx = int(round(cx + tx * along + px_perp * offset))
                sy = int(round(cy + ty * along + py_perp * offset))
                if 3 <= sx < NATIVE - 3 and 3 <= sy < NATIVE - 3:
                    abs_off = abs(offset)
                    if abs_off == 2:
                        continue  # don't paint over rails
                    px[sx, sy] = TIE_DARK

    # Third pass: bright top edge on each rail (top of curve = smaller y)
    # Already RAIL color; add highlight pixel above the rail (toward camera-up)
    for i in range(0, samples + 1, 6):
        t = i / samples
        cx, cy = curve_point(t)
        tx, ty = curve_tangent(t)
        px_perp, py_perp = -ty, tx
        for offset in (-2, 2):
            sx = int(round(cx + px_perp * offset))
            sy = int(round(cy + py_perp * offset))
            if 3 <= sx < NATIVE - 3 and 3 <= sy < NATIVE - 3:
                px[sx, sy] = RAIL_HI


def draw_footprint(img: Image.Image, cx: float, cy: float, angle_rad: float):
    """Stamp a chunky boot capsule. Symmetric for L/R clarity at small scale.
    Template (local +y = toe direction):
        .##.    (-1,-3)(0,-3)
        ####    (-2,-2)..(1,-2)
        ####
        ####
        .##.
    Plus 1px outline for contrast.
    """
    px = img.load()
    # Boot capsule (4w x 5h), origin at center
    foot_template = [
        # toes
        (-1, -2), (0, -2),
        # ball
        (-1, -1), (0, -1), (-2, -1), (1, -1),
        # arch
        (-1, 0), (0, 0), (-2, 0), (1, 0),
        # mid
        (-1, 1), (0, 1), (-2, 1), (1, 1),
        # heel
        (-1, 2), (0, 2),
    ]
    cos_a = math.cos(angle_rad)
    sin_a = math.sin(angle_rad)

    foot_pixels = set()
    for (fx, fy) in foot_template:
        rx = cos_a * fx - sin_a * fy
        ry = sin_a * fx + cos_a * fy
        sx = int(round(cx + rx))
        sy = int(round(cy + ry))
        foot_pixels.add((sx, sy))

    # Outline: 1px ring around foot
    outline_pixels = set()
    for (sx, sy) in foot_pixels:
        for (ox, oy) in [(-1, 0), (1, 0), (0, -1), (0, 1), (-1, -1), (1, -1), (-1, 1), (1, 1)]:
            np = (sx + ox, sy + oy)
            if np not in foot_pixels:
                outline_pixels.add(np)

    # Paint outline first (so foot overwrites at borders)
    for (sx, sy) in outline_pixels:
        if 3 <= sx < NATIVE - 3 and 3 <= sy < NATIVE - 3:
            px[sx, sy] = FOOT_OUTLINE
    # Paint foot
    for (sx, sy) in foot_pixels:
        if 3 <= sx < NATIVE - 3 and 3 <= sy < NATIVE - 3:
            px[sx, sy] = FOOT


def draw_footprints(img: Image.Image):
    """Stamp 3 boot prints along the curve, alternating perpendicular offset."""
    t_values = [0.20, 0.50, 0.80]
    sides = [+1, -1, +1]
    for t, side in zip(t_values, sides):
        cx, cy = curve_point(t)
        tx, ty = curve_tangent(t)
        # Foot template's +y is toe; rotate -90 to point along curve tangent
        angle = math.atan2(ty, tx) - math.pi / 2
        perp_x, perp_y = -ty, tx
        offset_dist = 1.0  # slight offset off the centerline for "stepping" look
        fx = cx + perp_x * offset_dist * side
        fy = cy + perp_y * offset_dist * side
        draw_footprint(img, fx, fy, angle)


def main():
    img = Image.new("RGBA", (NATIVE, NATIVE), (0, 0, 0, 255))
    draw = ImageDraw.Draw(img)

    make_background(img, draw)
    make_border(draw)
    draw_track(img)
    draw_footprints(img)

    # Save native + scaled
    native_path = OUT_DIR / "logo_native_64.png"
    scaled_path = OUT_DIR / "logo_256.png"
    img.save(native_path)
    scaled = img.resize((FINAL, FINAL), Image.Resampling.NEAREST)
    scaled.save(scaled_path)
    print(f"Saved {native_path} and {scaled_path}")


if __name__ == "__main__":
    main()
