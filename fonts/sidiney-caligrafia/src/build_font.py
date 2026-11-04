from PIL import Image,ImageDraw,ImageFont
import unicodedata, json, math, os, argparse
import numpy as np
from pathlib import Path
from fontTools.fontBuilder import FontBuilder
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont
from fontTools.feaLib.builder import addOpenTypeFeaturesFromString
parser=argparse.ArgumentParser(description='Construir a fonte inspirada no gabarito de Sidiney.')
parser.add_argument('--output-dir',type=Path,default=Path(__file__).resolve().parent.parent/'gerado')
parser.add_argument('--label-font',type=Path,help='TTF opcional para as legendas da prévia')
args=parser.parse_args()
args.output_dir.mkdir(parents=True,exist_ok=True)
ROOT=str(args.output_dir.resolve())
glyphs={};metrics={};cmap={};outlines={}
def add(ch,g,width):
 name='uni%04X'%ord(ch);glyphs[name]=g;metrics[name]=(int(width),0);cmap[ord(ch)]=name
 outlines[ch]=(g,int(width));return name
from calligraphy import build_alphabet, poly_symbol
build_alphabet(add)
# All supplemental symbols are drawn with the same slant and rounded pen construction.
def symbol(ch,strokes,w=440):
 return add(ch,poly_symbol(strokes),w)
