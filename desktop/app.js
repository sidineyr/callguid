'use strict';
const $=id=>document.getElementById(id), G=window.CallGuideGeometry;
const ns='http://www.w3.org/2000/svg';
let drawing, segments=[], total=0, nib, elapsed=0, previous=0, raf=0, playing=false;
let upper=false, favorites=[], storageAvailable=true;
try { favorites=JSON.parse(localStorage.getItem('callguide.words')||'[]').filter(x=>typeof x==='string'&&x.length<=64).slice(0,12); }
catch { favorites=[]; storageAvailable=false; }
const pad=new InkPad($('ink'),{
  onChange:({strokeCount,limitReached})=>{
    $('undo').disabled=$('clear').disabled=strokeCount===0;
    if(limitReached) $('deviceStatus').textContent='Papel cheio. Desfaça ou limpe para continuar.';
  },
  onDevice:info=>{
    $('deviceStatus').textContent=info.type==='pen'
      ? 'Caneta detectada · '+(info.pressureSupported?'pressão informada pelo dispositivo':'pressão não informada pelo driver')
      : 'Mouse detectado · espessura constante';
  }
});
function svgNode(tag,attrs={}) {
  const node=document.createElementNS(ns,tag);
  for(const [key,value] of Object.entries(attrs)) node.setAttribute(key,value);
  return node;
}
function stop() { playing=false; cancelAnimationFrame(raf); $('play').textContent=elapsed>0?'▶ Continuar':'▶ Escrever palavra'; }
function renderProgress() {
  const duration=drawing.text.length===1?4200:Math.max(4200,Math.min(40000,[...drawing.text].filter(c=>c!==' ').length*1700));
  const fraction=Math.min(1,elapsed/duration); let left=total*fraction,tip;
  for(const segment of segments) {
    const amount=Math.max(0,Math.min(segment.length,left));
    if(amount>=segment.length) segment.node.setAttribute('d',segment.full);
    else segment.node.setAttribute('d',G.envelope(G.prefix(segment.points,amount)));
    if(amount>0) { const points=G.prefix(segment.points,amount); tip=points[points.length-1]; }
    left-=segment.length;
  }
  nib.setAttribute('visibility',fraction>0&&fraction<1?'visible':'hidden');
  if(tip) nib.setAttribute('transform',`translate(${tip[0]} ${tip[1]}) rotate(-38)`);
  return fraction;
}
function tick(now) {
  if(!playing) return;
  elapsed+=(now-previous)*Number($('speed').value); previous=now;
  if(renderProgress()>=1) {
    stop(); $('play').textContent='▶ Escrever novamente';
    $('animationStatus').textContent='Escrita concluída. Observe novamente ou experimente no papel digital.';
  } else raf=requestAnimationFrame(tick);
}
function play(reset=false) {
  if(reset || renderProgress()>=1) elapsed=0;
  if(playing && !reset) { stop(); $('animationStatus').textContent='Animação pausada.'; return; }
  cancelAnimationFrame(raf); playing=true; previous=performance.now();
  $('play').textContent='Ⅱ Pausar'; $('animationStatus').textContent='Escrevendo… acompanhe a caneta.';
  raf=requestAnimationFrame(tick);
}
function setupAnimation() {
  stop(); elapsed=0; segments=[];total=drawing.total;
  const svg=$('animation');svg.replaceChildren();
  const bounds=G.bounds(drawing.strokes),left=bounds.left-70,top=Math.min(-800,bounds.top)-170;
  const bottom=Math.max(340,bounds.bottom)+65;
  svg.setAttribute('viewBox',`${left} ${top} ${bounds.width+140} ${bottom-top}`);
  svg.setAttribute('aria-label',`Modelo cursivo e animação de ${drawing.text}`);
  for(const y of [-700,-350,0,320]) svg.append(svgNode('line',{x1:left,x2:bounds.right+70,y1:y,y2:y,stroke:'#e0e2d4','stroke-width':2}));
  drawing.strokes.forEach((points,i)=>{
    const d=G.envelope(points); svg.append(svgNode('path',{d,fill:'#e0e5d9'}));
    const node=svgNode('path',{fill:'#1b493d'});svg.append(node);
    segments.push({node,points,full:d,length:drawing.lengths[i]});
  });
  const markers=svgNode('g',{id:'strokeMarkers',visibility:$('numbers').checked?'visible':'hidden'});
  drawing.strokes.forEach((s,i)=>{
    const mark=svgNode('g',{transform:`translate(${s[0][0]} ${s[0][1]-48})`});
    mark.append(svgNode('circle',{r:25,fill:'#b77d31'}));
    const label=svgNode('text',{'text-anchor':'middle',y:9,'font-size':25,'font-family':'system-ui',fill:'white'}); label.textContent=i+1;mark.append(label);markers.append(mark);
  });svg.append(markers);
  nib=svgNode('g',{visibility:'hidden'});
  nib.append(svgNode('path',{d:'M0 0L-13 -46L13 -46Z',fill:'#b77d31'}),svgNode('rect',{x:-11,y:-160,width:22,height:119,rx:7,fill:'#1b493d'})); svg.append(nib);
  renderProgress(); $('play').textContent='▶ Escrever palavra';
  $('animationStatus').textContent='Observe a palavra e depois experimente o seu traço.';
}
function validate() {
  try {
    const next=G.compose($('word').value,window.CALLGUIDE_LETTERS);
    $('error').hidden=true; $('word').removeAttribute('aria-invalid'); return next;
  } catch(error) {
    $('error').textContent=error.message; $('error').hidden=false; $('word').setAttribute('aria-invalid','true');$('word').focus();return null;
  }
}
function prepare(force=false) {
  const next=validate();if(!next) return false;
  if(!force && pad.strokeCount && !confirm('Preparar outro modelo vai limpar o desenho atual. Continuar?')) return false;
  drawing=next; $('word').value=drawing.text;updateCount();$('modelTitle').textContent=drawing.text;
  $('zoom').value='100';$('pan').value='0';pad.zoom=1;pad.panX=0;
  pad.setModel(drawing);setupAnimation();return true;
}
function setTab(practice) {
  stop(); $('practicePanel').hidden=!practice;$('observePanel').hidden=practice;
  $('practiceTab').classList.toggle('active',practice);$('observeTab').classList.toggle('active',!practice);
  $('practiceTab').setAttribute('aria-pressed',practice);$('observeTab').setAttribute('aria-pressed',!practice);
  if(practice) requestAnimationFrame(()=>pad.resize());
}
function updateCount() { $('count').textContent=`${[...G.normalize($('word').value)].length} / 32`; }
function persist() {
  try { localStorage.setItem('callguide.words',JSON.stringify(favorites)); }
  catch { storageAvailable=false; }
  if(!storageAvailable) { $('error').hidden=false;$('error').textContent='O armazenamento local está indisponível. As palavras ficarão apenas nesta sessão.'; }
  showFavorites();
}
function showFavorites() {
  $('favorites').replaceChildren();$('savedCount').textContent=favorites.length;
  if(!favorites.length) { const p=document.createElement('p');p.className='hint';p.textContent='Suas palavras vão aparecer aqui.';$('favorites').append(p); }
  favorites.forEach((word,index)=>{
    const row=document.createElement('div'),choose=document.createElement('button'),remove=document.createElement('button');
    choose.textContent=word;choose.title=word;choose.onclick=()=>{$('word').value=word;prepare();};
    remove.textContent='×';remove.setAttribute('aria-label','Excluir palavra '+word);
    remove.onclick=()=>{favorites.splice(index,1);persist();};row.append(choose,remove);$('favorites').append(row);
  });
}
function keyboard() {
  $('alphabet').replaceChildren();$('lower').setAttribute('aria-pressed',!upper);$('upper').setAttribute('aria-pressed',upper);
  for(const raw of 'abcdefghijklmnopqrstuvwxyz') {
    const letter=upper?raw.toUpperCase():raw,b=document.createElement('button');b.textContent=letter;
    b.setAttribute('aria-label','Preparar letra '+letter);b.onclick=()=>{$('word').value=letter;if(prepare()) {setTab(false);play(true);}};$('alphabet').append(b);
  }
}
$('prepare').onclick=()=>{if(prepare()) setTab(false);};$('word').oninput=updateCount;
$('word').onkeydown=event=>{if(event.key==='Enter'&&!event.shiftKey){event.preventDefault();$('prepare').click();}};
$('lower').onclick=()=>{upper=false;keyboard();};$('upper').onclick=()=>{upper=true;keyboard();};
$('observeTab').onclick=()=>setTab(false);$('practiceTab').onclick=$('startPractice').onclick=()=>setTab(true);
$('play').onclick=()=>play();$('replay').onclick=()=>play(true);
$('numbers').onchange=()=>{document.getElementById('strokeMarkers').setAttribute('visibility',$('numbers').checked?'visible':'hidden');pad.numbers=$('numbers').checked;pad.redraw();};
$('device').onchange=()=>{pad.mode=$('device').value;$('deviceStatus').textContent=pad.mode==='pen'?'Modo mesa / caneta: o mouse não desenha neste modo.':pad.mode==='mouse'?'Modo mouse: a caneta não desenha neste modo.':'Entrada automática: mouse ou caneta.';};
$('eraser').onclick=()=>{pad.eraser=!pad.eraser;$('eraser').setAttribute('aria-pressed',pad.eraser);$('eraser').textContent=pad.eraser?'Usar caneta':'Borracha';};
$('undo').onclick=()=>pad.undo();$('clear').onclick=()=>{if(!pad.strokeCount||confirm('Limpar todos os traços deste papel?')) pad.clear();};
$('color').oninput=()=>{pad.color=$('color').value;};$('size').oninput=()=>{pad.size=Number($('size').value);};
$('pressure').onchange=()=>{pad.pressure=$('pressure').checked;};$('showModel').onchange=()=>{pad.showModel=$('showModel').checked;pad.redraw();};
$('zoom').oninput=()=>{pad.zoom=Number($('zoom').value)/100;pad.redraw();};$('pan').oninput=()=>{pad.panX=Number($('pan').value)/100;pad.redraw();};
$('resetView').onclick=()=>{$('zoom').value='100';$('pan').value='0';pad.zoom=1;pad.panX=0;pad.redraw();};
$('export').onclick=()=>{const link=document.createElement('a');link.download='CallGuide-desenho.png';link.href=pad.exportPNG();link.click();};
$('saveWord').onclick=()=>{const next=validate();if(!next) return;favorites=[next.text,...favorites.filter(x=>x!==next.text)].slice(0,12);persist();};
$('about').onclick=()=>$('aboutDialog').showModal();$('closeAbout').onclick=$('closeAboutBottom').onclick=()=>$('aboutDialog').close();
document.addEventListener('visibilitychange',()=>{if(document.hidden) stop();});
document.addEventListener('keydown',event=>{
  if(event.target.matches('input,textarea,select')||event.ctrlKey||event.metaKey||event.altKey||$('aboutDialog').open) return;
  if(/^[a-z]$/i.test(event.key)) { $('word').value=upper?event.key.toUpperCase():event.key.toLowerCase();if(prepare()){setTab(false);play(true);} }
});
keyboard();showFavorites();prepare(true);

