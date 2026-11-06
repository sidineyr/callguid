package com.sidineyr.callguide

import android.graphics.*
import kotlin.math.*

/** Shared pressure geometry and framing for demonstration and practice. */
class InkRenderer(val drawing: PenDrawing, private val density:Float=1f) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokes = drawing.strokes
    private val lengths = strokes.map { it.last()[7] }
    private val paths = strokes.map(::envelope)
    private val bounds: RectF
    init {
        val all = strokes.flatten()
        bounds = RectF(all.minOf { min(it[2],it[4]) }-70f,
            min(-800f,all.minOf { min(it[3],it[5]) })-150f,
            all.maxOf { max(it[2],it[4]) }+70f,
            max(340f,all.maxOf { max(it[3],it[5]) })+70f)
    }
    fun matrix(width: Int, height: Int): Matrix {
        val scale = min((width-24f).coerceAtLeast(1f)/bounds.width(),
            (height-24f).coerceAtLeast(1f)/bounds.height())
        return Matrix().apply {
            setScale(scale,scale)
            postTranslate(width/2f-bounds.centerX()*scale,height/2f-bounds.centerY()*scale)
        }
    }
    private fun envelope(points: List<FloatArray>): Path = Path().apply {
        if (points.size < 2) return@apply
        moveTo(points[0][2],points[0][3]); points.drop(1).forEach { lineTo(it[2],it[3]) }
        val end=points.last(); quadTo(end[0],end[1],end[4],end[5])
        points.asReversed().drop(1).forEach { lineTo(it[4],it[5]) }
        val begin=points.first(); quadTo(begin[0],begin[1],begin[2],begin[3]); close()
    }
    private fun prefix(stroke: List<FloatArray>, distance: Float): List<FloatArray> {
        val result=ArrayList<FloatArray>()
        for (i in stroke.indices) {
            val p=stroke[i]
            if (p[7]<=distance) result.add(p)
            else {
                if (i>0) {
                    val b=stroke[i-1]; val t=(distance-b[7])/(p[7]-b[7])
                    result.add(FloatArray(8) { k -> b[k]+(p[k]-b[k])*t })
                }
                break
            }
        }
        return result
    }
    fun draw(canvas: Canvas, width: Int, height: Int, progress: Float,
             model: Boolean=true, numbers: Boolean=false, pen: Boolean=true) {
        val transform=matrix(width,height)
        val values=FloatArray(9); transform.getValues(values); val scale=values[0]
        canvas.save(); canvas.concat(transform)
        paint.style=Paint.Style.STROKE; paint.strokeWidth=1f/scale; paint.color=Color.rgb(214,218,203)
        for (y in listOf(-700f,-350f,0f,320f)) canvas.drawLine(bounds.left,y,bounds.right,y,paint)
        paint.style=Paint.Style.FILL
        if (model) { paint.color=Color.argb(45,27,73,61); paths.forEach { canvas.drawPath(it,paint) } }
        var remaining=lengths.sum()*progress.coerceIn(0f,1f)
        var tip: FloatArray?=null
        strokes.forEachIndexed { i,stroke ->
            val amount=min(remaining,lengths[i]).coerceAtLeast(0f)
            if (amount>0f) {
                paint.color=Color.rgb(27,73,61)
                if (amount>=lengths[i]) { canvas.drawPath(paths[i],paint); tip=stroke.last() }
                else { val points=prefix(stroke,amount); canvas.drawPath(envelope(points),paint); tip=points.lastOrNull() }
            }
            remaining-=lengths[i]
        }
        if (pen && progress>0f && progress<1f) tip?.let { point ->
            canvas.save(); canvas.translate(point[0],point[1]); canvas.rotate(-38f)
            paint.color=Color.rgb(183,125,49)
            canvas.drawPath(Path().apply { moveTo(0f,0f); lineTo(-13f,-46f); lineTo(13f,-46f); close() },paint)
            paint.color=Color.rgb(27,73,61); canvas.drawRoundRect(-11f,-160f,11f,-42f,7f,7f,paint)
            canvas.restore()
        }
        canvas.restore()
        if (numbers) {
            val positions=FloatArray(strokes.size*2)
            strokes.forEachIndexed { i,s -> positions[2*i]=s.first()[0]; positions[2*i+1]=s.first()[1] }
            transform.mapPoints(positions)
            paint.typeface=Typeface.DEFAULT_BOLD; paint.textAlign=Paint.Align.CENTER; paint.textSize=12f*density
            for (i in positions.indices step 2) {
                val x=positions[i]; val y=(positions[i+1]-16f*density).coerceAtLeast(10f*density)
                paint.color=Color.rgb(183,125,49); canvas.drawCircle(x,y,10f*density,paint)
                paint.color=Color.WHITE; canvas.drawText((i/2+1).toString(),x,y+4f*density,paint)
            }
        }
    }
}
