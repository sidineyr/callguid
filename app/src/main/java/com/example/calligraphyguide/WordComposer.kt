package com.sidineyr.callguide

import java.text.Normalizer

/** Pen samples: x, y, left x/y, right x/y, width, cumulative stroke distance. */
data class PenGlyph(val advance: Float, val strokes: List<List<FloatArray>>)
data class PenDrawing(val text: String, val strokes: List<List<FloatArray>>, val width: Float)

/** Places authored letters on their common baseline without altering their pressure or stroke order. */
object WordComposer {
    const val MAX_LENGTH = 32
    private const val SPACE_ADVANCE = 220f
    private const val CAPITAL_GAP = 30f

    fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFC)
        .replace(Regex("[\\s\\p{Z}]+"), " ").trim()

    fun compose(text: String, glyphs: Map<String, PenGlyph>): PenDrawing {
        val normalized = normalize(text)
        require(normalized.isNotEmpty()) { "Digite uma palavra ou uma frase curta." }
        val codepoints = normalized.codePoints().toArray()
        require(codepoints.size <= MAX_LENGTH) { "Use no máximo $MAX_LENGTH caracteres, incluindo espaços." }
        val characters = codepoints.map { String(Character.toChars(it)) }
        val unsupported = characters.filter { it != " " && it !in glyphs }.distinct()
        require(unsupported.isEmpty()) {
            "Ainda não há modelo para: ${unsupported.joinToString(" ")}. Use letras disponíveis e espaços."
        }

        val result = mutableListOf<List<FloatArray>>()
        var offset = 0f
        characters.forEachIndexed { index, character ->
            if (character == " ") {
                offset += SPACE_ADVANCE
            } else {
                val glyph = glyphs.getValue(character)
                glyph.strokes.forEach { stroke ->
                    result.add(stroke.map { sample ->
                        require(sample.size == 8) { "Modelo de letra inválido: $character." }
                        sample.copyOf().apply {
                            this[0] += offset
                            this[2] += offset
                            this[4] += offset
                        }
                    })
                }
                offset += glyph.advance
                // Extra room after a capital, but no trailing gap or duplicate space before a word break.
                if (index < characters.lastIndex && characters[index + 1] != " " &&
                    Character.isUpperCase(codepoints[index])) offset += CAPITAL_GAP
            }
        }
        return PenDrawing(normalized, result, offset)
    }
}
