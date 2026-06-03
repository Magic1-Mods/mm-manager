package bin.mg.editor.rendering.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.InputType
import android.text.TextPaint
import android.util.AttributeSet
import android.view.inputmethod.InputConnection
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import bin.mg.editor.core.buffer.TextSpan
import bin.mg.editor.core.document.EditorBuffer
import bin.mg.editor.core.cursor.CursorManager
import bin.mg.editor.rendering.input.EditorInputConnection
import bin.mg.editor.rendering.input.TouchHandler
import bin.mg.editor.rendering.layout.LayoutEngine
import bin.mg.editor.rendering.render.EditorRenderer
import bin.mg.editor.rendering.scroll.ScrollController
import kotlin.math.ceil

class CodeEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val buffer = EditorBuffer()
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val layoutEngine = LayoutEngine(textPaint)
    private val renderer = EditorRenderer(textPaint, layoutEngine)
    private val scrollController = ScrollController(context)
    private var touchHandler: TouchHandler? = null

    private var inputConnection: EditorInputConnection? = null
    private var fontSize = 14f
    private var tabSize = 4
    private var showLineNumbers = true
    private var wordWrap = false
    private var isEditable = true
    private var lineSpacingExtra = 2f
    private var lineSpacingMultiplier = 1.1f

    private var textSpans: Map<Int, List<TextSpan>> = emptyMap()

    var onContentChanged: ((String) -> Unit)? = null
    var onCursorMoved: ((Int, Int) -> Unit)? = null

    init {
        textPaint.typeface = Typeface.MONOSPACE
        textPaint.textSize = fontSize

        scrollController.onScrollChanged = { _, _ -> invalidate() }

        buffer.addDocumentListener(object : EditorBuffer.DocumentListener {
            override fun onContentChanged(startLine: Int, endLine: Int, newLineCount: Int) {
                layoutEngine.invalidateRange(startLine, endLine)
                updateScrollBounds()
                invalidate()
                onContentChanged?.invoke(buffer.getText())
            }
            override fun onUndoStateChanged(canUndo: Boolean, canRedo: Boolean) {}
        })

        buffer.cursorManager.addListener(object : CursorManager.CursorListener {
            override fun onCursorMoved(line: Int, column: Int) {
                onCursorMoved?.invoke(line, column)
                ensureCursorVisible()
            }
            override fun onSelectionChanged(startLine: Int, startCol: Int, endLine: Int, endCol: Int, isValid: Boolean) {
                invalidate()
            }
        })

        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        touchHandler = TouchHandler(
            context, buffer, layoutEngine, scrollController,
            { invalidate() },
            { ensureCursorVisible() }
        )
        configureEditor()
        updateScrollBounds()
        invalidate()
    }

    private fun configureEditor() {
        layoutEngine.configure(fontSize, tabSize)
        layoutEngine.setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
        renderer.configure(fontSize, tabSize)
        updateScrollBounds()
    }

    private fun updateScrollBounds() {
        val lineCount = buffer.getLineCount()
        val lineHeight = layoutEngine.lineHeight
        val contentHeight = lineCount * lineHeight
        val maxScrollY = (contentHeight - height).coerceAtLeast(0f)
        scrollController.updateMaxScroll(0f, maxScrollY)
    }

    fun setText(text: String) {
        buffer.setText(text)
        layoutEngine.clearCache()
        updateScrollBounds()
        buffer.cursorManager.moveTo(0, 0)
        invalidate()
    }

    fun getText(): String = buffer.getText()

    fun setTextSyntaxSpans(spans: Map<Int, List<TextSpan>>) {
        textSpans = spans
        invalidate()
    }

    fun setFontSize(size: Float) {
        fontSize = size
        configureEditor()
        invalidate()
    }

    fun setTabSize(size: Int) {
        tabSize = size
        configureEditor()
        invalidate()
    }

    fun setWordWrap(enabled: Boolean) {
        wordWrap = enabled
        invalidate()
    }

    fun setLineNumbers(enabled: Boolean) {
        showLineNumbers = enabled
        invalidate()
    }

    fun setEditable(editable: Boolean) {
        isEditable = editable
    }

    fun setLineSpacing(extra: Float, multiplier: Float) {
        lineSpacingExtra = extra
        lineSpacingMultiplier = multiplier
        layoutEngine.setLineSpacing(extra, multiplier)
        invalidate()
    }

    fun undo() {
        buffer.undo()
        invalidate()
    }

    fun redo() {
        buffer.redo()
        invalidate()
    }

    fun canUndo(): Boolean = buffer.undoManager.canUndo()
    fun canRedo(): Boolean = buffer.undoManager.canRedo()

    fun setCursorPosition(line: Int, column: Int) {
        buffer.cursorManager.moveTo(line, column)
        invalidate()
    }

    fun selectAll() {
        buffer.cursorManager.selectAll()
        invalidate()
    }

    fun setSelection(startLine: Int, startCol: Int, endLine: Int, endCol: Int) {
        buffer.cursorManager.moveTo(startLine, startCol)
        buffer.cursorManager.selection.set(startLine, startCol, endLine, endCol)
        invalidate()
    }

    fun getFirstVisibleLine(): Int = (scrollController.scrollY / layoutEngine.lineHeight).toInt()

    fun getVisibleLineCount(): Int = ceil(height.toFloat() / layoutEngine.lineHeight).toInt() + 1

    fun scrollToLine(line: Int) {
        val targetY = (line * layoutEngine.lineHeight - height / 2f).coerceAtLeast(0f)
        scrollController.smoothScrollTo(0f, targetY)
        invalidate()
    }

    private fun ensureCursorVisible() {
        val cursorLine = buffer.cursorManager.cursor.line
        val lineHeight = layoutEngine.lineHeight
        scrollController.ensureCursorVisible(cursorLine, lineHeight, height.toFloat(), layoutEngine.gutterWidth)
        invalidate()
    }

    fun showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        outAttrs.imeOptions = EditorInfo.IME_ACTION_NONE
        outAttrs.initialSelStart = buffer.cursorManager.cursor.line * 100000 + buffer.cursorManager.cursor.column
        outAttrs.initialSelEnd = outAttrs.initialSelStart

        inputConnection = EditorInputConnection(buffer) { invalidate() }
        return inputConnection!!
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val firstVisibleLine = getFirstVisibleLine()
        val visibleLineCount = getVisibleLineCount()

        val selStart: Pair<Int, Int>? = if (buffer.cursorManager.selection.isValid && !buffer.cursorManager.selection.isCollapsed()) {
            val s = buffer.cursorManager.selection.normalizedStart()
            Pair(s.line, s.column)
        } else null

        val selEnd: Pair<Int, Int>? = if (buffer.cursorManager.selection.isValid && !buffer.cursorManager.selection.isCollapsed()) {
            val e = buffer.cursorManager.selection.normalizedEnd()
            Pair(e.line, e.column)
        } else null

        val secondaryCursors = buffer.cursorManager.secondaryCursors.map { Pair(it.line, it.column) }

        renderer.render(
            canvas = canvas,
            buffer = buffer,
            scrollX = scrollController.scrollX,
            scrollY = scrollController.scrollY,
            firstVisibleLine = firstVisibleLine,
            visibleLineCount = visibleLineCount,
            currentLine = buffer.cursorManager.cursor.line,
            selectionStart = selStart,
            selectionEnd = selEnd,
            secondaryCursors = secondaryCursors,
            contentWidth = width.toFloat(),
            contentHeight = height.toFloat(),
            textSpans = textSpans
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            if (!isFocused) requestFocus()
        }
        val handled = touchHandler?.onTouchEvent(event) ?: false
        if (!handled) return super.onTouchEvent(event)
        return true
    }

    override fun computeScroll() {
        if (scrollController.computeScrollOffset()) {
            invalidate()
        }
    }

    override fun onCheckIsTextEditor(): Boolean = true

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus && isFocused) {
            renderer.startCursorBlink()
        } else {
            renderer.stopCursorBlink()
        }
    }
}
