package bin.mg.editor.rendering.input

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import bin.mg.editor.core.document.EditorBuffer
import bin.mg.editor.core.cursor.CursorManager
import bin.mg.editor.rendering.layout.LayoutEngine
import bin.mg.editor.rendering.scroll.ScrollController
import kotlin.math.abs

class TouchHandler(
    private val context: Context,
    private val buffer: EditorBuffer,
    private val layoutEngine: LayoutEngine,
    private val scrollController: ScrollController,
    private val invalidate: () -> Unit,
    private val ensureCursorVisible: () -> Unit
) {

    private var lastTapTime = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f
    private var isDragging = false
    private var isScaling = false
    private var scaleFactor = 1.0f
    private var lastSpanY = 0f

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val now = System.currentTimeMillis()
            val doubleTap = (now - lastTapTime < 300) &&
                abs(e.x - lastTapX) < 30 && abs(e.y - lastTapY) < 30
            lastTapTime = now
            lastTapX = e.x
            lastTapY = e.y

            if (doubleTap) {
                // Double tap → select word
                val lineCol = hitTest(e.x, e.y) ?: return true
                buffer.cursorManager.selectWordAt(lineCol.first, lineCol.second)
                invalidate()
                ensureCursorVisible()
                return true
            }

            // Single tap → place cursor
            val lineCol = hitTest(e.x, e.y) ?: return true
            buffer.cursorManager.moveTo(lineCol.first, lineCol.second)
            invalidate()
            ensureCursorVisible()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            val lineCol = hitTest(e.x, e.y) ?: return
            buffer.cursorManager.selectWordAt(lineCol.first, lineCol.second)
            isDragging = true
            invalidate()
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (isScaling) return false
            scrollController.scrollBy(distanceX, distanceY)
            invalidate()
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            scrollController.fling(-velocityX, -velocityY)
            invalidate()
            return true
        }
    })

    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isScaling = true
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(0.5f, 3.0f)
            invalidate()
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            isScaling = false
        }
    })

    fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        if (isScaling) return true

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = false
                scrollController.forceStop()
                gestureDetector.onTouchEvent(event)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val lineCol = hitTest(event.x, event.y)
                    if (lineCol != null) {
                        buffer.cursorManager.moveTo(lineCol.first, lineCol.second, true)
                        invalidate()
                        ensureCursorVisible()
                    }
                    return true
                }
                gestureDetector.onTouchEvent(event)
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    isDragging = false
                    invalidate()
                    return true
                }
                gestureDetector.onTouchEvent(event)
                return true
            }
        }
        return false
    }

    private fun hitTest(x: Float, y: Float): Pair<Int, Int>? {
        val gutterWidth = layoutEngine.gutterWidth
        val lineHeight = layoutEngine.lineHeight
        val contentX = x - gutterWidth + scrollController.scrollX
        val contentY = y + scrollController.scrollY

        if (contentX < 0) return null

        val line = (contentY / lineHeight).toInt()
        if (line < 0 || line >= buffer.getLineCount()) return null

        val lineText = buffer.getLineText(line)
        val layout = layoutEngine.getLineLayout(line, lineText)
        val column = layout.getColumnAtX(contentX)

        return Pair(line, column.coerceIn(0, lineText.length))
    }

    fun getScaleFactor(): Float = scaleFactor
}
