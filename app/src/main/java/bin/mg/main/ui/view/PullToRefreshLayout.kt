package bin.mg.main.ui.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.annotation.IntDef
import androidx.core.view.ViewCompat

class PullToRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private val DECELERATE_INTERPOLATOR = DecelerateInterpolator(10f)

        const val STATE_IDLE = 0
        const val STATE_DRAGGING = 1
        const val STATE_RELEASING = 2
        const val STATE_SETTLING = 3
        const val STATE_REFRESHING = 4
        const val STATE_REFRESHING_SETTLING = 5
    }

    @IntDef(
        STATE_IDLE,
        STATE_DRAGGING,
        STATE_RELEASING,
        STATE_SETTLING,
        STATE_REFRESHING,
        STATE_REFRESHING_SETTLING
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class State

    private var touchStartY = 0f
    private var currentY = 0f

    private var childView: View? = null
    private var header: FrameLayout? = null

    var pullHeight = 0f
        private set

    var headerHeight = 0f
        private set

    var triggerHeight = 0f

    @State
    private var state = STATE_IDLE

    private var refreshListener: PullToRefreshListener? = null
    private var pullingListener: PullToRefreshPullingListener? = null

    init {
        initLayout()
    }

    private fun initLayout() {
        if (isInEditMode) return

        if (childCount > 1) {
            throw RuntimeException("You can only attach one child")
        }

        pullHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            150f,
            resources.displayMetrics
        )

        headerHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            56f,
            resources.displayMetrics
        )

        triggerHeight = headerHeight

        post {
            childView = getChildAt(0)
            addHeaderContainer()
        }
    }

    fun setHeaderView(view: View) {
        post {
            header?.addView(view)
        }
    }

    private fun addHeaderContainer() {
        header = FrameLayout(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        addViewInternal(header!!)
        setupChildAnimator()
    }

    private fun setupChildAnimator() {
        childView?.animate()?.apply {
            interpolator = DecelerateInterpolator()
            setUpdateListener {
                pullingListener?.onTranslationYChanged(
                    childView?.translationY ?: 0f
                )
            }
        }
    }

    private fun addViewInternal(view: View) {
        super.addView(view)
    }

    override fun addView(child: View) {
        if (childCount >= 1) {
            throw RuntimeException("You can only attach one child")
        }

        childView = child
        super.addView(child)
        setupChildAnimator()
    }

    fun canChildScrollUp(): Boolean {
        return childView?.let {
            ViewCompat.canScrollVertically(it, -1)
        } ?: false
    }

    @State
    fun getState(): Int = state

    fun setState(@State newState: Int) {
        if (state != newState) {
            state = newState
            onStateChanged(newState)
        }
    }

    protected open fun onStateChanged(@State newState: Int) {}

    fun isRefreshing(): Boolean {
        return state == STATE_REFRESHING
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {

        if (
            state == STATE_REFRESHING ||
            state == STATE_SETTLING ||
            state == STATE_REFRESHING_SETTLING
        ) {
            return true
        }

        when (ev.actionMasked) {

            MotionEvent.ACTION_DOWN -> {
                touchStartY = ev.y
                currentY = touchStartY
            }

            MotionEvent.ACTION_MOVE -> {
                val dy = ev.y - touchStartY

                if (dy > 0f && !canChildScrollUp()) {
                    return true
                }
            }
        }

        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        if (
            state == STATE_REFRESHING ||
            state == STATE_RELEASING ||
            state == STATE_SETTLING ||
            state == STATE_REFRESHING_SETTLING
        ) {
            return super.onTouchEvent(event)
        }

        when (event.actionMasked) {

            MotionEvent.ACTION_MOVE -> {

                setState(STATE_DRAGGING)

                currentY = event.y
                val currentX = event.x

                val dy = constrain(
                    0f,
                    pullHeight * 2f,
                    currentY - touchStartY
                )

                childView?.let { view ->

                    val offsetY =
                        DECELERATE_INTERPOLATOR.getInterpolation(
                            dy / pullHeight / 2f
                        ) * dy / 2f

                    view.translationY = offsetY

                    pullingListener?.apply {
                        onTranslationYChanged(offsetY)
                        onPulling(
                            offsetY / headerHeight,
                            currentX
                        )
                    }
                }

                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {

                childView?.let { view ->

                    view.animate()
                        .translationY(0f)
                        .setDuration(300)
                        .setListener(
                            stateListener(
                                STATE_RELEASING,
                                STATE_IDLE
                            )
                        )
                        .start()

                    if (
                        view.translationY >= triggerHeight &&
                        refreshListener != null
                    ) {
                        refreshListener?.onRefresh(this)
                    }

                } ?: setState(STATE_IDLE)

                return true
            }
        }

        return super.onTouchEvent(event)
    }

    fun setPullToRefreshListener(listener: PullToRefreshListener?) {
        refreshListener = listener
    }

    fun setPullingListener(listener: PullToRefreshPullingListener?) {
        pullingListener = listener
    }

    fun setRefreshing(refreshing: Boolean) {

        val view = childView ?: run {
            setState(STATE_IDLE)
            return
        }

        if (refreshing) {

            view.animate()
                .translationY(headerHeight)
                .setListener(
                    stateListener(
                        STATE_SETTLING,
                        STATE_REFRESHING
                    )
                )
                .start()

        } else {

            if (!isRefreshing()) return

            view.animate()
                .translationY(0f)
                .setListener(
                    stateListener(
                        STATE_SETTLING,
                        STATE_IDLE
                    )
                )
                .start()
        }
    }

    private fun stateListener(
        startState: Int,
        endState: Int
    ) = object : AnimatorListenerAdapter() {

        override fun onAnimationStart(animation: Animator) {
            setState(startState)
        }

        override fun onAnimationEnd(animation: Animator) {
            setState(endState)
        }

        override fun onAnimationCancel(animation: Animator) {
            setState(endState)
        }
    }

    private fun constrain(
        min: Float,
        max: Float,
        value: Float
    ): Float {
        return value.coerceIn(min, max)
    }

    interface PullToRefreshListener {
        fun onRefresh(layout: PullToRefreshLayout)
    }

    interface PullToRefreshPullingListener {

        fun onPulling(
            fraction: Float,
            pointXPosition: Float
        )

        fun onTranslationYChanged(
            translationY: Float
        )
    }
}