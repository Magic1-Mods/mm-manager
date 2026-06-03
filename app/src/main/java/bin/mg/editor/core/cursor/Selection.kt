package bin.mg.editor.core.cursor

data class Selection(
    var start: Cursor = Cursor(),
    var end: Cursor = Cursor(),
    var isValid: Boolean = false
) {
    fun set(startLine: Int, startCol: Int, endLine: Int, endCol: Int) {
        start.moveTo(startLine, startCol)
        end.moveTo(endLine, endCol)
        isValid = true
    }

    fun setFromCursors(anchor: Cursor, focus: Cursor) {
        if (anchor.isBeforeOrAt(focus)) {
            start = anchor.copy()
            end = focus.copy()
        } else {
            start = focus.copy()
            end = anchor.copy()
        }
        isValid = true
    }

    fun clear() {
        start.moveTo(0, 0)
        end.moveTo(0, 0)
        isValid = false
    }

    fun isCollapsed(): Boolean = start.isAtSamePosition(end)

    fun contains(line: Int, column: Int): Boolean {
        if (!isValid || isCollapsed()) return false
        val c = Cursor(line, column)
        return c.isAfter(start) && c.isBefore(end)
    }

    fun containsOrEqual(line: Int, column: Int): Boolean {
        if (!isValid || isCollapsed()) return false
        val c = Cursor(line, column)
        return c.isAfterOrAt(start) && c.isBeforeOrAt(end)
    }

    fun normalizedStart(): Cursor = start.minOf(end)
    fun normalizedEnd(): Cursor = start.maxOf(end)

    fun textLength(): Int {
        if (!isValid || isCollapsed()) return 0
        val s = normalizedStart()
        val e = normalizedEnd()
        return if (s.line == e.line) {
            e.column - s.column
        } else {
            (e.line - s.line) * 100000 + e.column - s.column
        }
    }

    fun getSelectedText(buffer: bin.mg.editor.core.buffer.PieceTable): String {
        if (!isValid || isCollapsed()) return ""
        val s = normalizedStart()
        val e = normalizedEnd()
        val startOffset = buffer.lineColumnToOffset(s.line, s.column)
        val endOffset = buffer.lineColumnToOffset(e.line, e.column)
        return buffer.substring(startOffset, endOffset - startOffset)
    }
}
