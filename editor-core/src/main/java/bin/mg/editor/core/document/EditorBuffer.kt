package bin.mg.editor.core.document

import bin.mg.editor.core.buffer.PieceTable
import bin.mg.editor.core.cursor.CursorManager
import bin.mg.editor.core.undo.UndoManager

class EditorBuffer(initialText: String = "") {

    val buffer = PieceTable(initialText)
    val cursorManager = CursorManager(buffer)
    val undoManager = UndoManager(buffer)

    private val documentListeners = mutableListOf<DocumentListener>()

    init {
        undoManager.onStateChanged = { canUndo, canRedo ->
            for (l in documentListeners) l.onUndoStateChanged(canUndo, canRedo)
        }
    }

    interface DocumentListener {
        fun onContentChanged(startLine: Int, endLine: Int, newLineCount: Int)
        fun onUndoStateChanged(canUndo: Boolean, canRedo: Boolean)
    }

    fun addDocumentListener(listener: DocumentListener) { documentListeners.add(listener) }
    fun removeDocumentListener(listener: DocumentListener) { documentListeners.remove(listener) }

    fun setText(text: String) {
        buffer.delete(0, buffer.length())
        buffer.insert(0, text)
        cursorManager.moveTo(0, 0)
        undoManager.clear()
        notifyContentChanged(0, buffer.lineCount() - 1, buffer.lineCount())
    }

    fun getText(): String = buffer.toString()

    fun getLineCount(): Int = buffer.lineCount()

    fun getLineText(line: Int): String = buffer.getLineText(line)

    fun insertText(text: String) {
        val cursor = cursorManager.cursor
        val offset = buffer.lineColumnToOffset(cursor.line, cursor.column)
        undoManager.recordInsert(offset, text, cursor.line, cursor.column)
        cursorManager.insertText(text)
        notifyContentChanged(0, buffer.lineCount() - 1, buffer.lineCount())
    }

    fun deleteSelection() {
        if (!cursorManager.selection.isValid || cursorManager.selection.isCollapsed()) return
        val s = cursorManager.selection.normalizedStart()
        val e = cursorManager.selection.normalizedEnd()
        val startOffset = buffer.lineColumnToOffset(s.line, s.column)
        val endOffset = buffer.lineColumnToOffset(e.line, e.column)
        val deletedText = buffer.substring(startOffset, endOffset - startOffset)
        undoManager.recordDelete(startOffset, deletedText, s.line, s.column)
        cursorManager.deleteSelection()
        notifyContentChanged(0, buffer.lineCount() - 1, buffer.lineCount())
    }

    fun undo(): Boolean {
        val result = undoManager.undo()
        if (result) notifyContentChanged(0, buffer.lineCount() - 1, buffer.lineCount())
        return result
    }

    fun redo(): Boolean {
        val result = undoManager.redo()
        if (result) notifyContentChanged(0, buffer.lineCount() - 1, buffer.lineCount())
        return result
    }

    fun length(): Int = buffer.length()

    private fun notifyContentChanged(startLine: Int, endLine: Int, newLineCount: Int) {
        for (l in documentListeners) l.onContentChanged(startLine, endLine, newLineCount)
    }
}
