package bin.mg.editor.rendering.layout

import android.text.TextPaint

class LayoutEngine(private val textPaint: TextPaint) {

    private val lineCache = LineCache()

    var tabWidth: Float = 0f
        private set
    var tabSize: Int = 4
        private set
    var lineHeight: Float = 0f
        private set
    var gutterWidth: Float = 0f
        private set
    var gutterDigitCount: Int = 0

    fun configure(fontSize: Float, tabSize: Int = 4) {
        textPaint.textSize = fontSize
        this.tabSize = tabSize
        tabWidth = textPaint.measureText(" ").repeat(tabSize)
        val fm = textPaint.fontMetrics
        lineHeight = (fm.descent - fm.ascent) * 1.1f
        lineCache.clear()
    }

    fun setLineSpacing(extra: Float, multiplier: Float) {
        lineCache.setLineSpacing(extra, multiplier)
    }

    fun updateGutterWidth(lineCount: Int) {
        val newDigitCount = lineCount.coerceAtLeast(1).toString().length.coerceAtLeast(3)
        if (newDigitCount != gutterDigitCount) {
            gutterDigitCount = newDigitCount
            gutterWidth = textPaint.measureText("0".repeat(gutterDigitCount)) + 48f
        }
    }

    fun getLineLayout(line: Int, text: String): LineLayout {
        return lineCache.getLineLayout(line, text, textPaint, tabWidth, tabSize)
    }

    fun invalidateLine(line: Int) {
        lineCache.invalidate(line)
    }

    fun invalidateRange(startLine: Int, endLine: Int) {
        lineCache.invalidateRange(startLine, endLine)
    }

    fun clearCache() {
        lineCache.clear()
    }

    fun measureTextWidth(text: String): Float {
        return textPaint.measureText(text)
    }

    fun getLineHeight(): Float = lineHeight

    fun getGutterWidth(): Float = gutterWidth
}
