#!/usr/bin/env python3
"""Render the original CallGuide vector motif with Pillow, without external assets.

Run from any directory:
    python scripts/render-store-icon.py
    python scripts/render-store-icon.py --output-dir /absolute/path/to/store

Writes an opaque 512px Play Store PNG and replaces legacy launcher WEBPs under
app/src/main/res/mipmap-*. Adaptive-icon XML remains the source of truth for the
108-unit motif. No fonts or non-Pillow drawing libraries are required.
"""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw


GREEN = "#1B493D"
IVORY = "#FAF7ED"
GOLD = "#B77D31"
ROOT = Path(__file__).resolve().parents[1]


def cubic(start, control1, control2, end, steps=64):
    """Deterministically sample one cubic Bézier segment in viewport units."""
    return [
        tuple(
            (1 - t) ** 3 * start[i]
            + 3 * (1 - t) ** 2 * t * control1[i]
            + 3 * (1 - t) * t**2 * control2[i]
            + t**3 * end[i]
            for i in (0, 1)
        )
        for t in (step / steps for step in range(steps + 1))
    ]


def render(size: int, round_mask: bool = False) -> Image.Image:
    # Supersampling preserves the small gold slit on mdpi launcher assets.
    factor = 4
    side = size * factor
    # Flatten the central 72 units of the adaptive icon's 108-unit layer.
    # Android reserves the surrounding 18 units for adaptive-mask movement.
    scale = side / 72
    image = Image.new("RGB", (side, side), GREEN)
    draw = ImageDraw.Draw(image)

    def points(values):
        return [((x - 18) * scale, (y - 18) * scale) for x, y in values]

    def rounded_line(values, color, width):
        scaled = points(values)
        line_width = max(1, round(width * scale))
        draw.line(scaled, fill=color, width=line_width)
        radius = width * scale / 2
        for x, y in (scaled[0], scaled[-1]):
            draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=color)

    nib = cubic((33, 75), (32, 51), (47, 34), (75, 33))
    nib += cubic((75, 33), (74, 59), (57, 75), (33, 75))[1:]
    draw.polygon(points(nib), fill=IVORY)
    rounded_line([(33, 75), (55, 53)], GOLD, 2.5)
    draw.ellipse(tuple((v - 18) * scale for v in (52.2, 50.2, 57.8, 55.8)), fill=GREEN)

    leaf = cubic((34, 49), (26, 44), (27, 35), (34, 31))
    leaf += cubic((34, 31), (41, 37), (42, 45), (34, 49))[1:]
    draw.polygon(points(leaf), fill=GOLD)
    rounded_line([(34, 48), (33, 38)], GREEN, 1.7)

    if round_mask:
        image = image.convert("RGBA")
        alpha = Image.new("L", (side, side), 0)
        ImageDraw.Draw(alpha).ellipse((0, 0, side - 1, side - 1), fill=255)
        image.putalpha(alpha)
    return image.resize((size, size), Image.Resampling.LANCZOS)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-dir", type=Path, default=ROOT / "store")
    args = parser.parse_args()
    args.output_dir.mkdir(parents=True, exist_ok=True)
    store_icon = args.output_dir / "icon-512.png"
    render(512).save(store_icon, format="PNG", optimize=True)
    print(store_icon.resolve())

    for density, size in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        directory = ROOT / "app" / "src" / "main" / "res" / f"mipmap-{density}"
        directory.mkdir(parents=True, exist_ok=True)
        for name, round_mask in (("ic_launcher", False), ("ic_launcher_round", True)):
            output = directory / f"{name}.webp"
            render(size, round_mask).save(output, format="WEBP", lossless=True, method=6)
            print(output.resolve())


if __name__ == "__main__":
    main()
