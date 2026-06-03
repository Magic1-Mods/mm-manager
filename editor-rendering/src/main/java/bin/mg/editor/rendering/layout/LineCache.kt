package bin.mg.editor.rendering.layout

import android.util.LruCache
import android.text.TextPaint

class LineCache(private val maxCapacity: Int = 300) {

    private val cache = LruCache<Int, LineLayout>(maxCapacity)
    private var lineSpacingExtra: Float = 0f
    private var lineSpacingMultiplier: Float = 1.0f

    fun setLineSpacing(extra: Float, multiplier: Float) {
        lineSpacingExtra = extra
        lineSpacingMultiplier = multiplier
    }

    fun getLineLayout(
        line: Int,
        text: String,
        paint: TextPaint,
        tabWidth: Float,
        tabSize: Int = 4
    ): LineLayout {
        cache.get(line)?.let { cached ->
            if (cached.text == text) return cached
        }

        val charOffsets = computeCharOffsets(text, paint, tabWidth, tabSize)
        val width = if (charOffsets.isNotEmpty()) {
            val lastCharWidth = paint.measureText(text.last().toString())
            charOffsets.last() + lastCharWidth
        } else 0f

        val fm = paint.fontMetrics
        val rawHeight = fm.descent - fm.ascent
        val height = rawHeight * lineSpacingMultiplier + lineSpacingExtra
        val baseline = -fm.ascent

        val layout = LineLayout(line, text, width, height, baseline, charOffsets)
        cache.put(line, layout)
        return layout
    }

    fun invalidate(line: Int) {
        cache.remove(line)
    }

    fun invalidateRange(startLine: Int, endLine: Int) {
        for (i in startLine..endLine) cache.remove(i)
    }

    fun clear() {
        cache.evictAll()
    }

    private fun computeCharOffsets(text: String, paint: TextPaint, tabWidth: Float, tabSize: Int): FloatArray {
        if (text.isEmpty()) return FloatArray(0)
        val offsets = FloatArray(text.length)
        var x = 0f
        var measured = 0f
        var tabStop = tabWidth * tabSize
        for (i in text.indices) {
            offsets[i] = x
            val c = text[i]
            if (c == '\t') {
                val nextTabStop = ((x / tabWidth).toInt() + 1) * tabWidth
                x = nextTabStop
            } else {
                x += paint.measureText(c.toString())
            }
        }
        return offsets
    }

    fun getEstimatedMemoryBytes(): Int {
        return cache.size() * 256
    }
}
