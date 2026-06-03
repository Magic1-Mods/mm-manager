package bin.mg.main.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.widget.AbsListView
import android.widget.ListView
import java.lang.ref.WeakReference

/*
 Author : @developer-krushna (Krushna Chandra)
 Idea Extracted From MT Manager

 A perfect optimized ListView Fast Scroller
*/

class FastScrollerListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ListView(context, attrs, defStyleAttr) {

    private var trackColor: Int
    private var thumbColor: Int
    private var thumbWidth: Float
    private var thumbHeight: Float

    private var isDragging = false
    private var isScrolling = false
    private var isFastScrollEnabled = false
    private var isScrollbarVisible = false

    private var fadeStartTime = 0L
    private val thumbRect = RectF()
    private var dragOffsetY = 0f

    private val scrollHandler = ScrollHandler(this)
    private var lastKnownChildCount = 0
    private var externalScrollListener: AbsListView.OnScrollListener? = null

    private var transparentTrackBackground = false

    init {
        setHorizontalScrollBarEnabled(false)
        isVerticalScrollBarEnabled = false

        setOnScrollListener(InternalScrollListener(this))

        // Handle system fast scroll conflict
        if (android.os.Build.VERSION.SDK_INT >= 11 && super.isFastScrollEnabled) {
            isFastScrollEnabled = true
        }
        super.setFastScrollEnabled(false)

        if (!isInEditMode) {
            selector = createListSelector(context)
        }

        val density = context.resources.displayMetrics.density
        thumbColor = 0xDD777777.toInt()
        trackColor = 0x39777777
        thumbWidth = 8.0f * density
        thumbHeight = 48.0f * density
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        if (!isFastScrollEnabled) return

        val alphaMultiplier = calculateAlphaMultiplier()
        if (alphaMultiplier <= 0.0f) {
            isScrollbarVisible = false
            return
        }

        val totalCount = count
        val childCount = childCount

        // Sync child count logic
        if (childCount != lastKnownChildCount) {
            lastKnownChildCount = maxOf(childCount, lastKnownChildCount)
        }

        val scrollableRange = totalCount - childCount
        if (childCount <= 0 || scrollableRange <= 0) {
            isScrollbarVisible = false
            return
        }

        // Only show if list is long enough or we are currently dragging
        if (!isDragging && (totalCount / childCount.toFloat()) <= 4) {
            isScrollbarVisible = false
            return
        }

        isScrollbarVisible = true
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val width = width
        val height = height

        // Draw Track
        val trackAlpha = (Color.alpha(trackColor) * alphaMultiplier).toInt()
        if (!transparentTrackBackground) {
            paint.color = (trackAlpha shl 24) or (trackColor and 0x00FFFFFF.toInt())
        } else {
            paint.color = Color.TRANSPARENT
        }
        val currentThumbWidth = thumbWidth * alphaMultiplier
        val trackLeft = width - currentThumbWidth
        canvas.drawRect(trackLeft, 0f, width.toFloat(), height.toFloat(), paint)

        // Draw Thumb
        val activeThumbColor = if (isDragging) 0xFF1E88E5.toInt() else thumbColor
        val thumbAlpha = (Color.alpha(activeThumbColor) * alphaMultiplier).toInt()
        paint.color = (thumbAlpha shl 24) or (activeThumbColor and 0x00FFFFFF.toInt())

        val thumbTop = ((height - thumbHeight) / scrollableRange) * firstVisiblePosition
        val thumbBottom = thumbTop + thumbHeight

        thumbRect.set(trackLeft, thumbTop, width.toFloat(), thumbBottom)
        canvas.drawRect(trackLeft, thumbTop, width.toFloat(), thumbBottom, paint)
    }

