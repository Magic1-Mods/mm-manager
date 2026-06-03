package bin.mg.editor.rendering.layout

import android.graphics.Paint
import android.text.TextPaint

data class LineLayout(
    val line: Int,
    val text: String,
    val width: Float,
    val height: Float,
    val baseline: Float,
    val charOffsets: FloatArray
) {
    fun getCharOffset(column: Int): Float {
        if (column < 0) return 0f
        if (column >= charOffsets.size) return charOffsets.lastOrNull() ?: 0f
        return charOffsets[column]
    }

    fun getColumnAtX(x: Float): Int {
        if (charOffsets.isEmpty()) return 0
        if (x <= charOffsets[0]) return 0
        var lo = 0
        var hi = charOffsets.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) ushr 1
            if (charOffsets[mid] <= x) lo = mid else hi = mid - 1
        }
        return lo
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LineLayout) return false
        return line == other.line && text == other.text
    }

    override fun hashCode(): Int = 31 * line + text.hashCode()
}
