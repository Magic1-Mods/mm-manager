package bin.mg.editor.core.cursor

import bin.mg.editor.core.buffer.PieceTable

class CursorManager(private val buffer: PieceTable) {

    val cursor = Cursor(0, 0)
    val selection = Selection()
    val secondaryCursors = mutableListOf<Cursor>()

    private val listeners = mutableListOf<CursorListener>()

    interface CursorListener {
        fun onCursorMoved(line: Int, column: Int)
        fun onSelectionChanged(startLine: Int, startCol: Int, endLine: Int, endCol: Int, isValid: Boolean)
    }

    fun addListener(listener: CursorListener) { listeners.add(listener) }
    fun removeListener(listener: CursorListener) { listeners.remove(listener) }

    fun moveTo(line: Int, column: Int, select: Boolean = false) {
        val maxLine = buffer.lineCount() - 1
        val clampedLine = line.coerceIn(0, maxLine)
        val maxCol = buffer.getLineText(clampedLine).length
        val clampedColumn = column.coerceIn(0, maxCol)

        if (select) {
            if (!selection.isValid) {
                selection.set(cursor.line, cursor.column, clampedLine, clampedColumn)
            } else {
                selection.end.moveTo(clampedLine, clampedColumn)
            }
            cursor.moveTo(clampedLine, clampedColumn)
        } else {
            selection.clear()
            cursor.moveTo(clampedLine, clampedColumn)
        }
        notifyCursorMoved()
    }

    fun moveBy(offset: Int, select: Boolean = false) {
        val currentOffset = buffer.lineColumnToOffset(cursor.line, cursor.column)
        val newOffset = (currentOffset + offset).coerceIn(0, buffer.length())
        val newLine = buffer.offsetToLine(newOffset)
        val newCol = buffer.offsetToColumn(newOffset)
        moveTo(newLine, newCol, select)
    }

    fun moveLeft(select: Boolean = false) {
        if (selection.isValid && !selection.isCollapsed() && !select) {
            val s = selection.normalizedStart()
            selection.clear()
            cursor.moveTo(s.line, s.column)
            notifyCursorMoved()
            return
        }
        moveBy(-1, select)
    }

    fun moveRight(select: Boolean = false) {
        if (selection.isValid && !selection.isCollapsed() && !select) {
            val e = selection.normalizedEnd()
            selection.clear()
            cursor.moveTo(e.line, e.column)
            notifyCursorMoved()
            return
        }
        moveBy(1, select)
    }

    fun moveUp(select: Boolean = false) {
        moveTo(cursor.line - 1, cursor.column, select)
    }

    fun moveDown(select: Boolean = false) {
        moveTo(cursor.line + 1, cursor.column, select)
    }

    fun moveHome(select: Boolean = false) {
        moveTo(cursor.line, 0, select)
    }

    fun moveEnd(select: Boolean = false) {
        val lineLen = buffer.getLineText(cursor.line).length
        moveTo(cursor.line, lineLen, select)
    }

    fun moveWordLeft(select: Boolean = false) {
        var offset = buffer.lineColumnToOffset(cursor.line, cursor.column) - 1
        if (offset < 0) { moveTo(0, 0, select); return }
        while (offset > 0 && !buffer.charAt(offset).isLetterOrDigit() && buffer.charAt(offset) != '\n') offset--
        while (offset > 0 && buffer.charAt(offset - 1).isLetterOrDigit()) offset--
        moveTo(buffer.offsetToLine(offset), buffer.offsetToColumn(offset), select)
    }

    fun moveWordRight(select: Boolean = false) {
        var offset = buffer.lineColumnToOffset(cursor.line, cursor.column)
        if (offset >= buffer.length()) return
        while (offset < buffer.length() && !buffer.charAt(offset).isLetterOrDigit() && buffer.charAt(offset) != '\n') offset++
        while (offset < buffer.length() && buffer.charAt(offset).isLetterOrDigit()) offset++
        moveTo(buffer.offsetToLine(offset), buffer.offsetToColumn(offset), select)
    }

    fun selectAll() {
        val lastLine = buffer.lineCount() - 1
        val lastCol = buffer.getLineText(lastLine).length
        selection.set(0, 0, lastLine, lastCol)
        cursor.moveTo(lastLine, lastCol)
        notifySelectionChanged()
    }

    fun selectLine(line: Int) {
        val lineLen = buffer.getLineText(line).length
        selection.set(line, 0, line, lineLen)
        cursor.moveTo(line, lineLen)
        notifySelectionChanged()
    }

    fun selectWordAt(line: Int, column: Int) {
        val text = buffer.getLineText(line)
        if (column >= text.length) return
        var start = column
        var end = column
        while (start > 0 && text[start - 1].isLetterOrDigit()) start--
        while (end < text.length && text[end].isLetterOrDigit()) end++
        selection.set(line, start, line, end)
        cursor.moveTo(line, end)
        notifySelectionChanged()
    }

    fun getSelectedText(): String = selection.getSelectedText(buffer)

    fun deleteSelection() {
        if (!selection.isValid || selection.isCollapsed()) return
        val s = selection.normalizedStart()
        val e = selection.normalizedEnd()
        val startOffset = buffer.lineColumnToOffset(s.line, s.column)
        val endOffset = buffer.lineColumnToOffset(e.line, e.column)
        buffer.delete(startOffset, endOffset - startOffset)
        selection.clear()
        cursor.moveTo(s.line, s.column)
        notifyCursorMoved()
    }

    fun insertText(text: String) {
        if (selection.isValid && !selection.isCollapsed()) {
            deleteSelection()
        }
        val offset = buffer.lineColumnToOffset(cursor.line, cursor.column)
        buffer.insert(offset, text)
        // Update cursor position
        val newlines = text.count { it == '\n' }
        if (newlines > 0) {
            val lastNl = text.lastIndexOf('\n')
            cursor.line += newlines
            cursor.column = text.length - lastNl - 1
        } else {
            cursor.column += text.length
        }
        notifyCursorMoved()
    }

    fun addSecondaryCursor(line: Int, column: Int) {
        secondaryCursors.add(Cursor(line, column))
    }

    fun clearSecondaryCursors() {
        secondaryCursors.clear()
    }

    fun getVisibleLineRange(firstVisibleLine: Int, visibleLineCount: Int): IntRange {
        val lastLine = buffer.lineCount() - 1
        return firstVisibleLine..(firstVisibleLine + visibleLineCount).coerceAtMost(lastLine)
    }

    private fun notifyCursorMoved() {
        for (l in listeners) l.onCursorMoved(cursor.line, cursor.column)
        if (selection.isValid) notifySelectionChanged()
    }

    private fun notifySelectionChanged() {
        val s = selection.normalizedStart()
        val e = selection.normalizedEnd()
        for (l in listeners) l.onSelectionChanged(s.line, s.column, e.line, e.column, selection.isValid)
    }
}
