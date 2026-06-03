package bin.mg.editor.syntax.highlight

import android.graphics.Color
import bin.mg.editor.syntax.tokenizer.Token
import bin.mg.editor.syntax.tokenizer.TokenType
import bin.mg.editor.rendering.render.EditorRenderer

class EditorTheme(
    val name: String = "Dark",
    val backgroundColor: Int = Color.parseColor("#1E1E1E"),
    val gutterBackgroundColor: Int = Color.parseColor("#1E1E1E"),
    val lineNumberColor: Int = Color.parseColor("#606366"),
    val lineNumberCurrentColor: Int = Color.parseColor("#A0A0A0"),
    val currentLineColor: Int = Color.parseColor("#2A2D2E"),
    val selectionColor: Int = Color.parseColor("#214283"),
    val textColor: Int = Color.parseColor("#A9B7C6"),
    val cursorColor: Int = Color.parseColor("#A9B7C6"),
    val separatorColor: Int = Color.parseColor("#333333"),
    val tokenColors: Map<TokenType, Int> = defaultTokenColors()
) {
    fun getTokenColor(type: TokenType): Int = tokenColors[type] ?: textColor

    companion object {
        fun defaultTokenColors(): Map<TokenType, Int> = mapOf(
            TokenType.KEYWORD to Color.parseColor("#CC7832"),
            TokenType.KEYWORD2 to Color.parseColor("#AE8ABE"),
            TokenType.STRING to Color.parseColor("#6A8759"),
            TokenType.NUMBER to Color.parseColor("#6897BB"),
            TokenType.COMMENT to Color.parseColor("#808080"),
            TokenType.BLOCK_COMMENT to Color.parseColor("#808080"),
            TokenType.ANNOTATION to Color.parseColor("#BBB529"),
            TokenType.CONSTANT to Color.parseColor("#9876AA"),
            TokenType.FUNCTION to Color.parseColor("#FFC66D"),
            TokenType.TYPE to Color.parseColor("#808000"),
            TokenType.OPERATOR to Color.parseColor("#508090"),
            TokenType.VARIABLE to Color.parseColor("#58908A"),
            TokenType.PROPERTY to Color.parseColor("#BABABA"),
            TokenType.LABEL to Color.parseColor("#6080B0"),
            TokenType.NAMESPACE to Color.parseColor("#9876AA"),
            TokenType.META to Color.parseColor("#BBB529"),
            TokenType.ERROR to Color.parseColor("#BC3F3C"),
            TokenType.TEXT to Color.parseColor("#A9B7C6")
        )

        fun lightTheme(): EditorTheme = EditorTheme(
            name = "Light",
            backgroundColor = Color.parseColor("#FAFAFA"),
            gutterBackgroundColor = Color.parseColor("#F5F5F5"),
            lineNumberColor = Color.parseColor("#AAAAAA"),
            lineNumberCurrentColor = Color.parseColor("#666666"),
            currentLineColor = Color.parseColor("#F0F4FF"),
            selectionColor = Color.parseColor("#BBDEFB"),
            textColor = Color.parseColor("#212121"),
            cursorColor = Color.parseColor("#212121"),
            separatorColor = Color.parseColor("#E0E0E0"),
            tokenColors = mapOf(
                TokenType.KEYWORD to Color.parseColor("#0000FF"),
                TokenType.KEYWORD2 to Color.parseColor("#795548"),
                TokenType.STRING to Color.parseColor("#2E7D32"),
                TokenType.NUMBER to Color.parseColor("#1565C0"),
                TokenType.COMMENT to Color.parseColor("#888888"),
                TokenType.BLOCK_COMMENT to Color.parseColor("#888888"),
                TokenType.ANNOTATION to Color.parseColor("#B0681E"),
                TokenType.CONSTANT to Color.parseColor("#795548"),
                TokenType.FUNCTION to Color.parseColor("#795548"),
                TokenType.TYPE to Color.parseColor("#0000FF"),
                TokenType.OPERATOR to Color.parseColor("#333333"),
                TokenType.VARIABLE to Color.parseColor("#212121"),
                TokenType.PROPERTY to Color.parseColor("#333333"),
                TokenType.LABEL to Color.parseColor("#0000FF"),
                TokenType.NAMESPACE to Color.parseColor("#795548"),
                TokenType.META to Color.parseColor("#B0681E"),
                TokenType.ERROR to Color.parseColor("#BC3F3C"),
                TokenType.TEXT to Color.parseColor("#212121")
            )
        )
    }
}
