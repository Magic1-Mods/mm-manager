package bin.mg.editor.core.document

interface DocumentListener {
    fun onContentChanged(startLine: Int, endLine: Int, newLineCount: Int)
    fun onUndoStateChanged(canUndo: Boolean, canRedo: Boolean)
}
