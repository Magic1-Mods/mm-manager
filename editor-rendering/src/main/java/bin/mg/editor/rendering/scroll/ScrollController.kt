package bin.mg.editor.rendering.scroll

import android.widget.OverScroller
import android.content.Context

class ScrollController(context: Context) {

    private val scroller = OverScroller(context)

    var scrollX: Float = 0f
        private set
    var scrollY: Float = 0f
        private set
    var maxScrollX: Float = 0f
    var maxScrollY: Float = 0f

    var onScrollChanged: ((scrollX: Float, scrollY: Float) -> Unit)? = null

    fun scrollTo(x: Float, y: Float) {
        scrollX = x.coerceIn(0f, maxScrollX)
        scrollY = y.coerceIn(0f, maxScrollY)
        onScrollChanged?.invoke(scrollX, scrollY)
    }

    fun scrollBy(dx: Float, dy: Float) {
        scrollTo(scrollX + dx, scrollY + dy)
    }

    fun fling(velocityX: Float, velocityY: Float) {
        scroller.fling(
            scrollX.toInt(), scrollY.toInt(),
            velocityX.toInt(), velocityY.toInt(),
            0, maxScrollX.toInt(),
            0, maxScrollY.toInt()
        )
    }

    fun computeScrollOffset(): Boolean {
        if (scroller.isFinished) return false
        scroller.computeScrollOffset()
        val oldX = scrollX
        val oldY = scrollY
        scrollX = scroller.currX.toFloat().coerceIn(0f, maxScrollX)
        scrollY = scroller.currY.toFloat().coerceIn(0f, maxScrollY)
        return (scrollX != oldX || scrollY != oldY).also {
            if (it) onScrollChanged?.invoke(scrollX, scrollY)
        }
    }

    fun forceStop() {
        scroller.forceFinished(true)
    }

    fun smoothScrollTo(targetX: Float, targetY: Float, duration: Int = 300) {
        scroller.startScroll(
            scrollX.toInt(), scrollY.toInt(),
            (targetX - scrollX).toInt(), (targetY - scrollY).toInt(),
            duration
        )
    }

    fun updateMaxScroll(maxX: Float, maxY: Float) {
        maxScrollX = maxX.coerceAtLeast(0f)
        maxScrollY = maxY.coerceAtLeast(0f)
        scrollX = scrollX.coerceIn(0f, maxScrollX)
        scrollY = scrollY.coerceIn(0f, maxScrollY)
    }

    fun ensureCursorVisible(cursorLine: Int, lineHeight: Float, viewHeight: Float, gutterWidth: Float) {
        val cursorY = cursorLine * lineHeight
        val topThreshold = scrollY + lineHeight * 2
        val bottomThreshold = scrollY + viewHeight - lineHeight * 2

        when {
            cursorY < topThreshold -> smoothScrollTo(scrollX, (cursorY - lineHeight * 2).coerceAtLeast(0f))
            cursorY > bottomThreshold -> smoothScrollTo(scrollX, (cursorY - viewHeight + lineHeight * 2).coerceAtMost(maxScrollY))
        }
    }
}
