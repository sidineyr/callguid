package com.sidineyr.callguide

import org.junit.Assert.*
import org.junit.Test

class WordComposerTest {
    private fun sample(x: Float, arc: Float) = floatArrayOf(x, -65f, x-2f, -67f, x+2f, -63f, 4f, arc)
    private fun glyph(advance: Float = 100f) = PenGlyph(advance,
        listOf(listOf(sample(0f, 0f), sample(60f, 75f))))
    private val glyphs = mapOf("a" to glyph(), "A" to glyph(150f), "ç" to glyph(), "ã" to glyph())

    @Test fun decomposedPortugueseAccentsBecomeAvailableLettersWithoutLosingCase() {
        val drawing = WordComposer.compose(" A\u00a0c\u0327a\u0303 ", glyphs)
        assertEquals("A çã", drawing.text)
        assertEquals(3, drawing.strokes.size)
    }

    @Test fun whitespaceIsCollapsedIncludingTabsLineBreaksAndUnicodeSpaces() {
        assertEquals("a A a", WordComposer.normalize(" \ta\n\r A\u2003\u00a0a  "))
    }

    @Test fun advancesSpaceAndCapitalRoomHaveNoTrailingExtraGap() {
        assertEquals(200f, WordComposer.compose("aa", glyphs).width, 0f)
        assertEquals(280f, WordComposer.compose("Aa", glyphs).width, 0f)
        assertEquals(150f, WordComposer.compose("A", glyphs).width, 0f)
        assertEquals(470f, WordComposer.compose("A a", glyphs).width, 0f)
        assertEquals(180f, WordComposer.compose("Aa", glyphs).strokes[1][0][0], 0f)
        assertEquals(370f, WordComposer.compose("A a", glyphs).strokes[1][0][0], 0f)
    }

    @Test fun translatedCopiesPreservePressureBaselineArcLengthsAndOriginalGlyph() {
        val drawing = WordComposer.compose("aa", glyphs)
        val original = glyphs.getValue("a").strokes[0][1]
        val translated = drawing.strokes[1][1]
        assertArrayEquals(floatArrayOf(160f,-65f,158f,-67f,162f,-63f,4f,75f), translated, 0f)
        assertEquals(60f, original[0], 0f)
        drawing.strokes[0][1][0] = 999f
        assertEquals(60f, original[0], 0f)
        assertEquals(160f, translated[0], 0f)
    }

    @Test fun characterLimitAppliesAfterNormalizationAndIncludesSpaces() {
        assertEquals(32, WordComposer.compose("a".repeat(32), glyphs).text.length)
        assertRejected("a".repeat(33), "32")
        assertRejected("a".repeat(16) + " " + "a".repeat(16), "32")
    }

    @Test fun emptyUnsupportedDigitsAndEmojiFailExplicitlyInsteadOfBeingDropped() {
        assertRejected(" \n ", "Digite")
        assertRejected("a1", "1")
        assertRejected("a😀", "😀")
        assertRejected("á", "á")
        assertEquals("1", WordComposer.compose("1", mapOf("1" to glyph())).text)
    }

    private fun assertRejected(text: String, messagePart: String) {
        try {
            WordComposer.compose(text, glyphs)
            fail("A entrada deveria ter sido recusada: $text")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message.orEmpty().contains(messagePart))
        }
    }
}
