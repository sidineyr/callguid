const letters = LETTERS;
const $ = id => document.getElementById(id), ns = 'http://www.w3.org/2000/svg';
let upper = false, current = 'a', running = false, start = 0, elapsed = 0;
let raf = 0, returnTimer = 0, segments = [], total = 0, pen;
function screen(id) {
  cancelAnimationFrame(raf); clearTimeout(returnTimer); running = false;
  for (const x of ['welcome', 'alphabet', 'lesson']) $(x).hidden = x !== id;
}
function keyboard() {
  screen('alphabet'); $('keys').replaceChildren();
  for (const c of 'abcdefghijklmnopqrstuvwxyz') {
    const label = upper ? c.toUpperCase() : c, b = document.createElement('button');
    b.textContent = label; b.setAttribute('aria-label', 'Ver traçado da letra ' + label);
    b.onclick = () => play(label); $('keys').append(b);
  }
  $('case').textContent = upper ? 'Maiúsculas · mudar para minúsculas' : 'Minúsculas · mudar para maiúsculas';
}
function node(name, attrs) {
  const e = document.createElementNS(ns, name);
  for (const [k,v] of Object.entries(attrs)) e.setAttribute(k,v);
  return e;
}
function envelope(points) {
  if (points.length < 2) return '';
  let d = 'M' + points[0][2] + ' ' + points[0][3];
  for (const p of points.slice(1)) d += 'L' + p[2] + ' ' + p[3];
  const end = points[points.length-1], begin = points[0];
  d += 'Q' + end[0] + ' ' + end[1] + ' ' + end[4] + ' ' + end[5];
  for (const p of points.slice(0,-1).reverse()) d += 'L' + p[4] + ' ' + p[5];
  return d + 'Q' + begin[0] + ' ' + begin[1] + ' ' + begin[2] + ' ' + begin[3] + 'Z';
}
function prefix(stroke, distance) {
  if (distance >= stroke[stroke.length-1][7]) return stroke;
  const points = [];
  for (let i = 0; i < stroke.length; i++) {
    const p = stroke[i];
    if (p[7] <= distance) points.push(p);
    else {
      if (i > 0) {
        const a = stroke[i-1], t = (distance-a[7])/(p[7]-a[7]);
        points.push(a.map((v,k) => v+(p[k]-v)*t));
      }
      break;
    }
  }
  return points;
}
function play(c) {
  screen('lesson'); current = c; $('title').textContent = 'O caminho da letra ' + c;
  $('status').textContent = '↓ Descida: mais pressão · ↑ Subida: traço leve';
  $('pause').textContent = 'Pausar';
  const svg = $('drawing'); svg.replaceChildren(); segments = []; total = 0;
  const xs = [], ys = [];
  for (const points of letters[c].penStrokes) {
    svg.append(node('path', {d:envelope(points), fill:'#e4e6d8'}));
    const path = node('path', {fill:'#1b493d'}); svg.append(path);
    const length = points[points.length-1][7];
    segments.push({path,points,length}); total += length;
    for (const p of points) { xs.push(p[2],p[4]); ys.push(p[3],p[5]); }
  }
  const left = Math.min(...xs)-50, top = Math.min(...ys)-170;
  svg.setAttribute('viewBox', [left,top,Math.max(...xs)-left+50,Math.max(...ys)-top+50].join(' '));
  pen = node('g', {});
  pen.append(node('path', {d:'M0 0 L-13 -46 L13 -46 Z',fill:'#b77d31'}),
    node('rect', {x:-11,y:-160,width:22,height:119,rx:7,fill:'#1b493d'}),
    node('path', {d:'M0 -8 L0 -42',stroke:'#f5d9a3','stroke-width':2}));
  svg.append(pen); elapsed = 0; start = performance.now(); running = true;
  raf = requestAnimationFrame(tick);
}
function tick(t) {
  if (!running) return;
  elapsed = t-start; const fraction = Math.min(1,elapsed/4200);
  let left = total*fraction, last;
  for (const s of segments) {
    const amount = Math.min(s.length, Math.max(0,left));
    const points = prefix(s.points,amount); s.path.setAttribute('d',envelope(points));
    if (amount > 0 && points.length) last = points[points.length-1];
    left -= s.length;
  }
  if (last) pen.setAttribute('transform','translate('+last[0]+' '+last[1]+') rotate(-38)');
  pen.style.visibility = fraction > 0 && fraction < 1 ? 'visible' : 'hidden';
  if (fraction < 1) raf = requestAnimationFrame(tick);
  else {
    running = false; $('status').textContent = 'Traçado concluído · descidas espessas, subidas leves.';
    if ($('auto').checked) returnTimer = setTimeout(keyboard,1300);
  }
}
$('start').onclick = keyboard;
$('case').onclick = () => { upper = !upper; keyboard(); };
$('back').onclick = keyboard;
$('repeat').onclick = () => play(current);
$('pause').onclick = () => {
  clearTimeout(returnTimer); if (elapsed >= 4200) return;
  if (running) { running=false; cancelAnimationFrame(raf); $('pause').textContent='Continuar'; }
  else { running=true; start=performance.now()-elapsed; $('pause').textContent='Pausar'; raf=requestAnimationFrame(tick); }
};
document.addEventListener('keydown',e => {
  if (!$('alphabet').hidden && /^[a-z]$/i.test(e.key) && !e.ctrlKey && !e.altKey)
    play(upper ? e.key.toUpperCase() : e.key.toLowerCase());
});
document.addEventListener('visibilitychange',() => {
  if (document.hidden) { cancelAnimationFrame(raf); clearTimeout(returnTimer); running=false; $('pause').textContent='Continuar'; }
});
