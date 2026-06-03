package bin.mg.editor.rendering.render

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.text.TextPaint
import bin.mg.editor.rendering.layout.LineLayout

class CursorRenderer {

    private val cursorPaint = Paint()
    private val secondaryCursorPaint = Paint()
    private val cursorBlinkHandler = Handler(Looper.getMainLooper())
    private var cursorVisible = true
    private var blinkRunnable: Runnable? = null

    var cursorColor = 0xFFA9B7C6.toInt()
        set(value) { field = value; cursorPaint.color = value; cursorPaint.alpha = 255 }
    var cursorWidth: Float = 2f

    fun startBlinking() {
        stopBlinking()
        cursorVisible = true
        blinkRunnable = object : Runnable {
            override fun run() {
                cursorVisible = !cursorVisible
                cursorPaint.alpha = if (cursorVisible) 255 else 0
                cursorBlinkHandler.postDelayed(this, 530)
            }
        }
        cursorBlinkHandler.postDelayed(blinkRunnable!!, 530)
    }

    fun stopBlinking() {
        blinkRunnable?.let { cursorBlinkHandler.removeCallbacks(it) }
        blinkRunnable = null
        cursorVisible = true
        cursorPaint.alpha = 255
    }

    fun resetBlink() {
        stopBlinking()
        startBlinking()
    }

    fun draw(
        canvas: Canvas,
        x: Float,
        y: Float,
        lineHeight: Float
    ) {
        if (cursorPaint.alpha == 0) return
        canvas.drawRect(x, y, x + cursorWidth, y + lineHeight, cursorPaint)
    }

    fun drawSecondary(
        canvas: Canvas,
        x: Float,
        y: Float,
        lineHeight: Float
    ) {
        secondaryCursorPaint.color = cursorColor
        secondaryCursorPaint.alpha = 128
        canvas.drawRect(x, y, x + cursorWidth, y + lineHeight, secondaryCursorPaint)
    }
}
