package bin.mg.main.file

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.graphics.Typeface
import bin.mg.editor.core.buffer.TextSpan
import bin.mg.editor.rendering.view.CodeEditorView
import java.util.regex.Pattern

class MmsxSyntaxEngine(private val context: Context) {

    data class SyntaxDef(
        val name: String,
        val extensions: List<String>,
        val commentLine: String?,
        val commentBlockStart: String?,
        val commentBlockEnd: String?,
        val keywords: Set<String>,
        val keyword2: Set<String>,
        val stringPatterns: List<Pattern>,
        val numberPattern: Pattern?,
        val annotationPattern: Pattern?,
        val constantPattern: Pattern?,
        val metaPatterns: List<Pattern>,
        val customStyles: Map<String, Pair<Int, Int>>
    )

    private val loadedDefs = mutableMapOf<String, SyntaxDef>()
    private val extToSyntax = mutableMapOf<String, String>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var highlightRunnable: Runnable? = null

    @Volatile var isLoaded = false
        private set
    var onSyntaxesLoaded: (() -> Unit)? = null

    @Volatile var isHighlighting = false
        private set

    // Hardcoded fallback: extension -> syntax name (used when .mmsx files haven't loaded yet)
    private val builtinExtMap = mapOf(
        "java" to "java", "kt" to "kotlin", "kts" to "kotlin",
        "py" to "python", "pyw" to "python",
        "js" to "javascript", "es" to "javascript", "mjs" to "javascript",
        "ts" to "typescript", "tsx" to "typescript",
        "html" to "html", "htm" to "html",
        "css" to "css", "scss" to "css", "less" to "css",
        "xml" to "xml", "svg" to "xml",
        "json" to "json",
        "sh" to "shell", "bash" to "shell", "zsh" to "shell",
        "c" to "c", "h" to "c",
        "cpp" to "cpp", "cc" to "cpp", "cxx" to "cpp", "hpp" to "cpp",
        "cs" to "cs",
        "go" to "go",
        "rs" to "rust",
        "swift" to "swift",
        "rb" to "ruby",
        "php" to "php",
        "sql" to "sql",
        "lua" to "lua",
        "r" to "r", "R" to "r",
        "dart" to "dart",
        "groovy" to "groovy",
        "toml" to "toml",
        "yml" to "yml", "yaml" to "yml",
        "md" to "markdown", "markdown" to "markdown",
        "smali" to "smali",
        "nix" to "nix",
        "zig" to "zig",
        "vb" to "vb",
        "bat" to "bat", "cmd" to "bat",
        "csv" to "csv",
        "diff" to "diff",
        "tex" to "latex", "latex" to "latex",
        "glsl" to "glsl", "hlsl" to "hlsl",
        "asm" to "asm", "s" to "asm", "S" to "asm",
        "prop" to "prop", "properties" to "prop",
        "svg" to "svg",
        "dts" to "dts",
        "abnf" to "abnf",
        "bf" to "brainfuck", "brainfuck" to "brainfuck"
    )

    // Dark theme colors (IntelliJ Darcula)
    private val darkColors = mapOf(
        "default" to Color.parseColor("#BBBBBB"),
        "string" to Color.parseColor("#6A8759"),
        "strEscape" to Color.parseColor("#CC7832"),
        "comment" to Color.parseColor("#808080"),
        "meta" to Color.parseColor("#BBB529"),
        "number" to Color.parseColor("#6897BB"),
        "keyword" to Color.parseColor("#CC7832"),
        "keyword2" to Color.parseColor("#AE8ABE"),
        "constant" to Color.parseColor("#9876AA"),
        "type" to Color.parseColor("#808000"),
        "label" to Color.parseColor("#6080B0"),
        "variable" to Color.parseColor("#58908A"),
        "operator" to Color.parseColor("#508090"),
        "propKey" to Color.parseColor("#CC7832"),
        "propVal" to Color.parseColor("#6A8759"),
        "tagName" to Color.parseColor("#E8BF6A"),
        "attrName" to Color.parseColor("#BABABA"),
        "namespace" to Color.parseColor("#9876AA"),
        "error" to Color.parseColor("#BC3F3C")
    )

