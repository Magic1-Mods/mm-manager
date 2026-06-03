package bin.mg.editor.syntax.highlight

import bin.mg.editor.core.buffer.TextSpan
import bin.mg.editor.core.document.EditorBuffer
import bin.mg.editor.core.document.DocumentListener
import bin.mg.editor.syntax.tokenizer.Token
import bin.mg.editor.syntax.tokenizer.Tokenizer
import bin.mg.editor.syntax.tokenizer.TokenizerThread
import bin.mg.editor.syntax.tokenizer.TokenType
import bin.mg.editor.syntax.grammar.Grammar

class SyntaxHighlighter(
    private val buffer: EditorBuffer,
    private val onHighlightChanged: (Map<Int, List<TextSpan>>) -> Unit
) {

    private val tokenizer = Tokenizer()
    private val tokenizerThread = TokenizerThread(tokenizer) { line, tokens ->
        tokenCache[line] = tokens
        rebuildSpansForLine(line)
    }

    private var theme = EditorTheme()
    private val tokenCache = mutableMapOf<Int, List<Token>>()
    private var spanCache = mutableMapOf<Int, List<TextSpan>>()
    private var isEnabled = true
    private var dirtyStartLine = 0
    private var dirtyEndLine = 0

    private val documentListener = object : DocumentListener {
        override fun onContentChanged(startLine: Int, endLine: Int, newLineCount: Int) {
            if (!isEnabled) return
            invalidateRange(startLine, newLineCount)
        }
        override fun onUndoStateChanged(canUndo: Boolean, canRedo: Boolean) {}
    }

    init {
        buffer.addDocumentListener(documentListener)
    }

    fun setTheme(newTheme: EditorTheme) {
        theme = newTheme
        for ((line, _) in tokenCache) {
            rebuildSpansForLine(line)
        }
        onHighlightChanged(spanCache.toMap())
    }

    fun loadGrammar(grammar: Grammar) {
        tokenizer.configure(
            keywords = grammar.keywords,
            keyword2 = grammar.keyword2,
            lineComment = grammar.lineComment,
            blockCommentStart = grammar.blockCommentStart,
            blockCommentEnd = grammar.blockCommentEnd,
            stringPatterns = grammar.stringPatterns,
            numberPattern = grammar.numberPattern,
            annotationPattern = grammar.annotationPattern,
            constantPattern = grammar.constantPattern,
            metaPatterns = grammar.metaPatterns
        )

        if (grammar.tokenColors.isNotEmpty()) {
            val newTokenColors = theme.tokenColors.toMutableMap()
            for ((type, color) in grammar.tokenColors) {
                newTokenColors[type] = color
            }
            theme = EditorTheme(
                name = theme.name,
                backgroundColor = theme.backgroundColor,
                gutterBackgroundColor = theme.gutterBackgroundColor,
                lineNumberColor = theme.lineNumberColor,
                lineNumberCurrentColor = theme.lineNumberCurrentColor,
                currentLineColor = theme.currentLineColor,
                selectionColor = theme.selectionColor,
                textColor = theme.textColor,
                cursorColor = theme.cursorColor,
                separatorColor = theme.separatorColor,
                tokenColors = newTokenColors
            )
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
            tokenCache.clear()
            spanCache.clear()
            onHighlightChanged(emptyMap())
        }
    }

    fun tokenizeAll() {
        val lineCount = buffer.getLineCount()
        for (line in 0 until lineCount) {
            val text = buffer.getLineText(line)
            tokenizerThread.requestTokenize(line, text)
        }
    }

    fun invalidateRange(startLine: Int, endLine: Int) {
        dirtyStartLine = minOf(dirtyStartLine, startLine)
        dirtyEndLine = maxOf(dirtyEndLine, endLine)
        scheduleTokenize()
    }

    private fun scheduleTokenize() {
        val start = dirtyStartLine
        val end = dirtyEndLine
        dirtyStartLine = Int.MAX_VALUE
        dirtyEndLine = Int.MIN_VALUE

        val lines = mutableListOf<Pair<Int, String>>()
        for (line in start..end) {
            if (line < buffer.getLineCount()) {
                lines.add(Pair(line, buffer.getLineText(line)))
            }
        }
        tokenizerThread.requestTokenizeRange(lines) {}
    }

    private fun rebuildSpansForLine(line: Int) {
        val tokens = tokenCache[line] ?: return
        val spans = mutableListOf<TextSpan>()
        for (token in tokens) {
            val color = theme.getTokenColor(token.type)
            val bold = token.type == TokenType.KEYWORD
            val italic = token.type == TokenType.COMMENT || token.type == TokenType.BLOCK_COMMENT
            spans.add(TextSpan(token.start, token.end, color, bold, italic))
        }
        spanCache[line] = spans
        onHighlightChanged(spanCache.toMap())
    }

    fun shutdown() {
        tokenizerThread.shutdown()
        buffer.removeDocumentListener(documentListener)
    }
}
