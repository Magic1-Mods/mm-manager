package bin.mg.main.ui.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;

public class LinedEditText extends androidx.appcompat.widget.AppCompatEditText {
    private Rect rect;
    private Paint paint;
    private Paint bgPaint;
    private int gutterWidth;

    public LinedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        rect = new Rect();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#9E9E9E"));
        paint.setTextSize(getTextSize() * 0.9f);
        
        bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#EEEEEE"));

        // Set padding to make room for line numbers
        gutterWidth = (int) (paint.measureText("9999") + 20);
        setPadding(gutterWidth + 10, getPaddingTop(), getPaddingRight(), getPaddingBottom());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw gutter background
        int scrollY = getScrollY();
        canvas.drawRect(0, scrollY, gutterWidth, scrollY + getHeight(), bgPaint);

        int baseline = getBaseline();
        int lineCount = getLineCount();
        
        // Optimize drawing to only visible lines
        int firstVisibleLine = getLayout().getLineForVertical(scrollY);
        int lastVisibleLine = getLayout().getLineForVertical(scrollY + getHeight());

        for (int i = firstVisibleLine; i <= lastVisibleLine; i++) {
            int lineNum = i + 1;
            String lineNumStr = String.valueOf(lineNum);
            
            // Calculate Y position for the baseline of the current line
            int yPos = getLayout().getLineBaseline(i);
            
            canvas.drawText(lineNumStr, gutterWidth - paint.measureText(lineNumStr) - 10, yPos, paint);
        }
        super.onDraw(canvas);
    }
}