    // Light theme colors (IntelliJ Light + MT Manager light style)
    private val lightColors = mapOf(
        "default" to Color.parseColor("#1A1A1A"),
        "string" to Color.parseColor("#008000"),
        "strEscape" to Color.parseColor("#CC7832"),
        "comment" to Color.parseColor("#7A7A7A"),
        "meta" to Color.parseColor("#808000"),
        "number" to Color.parseColor("#1C00CF"),
        "keyword" to Color.parseColor("#0000FF"),
        "keyword2" to Color.parseColor("#7A3E9D"),
        "constant" to Color.parseColor("#233F9E"),
        "type" to Color.parseColor("#0000FF"),
        "label" to Color.parseColor("#1750EB"),
        "variable" to Color.parseColor("#0070C1"),
        "operator" to Color.parseColor("#1A1A1A"),
        "propKey" to Color.parseColor("#0000FF"),
        "propVal" to Color.parseColor("#008000"),
        "tagName" to Color.parseColor("#1A1A1A"),
        "attrName" to Color.parseColor("#7A3E9D"),
        "namespace" to Color.parseColor("#233F9E"),
        "error" to Color.parseColor("#A61717")
    )

    private fun getColors(isDark: Boolean): Map<String, Int> = if (isDark) darkColors else lightColors

