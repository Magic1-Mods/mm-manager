package bin.mg.editor.rendering.input

import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import bin.mg.editor.core.document.EditorBuffer

class EditorInputConnection(
    private val buffer: EditorBuffer,
    private val onUpdate: () -> Unit
) : InputConnection {

    private var composingText: String = ""
    private var composingStart = -1
    private var composingEnd = -1

    override fun beginBatchEdit(): Boolean = true
    override fun endBatchEdit(): Boolean = true

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (text.isNullOrEmpty()) return true
        finishComposingText()
        buffer.insertText(text.toString())
        onUpdate()
        return true
    }

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (composingStart >= 0 && composingEnd > composingStart) {
            val cursor = buffer.cursorManager.cursor
            val savedLine = cursor.line
            val savedCol = cursor.column
            buffer.cursorManager.moveTo(composingStart / 100000, composingStart % 100000)
            buffer.cursorManager.selection.set(
                composingStart / 100000, composingStart % 100000,
                composingEnd / 100000, composingEnd % 100000
            )
            buffer.deleteSelection()
            cursor.moveTo(savedLine, savedCol)
        }
        if (text.isNullOrEmpty()) {
            composingText = ""
            composingStart = -1
            composingEnd = -1
            onUpdate()
            return true
        }
        composingText = text.toString()
        val cursor = buffer.cursorManager.cursor
        composingStart = cursor.line * 100000 + cursor.column
        buffer.insertText(composingText)
        composingEnd = composingStart + composingText.length
        onUpdate()
        return true
    }

    override fun finishComposingText(): Boolean {
        composingText = ""
        composingStart = -1
        composingEnd = -1
        onUpdate()
        return true
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        if (beforeLength > 0) {
            for (i in 0 until beforeLength) buffer.cursorManager.moveLeft()
            buffer.deleteSelection()
        }
        if (afterLength > 0) {
            val c = buffer.cursorManager.cursor
            buffer.cursorManager.selection.set(c.line, c.column, c.line, c.column + afterLength)
            buffer.deleteSelection()
        }
        onUpdate()
        return true
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        buffer.cursorManager.moveTo(start / 100000, start % 100000)
        buffer.cursorManager.selection.set(start / 100000, start % 100000, end / 100000, end % 100000)
        onUpdate()
        return true
    }

    override fun getExtractedText(request: android.view.inputmethod.ExtractedTextRequest?, flags: Int): android.view.inputmethod.ExtractedText {
        val et = android.view.inputmethod.ExtractedText()
        et.text = buffer.getText()
        et.startOffset = 0
        et.length = buffer.length()
        return et
    }

    override fun performEditorAction(actionCode: Int, event: KeyEvent?): Boolean {
        if (actionCode == EditorInfo.IME_ACTION_SEND || actionCode == EditorInfo.IME_ACTION_DONE) {
            finishComposingText()
            return true
        }
        return false
    }

    private fun getCurrentIndent(): String {
        val line = buffer.getLineText(buffer.cursorManager.cursor.line)
        val sb = StringBuilder()
        for (c in line) {
            if (c == ' ' || c == '\t') sb.append(c) else break
        }
        return sb.toString()
    }

    override fun closeConnection() {}
    override fun reportFullscreenMode(enabled: Boolean): Boolean = true
    override fun performContextMenuAction(id: Int): Boolean = false
    override fun performPrivateCommand(action: String?, extras: Bundle?): Boolean = false
    override fun clearMetaKeyStates(states: Int) {}
    override fun clearComposition() = finishComposingText()
    override fun deleteAllText(): Boolean = true
    override fun getCursorCapsMode(reqModes: Int): Int = 0
    override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? {
        val c = buffer.cursorManager.cursor
        val startOffset = (buffer.lineColumnToOffset(c.line, c.column) - n).coerceAtLeast(0)
        val length = buffer.lineColumnToOffset(c.line, c.column) - startOffset
        return buffer.getText().substring(startOffset, startOffset + length)
    }
    override fun getTextAfterCursor(n: Int, flags: Int): CharSequence? {
        val c = buffer.cursorManager.cursor
        val startOffset = buffer.lineColumnToOffset(c.line, c.column)
        val length = n.coerceAtMost(buffer.length() - startOffset)
        return buffer.getText().substring(startOffset, startOffset + length)
    }
    override fun getSelectedText(flags: Int): CharSequence? = null
    override fun requestCursorUpdates(cursorUpdateMode: Int) {}
    override fun commitCorrection(info: android.view.inputmethod.CorrectionInfo?): Boolean = false
    override fun commitContent(inputContentInfo: android.view.inputmethod.InputContentInfo?, flags: Int, opts: Bundle?): Boolean = false
}
