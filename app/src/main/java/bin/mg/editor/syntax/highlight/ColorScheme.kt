package bin.mg.editor.syntax.highlight

import android.graphics.Color
import bin.mg.editor.syntax.tokenizer.TokenType

class ColorScheme {

    data class TokenStyle(
        val color: Int,
        val bold: Boolean = false,
        val italic: Boolean = false
    )

    private val styles = mutableMapOf<TokenType, TokenStyle>()

    fun setStyle(type: TokenType, color: Int, bold: Boolean = false, italic: Boolean = false) {
        styles[type] = TokenStyle(color, bold, italic)
    }

    fun getStyle(type: TokenType): TokenStyle? = styles[type]

    fun toTheme(): EditorTheme {
        val tokenColors = mutableMapOf<TokenType, Int>()
        for ((type, style) in styles) {
            tokenColors[type] = style.color
        }
        return EditorTheme(tokenColors = tokenColors)
    }

    companion object {
        fun vsDark(): ColorScheme = ColorScheme().apply {
            setStyle(TokenType.TEXT, Color.parseColor("#A9B7C6"))
            setStyle(TokenType.KEYWORD, Color.parseColor("#CC7832"), bold = true)
            setStyle(TokenType.KEYWORD2, Color.parseColor("#AE8ABE"))
            setStyle(TokenType.STRING, Color.parseColor("#6A8759"))
            setStyle(TokenType.NUMBER, Color.parseColor("#6897BB"))
            setStyle(TokenType.COMMENT, Color.parseColor("#808080"), italic = true)
            setStyle(TokenType.BLOCK_COMMENT, Color.parseColor("#808080"), italic = true)
            setStyle(TokenType.ANNOTATION, Color.parseColor("#BBB529"))
            setStyle(TokenType.CONSTANT, Color.parseColor("#9876AA"))
            setStyle(TokenType.FUNCTION, Color.parseColor("#FFC66D"))
            setStyle(TokenType.TYPE, Color.parseColor("#808000"))
            setStyle(TokenType.OPERATOR, Color.parseColor("#508090"))
            setStyle(TokenType.VARIABLE, Color.parseColor("#58908A"))
            setStyle(TokenType.PROPERTY, Color.parseColor("#BABABA"))
            setStyle(TokenType.LABEL, Color.parseColor("#6080B0"))
            setStyle(TokenType.NAMESPACE, Color.parseColor("#9876AA"))
            setStyle(TokenType.META, Color.parseColor("#BBB529"))
            setStyle(TokenType.ERROR, Color.parseColor("#BC3F3C"))
        }

        fun monokai(): ColorScheme = ColorScheme().apply {
            setStyle(TokenType.TEXT, Color.parseColor("#F8F8F2"))
            setStyle(TokenType.KEYWORD, Color.parseColor("#F92672"), bold = true)
            setStyle(TokenType.KEYWORD2, Color.parseColor("#66D9EF"))
            setStyle(TokenType.STRING, Color.parseColor("#E6DB74"))
            setStyle(TokenType.NUMBER, Color.parseColor("#AE81FF"))
            setStyle(TokenType.COMMENT, Color.parseColor("#75715E"), italic = true)
            setStyle(TokenType.BLOCK_COMMENT, Color.parseColor("#75715E"), italic = true)
            setStyle(TokenType.ANNOTATION, Color.parseColor("#A6E22E"))
            setStyle(TokenType.CONSTANT, Color.parseColor("#AE81FF"))
            setStyle(TokenType.FUNCTION, Color.parseColor("#A6E22E"))
            setStyle(TokenType.TYPE, Color.parseColor("#66D9EF"))
            setStyle(TokenType.OPERATOR, Color.parseColor("#F92672"))
            setStyle(TokenType.VARIABLE, Color.parseColor("#F8F8F2"))
            setStyle(TokenType.PROPERTY, Color.parseColor("#F8F8F2"))
            setStyle(TokenType.ERROR, Color.parseColor("#F92672"))
        }
    }
}