    fun loadAllSyntaxes() {
        try {
            val syntaxDir = context.assets.list("syntax") ?: return
            for (fileName in syntaxDir) {
                if (fileName == "init" || fileName == "internal") continue
                if (!fileName.endsWith(".mmsx")) continue
                val name = fileName.removeSuffix(".mmsx")
                val filePath = "syntax/$fileName"
                try {
                    val content = context.assets.open(filePath).bufferedReader().use { it.readText() }
                    val def = parseSyntax(name, content)
                    if (def != null) {
                        loadedDefs[name] = def
                        for (ext in def.extensions) {
                            extToSyntax[ext] = name
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        isLoaded = true
        mainHandler.post { onSyntaxesLoaded?.invoke() }
    }

    fun getSyntaxForFile(filePath: String): String? {
        val ext = filePath.substringAfterLast(".", "").lowercase()
        return extToSyntax[ext] ?: builtinExtMap[ext]
    }

    fun getDef(name: String): SyntaxDef? = loadedDefs[name]

    private fun parseSyntax(name: String, content: String): SyntaxDef? {
        try {
            val cleaned = cleanSyntax(content)
            val languageName = extractName(cleaned) ?: name
            val extensions = extractExtensions(cleaned)
            val commentLine = extractCommentLine(cleaned)
            val commentBlock = extractCommentBlock(cleaned)
            val keywords = extractKeywords(cleaned, "keyword")
            val keyword2 = extractKeywords(cleaned, "keyword2")
            val metaPatterns = extractMetaPatterns(cleaned)
            val customStyles = extractCustomStyles(cleaned)

            return SyntaxDef(
                name = languageName,
                extensions = extensions,
                commentLine = commentLine,
                commentBlockStart = commentBlock.first,
                commentBlockEnd = commentBlock.second,
                keywords = keywords,
                keyword2 = keyword2,
                stringPatterns = listOf(
                    Pattern.compile("\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\""),
                    Pattern.compile("'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'")
                ),
                numberPattern = Pattern.compile("\\b(?:0[xX][0-9a-fA-F]+|0[bB][01]+|0[oO][0-7]+|[0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?)[lLfFdD]?\\b"),
                annotationPattern = Pattern.compile("@\\w+(?:\\.\\w+)*"),
                constantPattern = Pattern.compile("\\b[A-Z][A-Z0-9]*(?:_[A-Z0-9]+)+\\b"),
                metaPatterns = metaPatterns,
                customStyles = customStyles
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun cleanSyntax(content: String): String {
        val sb = StringBuilder()
        var inString = false
        var escape = false
        var i = 0
        while (i < content.length) {
            val c = content[i]
            if (escape) { sb.append(c); escape = false; i++; continue }
            if (c == '\\' && inString) { sb.append(c); escape = true; i++; continue }
            if (c == '"') { inString = !inString; sb.append(c); i++; continue }
            if (!inString) {
                if (c == '/' && i + 1 < content.length && content[i + 1] == '/') {
                    while (i < content.length && content[i] != '\n') i++
                    continue
                }
                if (c == '/' && i + 1 < content.length && content[i + 1] == '*') {
                    i += 2
                    while (i < content.length - 1 && !(content[i] == '*' && content[i + 1] == '/')) i++
                    i += 2; continue
                }
            }
            sb.append(c); i++
        }
        return sb.toString()
    }

    private fun extractName(content: String): String? {
        val pattern = Pattern.compile("name\\s*:\\s*\\[\\s*\"([^\"]+)\"")
        val matcher = pattern.matcher(content)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun extractExtensions(content: String): List<String> {
        val extensions = mutableListOf<String>()
        val pattern = Pattern.compile("\"\\.(\\w+)\"")
        val matcher = pattern.matcher(content)
        while (matcher.find()) {
            extensions.add(matcher.group(1)!!)
        }
        return extensions
    }

    private fun extractCommentLine(content: String): String? {
        val pattern = Pattern.compile("comment\\s*:\\s*\\{[^}]*startsWith\\s*:\\s*\"([^\"]+)\"[^}]*\\}")
        val matcher = pattern.matcher(content)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun extractCommentBlock(content: String): Pair<String?, String?> {
        val pattern = Pattern.compile("comment\\s*:\\s*\\{[^}]*startsWith\\s*:\\s*\"([^\"]+)\"[^}]*endsWith\\s*:\\s*\"([^\"]+)\"")
        val matcher = pattern.matcher(content)
        return if (matcher.find()) Pair(matcher.group(1), matcher.group(2)) else Pair(null, null)
    }

    private fun extractKeywords(content: String, style: String): Set<String> {
        val keywords = mutableSetOf<String>()
        val fullPattern = Pattern.compile("keywordsToRegex\\(\\s*\"([^\"]+(?:\"\\s*\\+\\s*\"[^\"]+)*)\"\\s*\\)")
        val matcher = fullPattern.matcher(content)
        while (matcher.find()) {
            val keywordBlock = matcher.group(1) ?: continue
            val parts = keywordBlock.split("\"\\s*\\+\\s*\"".toRegex())
            for (part in parts) {
                val cleaned = part.replace("\"", "").trim()
                keywords.addAll(cleaned.split("\\s+".toRegex()).filter { it.isNotEmpty() })
            }
        }
        return keywords
    }

    private fun extractMetaPatterns(content: String): List<Pattern> {
        val patterns = mutableListOf<Pattern>()
        val matcher = Pattern.compile("\\{match\\s*:\\s*/([^/]+)/[^}]*0\\s*:\\s*\"meta\"").matcher(content)
        while (matcher.find()) {
            try { patterns.add(Pattern.compile(matcher.group(1)!!)) } catch (_: Exception) {}
        }
        return patterns
    }

    private fun extractCustomStyles(content: String): Map<String, Pair<Int, Int>> {
        val styles = mutableMapOf<String, Pair<Int, Int>>()
        val pattern = Pattern.compile("\"(\\w+)\"\\s+#([0-9A-Fa-f]{6})\\s+#([0-9A-Fa-f]{6})")
        val matcher = pattern.matcher(content)
        while (matcher.find()) {
            val name = matcher.group(1)!!
            val dayColor = Color.parseColor("#${matcher.group(2)}")
            val nightColor = Color.parseColor("#${matcher.group(3)}")
            styles[name] = Pair(dayColor, nightColor)
        }
        return styles
    }

    /**
     * Tokenize a single line into TextSpan objects using this syntax definition.
     * This does NOT call setText — it only returns span data for the renderer.
     */
    fun tokenizeLine(line: String, def: SyntaxDef, isDark: Boolean = true): List<TextSpan> {
        if (line.isEmpty()) return emptyList()
        val spans = mutableListOf<TextSpan>()
        val colors = getColors(isDark)

        // Priority order: keywords, keyword2, strings, numbers, annotations, constants, meta, comments
        val taken = BooleanArray(line.length) { false }

        fun addSpans(pattern: Pattern, color: Int, bold: Boolean = false, italic: Boolean = false) {
            val matcher = pattern.matcher(line)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                if (start < end && end <= line.length) {
                    var overlap = false
                    for (i in start until end) { if (taken[i]) { overlap = true; break } }
                    if (!overlap) {
                        spans.add(TextSpan(start, end, color, bold, italic))
                        for (i in start until end) taken[i] = true
                    }
                }
            }
        }

        // Comments (highest priority — they eat everything after them)
        if (def.commentLine != null) {
            val lineCommentPattern = Pattern.compile(Pattern.quote(def.commentLine) + ".*$", Pattern.MULTILINE)
            addSpans(lineCommentPattern, colors["comment"]!!, italic = true)
        }
        if (def.commentBlockStart != null && def.commentBlockEnd != null) {
            val blockPattern = Pattern.compile(
                Pattern.quote(def.commentBlockStart) + "[\\s\\S]*?" + Pattern.quote(def.commentBlockEnd),
                Pattern.DOTALL
            )
            addSpans(blockPattern, colors["comment"]!!, italic = true)
        }

        // Strings
        for (pattern in def.stringPatterns) {
            addSpans(pattern, colors["string"]!!)
        }

        // Keywords
        if (def.keywords.isNotEmpty()) {
            val keywordPattern = Pattern.compile("\\b(${def.keywords.joinToString("|") { Pattern.quote(it) }})\\b")
            addSpans(keywordPattern, colors["keyword"]!!, bold = true)
        }

        if (def.keyword2.isNotEmpty()) {
            val keyword2Pattern = Pattern.compile("\\b(${def.keyword2.joinToString("|") { Pattern.quote(it) }})\\b")
            addSpans(keyword2Pattern, colors["keyword2"]!!)
        }

        // Numbers
        def.numberPattern?.let { addSpans(it, colors["number"]!!) }

        // Annotations
        def.annotationPattern?.let { addSpans(it, colors["meta"]!!) }

        // Constants
        def.constantPattern?.let { addSpans(it, colors["constant"]!!) }

        // Meta patterns
        for (pattern in def.metaPatterns) {
            addSpans(pattern, colors["meta"]!!)
        }

        return spans
    }

    /**
     * Highlight the entire document. Produces a Map<lineIndex, List<TextSpan>>
     * and calls editor.setTextSyntaxSpans() without modifying content.
     */
    fun highlight(editor: CodeEditorView, syntaxName: String?, isDark: Boolean = true) {
        if (syntaxName == null || syntaxName == "text") {
            editor.setTextSyntaxSpans(emptyMap())
            return
        }
        if (isHighlighting) return
        val def = loadedDefs[syntaxName] ?: return

        highlightRunnable?.let { mainHandler.removeCallbacks(it) }
        highlightRunnable = Runnable { performHighlight(editor, def, isDark) }
        mainHandler.postDelayed(highlightRunnable!!, 150)
    }

    private fun performHighlight(editor: CodeEditorView, def: SyntaxDef, isDark: Boolean) {
        if (isHighlighting) return
        isHighlighting = true
        try {
            val buffer = editor.buffer
            val lineCount = buffer.getLineCount()
            val spansMap = mutableMapOf<Int, List<TextSpan>>()

            for (lineIdx in 0 until lineCount) {
                val lineText = buffer.getLineText(lineIdx)
                if (lineText.isNotEmpty()) {
                    val lineSpans = tokenizeLine(lineText, def, isDark)
                    if (lineSpans.isNotEmpty()) {
                        spansMap[lineIdx] = lineSpans
                    }
                }
            }

            mainHandler.post {
                editor.setTextSyntaxSpans(spansMap)
                isHighlighting = false
            }
        } catch (_: Exception) {
            isHighlighting = false
        }
    }
}
