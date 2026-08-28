package com.sidineyr.callguide

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

class TraceView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {
    private val paths = mutableListOf<Path>()
    private var currentPath: Path? = null
    private var strokesChangedListener: ((Boolean) -> Unit)? = null
    private val density = resources.displayMetrics.density
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(37, 72, 61); style = Paint.Style.STROKE; strokeWidth = 6f * density
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val guide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(211, 173, 112); style = Paint.Style.STROKE; strokeWidth = density
    }

    init {
        setBackgroundColor(Color.rgb(255, 253, 247))
        contentDescription = context.getString(R.string.practice_canvas_description)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val gap = max(height / 4f, 1f)
        for (i in 1..3) canvas.drawLine(0f, gap * i, width.toFloat(), gap * i, guide)
        paths.forEach { canvas.drawPath(it, ink) }
        currentPath?.let { canvas.drawPath(it, ink) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                currentPath = Path().apply { moveTo(event.x, event.y) }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath?.let { path ->
                    for (i in 0 until event.historySize) {
                        path.lineTo(event.getHistoricalX(i), event.getHistoricalY(i))
                    }
                    path.lineTo(event.x, event.y)
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                currentPath?.lineTo(event.x, event.y)
                currentPath?.let(paths::add)
                currentPath = null
                strokesChangedListener?.invoke(paths.isNotEmpty())
                parent.requestDisallowInterceptTouchEvent(false); performClick(); invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                currentPath = null
                parent.requestDisallowInterceptTouchEvent(false)
                invalidate()
            }
        }
        return true
    }

    override fun performClick(): Boolean { super.performClick(); return true }
    fun setOnStrokesChangedListener(listener: (Boolean) -> Unit) {
        strokesChangedListener = listener
        listener(paths.isNotEmpty())
    }

    fun clear() {
        paths.clear(); currentPath = null; invalidate()
        strokesChangedListener?.invoke(false)
    }

    fun undo() {
        if (paths.isNotEmpty()) paths.removeAt(paths.lastIndex)
        invalidate()
        strokesChangedListener?.invoke(paths.isNotEmpty())
    }
}
