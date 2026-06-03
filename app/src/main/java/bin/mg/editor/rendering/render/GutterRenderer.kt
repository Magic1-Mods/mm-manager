package bin.mg.editor.rendering.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import bin.mg.editor.rendering.layout.LineLayout

class GutterRenderer {

    private val lineNumberPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val lineNumberCurrentPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val gutterBgPaint = Paint()
    private val currentLineBgPaint = Paint()
    private val lineSeparatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    var lineNumberColor = 0xFF606366.toInt()
        set(value) { field = value; lineNumberPaint.color = value }
    var lineNumberCurrentColor = 0xFFA0A0A0.toInt()
        set(value) { field = value; lineNumberCurrentPaint.color = value }
    var gutterBackgroundColor = 0xFF1E1E1E.toInt()
        set(value) { field = value; gutterBgPaint.color = value }
    var currentLineColor = 0xFF2A2D2E.toInt()
        set(value) { field = value; currentLineBgPaint.color = value }
    var separatorColor = 0xFF333333.toInt()
        set(value) { field = value; lineSeparatorPaint.color = value }

    fun configure(fontSize: Float) {
        lineNumberPaint.textSize = fontSize
        lineNumberPaint.typeface = android.graphics.Typeface.MONOSPACE
        lineNumberPaint.textAlign = Paint.Align.RIGHT
        lineNumberCurrentPaint.textSize = fontSize
        lineNumberCurrentPaint.typeface = android.graphics.Typeface.MONOSPACE
        lineNumberCurrentPaint.textAlign = Paint.Align.RIGHT
        lineSeparatorPaint.strokeWidth = 1f
    }

    fun draw(
        canvas: Canvas,
        gutterWidth: Float,
        firstVisibleLine: Int,
        visibleLineCount: Int,
        scrollY: Float,
        lineHeight: Float,
        currentLine: Int,
        lineHeightPx: Float,
        lineStartOffset: Float
    ) {
        val totalHeight = canvas.height.toFloat()
        val top = 0f

        // Gutter background
        canvas.drawRect(0f, top, gutterWidth, totalHeight, gutterBgPaint)

        // Separator line
        canvas.drawLine(gutterWidth - 1, top, gutterWidth - 1, totalHeight, lineSeparatorPaint)

        // Line numbers
        val textY = lineStartOffset
        for (i in 0..visibleLineCount) {
            val line = firstVisibleLine + i
            val y = textY + i * lineHeightPx

            // Current line highlight in gutter
            if (line == currentLine) {
                canvas.drawRect(0f, y, gutterWidth, y + lineHeightPx, currentLineBgPaint)
            }

            val lineNum = (line + 1).toString()
            val textBounds = android.graphics.Rect()
            val paint = if (line == currentLine) lineNumberCurrentPaint else lineNumberPaint
            paint.getTextBounds(lineNum, 0, lineNum.length, textBounds)
            val textHeight = textBounds.height().toFloat()

            canvas.drawText(
                lineNum,
                gutterWidth - 16f,
                y + (lineHeightPx + textHeight) / 2f - 2f,
                paint
            )
        }
    }
}
