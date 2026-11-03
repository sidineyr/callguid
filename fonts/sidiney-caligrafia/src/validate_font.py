"""Validate a generated or published TrueType font without installing it."""
import argparse
from pathlib import Path
from fontTools.ttLib import TTFont
from PIL import ImageFont

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('font',type=Path)
    args=parser.parse_args()
    font=TTFont(args.font,checkChecksums=2)
    cmap=font.getBestCmap()
    required='ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789ÁÀÂÃÉÊÍÓÔÕÚÜÇáàâãéêíóôõúüç.,;:!?@#$%&*()[]{}+-=/€'
    missing=set(map(ord,required))-set(cmap)
    if missing:raise ValueError('Missing characters: '+repr(sorted(missing)))
    for name in font.getGlyphOrder():font['glyf'][name].getCoordinates(font['glyf'])
    if not all(width>=0 for width,_ in font['hmtx'].metrics.values()):raise ValueError('Negative advance width')
    rendered=ImageFont.truetype(str(args.font),64)
    for phrase in ['A educação transforma vidas.','Poesia, café e coração.','0123456789 — R$ 123,45']:
        if rendered.getlength(phrase)<=0:raise ValueError('Empty rendered phrase')
    print(f'OK: {len(cmap)} characters; glyphs, metrics, Portuguese coverage and rendering validated.')

if __name__=='__main__':main()
