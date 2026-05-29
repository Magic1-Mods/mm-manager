package bin.mg.main;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class PullToRefreshLayout extends FrameLayout {

    public interface PullToRefreshListener {
        void onRefresh(PullToRefreshLayout pullToRefreshLayout);
    }

    private PullToRefreshListener listener;

    public PullToRefreshLayout(Context context) {
        this(context, null);
    }

    public PullToRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullToRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setPullToRefreshListener(PullToRefreshListener listener) {
        this.listener = listener;
    }

    public void setRefreshing(boolean refreshing) {
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return true;
    }
}
