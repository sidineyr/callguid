package com.sidineyr.callguide

import android.content.Context
import org.json.JSONObject

object StrokeRepository {
    private var cached: Map<String, PenGlyph>? = null
    @Synchronized fun glyphs(context: Context): Map<String, PenGlyph> {
        cached?.let { return it }
        val json = JSONObject(context.assets.open("letters.json").bufferedReader().use { it.readText() })
        val result = json.keys().asSequence().associateWith { char ->
            val item = json.getJSONObject(char)
            val lines = item.getJSONArray("penStrokes")
            PenGlyph(item.getDouble("advance").toFloat(), (0 until lines.length()).map { i ->
                val samples = lines.getJSONArray(i)
                (0 until samples.length()).map { j ->
                    val point = samples.getJSONArray(j)
                    FloatArray(8) { k -> point.getDouble(k).toFloat() }
                }
            })
        }
        cached = result
        return result
    }
    fun drawing(context: Context, text: String) = WordComposer.compose(text, glyphs(context))
}
