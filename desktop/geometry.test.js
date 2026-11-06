'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const geometry = require('./geometry.js');
const candidates = [path.join(__dirname, 'assets/letters.json'), path.join(__dirname, '../callguid/app/src/main/assets/letters.json')];
const asset = candidates.find(file => fs.existsSync(file));
assert.ok(asset, 'The actual pen geometry asset must be available.');
const glyphs = JSON.parse(fs.readFileSync(asset, 'utf8'));

test('NFC and Unicode whitespace normalization preserves case and Portuguese accents', () => {
  assert.equal(geometry.normalize(' \tJoa\u0303o\u00a0\n c\u0327a\u0303o  '), 'João ção');
  const composed = geometry.compose('c\u0327a\u0303o', glyphs);
  assert.equal(composed.text, 'ção');
});

test('all 112 authored glyphs and real Portuguese words compose finite ordered pressure geometry', () => {
  assert.equal(Object.keys(glyphs).length, 112);
  for (const text of [...Object.keys(glyphs), 'João', 'coração', 'minha escola', 'Ação 123', 'criança']) {
    const drawing = geometry.compose(text, glyphs);
    assert.ok(drawing.total > 0);
    assert.equal(drawing.strokes.length, drawing.lengths.length);
    drawing.strokes.forEach((stroke, index) => {
      assert.equal(stroke[0][7], 0);
      assert.ok(drawing.lengths[index] > 0);
      stroke.forEach((point, i) => {
        assert.equal(point.length, 8);
        assert.ok(point.every(Number.isFinite));
        assert.ok(point[6] > 0);
        if (i) assert.ok(point[7] > stroke[i - 1][7]);
      });
    });
  }
});

test('lowercase a joins exactly on the common baseline without a synthetic connector', () => {
  const drawing = geometry.compose('aa', glyphs);
  assert.deepEqual(drawing.strokes[0].at(-1).slice(0, 2), drawing.strokes[1][0].slice(0, 2));
  assert.equal(drawing.width, 2 * glyphs.a.advance);
});

test('letter placement preserves original geometry, stroke length and pressure in independent copies', () => {
  const before = JSON.stringify(glyphs.a);
  const drawing = geometry.compose('aa', glyphs);
  const original = glyphs.a.penStrokes[0][10], second = drawing.strokes[1][10];
  for (let k = 0; k < 8; k++) assert.equal(second[k], original[k] + ([0, 2, 4].includes(k) ? glyphs.a.advance : 0));
  drawing.strokes[0][10][0] = 99999;
  assert.equal(JSON.stringify(glyphs.a), before);
  assert.notEqual(drawing.strokes[1][10][0], 99999);
});

test('capital gap and word spaces match the Android placement rules without trailing gaps', () => {
  assert.equal(geometry.compose('A', glyphs).width, glyphs.A.advance);
  assert.equal(geometry.compose('Aa', glyphs).width, glyphs.A.advance + 30 + glyphs.a.advance);
  assert.equal(geometry.compose('A a', glyphs).width, glyphs.A.advance + 220 + glyphs.a.advance);
});

test('length is checked after normalization; empty and unsupported input fail explicitly', () => {
  assert.equal(geometry.compose(' a '.repeat(16), glyphs).text.length, 31);
  assert.equal(geometry.compose('a'.repeat(32), glyphs).text.length, 32);
  assert.throws(() => geometry.compose('a'.repeat(33), glyphs), /32/);
  assert.throws(() => geometry.compose(' \n\t ', glyphs), /Digite/);
  assert.throws(() => geometry.compose('Olá😀', glyphs), /😀/);
  assert.throws(() => geometry.compose('Ω', glyphs), /Ω/);
});

test('envelopes close the pressure mesh and prefixes interpolate every channel at the requested length', () => {
  const stroke = glyphs.a.penStrokes[0], length = stroke.at(-1)[7];
  assert.equal(geometry.envelope([]), '');
  assert.equal(geometry.envelope([stroke[0]]), '');
  assert.ok(geometry.envelope(stroke).startsWith('M'));
  assert.ok(geometry.envelope(stroke).endsWith('Z'));
  assert.deepEqual(geometry.prefix(stroke, 0), []);
  assert.deepEqual(geometry.prefix(stroke, -1), []);
  assert.deepEqual(geometry.prefix([], 100), []);
  assert.equal(geometry.prefix(stroke, length), stroke);
  for (const fraction of [.001, .1, .333, .9]) {
    const distance = length * fraction, prefix = geometry.prefix(stroke, distance);
    assert.ok(prefix.length >= 2);
    assert.ok(Math.abs(prefix.at(-1)[7] - distance) < 1e-8);
    assert.ok(prefix.every((point, i) => !i || point[7] >= prefix[i - 1][7]));
    assert.ok(prefix.at(-1).every(Number.isFinite));
  }
});

test('bounds contain both nib edges, including accents and descenders', () => {
  const drawing = geometry.compose('João ç', glyphs), bounds = geometry.bounds(drawing.strokes);
  assert.ok(bounds.width > 0 && bounds.height > 0);
  for (const stroke of drawing.strokes) for (const p of stroke) {
    assert.ok(p[2] >= bounds.left && p[2] <= bounds.right);
    assert.ok(p[4] >= bounds.left && p[4] <= bounds.right);
    assert.ok(p[3] >= bounds.top && p[3] <= bounds.bottom);
    assert.ok(p[5] >= bounds.top && p[5] <= bounds.bottom);
  }
  assert.deepEqual(geometry.bounds([]), { left: 0, top: 0, right: 0, bottom: 0, width: 0, height: 0 });
});

test('the same engine loads in a browser without Node globals', () => {
  const browser = {};
  vm.runInNewContext(fs.readFileSync(path.join(__dirname, 'geometry.js'), 'utf8'), browser);
  assert.equal(browser.CallGuideGeometry.normalize(' c\u0327a\u0303o '), 'ção');
  assert.equal(browser.CallGuideGeometry.compose('Olá', glyphs).text, 'Olá');
});
