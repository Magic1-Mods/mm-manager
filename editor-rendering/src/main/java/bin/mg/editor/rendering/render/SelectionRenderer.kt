package bin.mg.editor.rendering.render

import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextPaint
import bin.mg.editor.rendering.layout.LineLayout

class SelectionRenderer {

    private val selectionPaint = Paint()
    private val currentLinePaint = Paint()
    private val matchedBracketPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    var selectionColor = 0x40214283
        set(value) { field = value; selectionPaint.color = value }
    var currentLineColor = 0x302A2D2E
        set(value) { field = value; currentLinePaint.color = value }
    var matchedBracketColor = 0x4032593A
        set(value) { field = value; matchedBracketPaint.color = value }

    fun drawCurrentLineHighlight(
        canvas: Canvas,
        gutterWidth: Float,
        y: Float,
        lineHeight: Float,
        contentWidth: Float
    ) {
        canvas.drawRect(gutterWidth, y, contentWidth, y + lineHeight, currentLinePaint)
    }

    fun drawSelection(
        canvas: Canvas,
        gutterWidth: Float,
        y: Float,
        lineHeight: Float,
        startColumn: Int,
        endColumn: Int,
        lineLayout: LineLayout,
        contentWidth: Float
    ) {
        if (startColumn == endColumn) return
        val startX = gutterWidth + lineLayout.getCharOffset(startColumn.coerceAtMost(lineLayout.charOffsets.size))
        val endX = gutterWidth + lineLayout.getCharOffset(endColumn.coerceAtMost(lineLayout.charOffsets.size))
        val clampedStartX = startX.coerceAtMost(contentWidth)
        val clampedEndX = endX.coerceIn(clampedStartX, contentWidth)
        if (clampedEndX > clampedStartX) {
            canvas.drawRect(clampedStartX, y, clampedEndX, y + lineHeight, selectionPaint)
        }
    }

    fun drawSelectionMultiLine(
        canvas: Canvas,
        gutterWidth: Float,
        firstLineY: Float,
        lineHeight: Float,
        startColumn: Int,
        endColumn: Int,
        startLineLayout: LineLayout,
        endLineLayout: LineLayout,
        startLine: Int,
        endLine: Int,
        contentWidth: Float
    ) {
        // First line (from startColumn to end of line)
        val firstStartX = gutterWidth + startLineLayout.getCharOffset(startColumn)
        canvas.drawRect(firstStartX, firstLineY, contentWidth, firstLineY + lineHeight, selectionPaint)

        // Middle lines (full width)
        for (line in (startLine + 1) until endLine) {
            val y = firstLineY + (line - startLine) * lineHeight
            canvas.drawRect(gutterWidth, y, contentWidth, y + lineHeight, selectionPaint)
        }

        // Last line (from start of line to endColumn)
        if (endLine > startLine) {
            val lastY = firstLineY + (endLine - startLine) * lineHeight
            val lastEndX = gutterWidth + endLineLayout.getCharOffset(endColumn.coerceAtMost(endLineLayout.charOffsets.size))
            canvas.drawRect(gutterWidth, lastY, lastEndX.coerceAtMost(contentWidth), lastY + lineHeight, selectionPaint)
        }
    }
}
