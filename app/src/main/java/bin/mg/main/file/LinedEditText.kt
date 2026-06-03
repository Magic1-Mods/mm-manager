package bin.mg.main.file

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import bin.mg.main.R

class LinedEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.editor_line_number)
        textSize = 28f
    }

    private val gutterPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.editor_gutter_bg)
    }

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.grey_300)
        strokeWidth = 1f
    }

    private val gutterWidth: Int = (linePaint.measureText("9999") + 30).toInt()

    init {
        setPadding(gutterWidth + 12, paddingTop, paddingRight, paddingBottom)
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
        setTextColor(ContextCompat.getColor(context, R.color.editor_text))
    }

    override fun onDraw(canvas: Canvas) {
        val scrollY = scrollY

        canvas.drawRect(0f, scrollY.toFloat(), gutterWidth.toFloat(), (scrollY + height).toFloat(), gutterPaint)

        val layout = layout ?: run {
            super.onDraw(canvas)
            return
        }

        val firstVisibleLine = layout.getLineForVertical(scrollY)
        val lastVisibleLine = layout.getLineForVertical(scrollY + height)

        for (i in firstVisibleLine..lastVisibleLine) {
            val lineNum = (i + 1).toString()
            val yPos = layout.getLineBaseline(i)
            val xPos = gutterWidth - linePaint.measureText(lineNum) - 12f
            canvas.drawText(lineNum, xPos, yPos.toFloat(), linePaint)
        }

        canvas.drawLine(gutterWidth.toFloat(), scrollY.toFloat(), gutterWidth.toFloat(), (scrollY + height).toFloat(), dividerPaint)

        super.onDraw(canvas)
    }
}
