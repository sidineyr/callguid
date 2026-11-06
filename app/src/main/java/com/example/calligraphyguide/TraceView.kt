package com.sidineyr.callguide

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlin.math.abs

/** Local-only practice. Ink uses model coordinates, staying aligned when the paper resizes. */
class TraceView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {
    private data class Mark(val path: Path, val color: Int, val width: Float, val eraser: Boolean, var points: Int)
    private val marks = mutableListOf<Mark>()
    private var current: Mark? = null
    private var activePointer = MotionEvent.INVALID_POINTER_ID
    private var savedPoints = 0
    private var listener: ((Boolean) -> Unit)? = null
    private var renderer = InkRenderer(StrokeRepository.drawing(context, "a"),resources.displayMetrics.density)
    private val density = resources.displayMetrics.density
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val clearMode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    var modelVisible = true
        set(value) { field = value; invalidate() }
    var showNumbers = false
        set(value) { field = value; invalidate() }
    var erasing = false
        set(value) { field = value; invalidate() }
    var penColor = Color.rgb(37, 72, 61)
        set(value) { field = value; invalidate() }
    var penWidthDp = 4f
        set(value) { field = if (value.isFinite()) value.coerceIn(.5f, 24f) else 4f; invalidate() }

    init {
        setBackgroundColor(Color.rgb(255, 253, 247))
        contentDescription = "Papel digital. Desenhe sobre o modelo com um dedo ou uma caneta."
    }

    fun setTemplate(text: String) {
        val next = InkRenderer(StrokeRepository.drawing(context, text),resources.displayMetrics.density)
        renderer = next
        contentDescription = "Papel digital para praticar $text. Desenhe sobre o modelo."
        clear()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas, width, height, 0f, modelVisible, showNumbers, false)
        // CLEAR affects this isolated layer only, preserving the model and the paper below.
        val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.concat(renderer.matrix(width, height))
        marks.forEach { drawMark(canvas, it) }
        current?.let { drawMark(canvas, it) }
        ink.xfermode = null
        canvas.restoreToCount(layer)
    }

    private fun drawMark(canvas: Canvas, mark: Mark) {
        ink.color = mark.color
        ink.strokeWidth = mark.width
        ink.xfermode = if (mark.eraser) clearMode else null
        canvas.drawPath(mark.path, ink)
    }

    private fun position(x: Float, y: Float): FloatArray? {
        val inverse = Matrix()
        if (!renderer.matrix(width, height).invert(inverse)) return null
        return floatArrayOf(x, y).also { inverse.mapPoints(it) }
    }

    private fun notifyChanged() { listener?.invoke(marks.isNotEmpty() || current != null) }

    private fun releasePointer() {
        activePointer = MotionEvent.INVALID_POINTER_ID
        parent?.requestDisallowInterceptTouchEvent(false)
    }

    private fun finishStroke() {
        current?.let { marks.add(it); savedPoints += it.points }
        current = null
        releasePointer()
        notifyChanged()
        invalidate()
    }

    private fun cancelStroke() {
        current = null
        releasePointer()
        notifyChanged()
        invalidate()
    }

    private fun append(x: Float, y: Float): Boolean {
        val mark = current ?: return false
        if (savedPoints + mark.points >= MAX_POINTS) {
            finishStroke()
            Toast.makeText(context, "Papel cheio. Desfaça um traço ou limpe para continuar.", Toast.LENGTH_SHORT).show()
            return false
        }
        val point = position(x, y) ?: return false
        mark.path.lineTo(point[0], point[1])
        mark.points++
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (marks.size >= MAX_MARKS || savedPoints >= MAX_POINTS) {
                    Toast.makeText(context, "Papel cheio. Desfaça um traço ou limpe para continuar.", Toast.LENGTH_SHORT).show()
                    return true
                }
                val point = position(event.x, event.y) ?: return true
                val values = FloatArray(9)
                renderer.matrix(width, height).getValues(values)
                val scale = abs(values[Matrix.MSCALE_X]).coerceAtLeast(.0001f)
                activePointer = event.getPointerId(event.actionIndex)
                current = Mark(Path().apply {
                    moveTo(point[0], point[1])
                    // A tiny segment makes a single tap visible with a round cap.
                    lineTo(point[0] + .01f, point[1])
                }, penColor, penWidthDp * density / scale, erasing, 1)
                parent?.requestDisallowInterceptTouchEvent(true)
                notifyChanged()
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.findPointerIndex(activePointer)
                if (pointerIndex < 0) return true
                for (history in 0 until event.historySize) {
                    if (!append(event.getHistoricalX(pointerIndex, history), event.getHistoricalY(pointerIndex, history))) break
                }
                if (current != null) append(event.getX(pointerIndex), event.getY(pointerIndex))
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                val pointerIndex = event.findPointerIndex(activePointer)
                if (pointerIndex >= 0) append(event.getX(pointerIndex), event.getY(pointerIndex))
                if (current != null) finishStroke()
                performClick()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.getPointerId(event.actionIndex) == activePointer) {
                    append(event.getX(event.actionIndex), event.getY(event.actionIndex))
                    if (current != null) finishStroke()
                }
            }
            MotionEvent.ACTION_CANCEL -> cancelStroke()
        }
        return true
    }

    override fun performClick(): Boolean { super.performClick(); return true }

    fun setOnStrokesChangedListener(listener: (Boolean) -> Unit) {
        this.listener = listener
        notifyChanged()
    }

    fun clear() {
        current = null
        marks.clear()
        savedPoints = 0
        releasePointer()
        notifyChanged()
        invalidate()
    }

    fun undo() {
        if (current != null) cancelStroke()
        else if (marks.isNotEmpty()) savedPoints -= marks.removeAt(marks.lastIndex).points
        notifyChanged()
        invalidate()
    }

    private companion object {
        const val MAX_POINTS = 50_000
        const val MAX_MARKS = 500
    }
}
