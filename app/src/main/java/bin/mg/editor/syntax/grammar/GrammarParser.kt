package bin.mg.editor.syntax.grammar

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import bin.mg.editor.syntax.tokenizer.TokenType

data class Grammar(
    val name: String,
    val scopeName: String,
    val extensions: List<String>,
    val keywords: Set<String>,
    val keyword2: Set<String>,
    val lineComment: String?,
    val blockCommentStart: String?,
    val blockCommentEnd: String?,
    val stringPatterns: List<String>,
    val numberPattern: String?,
    val annotationPattern: String?,
    val constantPattern: String?,
    val metaPatterns: List<String>,
    val tokenColors: Map<TokenType, Int>
)

class GrammarParser {

    fun parse(json: String, defaultTheme: Map<TokenType, Int> = emptyMap()): Grammar? {
        return try {
            val root = JsonParser.parseString(json).asJsonObject

            val name = root.get("name")?.asString ?: ""
            val scopeName = root.get("scopeName")?.asString ?: ""
            val extensions = root.get("extensions")?.let { ext ->
                if (ext.isJsonArray) ext.asJsonArray.map { it.asString } else emptyList()
            } ?: emptyList()

            val comments = root.get("comments")?.asJsonObject
            val lineComment = comments?.get("lineComment")?.asString
            val blockCommentStart = comments?.get("blockCommentStart")?.asString
            val blockCommentEnd = comments?.get("blockCommentEnd")?.asString

            val keywords = extractKeywordSet(root, "keywords")
            val keyword2 = extractKeywordSet(root, "keyword2")

            val stringPatterns = mutableListOf<String>()
            root.get("strings")?.let { strings ->
                if (strings.isJsonArray) {
                    for (s in strings.asJsonArray) stringPatterns.add(s.asString)
                }
            }
            if (stringPatterns.isEmpty()) {
                stringPatterns.add("\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"")
                stringPatterns.add("'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'")
            }

            val numberPattern = root.get("numberPattern")?.asString
                ?: "\\b(?:0[xX][0-9a-fA-F]+|0[bB][01]+|0[oO][0-7]+|[0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?)[lLfFdD]?\\b"

            val annotationPattern = root.get("annotationPattern")?.asString ?: "@\\w+(?:\\.\\w+)*"
            val constantPattern = root.get("constantPattern")?.asString ?: "\\b[A-Z][A-Z0-9]*(?:_[A-Z0-9]+)+\\b"

            val metaPatterns = mutableListOf<String>()
            root.get("metaPatterns")?.let { meta ->
                if (meta.isJsonArray) {
                    for (m in meta.asJsonArray) metaPatterns.add(m.asString)
                }
            }

            val tokenColors = mutableMapOf<TokenType, Int>()
            root.get("tokenColors")?.let { colors ->
                if (colors.isJsonObject) {
                    for ((key, value) in colors.asJsonObject.entrySet()) {
                        try {
                            val type = TokenType.valueOf(key.uppercase())
                            tokenColors[type] = android.graphics.Color.parseColor(value.asString)
                        } catch (_: Exception) {}
                    }
                }
            }

            Grammar(
                name = name,
                scopeName = scopeName,
                extensions = extensions,
                keywords = keywords,
                keyword2 = keyword2,
                lineComment = lineComment,
                blockCommentStart = blockCommentStart,
                blockCommentEnd = blockCommentEnd,
                stringPatterns = stringPatterns,
                numberPattern = numberPattern,
                annotationPattern = annotationPattern,
                constantPattern = constantPattern,
                metaPatterns = metaPatterns,
                tokenColors = tokenColors.ifEmpty { defaultTheme }
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun extractKeywordSet(root: JsonObject, key: String): Set<String> {
        val result = mutableSetOf<String>()
        root.get(key)?.let { kw ->
            if (kw.isJsonArray) {
                for (item in kw.asJsonArray) {
                    if (item.isJsonArray) {
                        for (sub in item.asJsonArray) result.add(sub.asString)
                    } else {
                        result.add(item.asString)
                    }
                }
            } else if (kw.isJsonPrimitive) {
                result.addAll(kw.asString.split("\\s+".toRegex()))
            }
        }
        return result
    }
}
