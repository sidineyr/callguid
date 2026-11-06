(function (root, factory) {
  'use strict';
  const api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  if (root) root.CallGuideGeometry = api;
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict';
  const MAX_LENGTH = 32;
  const own = (object, key) => Object.prototype.hasOwnProperty.call(object, key);

  function normalize(text) {
    return String(text ?? '').normalize('NFC').replace(/[\s\p{Z}]+/gu, ' ').trim();
  }

  function compose(text, glyphs) {
    const normalized = normalize(text);
    const chars = Array.from(normalized);
    if (!chars.length) throw new Error('Digite uma palavra ou uma frase curta.');
    if (chars.length > MAX_LENGTH) throw new Error(`Use no máximo ${MAX_LENGTH} caracteres, incluindo espaços.`);
    const unsupported = [...new Set(chars.filter(char => char !== ' ' && !own(glyphs, char)))];
    if (unsupported.length) throw new Error(`Ainda não há modelo para: ${unsupported.join(' ')}. Use letras disponíveis e espaços.`);

    const strokes = [];
    let offset = 0;
    chars.forEach((char, index) => {
      if (char === ' ') { offset += 220; return; }
      const glyph = glyphs[char];
      const source = glyph.penStrokes || glyph.strokes;
      if (!(glyph.advance > 0) || !Array.isArray(source) || !source.length) {
        throw new Error(`Modelo de letra inválido: ${char}.`);
      }
      source.forEach(stroke => {
        if (!Array.isArray(stroke) || stroke.length < 2) throw new Error(`Modelo de letra inválido: ${char}.`);
        strokes.push(stroke.map(point => {
          if (!Array.isArray(point) || point.length !== 8 || !point.every(Number.isFinite)) {
            throw new Error(`Modelo de letra inválido: ${char}.`);
          }
          const copy = point.slice();
          copy[0] += offset; copy[2] += offset; copy[4] += offset;
          return copy;
        }));
      });
      offset += glyph.advance;
      if (index < chars.length - 1 && chars[index + 1] !== ' ' && /\p{Lu}/u.test(char)) offset += 30;
    });
    const lengths = strokes.map(stroke => stroke[stroke.length - 1][7]);
    return { text: normalized, strokes, width: offset, lengths, total: lengths.reduce((a, b) => a + b, 0) };
  }

  function envelope(points) {
    if (!points || points.length < 2) return '';
    const begin = points[0], end = points[points.length - 1];
    let path = `M${begin[2]} ${begin[3]}`;
    for (let i = 1; i < points.length; i++) path += `L${points[i][2]} ${points[i][3]}`;
    path += `Q${end[0]} ${end[1]} ${end[4]} ${end[5]}`;
    for (let i = points.length - 2; i >= 0; i--) path += `L${points[i][4]} ${points[i][5]}`;
    return path + `Q${begin[0]} ${begin[1]} ${begin[2]} ${begin[3]}Z`;
  }

  function prefix(stroke, distance) {
    if (!stroke || !stroke.length || !(distance > 0)) return [];
    if (distance >= stroke[stroke.length - 1][7]) return stroke;
    const points = [];
    for (let i = 0; i < stroke.length; i++) {
      const point = stroke[i];
      if (point[7] <= distance) points.push(point);
      else {
        if (i > 0) {
          const before = stroke[i - 1], span = point[7] - before[7];
          const t = span > 0 ? (distance - before[7]) / span : 0;
          points.push(before.map((value, k) => value + (point[k] - value) * t));
        }
        break;
      }
    }
    return points;
  }

  function bounds(strokes) {
    let left = Infinity, top = Infinity, right = -Infinity, bottom = -Infinity;
    for (const stroke of strokes) for (const point of stroke) {
      left = Math.min(left, point[2], point[4]);
      top = Math.min(top, point[3], point[5]);
      right = Math.max(right, point[2], point[4]);
      bottom = Math.max(bottom, point[3], point[5]);
    }
    if (!Number.isFinite(left)) return { left: 0, top: 0, right: 0, bottom: 0, width: 0, height: 0 };
    return { left, top, right, bottom, width: right - left, height: bottom - top };
  }

  return Object.freeze({ normalize, compose, envelope, prefix, bounds });
});
