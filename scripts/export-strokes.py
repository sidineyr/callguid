"""Export the project's hand-authored pen paths without importing font build dependencies."""
import ast
import json
import math
import re
from pathlib import Path

def sample(path):
    tokens = re.findall(r'[MLC]|[-+]?(?:\d*\.)?\d+', path)
    cursor, current, points = 0, None, []
    while cursor < len(tokens):
        command = tokens[cursor]; cursor += 1
        size = {'M': 2, 'L': 2, 'C': 6}[command]
        values = list(map(float, tokens[cursor:cursor + size])); cursor += size
        if command == 'M':
            current = tuple(values); points.append(current)
        elif command == 'L':
            end = tuple(values)
            n = max(3, int(math.dist(current, end) / 4))
            points.extend(tuple(current[k] + (end[k] - current[k]) * i / (n-1) for k in range(2)) for i in range(1, n))
            current = end
        else:
            a, b, c, d = current, values[:2], values[2:4], values[4:]
            n = max(8, int((math.dist(a,b) + math.dist(b,c) + math.dist(c,d)) / 4))
            for i in range(1, n):
                t = i / (n-1)
                points.append(tuple((1-t)**3*a[k]+3*(1-t)**2*t*b[k]+3*(1-t)*t*t*c[k]+t**3*d[k] for k in range(2)))
            current = tuple(d)
    return [p for i,p in enumerate(points) if i == 0 or math.dist(p, points[i-1]) > .01]

def pen_geometry(path, constant=None):
    points = sample(path)
    arc = [0.0]
    for a,b in zip(points, points[1:]): arc.append(arc[-1] + math.dist(a,b))
    tangents, widths = [], []
    for i, p in enumerate(points):
        before, after = points[max(0,i-1)], points[min(len(points)-1,i+1)]
        dx,dy = after[0]-before[0], after[1]-before[1]
        length = math.hypot(dx,dy)
        tx,ty = dx/length,dy/length
        tangents.append((tx,ty))
        load = max(0, min(1, (ty+.15)/1.15))
        load = load*load*(3-2*load)
        widths.append(constant if constant is not None else 12 + 36*load)
    # Gaussian smoothing across the nib envelope: no abrupt width changes at turns.
    kernel = [math.exp(-j*j/8) for j in range(-6,7)]
    smooth = [sum(widths[min(len(widths)-1,max(0,i+j))]*w for j,w in zip(range(-6,7),kernel))/sum(kernel) for i in range(len(widths))]
    result, distance = [], 0.0
    for i, ((x,y),(tx,ty),width) in enumerate(zip(points,tangents,smooth)):
        taper = .58 + .42*min(1, min(arc[i],arc[-1]-arc[i])/35)
        width *= taper
        nx,ny = -ty,tx
        lx,ly = x+nx*width/2, y+ny*width/2
        rx,ry = x-nx*width/2, y-ny*width/2
        # Apply the same slant to both edges, preserving the actual pen envelope.
        center = (x-.67*y,y)
        if result: distance += math.dist(center, result[-1][:2])
        result.append([round(v,3) for v in (*center,lx-.67*ly,ly,rx-.67*ry,ry,width,distance)])
    return result

root = Path(__file__).resolve().parents[1]
tree = ast.parse((root / 'fonts/sidiney-caligrafia/src/calligraphy.py').read_text(encoding='utf-8'))
letters = {}
for node in tree.body:
    if isinstance(node, ast.Assign) and any(isinstance(t, ast.Name) and t.id in ('LOWER', 'UPPER') for t in node.targets):
        for letter, (advance, paths) in ast.literal_eval(node.value).items():
            letters[letter] = {
                'advance': advance,
                'strokes': [p[0] if isinstance(p, tuple) else p for p in paths],
                'penStrokes': [pen_geometry(p[0], p[1]) if isinstance(p,tuple) else pen_geometry(p) for p in paths]
            }
assert set(letters) == set('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ')

# Portuguese diacritics use real animated pen paths, not a fallback system font.
import unicodedata
for decorated in 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ':
    base, mark = unicodedata.normalize('NFD', decorated)
    source = letters[base]
    strokes = list(source['strokes'])
    if base == 'i': strokes = strokes[:-1]  # accented i has no separate dot
    y = -465 if base.islower() else -820
    if mark == '\u0301': accents = [f'M 110 {y} L 180 {y-100}']
    elif mark == '\u0300': accents = [f'M 190 {y} L 110 {y-100}']
    elif mark == '\u0302': accents = [f'M 75 {y} L 140 {y-90} L 205 {y}']
    elif mark == '\u0303': accents = [f'M 55 {y} C 105 {y-90} 155 {y+55} 220 {y-35}']
    elif mark == '\u0308': accents = [f'M 95 {y} L 96 {y-10}', f'M 195 {y} L 196 {y-10}']
    elif mark == '\u0327': accents = ['M 120 20 C 60 80 180 90 120 150 C 90 180 45 155 35 145']
    else: raise ValueError(mark)
    paths = strokes + accents
    letters[decorated] = {'advance': source['advance'], 'strokes': paths,
        'penStrokes': [pen_geometry(p) for p in strokes] + [pen_geometry(p, 18) for p in accents]}

# Numerals are useful for names, short phrases and classroom exercises.
for node in tree.body:
    if isinstance(node, ast.Assign) and any(isinstance(t, ast.Name) and t.id == 'NUMBERS' for t in node.targets):
        for char, (advance, paths) in ast.literal_eval(node.value).items():
            letters[char] = {'advance': advance,
                'strokes': [p[0] if isinstance(p,tuple) else p for p in paths],
                'penStrokes': [pen_geometry(p[0],p[1]) if isinstance(p,tuple) else pen_geometry(p) for p in paths]}
for char, path in {'.': 'M 70 -10 L 72 -20', ',':'M 90 -20 C 100 30 75 80 35 100',
                   '-':'M 20 -200 L 190 -200', "'":'M 130 -500 L 90 -390'}.items():
    letters[char] = {'advance': 230, 'strokes':[path], 'penStrokes':[pen_geometry(path,18)]}
target = root / 'app/src/main/assets/letters.json'
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text(json.dumps(letters, ensure_ascii=False, separators=(',',':')) + '\n', encoding='utf-8')
print(f'Exported {len(letters)} letters to {target}')
