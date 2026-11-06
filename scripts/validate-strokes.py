"""Validate complete, finite, nonempty cubic pen trajectories without Android tooling."""
import json
import math
import re
import statistics
from pathlib import Path

root = Path(__file__).resolve().parents[1]
letters = json.loads((root / 'app/src/main/assets/letters.json').read_text(encoding='utf-8'))
assert set('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZáàâãéêíóôõúüçÁÀÂÃÉÊÍÓÔÕÚÜÇ0123456789').issubset(letters)
count = 0
up, down = [], []
for char, letter in letters.items():
    assert letter['advance'] > 0, char
    assert letter['strokes'], char
    assert len(letter['penStrokes']) == len(letter['strokes']), char
    for samples in letter['penStrokes']:
        assert len(samples) >= 2
        assert samples[0][7] == 0
        assert all(len(p) == 8 and all(math.isfinite(v) for v in p) and p[6] > 0 for p in samples)
        assert all(b[7] > a[7] for a,b in zip(samples,samples[1:])), char
        for i,p in enumerate(samples[1:-1], 1):
            if p[7] < 50 or samples[-1][7]-p[7] < 50: continue
            before,after = samples[i-1],samples[i+1]
            dx = after[0]-before[0]+.67*(after[1]-before[1])
            dy = after[1]-before[1]
            ty = dy/math.hypot(dx,dy)
            if ty > .8: down.append(p[6])
            elif ty < -.8: up.append(p[6])
    for path in letter['strokes']:
        tokens = re.findall(r'[A-Za-z]|[-+]?(?:\d*\.)?\d+', path)
        assert tokens[0] == 'M', (char, path)
        cursor = 0
        while cursor < len(tokens):
            command = tokens[cursor]
            n = {'M': 2, 'L': 2, 'C': 6}[command]
            coords = [float(v) for v in tokens[cursor + 1:cursor + 1 + n]]
            assert len(coords) == n and all(math.isfinite(v) for v in coords), char
            cursor += n + 1
        assert cursor == len(tokens)
        count += 1
print(f'PASS: all {len(letters)} glyphs, {count} ordered strokes, supported commands and finite coordinates')
ratio = statistics.median(down)/statistics.median(up)
assert ratio > 2.5, ratio
print(f'PASS: median strong-downstroke/hairline-upstroke pressure ratio {ratio:.2f}:1')
