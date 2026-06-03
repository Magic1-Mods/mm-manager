package bin.mg.editor.rendering.input

import android.os.Bundle
import android.os.Handler
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputContentInfo
import android.view.inputmethod.InputConnection
import bin.mg.editor.core.document.EditorBuffer

@Suppress("NOTHING_TO_OVERRIDE")
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

    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
        return deleteSurroundingText(beforeLength, afterLength)
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        buffer.cursorManager.moveTo(start / 100000, start % 100000)
        buffer.cursorManager.selection.set(start / 100000, start % 100000, end / 100000, end % 100000)
        onUpdate()
        return true
    }

    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText? {
        val et = ExtractedText()
        et.text = buffer.getText()
        et.startOffset = 0
        return et
    }

    override fun performEditorAction(actionCode: Int): Boolean {
        finishComposingText()
        return true
    }

    override fun commitCompletion(text: CompletionInfo?): Boolean {
        text?.text?.let { commitText(it, 0) }
        return true
    }

    override fun commitCorrection(info: android.view.inputmethod.CorrectionInfo?): Boolean = false

    override fun commitContent(inputContentInfo: InputContentInfo, flags: Int, opts: Bundle?): Boolean = false

    override fun getCursorCapsMode(reqModes: Int): Int = 0

    override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? {
        val c = buffer.cursorManager.cursor
        val offset = buffer.lineColumnToOffset(c.line, c.column)
        val startOffset = (offset - n).coerceAtLeast(0)
        val length = offset - startOffset
        return buffer.getText().substring(startOffset, startOffset + length)
    }

    override fun getTextAfterCursor(n: Int, flags: Int): CharSequence? {
        val c = buffer.cursorManager.cursor
        val offset = buffer.lineColumnToOffset(c.line, c.column)
        val length = n.coerceAtMost(buffer.getText().length - offset)
        return buffer.getText().substring(offset, offset + length)
    }

    override fun getSelectedText(flags: Int): CharSequence? = null

    override fun clearMetaKeyStates(states: Int): Boolean = true

    override fun reportFullscreenMode(enabled: Boolean): Boolean = true

    override fun performContextMenuAction(id: Int): Boolean = false

    override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean = true

    override fun performPrivateCommand(action: String?, extras: Bundle?): Boolean = false

    override fun sendKeyEvent(event: android.view.KeyEvent?): Boolean = false

    override fun closeConnection() {}

    override fun getHandler(): Handler? = null
}
