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
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(37, 72, 61); style = Paint.Style.STROKE; strokeWidth = 9f
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val guide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(211, 173, 112); style = Paint.Style.STROKE; strokeWidth = 2f
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
            MotionEvent.ACTION_MOVE -> { currentPath?.lineTo(event.x, event.y); invalidate() }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                currentPath?.let(paths::add); currentPath = null
                parent.requestDisallowInterceptTouchEvent(false); performClick(); invalidate()
            }
        }
        return true
    }

    override fun performClick(): Boolean { super.performClick(); return true }
    fun clear() { paths.clear(); currentPath = null; invalidate() }
    fun undo() { if (paths.isNotEmpty()) paths.removeAt(paths.lastIndex); invalidate() }
}