def line(*p):return (p,18)
def dot(x,y):return ([(x,y),(x+1,y+1)],34)
S={
'.':([dot(95,12)],210), ',':([line((75,-105),(119,12)),dot(119,12)],210),
':':([dot(100,12),dot(100,330)],230),';':([line((75,-105),(119,12)),dot(119,12),dot(119,330)],230),
'!':([line((100,160),(100,690)),dot(100,15)],230),
'?':([line((20,550),(35,640),(100,685),(190,670),(240,615),(225,540),(120,420),(110,220)),dot(110,15)],330),
'-':([line((20,270),(290,270))],360),'_':([line((0,-70),(430,-70))],470),
'+':([line((20,300),(400,300)),line((210,110),(210,490))],460),
'=':([line((20,230),(400,230)),line((20,370),(400,370))],460),
'/':([line((10,-60),(180,740))],280),'\\':([line((80,740),(280,-60))],380),
'(':([line((210,760),(125,665),(65,510),(35,310),(55,110),(110,-80))],250),
')':([line((30,760),(115,665),(175,510),(205,310),(185,110),(130,-80))],260),
'[':([line((220,740),(65,740),(65,-80),(220,-80))],280),
']':([line((40,740),(195,740),(195,-80),(40,-80))],280),
'{':([line((240,740),(150,700),(130,480),(60,340),(130,240),(140,0),(240,-80))],300),
'}':([line((20,740),(110,700),(130,480),(200,340),(130,240),(120,0),(20,-80))],300),
'|':([line((90,-100),(90,760))],210),
"'":([line((70,560),(95,710))],170),'"':([line((65,560),(90,710)),line((180,560),(205,710))],290),
'`':([line((60,740),(160,620))],240),'^':([line((20,530),(150,720),(280,530))],340),
'~':([line((0,540),(55,585),(115,585),(190,530),(245,530),(290,580))],350),
'*':([line((160,380),(160,700)),line((10,460),(310,620)),line((10,620),(310,460))],370),
'#':([line((110,50),(110,680)),line((290,50),(290,680)),line((0,250),(400,250)),line((0,480),(400,480))],470),
'<':([line((360,550),(50,300),(360,50))],450),'>':([line((50,550),(360,300),(50,50))],450),
'%':([line((40,0),(390,700))],520),
'&':([line((430,60),(90,460),(70,590),(120,680),(220,690),(280,620),(250,520),(55,270),(30,130),(100,30),(235,10),(350,100),(440,310))],530),
'@':([line((370,160),(300,170),(300,430),(230,460),(140,420),(90,300),(105,180),(180,140),(280,220),(340,480),(400,480),(390,210),(450,180),(510,260),(540,410),(510,570),(400,670),(230,690),(80,630),(-10,490),(-40,280),(-10,110),(100,0),(280,-30),(430,0))],640),
'$':([line((360,600),(280,690),(130,670),(50,590),(70,470),(300,320),(330,190),(260,65),(120,35),(20,110)),line((200,-70),(200,760))],470),
'€':([line((380,610),(290,700),(150,660),(70,510),(50,310),(95,120),(230,35),(355,100)),line((0,420),(300,420)),line((0,290),(270,290))],470),
'£':([line((20,40),(400,40)),line((70,30),(160,260),(115,510),(130,630),(230,690),(340,650)),line((30,310),(300,310))],470),
'¥':([line((0,700),(190,370),(370,700)),line((190,0),(190,370)),line((25,250),(355,250)),line((25,360),(355,360))],450),
'§':([line((310,640),(200,710),(90,660),(70,570),(300,380),(320,280),(230,230),(90,290),(50,390),(130,460),(300,310),(310,160),(230,60),(100,40),(30,100))],420),
'°':([],260),
}
for ch,(st,w) in S.items():symbol(ch,st,w)
def ellipse_st(cx,cy,rx,ry):return ([ (cx+rx*math.cos(t),cy+ry*math.sin(t)) for t in np.linspace(0,2*math.pi,65)],17)
symbol('°',[ellipse_st(110,605,80,90)],260)
symbol('%',S['%'][0]+[ellipse_st(90,575,65,100),ellipse_st(340,125,65,100)],520)
# space, missing glyph, accents and Unicode compositions
pen=TTGlyphPen(None);glyphs['.notdef']=pen.glyph();metrics['.notdef']=(500,0)
for ch in [' ','\u00a0','\u2002','\u2003','\u2009','\u200b']:add(ch,pen.glyph(),{'\u2003':1000,'\u2002':500,'\u2009':160,'\u200b':0}.get(ch,300))
marks={'\u0301':[line((60,0),(190,130))],'\u0300':[line((70,130),(160,0))],'\u0302':[line((0,0),(90,110),(190,0))],'\u0303':[line((0,20),(50,65),(100,65),(160,10),(210,10),(250,60))],'\u0308':[dot(30,35),dot(180,35)],'\u030a':[ellipse_st(95,70,65,65)],'\u0304':[line((0,45),(210,45))],'\u0306':[line((0,100),(35,20),(100,0),(165,20),(200,100))],'\u0307':[dot(100,35)],'\u030b':[line((0,0),(100,120)),line((130,0),(230,120))],'\u030c':[line((0,110),(90,0),(190,110))],'\u0327':[line((100,0),(50,-65),(130,-85),(125,-140),(25,-160))],'\u0328':[line((130,0),(45,-65),(35,-130),(130,-140))]}
for ch,st in marks.items():symbol(ch,st,0)
# Curved diacritics: smooth pen gestures rather than angular sign polylines.
from calligraphy import glyph as pen_glyph
for code,path in {
 0x301:'M 30 0 C 60 -35 130 -95 175 -120',
 0x300:'M 60 -120 C 90 -95 130 -35 160 0',
 0x302:'M 0 0 C 35 -20 65 -75 95 -105 C 130 -75 160 -25 190 0',
 0x303:'M 0 -25 C 35 -95 80 -90 125 -45 C 170 0 215 5 250 -65',
 0x306:'M 0 -100 C 20 25 180 25 200 -100',
 0x30c:'M 0 -110 C 35 -85 65 -30 95 0 C 130 -30 160 -85 190 -110',
 0x327:'M 100 0 C 80 30 65 50 95 65 C 165 90 110 165 35 140',
 0x328:'M 130 0 C 30 45 5 150 130 135',
}.items():add(chr(code),pen_glyph([(path,16 if code<0x327 else 18)]),0)
def comp(ch,parts,width):
 p=TTGlyphPen(glyphs, handleOverflowingTransforms=False)
 for name,dx,dy,sx,sy in parts:glyphs[name].draw(TransformPen(p,(sx,0,0,sy,dx,dy)),glyphs)
 return add(ch,p.glyph(),width)
for code in list(range(160,384))+[0x1e80,0x1e81,0x1e82,0x1e83,0x1ef2,0x1ef3]:
 ch=chr(code)
 if ch in outlines:continue
 de=unicodedata.normalize('NFD',ch)
 if len(de)>=2 and de[0] in outlines and all(m in marks for m in de[1:]):
  base=de[0];g,w=outlines[base];g.recalcBounds(glyphs);top=g.yMax
  parts=[(cmap[ord(base)],0,0,1,1)]
  for m in de[1:]:
   below=m in '\u0327\u0328'; y=0 if below else max(top+65,500 if base.islower() else 760)
   x=w*.48-95+.7*y
   parts.append((cmap[ord(m)],x,y,1,1))
  comp(ch,parts,w)
# Derived alphabet members not represented independently on the page.
for ch,base in {'Ð':'D','ð':'d','Đ':'D','đ':'d','Ø':'O','ø':'o','Ł':'L','ł':'l','Ŧ':'T','ŧ':'t','Ħ':'H','ħ':'h','ı':'i','ſ':'s','Ŋ':'N','ŋ':'n','ß':'B','ƒ':'f','Þ':'P','þ':'p','ĸ':'k','ŉ':'n','µ':'u'}.items():
 if ch in outlines:continue
 g,w=outlines[base];parts=[(cmap[ord(base)],0,0,1,1)]
 if ch in 'ÐðĐđŁłŦŧĦħ':parts.append((cmap[ord('-')],w*.15,0,1,1))
 if ch in 'Øø':parts.append((cmap[ord('/')],w*.25,0,.7,.85 if ch=='Ø' else .5))
 comp(ch,parts,w)
