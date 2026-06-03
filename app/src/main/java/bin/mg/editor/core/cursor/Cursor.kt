package bin.mg.editor.core.cursor

data class Cursor(
    var line: Int = 0,
    var column: Int = 0
) {
    val offset: Int get() = line * 100000 + column

    fun moveTo(line: Int, column: Int) {
        this.line = line.coerceAtLeast(0)
        this.column = column.coerceAtLeast(0)
    }

    fun isBefore(other: Cursor): Boolean {
        return line < other.line || (line == other.line && column < other.column)
    }

    fun isAfter(other: Cursor): Boolean {
        return line > other.line || (line == other.line && column > other.column)
    }

    fun isAtSamePosition(other: Cursor): Boolean {
        return line == other.line && column == other.column
    }

    fun isBeforeOrAt(other: Cursor): Boolean {
        return isBefore(other) || isAtSamePosition(other)
    }

    fun isAfterOrAt(other: Cursor): Boolean {
        return isAfter(other) || isAtSamePosition(other)
    }

    fun minOf(other: Cursor): Cursor {
        return if (isBeforeOrAt(other)) this else other
    }

    fun maxOf(other: Cursor): Cursor {
        return if (isAfterOrAt(other)) this else other
    }

    fun copy(): Cursor = Cursor(line, column)
}
