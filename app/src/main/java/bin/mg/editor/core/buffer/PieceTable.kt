package bin.mg.editor.core.buffer

class PieceTable(initialText: String = "") {

    private var content = StringBuilder(initialText)
    private val lineStarts = mutableListOf<Int>()

    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()

    init {
        rebuildLineIndex()
    }

    fun length(): Int = content.length

    fun lineCount(): Int = lineStarts.size

    fun lineStartOffset(line: Int): Int {
        if (line < 0 || line >= lineStarts.size) return content.length
        return lineStarts[line]
    }

    fun lineEndOffset(line: Int): Int {
        if (line < 0) return 0
        if (line + 1 >= lineStarts.size) return content.length
        return lineStarts[line + 1].coerceAtMost(content.length)
    }

    fun offsetToLine(offset: Int): Int {
        if (offset <= 0) return 0
        val safeOffset = offset.coerceAtMost(content.length)
        var lo = 0
        var hi = lineStarts.size - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lineStarts[mid] <= safeOffset) lo = mid + 1 else hi = mid - 1
        }
        return maxOf(0, lo - 1)
    }

    fun offsetToColumn(offset: Int): Int {
        val line = offsetToLine(offset)
        if (line >= lineStarts.size) return 0
        return (offset - lineStarts[line]).coerceIn(0, getLineText(line).length)
    }

    fun lineColumnToOffset(line: Int, column: Int): Int {
        if (line < 0 || line >= lineStarts.size) return content.length
        val lineStart = lineStarts[line]
        val lineText = getLineText(line)
        return (lineStart + column).coerceIn(lineStart, lineStart + lineText.length)
    }

    fun charAt(offset: Int): Char {
        if (offset < 0 || offset >= content.length) throw IndexOutOfBoundsException("Offset $offset, length ${content.length}")
        return content[offset]
    }

    fun substring(offset: Int, length: Int): String {
        if (length <= 0 || offset >= content.length || offset < 0) return ""
        val safeOffset = offset.coerceIn(0, content.length)
        val end = (safeOffset + length).coerceAtMost(content.length)
        return content.substring(safeOffset, end)
    }

    fun getLineText(line: Int): String {
        if (line < 0 || line >= lineStarts.size) return ""
        val start = lineStarts[line].coerceAtMost(content.length)
        val end = lineEndOffset(line).coerceAtMost(content.length)
        if (start >= end) return ""
        return content.substring(start, end).trimEnd('\r', '\n')
    }

    fun insert(offset: Int, text: String) {
        if (text.isEmpty()) return
        saveUndoSnapshot()
        val safeOffset = offset.coerceIn(0, content.length)
        content.insert(safeOffset, text)
        updateLineIndexInsert(safeOffset, text)
        redoStack.clear()
    }

    fun delete(offset: Int, length: Int) {
        if (length <= 0 || offset < 0 || offset + length > content.length) return
        saveUndoSnapshot()
        content.delete(offset, offset + length)
        rebuildLineIndex()
        redoStack.clear()
    }

    fun replace(offset: Int, length: Int, text: String) {
        if (length > 0) delete(offset, length)
        insert(offset, text)
    }

    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        redoStack.add(content.toString())
        content = StringBuilder(undoStack.removeAt(undoStack.size - 1))
        rebuildLineIndex()
        return true
    }

    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        undoStack.add(content.toString())
        content = StringBuilder(redoStack.removeAt(redoStack.size - 1))
        rebuildLineIndex()
        return true
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    override fun toString(): String = content.toString()

    fun rebuildLineIndex() {
        lineStarts.clear()
        lineStarts.add(0)
        for (i in content.indices) {
            if (content[i] == '\n') {
                lineStarts.add(i + 1)
            }
        }
        if (lineStarts.isEmpty()) lineStarts.add(0)
    }

    private fun saveUndoSnapshot() {
        undoStack.add(content.toString())
        if (undoStack.size > 500) undoStack.removeAt(0)
    }

    private fun updateLineIndexInsert(offset: Int, text: String) {
        var newlines = 0
        for (c in text) if (c == '\n') newlines++
        if (newlines == 0) return
        rebuildLineIndex()
    }
}