for ch,left,right in [('Æ','A','E'),('æ','a','e'),('Œ','O','E'),('œ','o','e'),('Ĳ','I','J'),('ĳ','i','j')]:
 w=outlines[left][1];comp(ch,[(cmap[ord(left)],0,0,.8,1),(cmap[ord(right)],w*.65,0,.8,1)],w*.65+outlines[right][1]*.8)
# Typographic punctuation, currency and mathematical symbols.
for ch,base in {'–':'-','—':'-','−':'-','‘':"'",'’':"'",'‚':',','“':'"','”':'"','„':'"','‹':'<','›':'>','«':'<','»':'>','×':'+','÷':':','·':'.','•':'.','†':'+','‡':'+','¬':'-','¦':'|','¤':'°','©':'°','®':'°','™':'T','ª':'a','º':'o','¿':'?','¡':'!','±':'+','≈':'~','≠':'=','≤':'<','≥':'>','∞':'8'}.items():
 sx=2.5 if ch=='—' else 1.7 if ch=='–' else 1
 g,w=outlines[base]
 parts=[(cmap[ord(base)],0,0,sx,1)]
 if ch=='×':
  symbol(ch,[line((30,100),(340,500)),line((30,500),(340,100))],440);continue
 if ch=='÷':symbol(ch,[line((20,300),(380,300)),dot(200,100),dot(200,500)],460);continue
 if ch=='±':parts.append((cmap[ord('-')],0,-280,1,1))
 if ch=='≠':parts.append((cmap[ord('/')],100,0,1,.8))
 if ch in '≤≥':parts.append((cmap[ord('-')],20,-270,1,1))
 if ch=='≈':parts.append((cmap[ord('~')],0,-170,1,1))
 if ch in '·•':parts=[(cmap[ord('.')],0,280,1.4 if ch=='•' else 1,1.4 if ch=='•' else 1)]
 if ch in '¿¡':parts=[(cmap[ord(base)],w+400,700,-1,-1)]
 comp(ch,parts,w*sx)
comp('…',[(cmap[ord('.')],x,0,1,1) for x in (0,190,380)],610)
for ch,base in [('¹','1'),('²','2'),('³','3')]:comp(ch,[(cmap[ord(base)],150,420,.55,.55)],360)
for ch,n,d in [('¼','1','4'),('½','1','2'),('¾','3','4')]:comp(ch,[(cmap[ord(n)],0,370,.52,.52),(cmap[ord('/')],150,0,1,.8),(cmap[ord(d)],300,0,.52,.52)],640)
for ch in ['←','→','↔','↑','↓']:
 if ch in '←→↔':
  st=[line((0,300),(550,300))]
  if ch in '→↔':st.append(line((380,450),(550,300),(380,150)))
  if ch in '←↔':st.append(line((170,450),(0,300),(170,150)))
 else:
  st=[line((180,0),(180,700))];st.append(line((30,530),(180,700),(330,530)) if ch=='↑' else line((30,170),(180,0),(330,170)))
 symbol(ch,st,650 if ch in '←→↔' else 440)
# Soft hyphen and common joining controls.
comp('\u00ad',[(cmap[ord('-')],0,0,1,1)],outlines['-'][1])

# Use original outlines for typographic signs; omit unsupported alphabet extensions.
for ch in 'ßÞþðŋŊĸſı':
 name=cmap.pop(ord(ch),None)
 if name: glyphs.pop(name,None); metrics.pop(name,None); outlines.pop(ch,None)
symbol('©',[ellipse_st(300,350,280,365)],650)
comp('©',[(cmap[ord('©')],0,0,1,1),(cmap[ord('C')],120,125,.48,.58)],650)
symbol('®',[ellipse_st(300,350,280,365)],650)
comp('®',[(cmap[ord('®')],0,0,1,1),(cmap[ord('R')],100,125,.48,.58)],650)
comp('™',[(cmap[ord('T')],200,430,.35,.35),(cmap[ord('M')],430,430,.35,.35)],720)
for ch,base in [('«','<'),('»','>')]:comp(ch,[(cmap[ord(base)],0,0,.7,1),(cmap[ord(base)],220,0,.7,1)],650)
symbol('†',[line((180,-100),(180,700)),line((20,470),(340,470))],420)
symbol('‡',[line((180,-100),(180,700)),line((20,470),(340,470)),line((20,230),(340,230))],420)
symbol('¬',[line((20,350),(380,350),(380,150))],460)
symbol('¦',[line((100,-80),(100,270)),line((100,390),(100,740))],240)
symbol('¤',[ellipse_st(200,350,135,140),line((0,550),(65,490)),line((335,490),(400,550)),line((0,150),(65,210)),line((335,210),(400,150))],470)
fb=FontBuilder(1000,isTTF=True)
order=['.notdef']+[n for n in glyphs if n!='.notdef'];fb.setupGlyphOrder(order);fb.setupCharacterMap(cmap)
# Derive the actual left bearing to avoid phantom offsets in renderers.
for name,g in glyphs.items():
 g.recalcBounds(glyphs);metrics[name]=(metrics[name][0],getattr(g,'xMin',0))
