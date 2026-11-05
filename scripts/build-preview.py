"""Build a self-contained offline visual review of the Android pen trajectories."""
import base64
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
letters = (root / 'app/src/main/assets/letters.json').read_text(encoding='utf-8')
art = base64.b64encode((root / 'app/src/main/res/drawable-nodpi/welcome_prince.png').read_bytes()).decode()
html = '''<!doctype html><html lang="pt-BR"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>CallGuide · prévia das letras</title>
<style>*{box-sizing:border-box}body{margin:0;background:#e9ecdf;color:#1b493d;font-family:system-ui}main{max-width:760px;margin:auto;background:#faf7ed;min-height:100vh;padding:28px}h1{font:700 clamp(32px,6vw,52px) Georgia,serif;letter-spacing:-.035em}p{line-height:1.65}button{min-height:48px;padding:10px 14px;border:1px solid #bbc6b2;border-radius:12px;background:#e9ecdf;color:#1b493d;font:inherit;cursor:pointer}button:focus-visible{outline:3px solid #ae672a;outline-offset:3px}.primary{background:#1b493d;color:white}.grid{display:grid;grid-template-columns:repeat(6,1fr);gap:8px;margin:20px 0}.grid button{font-size:26px}.eyebrow{font-size:12px;letter-spacing:.15em}.hero{width:100%;height:340px;object-fit:contain}.toolbar{display:flex;gap:8px;flex-wrap:wrap;margin:18px 0}svg{width:100%;height:340px;display:block;background:#fcfaf2;border-radius:16px}.notice{font-size:13px;color:#5f715c}section[hidden]{display:none}#status{min-height:28px}.info{border-top:1px solid #ccd3bf;margin-top:28px;padding-top:12px}</style>
<main><section id="welcome"><p class="eyebrow">CALLGUIDE / ATELIÊ DE LETRAS</p><img class="hero" src="data:image/png;base64,ART" alt="Pequeno príncipe indígena imaginário com uma caneta e um caderno"><h1>Cada letra, um gesto bonito.</h1><p>Observe a caneta, descubra os caminhos da letra cursiva e encontre o seu próprio traço.</p><button class="primary" id="start">Vamos escrever</button><p class="notice">Prévia para revisar as animações. Não exibe anúncios nem representa um APK.</p></section>
<section id="alphabet" hidden><p class="eyebrow">52 DEMONSTRAÇÕES · CONTEÚDO OFFLINE</p><h1>Escolha uma letra</h1><p>A caneta percorre os traços originais em cerca de quatro segundos.</p><button id="case">Minúsculas · mudar para maiúsculas</button><div id="keys" class="grid" aria-label="Teclado do alfabeto"></div><label><input id="auto" type="checkbox" checked> Voltar automaticamente ao alfabeto</label><p class="info notice">Use também as letras do teclado físico. Letras de referência: Sidiney Caligrafia Refinada.</p></section>
<section id="lesson" hidden><p class="eyebrow">OBSERVE O GESTO</p><h1 id="title"></h1><svg id="drawing" role="img" aria-label="Traçado cursivo com caneta" viewBox="-200 -800 1000 1200"></svg><p id="status" aria-live="polite">Escrevendo…</p><div class="toolbar"><button id="pause">Pausar</button><button id="repeat">Repetir</button><button id="back" class="primary">Escolher outra letra</button></div></section></main>
<script>PRESSURE_JS</script></html>'''
pressure_js = (root / 'scripts/pressure-preview.js').read_text(encoding='utf-8')
html = html.replace('PRESSURE_JS', pressure_js)
html = html.replace('LETTERS', letters).replace('ART', art)
target = root / 'preview/index.html'
target.parent.mkdir(exist_ok=True)
target.write_text(html, encoding='utf-8')
print('Created self-contained preview:', target)
