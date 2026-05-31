package bin.mg.main.ui.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

class LinedEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {

    private val rect = android.graphics.Rect()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#9E9E9E")
        textSize = this@LinedEditText.textSize * 0.9f
    }
    private val bgPaint = Paint().apply {
        color = Color.parseColor("#EEEEEE")
    }
    private val gutterWidth: Int

    init {
        gutterWidth = (paint.measureText("9999") + 20).toInt()
        setPadding(gutterWidth + 10, paddingTop, paddingRight, paddingBottom)
    }

    override fun onDraw(canvas: Canvas) {
        val scrollY = scrollY
        canvas.drawRect(0f, scrollY.toFloat(), gutterWidth.toFloat(), (scrollY + height).toFloat(), bgPaint)

        val layout = layout ?: return
        val firstVisibleLine = layout.getLineForVertical(scrollY)
        val lastVisibleLine = layout.getLineForVertical(scrollY + height)

        for (i in firstVisibleLine..lastVisibleLine) {
            val lineNum = (i + 1).toString()
            val yPos = layout.getLineBaseline(i)
            canvas.drawText(lineNum, gutterWidth - paint.measureText(lineNum) - 10, yPos.toFloat(), paint)
        }
        super.onDraw(canvas)
    }
}
