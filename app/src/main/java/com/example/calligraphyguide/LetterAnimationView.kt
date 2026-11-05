package com.sidineyr.callguide

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.animation.LinearInterpolator
import org.json.JSONObject
import kotlin.math.*

/** Reveals precomputed variable-pressure ink envelopes shared with the review preview. */
class LetterAnimationView(context: Context, letter: String) : View(context) {
    private val strokes: List<List<FloatArray>>
    private val lengths: List<Float>
    private val bounds = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ink = Color.rgb(27, 73, 61)
    private var progress = 0f
    private var animator: ValueAnimator? = null
    var onFinished: (() -> Unit)? = null
    var paused = false
        private set
    val isPlaying: Boolean get() = animator?.isRunning == true && !paused
    private var completed = false

    init {
        val data = JSONObject(context.assets.open("letters.json").bufferedReader().use { it.readText() })
            .getJSONObject(letter).getJSONArray("penStrokes")
        strokes = (0 until data.length()).map { i ->
            val samples = data.getJSONArray(i)
            (0 until samples.length()).map { j ->
                val sample = samples.getJSONArray(j)
                FloatArray(8) { k -> sample.getDouble(k).toFloat() }
            }
        }
        lengths = strokes.map { it.last()[7] }
        val all = strokes.flatten()
        bounds.set(all.minOf { min(it[2], it[4]) }, all.minOf { min(it[3], it[5]) },
            all.maxOf { max(it[2], it[4]) }, all.maxOf { max(it[3], it[5]) })
        contentDescription = "Traçado cursivo da letra $letter: descidas com mais pressão, subidas leves."
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    fun start() {
        animator?.cancel(); paused = false; completed = false
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 4200L
            interpolator = LinearInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float; invalidate()
                if (progress >= 1f && !completed) { completed = true; onFinished?.invoke() }
            }
            start()
        }
    }
    fun togglePause() {
        if (paused) animator?.resume() else animator?.pause()
        paused = !paused
    }
    fun pause() { if (isPlaying) togglePause() }
    fun stop() { animator?.cancel(); animator = null }
    override fun onDetachedFromWindow() { stop(); super.onDetachedFromWindow() }

    /** Ends the mesh exactly at current arc length, including interpolated nib edges. */
    private fun prefix(stroke: List<FloatArray>, distance: Float): List<FloatArray> {
        if (distance >= stroke.last()[7]) return stroke
        val points = ArrayList<FloatArray>()
        for (i in stroke.indices) {
            val point = stroke[i]
            if (point[7] <= distance) points.add(point)
            else {
                if (i > 0) {
                    val before = stroke[i-1]
                    val span = point[7] - before[7]
                    val t = if (span > 0f) (distance-before[7])/span else 0f
                    points.add(FloatArray(8) { k -> before[k] + (point[k]-before[k])*t })
                }
                break
            }
        }
        return points
    }
    private fun envelope(points: List<FloatArray>): Path {
        val path = Path()
        if (points.size < 2) return path
        path.moveTo(points[0][2], points[0][3])
        points.drop(1).forEach { path.lineTo(it[2], it[3]) }
        val end = points.last()
        path.quadTo(end[0], end[1], end[4], end[5])
        points.asReversed().drop(1).forEach { path.lineTo(it[4], it[5]) }
        val begin = points.first()
        path.quadTo(begin[0], begin[1], begin[2], begin[3])
        path.close()
        return path
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pad = 18f * resources.displayMetrics.density
        val source = RectF(bounds.left-120f, bounds.top-165f, bounds.right+45f, bounds.bottom+45f)
        val scale = min((width-2*pad)/source.width(), (height-2*pad)/source.height()).coerceAtLeast(.01f)
        canvas.save()
        canvas.translate(width/2f-source.centerX()*scale, height/2f-source.centerY()*scale)
        canvas.scale(scale, scale)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1f/scale; paint.color = Color.rgb(209,214,197)
        for (y in listOf(-700f,-350f,0f,320f)) {
            if (y in source.top..source.bottom) canvas.drawLine(source.left,y,source.right,y,paint)
        }
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(25,27,73,61)
        strokes.forEach { canvas.drawPath(envelope(it),paint) }
        var left = lengths.sum()*progress
        var tip: FloatArray? = null
        strokes.forEachIndexed { i, stroke ->
            val amount = min(left,lengths[i]).coerceAtLeast(0f)
            if (amount > 0f) {
                val points = prefix(stroke,amount)
                paint.color = ink
                canvas.drawPath(envelope(points),paint)
                tip = points.lastOrNull()
            }
            left -= lengths[i]
        }
        if (progress > 0f && progress < 1f) tip?.let { point ->
            canvas.save(); canvas.translate(point[0],point[1]); canvas.rotate(-38f)
            paint.color = Color.rgb(183,125,49)
            val nib = Path().apply { moveTo(0f,0f); lineTo(-13f,-46f); lineTo(13f,-46f); close() }
            canvas.drawPath(nib,paint)
            paint.color = ink
            canvas.drawRoundRect(-11f,-160f,11f,-42f,7f,7f,paint)
            paint.color = Color.rgb(245,217,163); paint.strokeWidth = 2f; paint.style = Paint.Style.STROKE
            canvas.drawLine(0f,-8f,0f,-42f,paint)
            canvas.restore()
        }
        canvas.restore()
    }
}

