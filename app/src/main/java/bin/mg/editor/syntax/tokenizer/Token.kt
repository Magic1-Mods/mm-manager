package bin.mg.editor.syntax.tokenizer

data class Token(
    val type: TokenType,
    val start: Int,
    val end: Int,
    val line: Int
) {
    val length: Int get() = end - start
}

enum class TokenType {
    TEXT,
    KEYWORD,
    KEYWORD2,
    STRING,
    NUMBER,
    COMMENT,
    BLOCK_COMMENT,
    ANNOTATION,
    CONSTANT,
    FUNCTION,
    TYPE,
    OPERATOR,
    VARIABLE,
    PROPERTY,
    LABEL,
    NAMESPACE,
    META,
    ERROR,
    MATCHED_BRACKET
}
