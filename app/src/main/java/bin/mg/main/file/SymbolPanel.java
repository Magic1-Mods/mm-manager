package bin.mg.main.file;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SymbolPanel extends LinearLayout {

    public interface OnSymbolClickListener {
        void onSymbolClick(String symbol);
    }

    private OnSymbolClickListener mListener;
    private Context mContext;

    private static final String[][] SYMBOL_ROWS = {
        {"→", "/", "+", "-", "*", "=", "<"},
        {">", "\"", "'", ";", "|", "\\", "_"},
        {"()", "[]", "{}", "..."}
    };

    private static final String[][] INSERT_ROWS = {
        {null, "/", "+", "-", "*", "=", "<"},
        {">", "\"", "'", ";", "|", "\\", "_"},
        {"()", "[]", "{}", "..."}
    };

    private LinearLayout rowsContainer;
    private boolean isExpanded = false;
    private boolean isAnimating = false;

    private static final int ROW_HEIGHT_DP = 40;
    private static final int DRAG_THRESHOLD_DP = 32;
    private static final long ANIMATION_DURATION = 160;

    public SymbolPanel(Context context) {
        super(context);
        mContext = context;
        init(context);
    }

    public SymbolPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(context);
    }

    public SymbolPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        float density = getResources().getDisplayMetrics().density;

        LinearLayout handleLayout = new LinearLayout(context);
        handleLayout.setOrientation(HORIZONTAL);
        handleLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        handleLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (int)(12 * density)
        ));
        handleLayout.setPadding(0, (int)(4 * density), 0, (int)(3 * density));

        View handle = new View(context);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(
            (int)(28 * density), (int)(3 * density));
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setShape(GradientDrawable.RECTANGLE);
        handleBg.setColor(0xFF555555);
        handleBg.setCornerRadius((int)(2 * density));
        handle.setBackground(handleBg);
        handle.setLayoutParams(handleParams);
        handleLayout.addView(handle);
        addView(handleLayout);

        rowsContainer = new LinearLayout(context);
        rowsContainer.setOrientation(VERTICAL);
        rowsContainer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        for (int i = 0; i < SYMBOL_ROWS.length; i++) {
            View sep = new View(context);
            LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int)(0.8f * density));
            sep.setLayoutParams(sepParams);
            sep.setBackgroundColor(0xFF2C2C2C);
            rowsContainer.addView(sep);

            LinearLayout row = createSymbolRow(i, density);
            rowsContainer.addView(row);

            if (i > 0) {
                sep.setVisibility(View.GONE);
                row.setVisibility(View.GONE);
            }
        }

        addView(rowsContainer);
    }

    private LinearLayout createSymbolRow(final int rowIndex, float density) {
        final String[] labels = SYMBOL_ROWS[rowIndex];
        final String[] inserts = INSERT_ROWS[rowIndex];

        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (int)(ROW_HEIGHT_DP * density)
        ));

        int textColor = 0xFFCCCCCC;
        int pressedBg = 0x33FFFFFF;

        for (int i = 0; i < labels.length; i++) {
            final int idx = i;

            TextView btn = new TextView(getContext());
            btn.setText(labels[i]);
            btn.setTextColor(textColor);
            btn.setTextSize(15f);
            btn.setTypeface(Typeface.MONOSPACE);
            btn.setGravity(Gravity.CENTER);
            btn.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            ));
            btn.setClickable(true);
            btn.setFocusable(true);
            btn.setMinWidth(0);
            btn.setMinimumWidth(0);
            btn.setPadding(0, 0, 0, 0);

            GradientDrawable pressedShape = new GradientDrawable();
            pressedShape.setShape(GradientDrawable.RECTANGLE);
            pressedShape.setColor(pressedBg);
            GradientDrawable normalShape = new GradientDrawable();
            normalShape.setShape(GradientDrawable.RECTANGLE);
            normalShape.setColor(Color.TRANSPARENT);
            StateListDrawable bg = new StateListDrawable();
            bg.addState(new int[]{android.R.attr.state_pressed}, pressedShape);
            bg.addState(new int[]{}, normalShape);
            btn.setBackground(bg);

            btn.setOnClickListener(v -> {
                if (mListener == null) return;
                if (rowIndex == 0 && idx == 0) {
                    mListener.onSymbolClick(buildTabString());
                } else {
                    mListener.onSymbolClick(inserts[idx]);
                }
            });

            row.addView(btn);
        }

        return row;
    }

    private String buildTabString() {
        SharedPreferences prefs = mContext.getSharedPreferences("editor_prefs", Context.MODE_PRIVATE);
        boolean useTabs = prefs.getBoolean("use_tabs", false);
        if (useTabs) {
            return "\t";
        }
        int tabSize = prefs.getInt("tab_size", 4);
        StringBuilder sb = new StringBuilder(tabSize);
        for (int i = 0; i < tabSize; i++) sb.append(' ');
        return sb.toString();
    }

    public void setOnSymbolClickListener(OnSymbolClickListener listener) {
        mListener = listener;
    }

    public boolean isExpanded() { return isExpanded; }

    public void expand() {
        if (isExpanded || isAnimating) return;
        isExpanded = true;
        updateVisibility(true);
    }

    public void collapse() {
        if (!isExpanded || isAnimating) return;
        isExpanded = false;
        updateVisibility(false);
    }

    public void toggle() {
        if (isExpanded) collapse(); else expand();
    }

    private void updateVisibility(boolean showAll) {
        isAnimating = true;
        int childCount = rowsContainer.getChildCount();
        int animTargets = 0;

        for (int i = 2; i < childCount; i++) {
            final View child = rowsContainer.getChildAt(i);
            final boolean isLast = (i == childCount - 1);
            animTargets++;
            if (showAll) {
                child.setVisibility(View.VISIBLE);
                child.setAlpha(0f);
                child.animate()
                    .alpha(1f)
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> { if (isLast) isAnimating = false; })
                    .start();
            } else {
                child.animate()
                    .alpha(0f)
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> {
                        child.setVisibility(View.GONE);
                        child.setAlpha(1f);
                        if (isLast) isAnimating = false;
                    })
                    .start();
            }
        }

        if (animTargets == 0) isAnimating = false;
    }

    private float dragStartRawY;
    private boolean isDragging;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            float density = getResources().getDisplayMetrics().density;
            int[] loc = new int[2];
            getLocationOnScreen(loc);
            float handleBottom = loc[1] + 14 * density;
            if (ev.getRawY() >= loc[1] && ev.getRawY() <= handleBottom) {
                dragStartRawY = ev.getRawY();
                isDragging = false;
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float density = getResources().getDisplayMetrics().density;
        float threshold = DRAG_THRESHOLD_DP * density;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                dragStartRawY = event.getRawY();
                isDragging = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (Math.abs(dragStartRawY - event.getRawY()) > threshold) isDragging = true;
                return true;

            case MotionEvent.ACTION_UP:
                if (isDragging) {
                    float dy = dragStartRawY - event.getRawY();
                    if (dy > threshold && !isExpanded) expand();
                    else if (dy < -threshold && isExpanded) collapse();
                }
                isDragging = false;
                return true;
        }
        return false;
    }
}
