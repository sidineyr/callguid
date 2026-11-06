#!/usr/bin/env python3
"""Create CallGuide's opaque 1024 x 500 Play feature graphic with Pillow.

This is an original brand composition, not an Android screenshot. It shows the
same variable-pressure letter geometry as the app, without duplicating its icon.

Example:
    python scripts/render-feature-graphic.py

Requires Pillow and the Georgia / Segoe UI fonts supplied with Windows.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
GREEN = "#1B493D"
IVORY = "#FAF7ED"
GOLD = "#B77D31"
WIDTH, HEIGHT = 1024, 500
SCALE = 3


def render(font_dir: Path, strokes_path: Path) -> Image.Image:
    image = Image.new("RGB", (WIDTH * SCALE, HEIGHT * SCALE), IVORY)
    draw = ImageDraw.Draw(image)

    def font(name: str, size: int) -> ImageFont.FreeTypeFont:
        return ImageFont.truetype(str(font_dir / name), size * SCALE)

    def box(values):
        return tuple(round(value * SCALE) for value in values)

    def text(x, y, value, face, color):
        draw.text((x * SCALE, y * SCALE), value, font=face, fill=color, anchor="lt")

    # Fixed margins place all essential content well inside the store image.
    draw.rounded_rectangle(box((644, 48, 968, 452)), radius=32 * SCALE, fill=GREEN)
    draw.rounded_rectangle(box((72, 79, 96, 83)), radius=2 * SCALE, fill=GOLD)
    text(108, 72, "CALIGRAFIA EM MOVIMENTO", font("segoeuib.ttf", 16), GREEN)
    text(68, 139, "CallGuide", font("georgia.ttf", 80), GREEN)
    text(74, 267, "Cada letra,", font("segoeuil.ttf", 31), GREEN)
    text(74, 309, "um gesto bonito.", font("segoeuil.ttf", 31), GREEN)

    # Small editorial rule: a quiet echo of an ink line, not a UI affordance.
    draw.line(box((75, 392, 313, 392)), fill=GOLD, width=2 * SCALE)
    draw.ellipse(box((323, 389, 329, 395)), fill=GOLD)

    # Show the product's actual ink contrast instead of enlarging the app icon.
    strokes = json.loads(strokes_path.read_text(encoding="utf-8"))["a"]["penStrokes"]
    points = [point for stroke in strokes for point in stroke]
    left = min(min(p[2], p[4]) for p in points)
    right = max(max(p[2], p[4]) for p in points)
    top = min(min(p[3], p[5]) for p in points)
    bottom = max(max(p[3], p[5]) for p in points)
    scale = min(250 / (right-left), 260 / (bottom-top))
    x0 = 806 - (left+right)*scale/2
    y0 = 250 - (top+bottom)*scale/2
    def point(x, y):
        return ((x0+x*scale)*SCALE, (y0+y*scale)*SCALE)
    draw.line(box((684, 356, 928, 356)), fill="#386456", width=SCALE)
    for stroke in strokes:
        polygon = [point(p[2],p[3]) for p in stroke]
        polygon += [point(p[4],p[5]) for p in reversed(stroke)]
        draw.polygon(polygon, fill=IVORY)
    text(701, 391, "O movimento da escrita", font("segoeui.ttf", 16), IVORY)
    return image.resize((WIDTH, HEIGHT), Image.Resampling.LANCZOS)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--font-dir", type=Path, default=Path("C:/Windows/Fonts"))
    parser.add_argument("--strokes", type=Path, default=ROOT / "app/src/main/assets/letters.json")
    parser.add_argument("--output", type=Path, default=ROOT / "store" / "feature-graphic-1024x500.png")
    parser.add_argument(
        "--deliverable", type=Path,
        default=ROOT.parents[1] / "outputs" / "CallGuide-destaque-1024x500.png",
    )
    args = parser.parse_args()
    image = render(args.font_dir, args.strokes)
    assert image.size == (1024, 500) and image.mode == "RGB"
    for path in (args.output, args.deliverable):
        path.parent.mkdir(parents=True, exist_ok=True)
        image.save(path, format="PNG", optimize=True)
        with Image.open(path) as saved:
            assert saved.size == (1024, 500)
            assert saved.mode == "RGB" and "transparency" not in saved.info
        print(path.resolve())


if __name__ == "__main__":
    main()
