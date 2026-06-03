package bin.mg.main.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/*
 Author : @developer-krushna (Krushna Chandra)
 Idea Extracted From MT Manager

 A perfect optimized RecyclerView Fast Scroller for Android
*/

class FastScrollerRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {

    private var thumbWidth: Float
    private var thumbColor: Int
    private var isScrollerEnabled = true
    private val scrollerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var isDragging = false
    private var isManualVisibilityForced = false
    private var lastScrollPosition = -1
    private var isScrollerCurrentlyVisible = false
    private var lastChildCount = 0
    private val thumbRect = RectF()
    private var thumbHeight: Float
    private var lastInteractionTime = 0L
    private var touchOffset = 0f
    private var isDefaultScrollBarEnabled = true

    init {
        val density = context.resources.displayMetrics.density
        thumbColor = 0xDD666666
        thumbWidth = 8.0f * density
        thumbHeight = 48.0f * density

        // Essential: Allow drawing on the RecyclerView itself
        setWillNotDraw(false)

        // Essential: Force redraw during scrolling
        addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                // Update interaction time so scroller shows up when swiping
                if (!isDragging) {
                    lastInteractionTime = SystemClock.uptimeMillis()
                }
                invalidate()
            }
        })

        layoutManager = LinearLayoutManager(context)

        itemAnimator?.apply {
            addDuration = 100L
            removeDuration = 100L
            moveDuration = 200L
            changeDuration = 100L
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isScrollerEnabled || adapter == null) return

        val itemCount = adapter!!.itemCount
        val childCount = childCount

        // Check if list is long enough to show scroller (approx > 4 screens)
        if (childCount <= 0 || (itemCount - childCount) <= 0 || itemCount / childCount <= 4) {
            if (!isDefaultScrollBarEnabled) {
                isDefaultScrollBarEnabled = true
                isVerticalScrollBarEnabled = true
            }
            isScrollerCurrentlyVisible = false
            return
        }

        val range = computeVerticalScrollRange()
        val extent = computeVerticalScrollExtent()
        val offset = computeVerticalScrollOffset()
        val scrollableRange = range - extent

        if (scrollableRange <= 0) {
            isScrollerCurrentlyVisible = false
            return
        }

        // Fading Logic
        var alphaMultiplier = 1.0f
        if (!isDragging && !isManualVisibilityForced) {
            val timeSinceInteraction = SystemClock.uptimeMillis() - lastInteractionTime
            if (timeSinceInteraction > 1500) {
                val fadeTime = timeSinceInteraction - 1500
                alphaMultiplier = if (fadeTime < 300) 1.0f - (fadeTime / 300.0f) else 0.0f
            }
        }

        if (alphaMultiplier <= 0.0f) {
            isScrollerCurrentlyVisible = false
            return
        }

        isScrollerCurrentlyVisible = true
        if (isDefaultScrollBarEnabled) {
            isDefaultScrollBarEnabled = false
            isVerticalScrollBarEnabled = false
        }

        val width = width
        val height = height

        // 1. Draw Track
        val trackColor = 0x11000000 // Very faint track
        val trackAlpha = (Color.alpha(trackColor) * alphaMultiplier).toInt()
        scrollerPaint.color = (trackAlpha shl 24) or (trackColor and 0x00FFFFFF)
        val trackLeft = width - (thumbWidth * alphaMultiplier)
        canvas.drawRect(trackLeft, 0f, width.toFloat(), height.toFloat(), scrollerPaint)

        // 2. Draw Thumb
        val activeColor = if (isDragging) 0xFF1E88E5 else thumbColor // Blue if dragging
        val thumbAlpha = (Color.alpha(activeColor) * alphaMultiplier).toInt()
        scrollerPaint.color = (thumbAlpha shl 24) or (activeColor and 0x00FFFFFF)

        val thumbTop = (offset.toFloat() / scrollableRange) * (height - thumbHeight)
        val thumbBottom = thumbTop + thumbHeight

        // Update hit-rect for touch events
        thumbRect.set(width - (thumbWidth * 2.0f), thumbTop, width.toFloat(), thumbBottom)
        canvas.drawRect(trackLeft, thumbTop, width.toFloat(), thumbBottom, scrollerPaint)

        // Continue animating if in fade-out state
        if (alphaMultiplier < 1.0f && alphaMultiplier > 0.0f) {
            postInvalidateOnAnimation()
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isScrollerEnabled && isScrollerCurrentlyVisible && ev.action == MotionEvent.ACTION_DOWN) {
            if (thumbRect.contains(ev.x, ev.y)) {
                isDragging = true
                touchOffset = thumbRect.top - ev.y
                return true
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        lastChildCount = 0
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (isDragging) {
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastScrollPosition = -1
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    var relativeY = (ev.y + touchOffset) / (height - thumbHeight)
                    relativeY = relativeY.coerceIn(0.0f, 1.0f)

                    val position = ((adapter!!.itemCount - 1) * relativeY).toInt()
                    if (lastScrollPosition != position) {
                        lastScrollPosition = position
                        (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDragging = false
                    lastInteractionTime = SystemClock.uptimeMillis()
                    invalidate()
                    return true
                }
            }
        }
        return super.onTouchEvent(ev)
    }
}