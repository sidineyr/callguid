(function (root, factory) {
  'use strict';
  const api = factory(root);
  if (typeof module === 'object' && module.exports) module.exports = api;
  if (root) root.InkPad = api.InkPad;
})(typeof globalThis !== 'undefined' ? globalThis : this, function (root) {
  'use strict';
  const MAX_STROKES = 500, MAX_POINTS = 50000;
  const clamp = (value, min, max) => Math.min(max, Math.max(min, value));

  /** Mouse width is fixed. Pen width uses only the pressure reported by Pointer Events. */
  function widthFor(pointerType, pressure, baseSize, usePressure = true) {
    const base = Number.isFinite(baseSize) ? clamp(baseSize, .5, 40) : 4;
    if (pointerType !== 'pen' || !usePressure) return base;
    const value = Number.isFinite(pressure) ? clamp(pressure, 0, 1) : .5;
    return base * (.2 + .8 * value);
  }

  function viewportFor(box, width, height, zoom = 1, panX = 0, panY = 0) {
    const safeWidth = Math.max(1, width), safeHeight = Math.max(1, height);
    const scale = Math.min(Math.max(1, safeWidth - 32) / Math.max(1, box.width),
      Math.max(1, safeHeight - 32) / Math.max(1, box.height)) * clamp(Number(zoom) || 1, 1, 4);
    const x = (safeWidth - (box.left + box.right) * scale) / 2 +
      clamp(Number(panX) || 0, -1, 1) * Math.max(0, (box.width * scale - safeWidth + 32) / 2);
    const y = (safeHeight - (box.top + box.bottom) * scale) / 2 +
      clamp(Number(panY) || 0, -1, 1) * Math.max(0, (box.height * scale - safeHeight + 32) / 2);
    return { scale, x, y };
  }
  function worldPoint(x, y, transform) {
    return { x: (x - transform.x) / transform.scale, y: (y - transform.y) / transform.scale };
  }

  class InkPad {
    constructor(canvas, { onChange = () => {}, onDevice = () => {} } = {}) {
      this.canvas = canvas;
      this.context = canvas.getContext('2d');
      this.layer = canvas.ownerDocument.createElement('canvas');
      this.layerContext = this.layer.getContext('2d');
      this.onChange = onChange; this.onDevice = onDevice;
      this.mode = 'auto'; this.eraser = false; this.color = '#1b493d'; this.size = 4;
      this.pressure = true; this.showModel = true; this.numbers = false;
      this.zoom = 1; this.panX = 0; this.panY = 0;
      this.marks = []; this.current = null; this.activeId = null; this.pointCount = 0;
      this.limitReached = false; this.frame = 0; this.destroyed = false;
      this.model = null; this.modelPaths = []; this.box = { left: 0, top: -850, right: 1200, bottom: 390, width: 1200, height: 1240 };
      this.width = 1; this.height = 1; this.dpr = 1;
      canvas.style.touchAction = 'none';
      canvas.setAttribute('aria-label', 'Papel digital: desenhe com o mouse ou a caneta da mesa digitalizadora.');
      this.handlers = {
        pointerdown: event => this.down(event), pointermove: event => this.move(event),
        pointerup: event => this.up(event), pointercancel: event => this.cancel(event),
        lostpointercapture: event => this.cancel(event), contextmenu: event => event.preventDefault()
      };
      for (const [name, handler] of Object.entries(this.handlers)) canvas.addEventListener(name, handler);
      this.observer = typeof root.ResizeObserver === 'function' ? new root.ResizeObserver(() => this.resize()) : null;
      this.observer?.observe(canvas);
      this.windowResize = () => this.resize();
      root.addEventListener?.('resize', this.windowResize);
      this.resize();
    }

    get inputMode() { return this.mode; }
    set inputMode(value) { this.mode = value; }
    get strokeCount() { return this.marks.length + (this.current ? 1 : 0); }

    setModel(drawing) {
      this.cancel();
      this.model = drawing;
      const geometry = root.CallGuideGeometry;
      this.modelPaths = drawing ? drawing.strokes.map(points => new root.Path2D(geometry.envelope(points))) : [];
      if (drawing) {
        const b = geometry.bounds(drawing.strokes);
        const left = b.left - 70, top = Math.min(-800, b.top) - 150;
        const right = b.right + 70, bottom = Math.max(340, b.bottom) + 70;
        this.box = { left, top, right, bottom, width: right - left, height: bottom - top };
      }
      this.clear();
    }

    transform() { return viewportFor(this.box, this.width, this.height, this.zoom, this.panX, this.panY); }

    resize() {
      if (this.destroyed) return;
      const rect = this.canvas.getBoundingClientRect();
      this.width = Math.max(1, rect.width); this.height = Math.max(1, rect.height);
      this.dpr = clamp(root.devicePixelRatio || 1, 1, 3);
      const width = Math.round(this.width * this.dpr), height = Math.round(this.height * this.dpr);
      if (this.canvas.width !== width || this.canvas.height !== height) {
        this.canvas.width = this.layer.width = width; this.canvas.height = this.layer.height = height;
      }
      this.redraw();
    }

    accepts(event) {
      const type = event.pointerType || 'mouse';
      return (type === 'pen' || type === 'mouse') && (this.mode === 'auto' || this.mode === type);
    }

    notify() { this.onChange({ strokeCount: this.strokeCount, limitReached: this.limitReached }); }
    device(event) {
      this.onDevice({ type: event.pointerType || 'mouse',
        pressureSupported: event.pointerType === 'pen' && Number.isFinite(event.pressure),
        pressure: event.pointerType === 'pen' && Number.isFinite(event.pressure) ? event.pressure : null });
    }

    down(event) {
      if (this.destroyed || this.activeId !== null || !this.accepts(event) ||
          (event.pointerType !== 'pen' && event.button !== 0)) return;
      event.preventDefault();
      if (this.marks.length >= MAX_STROKES || this.pointCount >= MAX_POINTS) {
        this.limitReached = true; this.notify(); return;
      }
      this.activeId = event.pointerId;
      const transform = this.transform();
      this.current = { points: [], color: this.color, eraser: this.eraser || event.button === 5,
        size: (Number(this.size) || 4) / transform.scale, pressure: this.pressure,
        type: event.pointerType || 'mouse' };
      try { this.canvas.setPointerCapture(event.pointerId); } catch (_) { /* detached synthetic events */ }
      this.append(event); this.device(event); this.notify(); this.redraw();
    }

    append(event) {
      if (!this.current) return false;
      if (this.pointCount + this.current.points.length >= MAX_POINTS) {
        this.limitReached = true; this.finish(); return false;
      }
      const rect = this.canvas.getBoundingClientRect();
      const point = worldPoint(event.clientX - rect.left, event.clientY - rect.top, this.transform());
      // Sizes already live in model units; pressure calculation therefore has no CSS-size clamp.
      const factor = widthFor(this.current.type, event.pressure, 1, this.current.pressure);
      point.width = this.current.size * factor * (this.current.eraser ? 4 : 1);
      this.current.points.push(point);
      return true;
    }

    move(event) {
      if (event.pointerId !== this.activeId || !this.current) return;
      event.preventDefault();
      const events = typeof event.getCoalescedEvents === 'function' ? event.getCoalescedEvents() : [];
      for (const sample of events.length ? events : [event]) if (!this.append(sample)) break;
      this.device(event); this.redraw();
    }

    up(event) {
      if (event.pointerId !== this.activeId) return;
      event.preventDefault();
      // Pointer-up often reports zero pressure; retain contact width at its final coordinate.
      if (this.current?.points.length && this.pointCount + this.current.points.length < MAX_POINTS) {
        const previous = this.current.points[this.current.points.length - 1];
        const rect = this.canvas.getBoundingClientRect();
        const point = worldPoint(event.clientX - rect.left, event.clientY - rect.top, this.transform());
        if (Math.hypot(previous.x - point.x, previous.y - point.y) > .001) {
          this.current.points.push({ ...point, width: previous.width });
        }
      }
      this.finish();
    }

    finish() {
      const pointer = this.activeId; this.activeId = null;
      if (this.current?.points.length) {
        this.marks.push(this.current); this.pointCount += this.current.points.length;
      }
      this.current = null;
      if (pointer !== null) try { this.canvas.releasePointerCapture(pointer); } catch (_) { /* no capture */ }
      this.notify(); this.redraw();
    }

    cancel(event) {
      if (event && event.pointerId !== this.activeId) return;
      const pointer = this.activeId; this.activeId = null; this.current = null;
      if (pointer !== null) try { this.canvas.releasePointerCapture(pointer); } catch (_) { /* no capture */ }
      if (this.canvas) { this.notify(); this.redraw(); }
    }

    drawMark(context, mark) {
      const points = mark.points;
      if (!points.length) return;
      context.globalCompositeOperation = mark.eraser ? 'destination-out' : 'source-over';
      context.fillStyle = context.strokeStyle = mark.color;
      context.lineCap = context.lineJoin = 'round';
      context.beginPath(); context.arc(points[0].x, points[0].y, points[0].width / 2, 0, Math.PI * 2); context.fill();
      for (let i = 1; i < points.length; i++) {
        const a = points[i - 1], b = points[i];
        context.lineWidth = (a.width + b.width) / 2;
        context.beginPath(); context.moveTo(a.x, a.y); context.lineTo(b.x, b.y); context.stroke();
      }
    }

    paint() {
      if (this.destroyed) return;
      const context = this.context, ink = this.layerContext, t = this.transform();
      context.setTransform(this.dpr, 0, 0, this.dpr, 0, 0);
      context.fillStyle = '#fffdf7'; context.fillRect(0, 0, this.width, this.height);
      context.save(); context.translate(t.x, t.y); context.scale(t.scale, t.scale);
      context.strokeStyle = '#d6dacb'; context.lineWidth = 1 / t.scale;
      for (const y of [-700, -350, 0, 320]) {
        context.beginPath(); context.moveTo(this.box.left, y); context.lineTo(this.box.right, y); context.stroke();
      }
      if (this.showModel) {
        context.fillStyle = 'rgba(27,73,61,.19)'; this.modelPaths.forEach(path => context.fill(path));
      }
      context.restore();
      if (this.numbers && this.showModel && this.model) {
        context.textAlign = 'center'; context.textBaseline = 'middle'; context.font = 'bold 11px system-ui';
        this.model.strokes.forEach((stroke, index) => {
          const first = stroke[0], x = first[0] * t.scale + t.x, y = first[1] * t.scale + t.y - 15;
          context.fillStyle = '#b77d31'; context.beginPath(); context.arc(x, y, 10, 0, Math.PI * 2); context.fill();
          context.fillStyle = '#fff'; context.fillText(String(index + 1), x, y);
        });
      }
      ink.setTransform(1, 0, 0, 1, 0, 0); ink.clearRect(0, 0, this.layer.width, this.layer.height);
      ink.setTransform(this.dpr * t.scale, 0, 0, this.dpr * t.scale, this.dpr * t.x, this.dpr * t.y);
      this.marks.forEach(mark => this.drawMark(ink, mark));
      if (this.current) this.drawMark(ink, this.current);
      ink.globalCompositeOperation = 'source-over';
      context.setTransform(1, 0, 0, 1, 0, 0); context.drawImage(this.layer, 0, 0);
    }

    redraw() {
      if (this.destroyed || this.frame) return;
      this.frame = root.requestAnimationFrame(() => { this.frame = 0; this.paint(); });
    }
    undo() {
      if (this.current) this.cancel();
      else if (this.marks.length) this.pointCount -= this.marks.pop().points.length;
      this.limitReached = false; this.notify(); this.redraw();
    }
    clear() {
      this.cancel(); this.marks.length = 0; this.pointCount = 0; this.limitReached = false;
      this.notify(); this.redraw();
    }
    exportPNG() { this.paint(); return this.canvas.toDataURL('image/png'); }
    destroy() {
      this.cancel(); this.destroyed = true;
      if (this.frame) root.cancelAnimationFrame(this.frame);
      this.frame = 0; this.observer?.disconnect();
      root.removeEventListener?.('resize', this.windowResize);
      for (const [name, handler] of Object.entries(this.handlers)) this.canvas.removeEventListener(name, handler);
    }
  }

  return { InkPad, widthFor, viewportFor, worldPoint, MAX_STROKES, MAX_POINTS };
});
