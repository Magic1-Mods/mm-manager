package bin.mg.editor.core.buffer

data class TextSpan(
    val start: Int,
    val end: Int,
    val color: Int,
    val bold: Boolean = false,
    val italic: Boolean = false
)
