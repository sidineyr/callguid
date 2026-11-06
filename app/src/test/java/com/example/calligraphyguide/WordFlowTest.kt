package com.sidineyr.callguide

import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowChoreographer

/** Exercises Android views and assets locally; it does not replace physical-device rendering/touch tests. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@LooperMode(LooperMode.Mode.PAUSED)
class WordFlowTest {
    private lateinit var controller: ActivityController<MainActivity>
    private val activity: MainActivity get() = controller.get()

    @Before fun openApplication() {
        // PAUSED looper alone does not pause vsync: Robolectric otherwise auto-advances
        // the clock for every frame, completing an entire animation during click().idle().
        ShadowChoreographer.setPaused(true)
        ShadowChoreographer.setFrameDelay(java.time.Duration.ofMillis(16))
        controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        activity.getSharedPreferences("words", 0).edit().clear().commit()
        idle()
    }

    @After fun closeApplication() {
        controller.pause().stop().destroy()
    }

    private fun idle() { shadowOf(Looper.getMainLooper()).idle() }

    private fun views(view: View = activity.window.decorView): List<View> =
        listOf(view) + if (view is ViewGroup) (0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()

    private fun click(label: String) {
        val button = views().filterIsInstance<Button>().firstOrNull { it.text.toString() == label }
            ?: throw AssertionError("Botão não encontrado: $label")
        assertTrue(button.performClick())
        idle()
    }

    private fun words() {
        click("Vamos escrever")
        click("Escrever uma palavra ou meu nome")
    }

    private fun input(): EditText = views().filterIsInstance<EditText>().single()

    private fun drawLine(trace: TraceView) {
        val start = SystemClock.uptimeMillis()
        listOf(
            MotionEvent.ACTION_DOWN to (120f to 180f),
            MotionEvent.ACTION_MOVE to (160f to 200f),
            MotionEvent.ACTION_UP to (200f to 220f)
        ).forEachIndexed { index, (action, point) ->
            val event = MotionEvent.obtain(start, start + index * 16L, action, point.first, point.second, 0)
            try { assertTrue(trace.dispatchTouchEvent(event)) } finally { event.recycle() }
        }
    }

    @Test fun typedNameOpensPracticeAndInkToolsNotifyChanges() {
        words()
        input().setText("João")
        click("Desenhar sobre a palavra")
        val trace = views().filterIsInstance<TraceView>().single()
        trace.layout(0, 0, 900, 500)
        val changes = mutableListOf<Boolean>()
        trace.setOnStrokesChangedListener { changes.add(it) }
        assertFalse(changes.last())

        drawLine(trace)
        assertTrue(changes.last())
        click("Desfazer")
        assertFalse(changes.last())

        // Tap strokes must be accepted, and erasing must be an undoable operation too.
        val now = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(now, now + 1, action, 300f, 240f, 0)
            try { assertTrue(trace.dispatchTouchEvent(event)) } finally { event.recycle() }
        }
        assertTrue(changes.last())
        click("Borracha")
        assertTrue(trace.erasing)
        drawLine(trace)
        click("Desfazer")
        assertTrue(changes.last()) // The original tap remains after undoing only the eraser.
        click("Limpar")
        assertFalse(changes.last())
    }

    @Test fun unsupportedEmojiShowsAnErrorWithoutLeavingTheEditor() {
        words()
        input().setText("Olá😀")
        click("Observar palavra animada")
        assertTrue(input().error.toString().contains("😀"))
        assertTrue(views().filterIsInstance<TextView>().any { it.text.toString() == "Palavras com o seu traço" })
        assertFalse(views().any { it is LetterAnimationView })
    }

    @Test fun completedWordAnimationStaysAvailableForPracticeAndRepeat() {
        words()
        input().setText("coração")
        click("Observar palavra animada")
        val animation=views().filterIsInstance<LetterAnimationView>().single()
        animation.layout(0,0,900,500)
        animation.draw(android.graphics.Canvas(android.graphics.Bitmap.createBitmap(900,500,android.graphics.Bitmap.Config.ARGB_8888)))
        // Advance explicit bounded frames, then verify actual completion before replaying.
        repeat(100) { shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(500)) }
        assertTrue(views().any { it === animation })
        assertTrue(views().filterIsInstance<TextView>().any { it.text.toString().startsWith("Traçado concluído") })
        click("Repetir palavra")
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(32))
        assertTrue(animation.isPlaying)
        click("Pausar animação")
        assertTrue(animation.paused)
        click("Praticar este modelo")
        assertTrue(views().any { it is TraceView })
    }

    @Test fun explicitlySavedWordSurvivesActivityRecreation() {
        words()
        input().setText("João")
        click("Salvar palavra neste aparelho")
        val saved = JSONArray(activity.getSharedPreferences("words", 0).getString("favorites", "[]"))
        assertEquals("João", saved.getString(0))
        controller.recreate().visible()
        idle()
        assertTrue(views().filterIsInstance<Button>().any { it.text.toString() == "João" })
        assertEquals("João", input().text.toString())
    }

    @Test fun packagedGlyphsComposePortugueseWordsWithAccentsAndOrderedPressureSamples() {
        val glyphs = StrokeRepository.glyphs(activity)
        assertEquals(112, glyphs.size)
        for (text in listOf("João", "coração", "criança", "Ação 123")) {
            val drawing = StrokeRepository.drawing(activity, text)
            assertEquals(text, drawing.text)
            assertTrue(drawing.width > 0f)
            assertTrue(drawing.strokes.isNotEmpty())
            for (stroke in drawing.strokes) {
                assertTrue(stroke.size >= 2)
                assertEquals(0f, stroke.first()[7], .001f)
                assertTrue(stroke.last()[7] > 0f)
                assertTrue(stroke.zipWithNext().all { (first, next) -> next[7] >= first[7] })
                assertTrue(stroke.all { point -> point.size == 8 && point.all { it.isFinite() } })
            }
        }
    }
}
