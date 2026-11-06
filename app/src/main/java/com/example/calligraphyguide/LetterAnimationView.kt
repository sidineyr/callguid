package com.sidineyr.callguide

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.view.View
import android.view.animation.LinearInterpolator

class LetterAnimationView(context: Context, text: String) : View(context) {
    private val renderer = InkRenderer(StrokeRepository.drawing(context,text),resources.displayMetrics.density)
    private val baseDuration = if (text.length==1) 4200L else (text.count { it!=' ' }*1700L).coerceIn(4200L,40000L)
    private var progress=0f
    private var animator: ValueAnimator?=null
    private var completed=false
    var onFinished: (() -> Unit)?=null
    var modelVisible=true
        set(value) { field=value; invalidate() }
    var showNumbers=false
        set(value) { field=value; invalidate() }
    var speed=1f
        set(value) {
            field=value.coerceIn(.5f,2f)
            animator?.takeUnless { completed }?.let { current ->
                val fraction=progress; val nextDuration=(baseDuration/field).toLong()
                current.duration=nextDuration; current.currentPlayTime=(nextDuration*fraction).toLong()
            }
        }
    var paused=false
        private set
    val isPlaying get()=animator?.isRunning==true && !paused
    init {
        contentDescription="Demonstração da escrita de $text. Traços em ordem, com variação de pressão."
        importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    fun start() {
        animator?.cancel(); progress=0f; paused=false; completed=false
        animator=ValueAnimator.ofFloat(0f,1f).apply {
            duration=(baseDuration/speed).toLong(); interpolator=LinearInterpolator()
            addUpdateListener {
                progress=it.animatedValue as Float; invalidate()
                if (progress>=1f && !completed) { completed=true; onFinished?.invoke() }
            }
            start()
        }
    }
    fun togglePause() {
        val current=animator ?: return
        if (completed) return
        if (paused) current.resume() else current.pause()
        paused=!paused
    }
    fun pause() { if (isPlaying) togglePause() }
    fun stop() { animator?.cancel(); animator=null }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas); renderer.draw(canvas,width,height,progress,modelVisible,showNumbers)
    }
    override fun onDetachedFromWindow() { stop(); super.onDetachedFromWindow() }
}
