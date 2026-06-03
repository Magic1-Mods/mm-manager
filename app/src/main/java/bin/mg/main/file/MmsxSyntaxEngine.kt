package bin.mg.main.file

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Color
import android.graphics.Typeface
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.BufferedReader
import java.io.InputStreamReader
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

    data class HighlightRule(
        val pattern: Pattern,
        val style: String
    )

    private val loadedDefs = mutableMapOf<String, SyntaxDef>()
    private val extToSyntax = mutableMapOf<String, String>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var highlightRunnable: Runnable? = null

    // Night theme colors from styles.mmsx
    private val styleColors = mapOf(
        "default" to Color.parseColor("#A9B7C6"),
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
    }

    fun getSyntaxForFile(filePath: String): String? {
        val ext = filePath.substringAfterLast(".", "").lowercase()
        return extToSyntax[ext]
    }

    fun getDef(name: String): SyntaxDef? = loadedDefs[name]

    private fun parseSyntax(name: String, content: String): SyntaxDef? {
        try {
            val cleaned = cleanSyntax(content)
            val languageName = extractStringField(cleaned, "name") ?: name
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
            if (escape) {
                sb.append(c)
                escape = false
                i++
                continue
            }
            if (c == '\\' && inString) {
                sb.append(c)
                escape = true
                i++
                continue
            }
            if (c == '"') {
                inString = !inString
                sb.append(c)
                i++
                continue
            }
            if (!inString) {
                if (c == '/' && i + 1 < content.length && content[i + 1] == '/') {
                    while (i < content.length && content[i] != '\n') i++
                    continue
                }
                if (c == '/' && i + 1 < content.length && content[i + 1] == '*') {
                    i += 2
                    while (i < content.length - 1 && !(content[i] == '*' && content[i + 1] == '/')) i++
                    i += 2
                    continue
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    private fun extractStringField(content: String, field: String): String? {
        val pattern = Pattern.compile("\"$field\"\\s*:\\s*\\[?\\s*\"([^\"]+)\"")
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
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    private fun extractCommentBlock(content: String): Pair<String?, String?> {
        val pattern = Pattern.compile("comment\\s*:\\s*\\{[^}]*startsWith\\s*:\\s*\"([^\"]+)\"[^}]*endsWith\\s*:\\s*\"([^\"]+)\"")
        val matcher = pattern.matcher(content)
        return if (matcher.find()) {
            Pair(matcher.group(1), matcher.group(2))
        } else {
            Pair(null, null)
        }
    }

    private fun extractKeywords(content: String, style: String): Set<String> {
        val keywords = mutableSetOf<String>()
        val pattern = Pattern.compile("keywordsToRegex\\(\n?\\s*\"([^\"]+)\"(?:\\s*\\+\\s*\"([^\"]+)\")*\\s*\\)")
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
            try {
                patterns.add(Pattern.compile(matcher.group(1)!!))
            } catch (_: Exception) {}
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

    fun highlight(editor: CodeEditor, syntaxName: String?) {
        if (syntaxName == null || syntaxName == "text") {
            return
        }

        val def = loadedDefs[syntaxName] ?: return

        highlightRunnable?.let { mainHandler.removeCallbacks(it) }

        highlightRunnable = Runnable {
            performHighlight(editor, def)
        }
        mainHandler.postDelayed(highlightRunnable!!, 300)
    }

    private fun performHighlight(editor: CodeEditor, def: SyntaxDef) {
        try {
            val text = editor.text ?: return
            val content = text.toString()
            if (content.isEmpty()) return

            val ssb = SpannableStringBuilder(content)
            val defaultColor = styleColors["default"] ?: Color.parseColor("#A9B7C6")
            val commentColor = styleColors["comment"] ?: Color.parseColor("#808080")
            val stringColor = styleColors["string"] ?: Color.parseColor("#6A8759")
            val numberColor = styleColors["number"] ?: Color.parseColor("#6897BB")
            val keywordColor = styleColors["keyword"] ?: Color.parseColor("#CC7832")
            val metaColor = styleColors["meta"] ?: Color.parseColor("#BBB529")
            val constantColor = styleColors["constant"] ?: Color.parseColor("#9876AA")

            // Apply keyword highlighting
            if (def.keywords.isNotEmpty()) {
                val keywordPattern = Pattern.compile("\\b(${def.keywords.joinToString("|") { Pattern.quote(it) }})\\b")
                applyPatternStyle(ssb, content, keywordPattern, keywordColor, Typeface.BOLD)
            }

            // Apply keyword2 highlighting
            if (def.keyword2.isNotEmpty()) {
                val keyword2Pattern = Pattern.compile("\\b(${def.keyword2.joinToString("|") { Pattern.quote(it) }})\\b")
                applyPatternStyle(ssb, content, keyword2Pattern, styleColors["keyword2"] ?: Color.parseColor("#AE8ABE"), Typeface.NORMAL)
            }

            // Apply string highlighting
            for (pattern in def.stringPatterns) {
                applyPatternStyle(ssb, content, pattern, stringColor, Typeface.NORMAL)
            }

            // Apply number highlighting
            def.numberPattern?.let { applyPatternStyle(ssb, content, it, numberColor, Typeface.NORMAL) }

            // Apply annotation highlighting
            def.annotationPattern?.let { applyPatternStyle(ssb, content, it, metaColor, Typeface.NORMAL) }

            // Apply constant highlighting
            def.constantPattern?.let { applyPatternStyle(ssb, content, it, constantColor, Typeface.NORMAL) }

            // Apply meta patterns
            for (pattern in def.metaPatterns) {
                applyPatternStyle(ssb, content, pattern, metaColor, Typeface.NORMAL)
            }

            // Apply comment highlighting (line comments)
            if (def.commentLine != null) {
                val lineCommentPattern = Pattern.compile(Pattern.quote(def.commentLine) + ".*$", Pattern.MULTILINE)
                applyPatternStyle(ssb, content, lineCommentPattern, commentColor, Typeface.ITALIC)
            }

            // Apply block comment highlighting
            if (def.commentBlockStart != null && def.commentBlockEnd != null) {
                val blockPattern = Pattern.compile(
                    Pattern.quote(def.commentBlockStart) + "[\\s\\S]*?" + Pattern.quote(def.commentBlockEnd),
                    Pattern.DOTALL
                )
                applyPatternStyle(ssb, content, blockPattern, commentColor, Typeface.ITALIC)
            }

            editor.setText(ssb)
        } catch (_: Exception) {
        }
    }

    private fun applyPatternStyle(ssb: SpannableStringBuilder, content: String, pattern: Pattern, color: Int, style: Int) {
        val matcher = pattern.matcher(content)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            if (start < end && end <= ssb.length) {
                ssb.setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (style != Typeface.NORMAL) {
                    ssb.setSpan(StyleSpan(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }
}
