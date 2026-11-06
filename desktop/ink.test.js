'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const { InkPad, widthFor, viewportFor, worldPoint } = require('./ink.js');

test('mouse has constant width regardless of reported browser pressure', () => {
  assert.equal(widthFor('mouse', 0, 6, true), 6);
  assert.equal(widthFor('mouse', 1, 6, true), 6);
});

test('pen pressure changes width with a nonzero 20 percent baseline', () => {
  assert.equal(widthFor('pen', 0, 10), 2);
  assert.ok(Math.abs(widthFor('pen', .5, 10) - 6) < 1e-8);
  assert.equal(widthFor('pen', 1, 10), 10);
  assert.equal(widthFor('pen', .1, 10, false), 10);
  assert.ok(widthFor('pen', .9, 10) > widthFor('pen', .2, 10));
});

test('invalid or out-of-range pressure cannot create negative or infinite ink', () => {
  assert.equal(widthFor('pen', -5, 10), 2);
  assert.equal(widthFor('pen', 5, 10), 10);
  assert.ok(Number.isFinite(widthFor('pen', NaN, Infinity)));
  assert.ok(widthFor('pen', NaN, 0) > 0);
});

test('inverse mapping keeps strokes at the same world position through zoom and resize', () => {
  const box = {left:-100, top:-900, right:1900, bottom:400, width:2000, height:1300};
  const point = {x:550, y:-120};
  for (const [width,height,zoom,panX] of [[1000,600,1,0],[600,400,2,-1],[1600,900,3,.5]]) {
    const t = viewportFor(box,width,height,zoom,panX,.2);
    const actual = worldPoint(point.x*t.scale+t.x,point.y*t.scale+t.y,t);
    assert.ok(Math.abs(actual.x-point.x)<1e-8);
    assert.ok(Math.abs(actual.y-point.y)<1e-8);
  }
});

test('tiny surfaces and malformed zoom remain finite and invertible', () => {
  const t = viewportFor({left:0,top:0,right:1,bottom:1,width:1,height:1},0,0,NaN,NaN,NaN);
  assert.ok(t.scale>0 && Object.values(t).every(Number.isFinite));
});

function fakeCanvas() {
  const context = new Proxy({}, { get(target,key) { return key in target ? target[key] : () => {}; } });
  const canvas = { style:{}, width:1000, height:600,
    getContext:() => context,
    getBoundingClientRect:() => ({left:0,top:0,width:1000,height:600}),
    setAttribute() {}, addEventListener() {}, removeEventListener() {},
    setPointerCapture() {}, releasePointerCapture() {}
  };
  canvas.ownerDocument = {createElement:() => ({width:1000,height:600,getContext:() => context})};
  return canvas;
}
function event(type,x,y,id=1,pressure=.5) {
  return {pointerType:type,clientX:x,clientY:y,pointerId:id,pressure,button:0,preventDefault(){}};
}

test('input filtering rejects touch and mismatched devices while accepting real pen pointers', () => {
  const saved = globalThis.requestAnimationFrame;
  globalThis.requestAnimationFrame = () => 1;
  const pad = new InkPad(fakeCanvas());
  try {
    pad.mode='mouse'; pad.down(event('pen',100,100)); pad.down(event('touch',100,100));
    assert.equal(pad.strokeCount,0);
    pad.mode='pen'; pad.down(event('mouse',100,100)); assert.equal(pad.strokeCount,0);
    pad.down(event('pen',100,100,1,.2)); pad.move(event('pen',110,100,1,.8)); pad.up(event('pen',120,100,1,0));
    assert.equal(pad.strokeCount,1);
    assert.equal(pad.marks[0].points.length,3);
    assert.ok(pad.marks[0].points[1].width>pad.marks[0].points[0].width);
    assert.equal(pad.marks[0].points[2].width,pad.marks[0].points[1].width);
  } finally { globalThis.requestAnimationFrame=saved; }
});

test('eraser and ink are separate immutable operations; cancellation and undo preserve earlier marks', () => {
  const saved = globalThis.requestAnimationFrame;
  globalThis.requestAnimationFrame=() => 1;
  const pad=new InkPad(fakeCanvas());
  try {
    pad.color='#123456'; pad.down(event('mouse',100,100)); pad.up(event('mouse',120,100));
    pad.eraser=true; pad.color='#abcdef'; pad.down(event('mouse',100,100)); pad.up(event('mouse',120,100));
    assert.equal(pad.strokeCount,2);
    assert.equal(pad.marks[0].eraser,false); assert.equal(pad.marks[0].color,'#123456');
    assert.equal(pad.marks[1].eraser,true);
    pad.undo(); assert.equal(pad.strokeCount,1); assert.equal(pad.marks[0].color,'#123456');
    pad.down(event('mouse',140,120)); pad.cancel(event('mouse',150,130)); assert.equal(pad.strokeCount,1);
    pad.clear(); assert.equal(pad.strokeCount,0); assert.equal(pad.pointCount,0);
  } finally { globalThis.requestAnimationFrame=saved; }
});
