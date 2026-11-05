"""Render pressure contact sheets and optional GIFs from the exact Android meshes.

Requires Pillow. Outputs are review artifacts, not source assets used by the app.
"""
import argparse
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--output', type=Path, default=root / 'preview/pressure')
parser.add_argument('--gifs', action='store_true')
args = parser.parse_args()
args.output.mkdir(parents=True, exist_ok=True)
letters = json.loads((root / 'app/src/main/assets/letters.json').read_text(encoding='utf-8'))

def prefix(stroke, distance):
    if distance >= stroke[-1][7]: return stroke
    result = []
    for i,p in enumerate(stroke):
        if p[7] <= distance: result.append(p)
        else:
            if i:
                a = stroke[i-1]; t = (distance-a[7])/(p[7]-a[7])
                result.append([x+(y-x)*t for x,y in zip(a,p)])
            break
    return result

def render(char, progress=1, size=400):
    # Supersample for smooth thin hairlines and curved envelopes.
    ss = 2
    im = Image.new('RGB',(size*ss,size*ss),'#faf7ed'); draw = ImageDraw.Draw(im)
    strokes = letters[char]['penStrokes']; points = sum(strokes,[])
    left = min(min(p[2],p[4]) for p in points)-120
    right = max(max(p[2],p[4]) for p in points)+45
    top = min(min(p[3],p[5]) for p in points)-165
    bottom = max(max(p[3],p[5]) for p in points)+45
    scale = min((size-36)/(right-left),(size-64)/(bottom-top))*ss
    dx = size*ss/2-(left+right)*scale/2
    dy = (size+28)*ss/2-(top+bottom)*scale/2
    def xy(x,y): return (x*scale+dx,y*scale+dy)
    def mesh(points,fill):
        if len(points)<2:return
        polygon = [xy(p[2],p[3]) for p in points]+[xy(p[4],p[5]) for p in reversed(points)]
        draw.polygon(polygon,fill=fill)
    for stroke in strokes:mesh(stroke,'#e4e6d8')
    remaining = sum(s[-1][7] for s in strokes)*progress
    tip = None
    for stroke in strokes:
        amount = min(stroke[-1][7],max(0,remaining)); part = prefix(stroke,amount)
        if amount>0 and len(part)>1:mesh(part,'#1b493d');tip=part[-1]
        remaining -= stroke[-1][7]
    if tip and 0<progress<1:
        angle = math.radians(-38)
        def penpoint(x,y):
            return xy(tip[0]+x*math.cos(angle)-y*math.sin(angle),tip[1]+x*math.sin(angle)+y*math.cos(angle))
        draw.polygon([penpoint(0,0),penpoint(-13,-46),penpoint(13,-46)],fill='#b77d31')
        draw.line([penpoint(0,-45),penpoint(0,-155)],fill='#1b493d',width=max(2,int(22*scale)))
    font = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',26*ss)
    draw.text((14*ss,7*ss),char,fill='#1b493d',font=font)
    return im.resize((size,size),Image.Resampling.LANCZOS)

sheet = Image.new('RGB',(2600,1100),'#faf7ed')
for i,char in enumerate('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'):
    sheet.paste(render(char,size=200),((i%13)*200,50+(i//13)*260))
d = ImageDraw.Draw(sheet)
font = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',27)
d.text((20,8),'CallGuide | Pressão variável: descidas espessas, subidas leves',font=font,fill='#1b493d')
sheet.save(args.output/'alfabeto-pressao.png')
if args.gifs:
    for i,char in enumerate('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'):
        frames = [render(char, min(1,frame/42),320) for frame in range(49)]
        name = ('minuscula-' if char.islower() else 'maiuscula-')+char.lower()+'.gif'
        frames[0].save(args.output/name,save_all=True,append_images=frames[1:],duration=100,loop=0,optimize=True)
        print(f'{i+1}/52 {name}',flush=True)
print(args.output,flush=True)
