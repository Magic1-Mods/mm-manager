package bin.mg.editor.core.buffer

class PieceTable(initialText: String = "") {

    enum class BufferType { ORIGINAL, ADD }

    data class Piece(
        val bufferType: BufferType,
        val start: Int,
        val length: Int
    )

    private var originalBuffer: CharArray = initialText.toCharArray()
    private var addBuffer = CharArray(maxOf(1024, initialText.length * 2))
    private var addBufferLength = 0

    private val pieces = mutableListOf<Piece>()
    private var totalLength = initialText.length

    private val lineStarts = mutableListOf<Int>()

    private val undoStack = mutableListOf<Snapshot>()
    private val redoStack = mutableListOf<Snapshot>()

    init {
        if (initialText.isNotEmpty()) {
            pieces.add(Piece(BufferType.ORIGINAL, 0, initialText.length))
        }
        rebuildLineIndex()
    }

    fun length(): Int = totalLength

    fun lineCount(): Int = lineStarts.size

    fun lineStartOffset(line: Int): Int {
        if (line < 0 || line >= lineStarts.size) return totalLength
        return lineStarts[line]
    }

    fun lineEndOffset(line: Int): Int {
        if (line < 0) return 0
        if (line + 1 >= lineStarts.size) return totalLength
        return lineStarts[line + 1].coerceAtMost(totalLength)
    }

