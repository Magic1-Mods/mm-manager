package bin.mg.main.file;

import android.animation.ValueAnimator;
import android.content.Context;
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

/**
 * A draggable symbol panel that sits above the soft keyboard.
 * Matches MT Manager's symbol panel:
 * - One row visible when collapsed
 * - Drag up to expand and reveal all symbol rows
 * - Drag down to collapse back to one row
 * - Smooth animations, state preserved
 */
public class SymbolPanel extends LinearLayout {

    public interface OnSymbolClickListener {
        void onSymbolClick(String symbol);
    }

    private OnSymbolClickListener mListener;

    private static final String[][] SYMBOL_ROWS = {
        {"→", "/", "+", "-", "*", "=", "<"},
        {">", "\"", "'", ";", "|", "\\", "-"},
        {"()", "[]", "{}", "..."}
    };

    private static final String[][] INSERT_ROWS = {
        {"\t", "/", "+", "-", "*", "=", "<"},
        {">", "\"", "'", ";", "|", "\\", "-"},
        {"()", "[]", "{}", "..."}
    };

    private LinearLayout rowsContainer;
    private boolean isExpanded = false;
    private boolean isAnimating = false;

    private static final int ROW_HEIGHT_DP = 42;
    private static final int DRAG_THRESHOLD_DP = 36;
    private static final long ANIMATION_DURATION = 180;

    private int handleTop;
    private int handleBottom;

    public SymbolPanel(Context context) {
        super(context);
        init(context);
    }

    public SymbolPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SymbolPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        float density = getResources().getDisplayMetrics().density;

        // Drag handle
        LinearLayout handleLayout = new LinearLayout(context);
        handleLayout.setOrientation(HORIZONTAL);
        handleLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        handleLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (int)(10 * density)
        ));
        handleLayout.setPadding(0, (int)(3 * density), 0, (int)(2 * density));

        View handle = new View(context);
        int handleW = (int)(32 * density);
        int handleH = (int)(3 * density);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setShape(GradientDrawable.RECTANGLE);
        handleBg.setColor(0xFF555555);
        handleBg.setCornerRadius(handleH / 2f);
        handle.setBackground(handleBg);
        handleLayout.addView(handle);
        addView(handleLayout);

        rowsContainer = new LinearLayout(context);
        rowsContainer.setOrientation(VERTICAL);
        rowsContainer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        int textColor = 0xFFCCCCCC;
        int pressedColor = 0xFF444444;

        for (int i = 0; i < SYMBOL_ROWS.length; i++) {
            LinearLayout row = createSymbolRow(SYMBOL_ROWS[i], INSERT_ROWS[i], textColor, pressedColor, density);
            rowsContainer.addView(row);
            if (i > 0) {
                row.setVisibility(View.GONE);
            }
        }

        addView(rowsContainer);

        // Record handle area for drag detection
        handleLayout.post(() -> {
            int[] loc = new int[2];
            getLocationOnScreen(loc);
            handleTop = loc[1];
            handleBottom = handleTop + handleLayout.getHeight();
        });
    }

    private LinearLayout createSymbolRow(String[] symbols, String[] inserts, int textColor, int pressedColor, float density) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (int)(ROW_HEIGHT_DP * density)
        ));

        int dividerColor = 0xFF333333;

        for (int i = 0; i < symbols.length; i++) {
            final String insertText = inserts[i];

            TextView btn = new TextView(getContext());
            btn.setText(symbols[i]);
            btn.setTextColor(textColor);
            btn.setTextSize(16f);
            btn.setTypeface(Typeface.MONOSPACE);
            btn.setGravity(Gravity.CENTER);
            btn.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            ));
            btn.setClickable(true);
            btn.setFocusable(true);

            // Pressed state background
            StateListDrawable bg = new StateListDrawable();
            GradientDrawable pressed = new GradientDrawable();
            pressed.setShape(GradientDrawable.RECTANGLE);
            pressed.setColor(pressedColor);
            GradientDrawable normal = new GradientDrawable();
            normal.setShape(GradientDrawable.RECTANGLE);
            normal.setColor(Color.TRANSPARENT);
            bg.addState(new int[]{android.R.attr.state_pressed}, pressed);
            bg.addState(new int[]{}, normal);
            btn.setBackground(bg);

            btn.setMinWidth(0);
            btn.setMinimumWidth(0);
            btn.setPadding(0, 0, 0, 0);

            btn.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onSymbolClick(insertText);
                }
            });

            row.addView(btn);

            if (i < symbols.length - 1) {
                View divider = new View(getContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                    (int)(1 * density),
                    LinearLayout.LayoutParams.MATCH_PARENT
                ));
                divider.setBackgroundColor(dividerColor);
                row.addView(divider);
            }
        }

        return row;
    }

    public void setOnSymbolClickListener(OnSymbolClickListener listener) {
        mListener = listener;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

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
        if (isExpanded) collapse();
        else expand();
    }

    private void updateVisibility(boolean showAll) {
        isAnimating = true;
        int childCount = rowsContainer.getChildCount();

        for (int i = 1; i < childCount; i++) {
            final View row = rowsContainer.getChildAt(i);
            if (showAll) {
                row.setVisibility(View.VISIBLE);
                row.setAlpha(0f);
                row.animate()
                    .alpha(1f)
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> {
                        if (i == childCount - 1) isAnimating = false;
                    })
                    .start();
            } else {
                final int index = i;
                row.animate()
                    .alpha(0f)
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> {
                        row.setVisibility(View.GONE);
                        row.setAlpha(1f);
                        if (index == childCount - 1) isAnimating = false;
                    })
                    .start();
            }
        }

        if (childCount <= 1) {
            isAnimating = false;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Only intercept drags that start on the handle area
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            int[] loc = new int[2];
            getLocationOnScreen(loc);
            float rawY = ev.getRawY();
            // Check if touch is in the top 14dp (handle area)
            float density = getResources().getDisplayMetrics().density;
            float handleAreaBottom = loc[1] + 14 * density;
            if (rawY >= loc[1] && rawY <= handleAreaBottom) {
                dragStartRawY = ev.getRawY();
                isDragging = false;
                return false; // Don't intercept yet, wait for movement
            }
        }
        return false;
    }

    private float dragStartRawY;
    private boolean isDragging;

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
                float dy = dragStartRawY - event.getRawY();
                if (Math.abs(dy) > threshold) {
                    isDragging = true;
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (isDragging) {
                    float totalDy = dragStartRawY - event.getRawY();
                    if (totalDy > threshold && !isExpanded) {
                        expand();
                    } else if (totalDy < -threshold && isExpanded) {
                        collapse();
                    }
                }
                isDragging = false;
                return true;
        }
        return false;
    }
}