    private fun calculateAlphaMultiplier(): Float {
        if (isDragging || isScrolling) return 1.0f

        val elapsed = SystemClock.uptimeMillis() - fadeStartTime
        if (elapsed < VISIBILITY_TIMEOUT) return 1.0f

        val fadeProgress = (elapsed - VISIBILITY_TIMEOUT) / FADE_DURATION_MS.toFloat()
        return (1.0f - fadeProgress).coerceAtLeast(0.0f)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Only intercept if we are touching the thumb area
        if (isFastScrollEnabled && isScrollbarVisible && ev.action == MotionEvent.ACTION_DOWN) {
            if (thumbRect.contains(ev.x, ev.y)) {
                return true
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isFastScrollEnabled || !isScrollbarVisible) {
            return super.onTouchEvent(event)
        }

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                if (thumbRect.contains(event.x, event.y)) {
                    isDragging = true
                    dragOffsetY = thumbRect.top - event.y
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    var relativeY = (event.y + dragOffsetY) / (height - thumbHeight)
                    relativeY = relativeY.coerceIn(0.0f, 1.0f)

                    val totalCount = count
                    val position = ((totalCount - childCount) * relativeY).toInt()
                    setSelection(position.coerceAtMost(totalCount - 1))
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    resetFadeTimer()
                    externalScrollListener?.onScrollStateChanged(this, OnScrollListener.SCROLL_STATE_IDLE)
                    invalidate()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun resetFadeTimer() {
        fadeStartTime = SystemClock.uptimeMillis()
        scrollHandler.removeMessages(0)
        scrollHandler.sendEmptyMessageDelayed(0, VISIBILITY_TIMEOUT)
    }

    override fun setFastScrollEnabled(enabled: Boolean) {
        isFastScrollEnabled = enabled
        invalidate()
    }

    fun setTransparentTrackBackground(bg: Boolean) {
        transparentTrackBackground = bg
    }

    override fun isFastScrollEnabled(): Boolean = isFastScrollEnabled

    override fun setOnScrollListener(l: OnScrollListener?) {
        externalScrollListener = l
    }

    companion object {
        private const val VISIBILITY_TIMEOUT = 3000L
        private const val FADE_DURATION_MS = 300L

        fun createListSelector(context: Context): Drawable {
            val stateList = StateListDrawable()
            val value = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, value, true)

            val baseColor = value.data and 0x10ffffff
            val highlightColor = value.data and 0x40ffffff

            val transition = TransitionDrawable(arrayOf(
                ColorDrawable(baseColor),
                ColorDrawable(highlightColor)
            ))

            // Add states (Simplified)
            val pressed = intArrayOf(android.R.attr.state_pressed)
            val selected = intArrayOf(android.R.attr.state_selected)

            stateList.addState(pressed, ColorDrawable(0))
            stateList.addState(selected, transition)
            stateList.addState(intArrayOf(), ColorDrawable(0))
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                stateList.exitFadeDuration = 300
            }

            return stateList
        }
    }

    // Static inner classes to prevent memory leaks
    private class ScrollHandler(view: FastScrollerListView) : Handler() {
        private val viewRef = WeakReference(view)
        private var isFading = false

        override fun handleMessage(msg: Message) {
            val view = viewRef.get() ?: return

            if (view.isScrolling) {
                view.resetFadeTimer()
                return
            }

            val elapsedSinceFadeStart = SystemClock.uptimeMillis() - view.fadeStartTime
            when {
                elapsedSinceFadeStart < VISIBILITY_TIMEOUT -> {
                    sendEmptyMessageDelayed(0, VISIBILITY_TIMEOUT - elapsedSinceFadeStart)
                    isFading = true
                }
                elapsedSinceFadeStart < VISIBILITY_TIMEOUT + FADE_DURATION_MS -> {
                    isFading = true
                    view.invalidate()
                    sendEmptyMessage(0) // Trigger next frame of fade
                }
                isFading -> {
                    isFading = false
                    view.invalidate()
                }
            }
        }
    }

    private class InternalScrollListener(view: FastScrollerListView) : AbsListView.OnScrollListener {
        private val viewRef = WeakReference(view)

        override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
            viewRef.get()?.externalScrollListener?.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount)
        }

        override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
            val parent = viewRef.get() ?: return

            parent.isScrolling = scrollState != OnScrollListener.SCROLL_STATE_IDLE
            if (!parent.isScrolling) {
                parent.resetFadeTimer()
            }

            parent.externalScrollListener?.onScrollStateChanged(view, scrollState)
        }
    }
}