    fun offsetToLine(offset: Int): Int {
        if (offset <= 0) return 0
        var lo = 0
        var hi = lineStarts.size - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lineStarts[mid] <= offset) lo = mid + 1 else hi = mid - 1
        }
        return maxOf(0, lo - 1)
    }

    fun offsetToColumn(offset: Int): Int {
        val line = offsetToLine(offset)
        return offset - lineStarts[line]
    }

    fun lineColumnToOffset(line: Int, column: Int): Int {
        if (line < 0 || line >= lineStarts.size) return totalLength
        val lineStart = lineStarts[line]
        val lineEnd = lineEndOffset(line)
        return (lineStart + column).coerceIn(lineStart, lineEnd)
    }

    fun charAt(offset: Int): Char {
        if (offset < 0 || offset >= totalLength) throw IndexOutOfBoundsException("Offset $offset, length $totalLength")
        val idx = findPieceIndex(offset)
        val piece = pieces[idx]
        val pieceStart = computePieceStart(idx)
        val localOffset = offset - pieceStart
        return when (piece.bufferType) {
            BufferType.ORIGINAL -> originalBuffer[piece.start + localOffset]
            BufferType.ADD -> addBuffer[piece.start + localOffset]
        }
    }

    fun substring(offset: Int, length: Int): String {
        if (length <= 0 || offset >= totalLength) return ""
        val safeOffset = offset.coerceIn(0, totalLength)
        val end = (safeOffset + length).coerceAtMost(totalLength)
        val sb = StringBuilder(end - safeOffset)
        var pos = safeOffset
        while (pos < end) {
            val idx = findPieceIndex(pos)
            val piece = pieces[idx]
            val pieceStart = computePieceStart(idx)
            val localOffset = pos - pieceStart
            val charsAvailable = piece.length - localOffset
            val charsNeeded = end - pos
            val charsToCopy = minOf(charsAvailable, charsNeeded)
            val buf = if (piece.bufferType == BufferType.ORIGINAL) originalBuffer else addBuffer
            sb.append(buf, piece.start + localOffset, piece.start + localOffset + charsToCopy)
            pos += charsToCopy
        }
        return sb.toString()
    }

    fun getLineText(line: Int): String {
        if (line < 0 || line >= lineStarts.size) return ""
        val start = lineStarts[line].coerceAtMost(totalLength)
        val end = lineEndOffset(line).coerceAtMost(totalLength)
        if (start >= end) return ""
        return substring(start, end - start).trimEnd('\r', '\n')
    }

    fun insert(offset: Int, text: String) {
        if (text.isEmpty()) return
        saveUndoSnapshot()
        val clampedOffset = offset.coerceIn(0, totalLength)
        val addStart = addBufferLength
        ensureAddBufferCapacity(addBufferLength + text.length)
        text.toCharArray().copyInto(addBuffer, addStart)
        addBufferLength += text.length
        val newPiece = Piece(BufferType.ADD, addStart, text.length)
        insertPieceAt(clampedOffset, newPiece)
        totalLength += text.length
        updateLineIndexInsert(clampedOffset, text)
        redoStack.clear()
    }

    fun delete(offset: Int, length: Int) {
        if (length <= 0 || offset < 0 || offset + length > totalLength) return
        saveUndoSnapshot()
        removePieces(offset, length)
        totalLength -= length
        updateLineIndexDelete(offset, length)
        redoStack.clear()
    }

    fun replace(offset: Int, length: Int, text: String) {
        if (length > 0) delete(offset, length)
        insert(offset, text)
    }

    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        redoStack.add(Snapshot(pieces.map { it }, addBufferLength, totalLength, lineStarts.toList()))
        val snapshot = undoStack.removeAt(undoStack.size - 1)
        restoreSnapshot(snapshot)
        return true
    }

    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        undoStack.add(Snapshot(pieces.map { it }, addBufferLength, totalLength, lineStarts.toList()))
        val snapshot = redoStack.removeAt(redoStack.size - 1)
        restoreSnapshot(snapshot)
        return true
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun toCharArray(): CharArray {
        val result = CharArray(totalLength)
        var pos = 0
        for (piece in pieces) {
            val buf = if (piece.bufferType == BufferType.ORIGINAL) originalBuffer else addBuffer
            buf.copyInto(result, pos, piece.start, piece.start + piece.length)
            pos += piece.length
        }
        return result
    }

    fun toString(offset: Int, length: Int): String = substring(offset, length)

    override fun toString(): String = substring(0, totalLength)

    // ─── Internal ─────────────────────────────────────────────────────────────

    private data class Snapshot(
        val pieces: List<Piece>,
        val addBufferLength: Int,
        val totalLength: Int,
        val lineStarts: List<Int>
    )

    private fun saveUndoSnapshot() {
        undoStack.add(Snapshot(pieces.map { it }, addBufferLength, totalLength, lineStarts.toList()))
        if (undoStack.size > 500) undoStack.removeAt(0)
    }

    private fun restoreSnapshot(snapshot: Snapshot) {
        pieces.clear()
        pieces.addAll(snapshot.pieces)
        addBufferLength = snapshot.addBufferLength
        totalLength = snapshot.totalLength
        lineStarts.clear()
        lineStarts.addAll(snapshot.lineStarts)
    }

    private fun ensureAddBufferCapacity(needed: Int) {
        if (needed <= addBuffer.size) return
        var newSize = addBuffer.size * 2
        while (newSize < needed) newSize *= 2
        val newBuffer = CharArray(newSize)
        addBuffer.copyInto(newBuffer, 0, 0, addBufferLength)
        addBuffer = newBuffer
    }

    private fun findPieceIndex(offset: Int): Int {
        var accumulated = 0
        for (i in pieces.indices) {
            accumulated += pieces[i].length
            if (offset < accumulated) return i
        }
        return (pieces.size - 1).coerceAtLeast(0)
    }

    private fun computePieceStart(pieceIndex: Int): Int {
        var accumulated = 0
        for (i in 0 until pieceIndex) {
            accumulated += pieces[i].length
        }
        return accumulated
    }

    private fun insertPieceAt(offset: Int, newPiece: Piece) {
        if (pieces.isEmpty()) {
            pieces.add(newPiece)
            return
        }
        var accumulated = 0
        for (i in pieces.indices) {
            val piece = pieces[i]
            val pieceEnd = accumulated + piece.length
            if (offset == pieceEnd) {
                pieces.add(i + 1, newPiece)
                return
            }
            if (offset >= accumulated && offset < pieceEnd) {
                val localOffset = offset - accumulated
                if (localOffset > 0) {
                    val left = Piece(piece.bufferType, piece.start, localOffset)
                    val right = Piece(piece.bufferType, piece.start + localOffset, piece.length - localOffset)
                    pieces[i] = left
                    pieces.add(i + 1, newPiece)
                    pieces.add(i + 2, right)
                } else {
                    pieces.add(i, newPiece)
                }
                return
            }
            accumulated = pieceEnd
        }
        pieces.add(newPiece)
    }

    private fun removePieces(offset: Int, length: Int) {
        if (pieces.isEmpty()) return
        var accumulated = 0
        var startIdx = -1
        var endIdx = -1
        for (i in pieces.indices) {
            val pieceEnd = accumulated + pieces[i].length
            if (startIdx == -1 && offset < pieceEnd) startIdx = i
            if (offset + length <= pieceEnd) { endIdx = i; break }
            accumulated = pieceEnd
        }
        if (startIdx == -1) return
        if (endIdx == -1) endIdx = pieces.size - 1

        // Recalculate accumulated for startIdx
        var accStart = 0
        for (i in 0 until startIdx) accStart += pieces[i].length

        val deleteStart = offset - accStart
        val deleteEnd = offset + length - accStart

        // Trim start piece
        if (deleteStart > 0) {
            val piece = pieces[startIdx]
            pieces[startIdx] = Piece(piece.bufferType, piece.start, deleteStart)
            startIdx++
        }

        // Trim end piece
        if (endIdx < pieces.size) {
            var accEnd = 0
            for (i in 0..endIdx) accEnd += pieces[i].length
            val endTrim = accEnd - (offset + length)
            if (endTrim > 0 && endIdx >= startIdx) {
                val piece = pieces[endIdx]
                pieces[endIdx] = Piece(piece.bufferType, piece.start + (piece.length - endTrim), endTrim)
            }
        }

        // Remove fully deleted pieces
        if (startIdx <= endIdx) {
            val toRemove = minOf(endIdx, pieces.size - 1) - startIdx + 1
            if (toRemove > 0) {
                for (i in 0 until toRemove) {
                    if (startIdx < pieces.size) pieces.removeAt(startIdx)
                }
            }
        }

        // Merge adjacent pieces of same type
        var i = 0
        while (i < pieces.size - 1) {
            val a = pieces[i]
            val b = pieces[i + 1]
            if (a.bufferType == b.bufferType && a.start + a.length == b.start) {
                pieces[i] = Piece(a.bufferType, a.start, a.length + b.length)
                pieces.removeAt(i + 1)
            } else {
                i++
            }
        }
    }

    fun rebuildLineIndex() {
        lineStarts.clear()
        if (totalLength == 0) {
            lineStarts.add(0)
            return
        }
        lineStarts.add(0)
        var offset = 0
        for (piece in pieces) {
            val buf = if (piece.bufferType == BufferType.ORIGINAL) originalBuffer else addBuffer
            for (j in 0 until piece.length) {
                if (buf[piece.start + j] == '\n') {
                    lineStarts.add(offset + j + 1)
                }
            }
            offset += piece.length
        }
        if (lineStarts.isEmpty()) lineStarts.add(0)
    }

    private fun updateLineIndexInsert(offset: Int, text: String) {
        var newlines = 0
        for (c in text) if (c == '\n') newlines++
        if (newlines == 0) return
        val insertLine = offsetToLine(offset)
        var searchFrom = 0
        for (i in 0 until newlines) {
            val nlPos = text.indexOf('\n', searchFrom)
            if (nlPos >= 0) {
                searchFrom = nlPos + 1
                val lineOffset = offset + nlPos + 1
                val insertIdx = (insertLine + i + 1).coerceAtMost(lineStarts.size)
                lineStarts.add(insertIdx, lineOffset)
            }
        }
        // Fix subsequent line offsets
        for (i in (insertLine + newlines + 1) until lineStarts.size) {
            lineStarts[i] += text.length
        }
        if (lineStarts.isEmpty()) lineStarts.add(0)
    }

    private fun updateLineIndexDelete(offset: Int, length: Int) {
        val deleteStart = offset
        val deleteEnd = offset + length
        val firstLine = offsetToLine(deleteStart)
        val lastLine = offsetToLine(deleteEnd)

        val removed = lastLine - firstLine
        if (removed > 0) {
            for (i in 0 until removed) {
                if (firstLine + 1 < lineStarts.size) lineStarts.removeAt(firstLine + 1)
            }
        }

        // Fix subsequent line offsets
        for (i in (firstLine + 1) until lineStarts.size) {
            lineStarts[i] -= length
            if (lineStarts[i] < lineStarts[i - 1]) lineStarts[i] = lineStarts[i - 1]
        }
        if (lineStarts.isEmpty()) lineStarts.add(0)
        if (lineStarts.size == 1 && lineStarts[0] > totalLength) lineStarts[0] = 0
    }
}
