package bin.mg.editor.core.undo

import bin.mg.editor.core.buffer.PieceTable

class UndoManager(private val buffer: PieceTable) {

    data class EditAction(
        val type: Type,
        val offset: Int,
        val deletedText: String,
        val insertedText: String,
        val cursorLine: Int,
        val cursorColumn: Int
    ) {
        enum class Type { INSERT, DELETE, REPLACE }
    }

    private val actions = mutableListOf<EditAction>()
    private var currentIndex = -1

    var onStateChanged: ((canUndo: Boolean, canRedo: Boolean) -> Unit)? = null

    fun recordInsert(offset: Int, text: String, cursorLine: Int, cursorColumn: Int) {
        trimRedoStack()
        actions.add(EditAction(EditAction.Type.INSERT, offset, "", text, cursorLine, cursorColumn))
        if (actions.size > 500) actions.removeAt(0)
        currentIndex = actions.size - 1
        notifyStateChanged()
    }

    fun recordDelete(offset: Int, text: String, cursorLine: Int, cursorColumn: Int) {
        trimRedoStack()
        actions.add(EditAction(EditAction.Type.DELETE, offset, text, "", cursorLine, cursorColumn))
        if (actions.size > 500) actions.removeAt(0)
        currentIndex = actions.size - 1
        notifyStateChanged()
    }

    fun recordReplace(offset: Int, deletedText: String, insertedText: String, cursorLine: Int, cursorColumn: Int) {
        trimRedoStack()
        actions.add(EditAction(EditAction.Type.REPLACE, offset, deletedText, insertedText, cursorLine, cursorColumn))
        if (actions.size > 500) actions.removeAt(0)
        currentIndex = actions.size - 1
        notifyStateChanged()
    }

    fun undo(): Boolean {
        if (currentIndex < 0) return false
        val action = actions[currentIndex]
        when (action.type) {
            EditAction.Type.INSERT -> {
                buffer.delete(action.offset, action.insertedText.length)
            }
            EditAction.Type.DELETE -> {
                buffer.insert(action.offset, action.deletedText)
            }
            EditAction.Type.REPLACE -> {
                buffer.delete(action.offset, action.insertedText.length)
                buffer.insert(action.offset, action.deletedText)
            }
        }
        currentIndex--
        notifyStateChanged()
        return true
    }

    fun redo(): Boolean {
        if (currentIndex >= actions.size - 1) return false
        currentIndex++
        val action = actions[currentIndex]
        when (action.type) {
            EditAction.Type.INSERT -> {
                buffer.insert(action.offset, action.insertedText)
            }
            EditAction.Type.DELETE -> {
                buffer.delete(action.offset, action.deletedText.length)
            }
            EditAction.Type.REPLACE -> {
                buffer.delete(action.offset, action.deletedText.length)
                buffer.insert(action.offset, action.insertedText)
            }
        }
        notifyStateChanged()
        return true
    }

    fun canUndo(): Boolean = currentIndex >= 0
    fun canRedo(): Boolean = currentIndex < actions.size - 1

    fun clear() {
        actions.clear()
        currentIndex = -1
        notifyStateChanged()
    }

    private fun trimRedoStack() {
        if (currentIndex < actions.size - 1) {
            actions.subList(currentIndex + 1, actions.size).clear()
        }
    }

    private fun notifyStateChanged() {
        onStateChanged?.invoke(canUndo(), canRedo())
    }
}
