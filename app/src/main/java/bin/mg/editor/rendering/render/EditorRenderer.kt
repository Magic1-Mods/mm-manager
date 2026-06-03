package bin.mg.editor.rendering.render

import android.graphics.Canvas
import android.text.TextPaint
import bin.mg.editor.core.buffer.TextSpan
import bin.mg.editor.core.document.EditorBuffer
import bin.mg.editor.rendering.layout.LayoutEngine

class EditorRenderer(
    private val textPaint: TextPaint,
    private val layoutEngine: LayoutEngine
) {

    private val gutterRenderer = GutterRenderer()
    private val selectionRenderer = SelectionRenderer()
    private val cursorRenderer = CursorRenderer()

    var lineNumberColor: Int
        get() = gutterRenderer.lineNumberColor
        set(value) { gutterRenderer.lineNumberColor = value }
    var lineNumberCurrentColor: Int
        get() = gutterRenderer.lineNumberCurrentColor
        set(value) { gutterRenderer.lineNumberCurrentColor = value }
    var gutterBgColor: Int
        get() = gutterRenderer.gutterBackgroundColor
        set(value) { gutterRenderer.gutterBackgroundColor = value }
    var currentLineColor: Int
        get() = gutterRenderer.currentLineColor
        set(value) { gutterRenderer.currentLineColor = value; selectionRenderer.currentLineColor = value }
    var separatorColor: Int
        get() = gutterRenderer.separatorColor
        set(value) { gutterRenderer.separatorColor = value }
    var selectionColor: Int
        get() = selectionRenderer.selectionColor
        set(value) { selectionRenderer.selectionColor = value }
    var textColor: Int
        get() = textPaint.color
        set(value) { textPaint.color = value }
    var backgroundColor: Int = 0xFF1E1E1E.toInt()
    var cursorColor: Int
        get() = cursorRenderer.cursorColor
        set(value) { cursorRenderer.cursorColor = value }

    fun configure(fontSize: Float, tabSize: Int = 4) {
        layoutEngine.configure(fontSize, tabSize)
        gutterRenderer.configure(fontSize)
    }

    fun startCursorBlink() = cursorRenderer.startBlinking()
    fun stopCursorBlink() = cursorRenderer.stopBlinking()
    fun resetCursorBlink() = cursorRenderer.resetBlink()

    fun render(
        canvas: Canvas,
        buffer: EditorBuffer,
        scrollX: Float,
        scrollY: Float,
        firstVisibleLine: Int,
        visibleLineCount: Int,
        currentLine: Int,
        selectionStart: Pair<Int, Int>?,
        selectionEnd: Pair<Int, Int>?,
        secondaryCursors: List<Pair<Int, Int>>?,
        contentWidth: Float,
        contentHeight: Float,
        textSpans: Map<Int, List<TextSpan>>?
    ) {
        val totalWidth = canvas.width.toFloat()
        val totalHeight = canvas.height.toFloat()
        val lineHeight = layoutEngine.lineHeight
        val lineCount = buffer.getLineCount()

        // Background
        canvas.drawColor(backgroundColor)

        // Gutter
        layoutEngine.updateGutterWidth(lineCount)
        val effectiveGutterWidth = layoutEngine.gutterWidth

        gutterRenderer.draw(
            canvas,
            effectiveGutterWidth,
            firstVisibleLine,
            visibleLineCount,
            scrollY,
            lineHeight,
            currentLine,
            lineHeight,
            -scrollY
        )

        // Draw lines
        val lastVisibleLine = (firstVisibleLine + visibleLineCount).coerceAtMost(lineCount - 1)
        for (line in firstVisibleLine..lastVisibleLine) {
            val lineText = buffer.getLineText(line)
            val layout = layoutEngine.getLineLayout(line, lineText)
            val y = -scrollY + (line - firstVisibleLine) * lineHeight

            // Current line highlight
            if (line == currentLine) {
                selectionRenderer.drawCurrentLineHighlight(
                    canvas, effectiveGutterWidth, y, lineHeight, totalWidth
                )
            }

            // Selection highlight
            if (selectionStart != null && selectionEnd != null) {
                val selStartLine = selectionStart.first
                val selEndLine = selectionEnd.first
                val selStartCol = selectionStart.second
                val selEndCol = selectionEnd.second

                if (selStartLine == selEndLine && line == selStartLine) {
                    selectionRenderer.drawSelection(
                        canvas, effectiveGutterWidth, y, lineHeight,
                        selStartCol, selEndCol, layout, totalWidth
                    )
                } else if (line == selStartLine) {
                    selectionRenderer.drawSelection(
                        canvas, effectiveGutterWidth, y, lineHeight,
                        selStartCol, lineText.length, layout, totalWidth
                    )
                } else if (line == selEndLine) {
                    selectionRenderer.drawSelection(
                        canvas, effectiveGutterWidth, y, lineHeight,
                        0, selEndCol, layout, totalWidth
                    )
                } else if (line in (selStartLine + 1) until selEndLine) {
                    selectionRenderer.drawSelection(
                        canvas, effectiveGutterWidth, y, lineHeight,
                        0, lineText.length, layout, totalWidth
                    )
                }
            }

            // Draw text with syntax colors
            drawLineWithSpans(canvas, effectiveGutterWidth, y, layout, textSpans?.get(line), totalWidth)
        }

        // Draw secondary cursors
        secondaryCursors?.forEach { (line, col) ->
            if (line in firstVisibleLine..lastVisibleLine) {
                val y = -scrollY + (line - firstVisibleLine) * lineHeight
                val layout = layoutEngine.getLineLayout(line, buffer.getLineText(line))
                val x = effectiveGutterWidth + layout.getCharOffset(col)
                cursorRenderer.drawSecondary(canvas, x, y, lineHeight)
            }
        }

        // Draw primary cursor
        if (currentLine in firstVisibleLine..lastVisibleLine) {
            val y = -scrollY + (currentLine - firstVisibleLine) * lineHeight
            val layout = layoutEngine.getLineLayout(currentLine, buffer.getLineText(currentLine))
            val cursorCol = buffer.cursorManager.cursor.column
            val x = effectiveGutterWidth + layout.getCharOffset(cursorCol)
            cursorRenderer.draw(canvas, x, y, lineHeight)
        }
    }

    private fun drawLineWithSpans(
        canvas: Canvas,
        x: Float,
        y: Float,
        layout: bin.mg.editor.rendering.layout.LineLayout,
        spans: List<TextSpan>?,
        contentWidth: Float
    ) {
        if (layout.text.isEmpty()) return
        if (spans == null || spans.isEmpty()) {
            canvas.drawText(layout.text, 0, layout.text.length, x, y + layout.baseline, textPaint)
            return
        }

        var pos = 0
        for (span in spans) {
            if (span.start >= layout.text.length) continue
            if (pos < span.start) {
                textPaint.color = defaultTextColor()
                val startX = x + layout.getCharOffset(pos)
                val endIdx = span.start.coerceAtMost(layout.text.length)
                canvas.drawText(layout.text, pos, endIdx, startX, y + layout.baseline, textPaint)
            }
            if (span.start < layout.text.length) {
                textPaint.color = span.color
                if (span.bold) textPaint.isFakeBoldText = true
                if (span.italic) textPaint.textSkewX = -0.1f
                val startX = x + layout.getCharOffset(span.start)
                val endIdx = span.end.coerceAtMost(layout.text.length)
                canvas.drawText(layout.text, span.start, endIdx, startX, y + layout.baseline, textPaint)
                textPaint.isFakeBoldText = false
                textPaint.textSkewX = 0f
                pos = endIdx
            }
        }
        if (pos < layout.text.length) {
            textPaint.color = defaultTextColor()
            val startX = x + layout.getCharOffset(pos)
            canvas.drawText(layout.text, pos, layout.text.length, startX, y + layout.baseline, textPaint)
        }
    }

    private fun defaultTextColor(): Int = 0xFFBBBBBB.toInt()
}
