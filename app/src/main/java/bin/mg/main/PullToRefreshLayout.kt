package bin.mg.main

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class PullToRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface PullToRefreshListener {
        fun onRefresh(pullToRefreshLayout: PullToRefreshLayout)
    }

    private var listener: PullToRefreshListener? = null

    fun setPullToRefreshListener(listener: PullToRefreshListener?) {
        this.listener = listener
    }

    fun setRefreshing(refreshing: Boolean) {
        // Stub
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return true
    }
}