fb.setupGlyf(glyphs);fb.setupHorizontalMetrics(metrics)
fb.setupHorizontalHeader(ascent=1200,descent=-450,lineGap=100)
fb.setupNameTable({'familyName':'Sidiney Caligrafia Refinada','styleName':'Regular','uniqueFontIdentifier':'SidineyCaligrafia-2.001','fullName':'Sidiney Caligrafia Refinada','psName':'SidineyCaligrafia-Refinada','version':'Version 2.001','copyright':'Caligrafia pessoal fornecida pelo usuario. Digitalizacao e sinais complementares, 2026.','description':'Fonte pessoal derivada de amostra manuscrita. Letras e algarismos redesenhados por percursos de pena a partir do gabarito; acentos e simbolos reconstruidos.'})
fb.setupOS2(sTypoAscender=1200,sTypoDescender=-450,sTypoLineGap=100,usWinAscent=1500,usWinDescent=550,sxHeight=380,sCapHeight=700,fsSelection=64)
fb.setupPost(italicAngle=-35);fb.setupMaxp()
fb.save(ROOT+'/SidineyCaligrafia-Refinada.ttf')
font=TTFont(ROOT+'/SidineyCaligrafia-Refinada.ttf')
features='languagesystem DFLT dflt; languagesystem latn dflt; feature kern {\n'
for a,b,k in [('A','V',-90),('A','W',-80),('T','a',-65),('T','o',-60),('V','a',-65),('W','a',-40),('P','a',-45),('L','T',-25)]:features+=f'pos {cmap[ord(a)]} {cmap[ord(b)]} {k};\n'
features+='} kern;';addOpenTypeFeaturesFromString(font,features);font.save(ROOT+'/SidineyCaligrafia-Refinada.ttf')
json.dump({'characters':len(cmap),'glyphs':len(glyphs),'coverage':''.join(chr(c) for c in sorted(cmap))},open(ROOT+'/coverage.json','w',encoding='utf8'),ensure_ascii=False,indent=2)
print('Characters',len(cmap),'glyphs',len(glyphs))
# Render a proof through FreeType/Pillow, exercising the real TTF.
im=Image.new('RGB',(1800,1700),'#faf7f0');d=ImageDraw.Draw(im)
candidates=[args.label_font,Path('C:/Windows/Fonts/arial.ttf'),Path('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf')]
sans=next((p for p in candidates if p and p.is_file()),None)
def label_font(size):
 return ImageFont.truetype(str(sans),size) if sans else ImageFont.load_default(size=size)
d.text((95,55),'SIDINEY CALIGRAFIA — REFINADA',font=label_font(31),fill='#755a42')
fp=ROOT+'/SidineyCaligrafia-Refinada.ttf'
lines=[('A palavra guarda o gesto.',77),('Sidiney Rodrigues',80),('ABCDEFGHIJKLMNOPQRSTUVWXYZ',44),('abcdefghijklmnopqrstuvwxyz',65),('0123456789',77),('Á À Â Ã É Ê Í Ó Ô Õ Ú Ü Ç',56),('á à â ã é ê í ó ô õ ú ü ç',65),('Poesia, café e coração.',77),('A educação transforma vidas.',72),('! ? . , : ; “ ” ( ) [ ] { }',59),('@ # & % + − = / € $ R$ £ ¥',54),('© ® ™ ° ½ ² × ÷ ≠ ≤ ≥ →',52)]
y=120
for text,size in lines:
 f=ImageFont.truetype(fp,size)
 while d.textlength(text,font=f)>1580:size-=1;f=ImageFont.truetype(fp,size)
 d.text((100,y),text,font=f,fill='#233b67',anchor='lt');y+=123
small=label_font(22)
d.text((100,1625),'Contornos redesenhados • Pressão variável • Curvas e terminais arredondados',font=small,fill='#755a42')
im.save(ROOT+'/Previa-Caligrafia-Refinada.png')
