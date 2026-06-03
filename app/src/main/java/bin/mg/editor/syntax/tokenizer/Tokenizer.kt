package bin.mg.editor.syntax.tokenizer

import java.util.regex.Pattern

data class TokenPattern(
    val type: TokenType,
    val pattern: Pattern,
    val priority: Int = 0
)

class Tokenizer {

    private val patterns = mutableListOf<TokenPattern>()
    private var lineComment: String? = null
    private var blockCommentStart: String? = null
    private var blockCommentEnd: String? = null

    fun configure(
        keywords: Set<String> = emptySet(),
        keyword2: Set<String> = emptySet(),
        lineComment: String? = null,
        blockCommentStart: String? = null,
        blockCommentEnd: String? = null,
        stringPatterns: List<String> = emptyList(),
        numberPattern: String? = null,
        annotationPattern: String? = null,
        constantPattern: String? = null,
        metaPatterns: List<String> = emptyList()
    ) {
        this.lineComment = lineComment
        this.blockCommentStart = blockCommentStart
        this.blockCommentEnd = blockCommentEnd

        patterns.clear()

        // Comments (highest priority)
        if (lineComment != null) {
            patterns.add(TokenPattern(TokenType.COMMENT, Pattern.compile(Pattern.quote(lineComment) + ".*$"), 100))
        }
        if (blockCommentStart != null && blockCommentEnd != null) {
            patterns.add(TokenPattern(TokenType.BLOCK_COMMENT,
                Pattern.compile(Pattern.quote(blockCommentStart) + "[\\s\\S]*?" + Pattern.quote(blockCommentEnd)),
                99))
        }

        // Strings
        for (sp in stringPatterns) {
            patterns.add(TokenPattern(TokenType.STRING, Pattern.compile(sp), 90))
        }

        // Numbers
        numberPattern?.let {
            patterns.add(TokenPattern(TokenType.NUMBER, Pattern.compile(it), 80))
        }

        // Annotations
        annotationPattern?.let {
            patterns.add(TokenPattern(TokenType.ANNOTATION, Pattern.compile(it), 70))
        }

        // Constants
        constantPattern?.let {
            patterns.add(TokenPattern(TokenType.CONSTANT, Pattern.compile(it), 60))
        }

        // Meta patterns
        for (mp in metaPatterns) {
            patterns.add(TokenPattern(TokenType.META, Pattern.compile(mp), 50))
        }

        // Keywords
        if (keywords.isNotEmpty()) {
            val kwPattern = "\\b(${keywords.joinToString("|") { Pattern.quote(it) }})\\b"
            patterns.add(TokenPattern(TokenType.KEYWORD, Pattern.compile(kwPattern), 40))
        }

        if (keyword2.isNotEmpty()) {
            val kw2Pattern = "\\b(${keyword2.joinToString("|") { Pattern.quote(it) }})\\b"
            patterns.add(TokenPattern(TokenType.KEYWORD2, Pattern.compile(kw2Pattern), 39))
        }
    }

    fun tokenize(text: String): List<Token> {
        if (text.isEmpty()) return emptyList()

        val tokens = mutableListOf<Token>()
        val length = text.length

        // Find all matches
        val matches = mutableListOf<Pair<TokenType, IntRange>>()
        for (tokenPattern in patterns) {
            val matcher = tokenPattern.pattern.matcher(text)
            while (matcher.find()) {
                matches.add(Pair(tokenPattern.type, matcher.start()..matcher.end()))
            }
        }

        // Sort by start position, then by priority (higher priority first)
        matches.sortWith(compareBy({ it.second.first }, { -getPriority(it.first) }))

        // Build token list, skipping overlapping regions
        var pos = 0
        for ((type, range) in matches) {
            if (range.first < pos) continue
            if (range.first > pos) {
                tokens.add(Token(TokenType.TEXT, pos, range.first, 0))
            }
            tokens.add(Token(type, range.first, range.last, 0))
            pos = range.last
        }
        if (pos < length) {
            tokens.add(Token(TokenType.TEXT, pos, length, 0))
        }

        return tokens
    }

    fun tokenizeLine(text: String, lineIndex: Int): List<Token> {
        return tokenize(text).map { it.copy(line = lineIndex) }
    }

    private fun getPriority(type: TokenType): Int {
        return when (type) {
            TokenType.COMMENT, TokenType.BLOCK_COMMENT -> 100
            TokenType.STRING -> 90
            TokenType.NUMBER -> 80
            TokenType.ANNOTATION -> 70
            TokenType.CONSTANT -> 60
            TokenType.META -> 50
            TokenType.KEYWORD -> 40
            TokenType.KEYWORD2 -> 39
            TokenType.FUNCTION -> 35
            TokenType.TYPE -> 30
            TokenType.OPERATOR -> 20
            TokenType.VARIABLE -> 10
            TokenType.PROPERTY -> 10
            TokenType.LABEL -> 10
            TokenType.NAMESPACE -> 10
            TokenType.MATCHED_BRACKET -> 5
            TokenType.TEXT -> 0
            TokenType.ERROR -> 0
        }
    }
}
