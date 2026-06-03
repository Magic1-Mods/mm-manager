package bin.mg.editor.syntax.tokenizer

import android.os.Handler
import android.os.Looper

class TokenizerThread(
    private val tokenizer: Tokenizer,
    private val onTokenizeComplete: (line: Int, tokens: List<Token>) -> Unit
) {

    private var workerThread: Thread? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingRequest: TokenizeRequest? = null

    data class TokenizeRequest(
        val line: Int,
        val text: String
    )

    fun requestTokenize(line: Int, text: String) {
        pendingRequest = TokenizeRequest(line, text)
        startIfNeeded()
    }

    fun requestTokenizeRange(
        lines: List<Pair<Int, String>>,
        onComplete: () -> Unit
    ) {
        Thread {
            for ((lineIndex, lineText) in lines) {
                val tokens = tokenizer.tokenizeLine(lineText, lineIndex)
                mainHandler.post { onTokenizeComplete(lineIndex, tokens) }
            }
            mainHandler.post(onComplete)
        }.start()
    }

    private fun startIfNeeded() {
        if (workerThread?.isAlive == true) return
        workerThread = Thread {
            val request = pendingRequest ?: return@Thread
            pendingRequest = null
            val tokens = tokenizer.tokenizeLine(request.text, request.line)
            mainHandler.post { onTokenizeComplete(request.line, tokens) }
        }
        workerThread?.start()
    }

    fun shutdown() {
        workerThread?.interrupt()
        workerThread = null
    }
}
