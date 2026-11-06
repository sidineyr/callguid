"""Capture actual APK screens; fail instead of publishing missing/duplicate states."""
import subprocess,time,xml.etree.ElementTree as ET,re,json, pathlib,hashlib
out=pathlib.Path('docs/assets/screenshots');out.mkdir(parents=True,exist_ok=True)
def adb(*a):return subprocess.check_output(['adb',*a])
def nodes():
 adb('shell','uiautomator','dump','/sdcard/window.xml');return ET.fromstring(adb('shell','cat','/sdcard/window.xml')).iter('node')
def click(pattern):
 for n in nodes():
  if re.search(pattern,(n.get('resource-id','')+' '+n.get('text','')),re.I):
   b=list(map(int,re.findall(r'\d+',n.get('bounds',''))));adb('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2));time.sleep(2);return
 raise RuntimeError('Control not found: '+pattern)
shots=[]
def shot(name,caption):
 time.sleep(2);data=adb('exec-out','screencap','-p');assert data[:8]==b'\x89PNG\r\n\x1a\n';(out/name).write_bytes(data);shots.append({'file':name,'caption':caption})
adb('shell','am','force-stop','com.sidineyr.callguide');adb('shell','monkey','-p','com.sidineyr.callguide','-c','android.intent.category.LAUNCHER','1');time.sleep(5)
shot('01-inicio.png','Página inicial do CallGuide 2.1')
# Match actual accessibility hierarchy, not assumed screen coordinates.
click('practice_button|Começar|Praticar|Iniciar');shot('02-pratica.png','Área de prática no papel digital')
adb('shell','input','swipe','180','500','350','650','600');shot('03-traco.png','Prática com um traço desenhado no emulador')
adb('shell','input','keyevent','4');time.sleep(2)
click('guide_button|Orienta|Guia');shot('04-orientacoes.png','Orientações do aplicativo')
adb('shell','input','swipe','270','800','270','300','600');shot('05-orientacoes.png','Continuação das orientações de uso')
assert len({hashlib.sha256((out/s['file']).read_bytes()).hexdigest() for s in shots})==5,'Duplicate screenshots'
(out/'manifest.json').write_text(json.dumps(shots,ensure_ascii=False))