// Exercise the real controls and synthetic pointer events in a hidden desktop smoke run.
window.callGuideSelfTest=async()=>{
  const checks=[]; const original=favorites.slice();
  const assert=(condition,name)=>{if(!condition) throw new Error(name);checks.push(name);};
  const frame=()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)));
  try {
    $('word').value='João';$('prepare').click();assert(drawing.text==='João','Portuguese word composition');
    $('play').click();await frame();assert(playing,'Animation started');$('play').click();assert(!playing&&elapsed>0,'Pause preserves progress');
    $('practiceTab').click();await frame();pad.resize();assert(!$('practicePanel').hidden,'Practice navigation');
    const canvas=$('ink'),rect=canvas.getBoundingClientRect();
    function stroke(type,id=1) {
      for(const [action,x,y,pressure] of [['pointerdown',.25,.48,.2],['pointermove',.4,.58,.8],['pointerup',.5,.48,0]]) {
        canvas.dispatchEvent(new PointerEvent(action,{bubbles:true,pointerId:id,pointerType:type,isPrimary:true,buttons:action==='pointerup'?0:1,button:0,pressure,clientX:rect.left+rect.width*x,clientY:rect.top+rect.height*y}));
      }
    }
    stroke('mouse');assert(pad.strokeCount===1,'Mouse drawing');
    $('undo').click();assert(pad.strokeCount===0,'Undo');
    $('device').value='pen';$('device').dispatchEvent(new Event('change'));stroke('mouse',2);assert(pad.strokeCount===0,'Pen-only mode rejects mouse');
    stroke('pen',3);assert(pad.strokeCount===1,'Pen pointer input');
    $('eraser').click();stroke('pen',4);assert(pad.strokeCount===2&&pad.eraser,'Eraser is an undoable operation');
    $('undo').click();assert(pad.strokeCount===1,'Undo eraser');pad.clear();assert(pad.strokeCount===0,'Clear');
    $('saveWord').click();assert(favorites.includes('João'),'Local favorite');
    $('word').value='Olá😀';assert(!validate()&&!$('error').hidden,'Unsupported input validation');
    $('word').value='coração';prepare(true);setTab(true);await frame();pad.resize();
    $('device').value='auto';$('device').dispatchEvent(new Event('change'));pad.eraser=false;$('eraser').textContent='Borracha';$('eraser').setAttribute('aria-pressed','false');
    const image=pad.exportPNG();assert(image.startsWith('data:image/png;base64,'),'PNG export');
    $('about').click();assert($('aboutDialog').open,'Privacy dialog');$('closeAbout').click();
    await frame();return {passed:true,checks,hardwareTest:'Synthetic pointers only; physical tablet not tested'};
  } finally {favorites=original;persist();}
};
