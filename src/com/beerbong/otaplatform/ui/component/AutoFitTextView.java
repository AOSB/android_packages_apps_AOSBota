package com.beerbong.otaplatform.ui.component;

import com.beerbong.otaplatform.R;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

public class AutoFitTextView extends TextView {

    private Paint mTestPaint;
    private float mMaxSize;

    public AutoFitTextView(Context context) {
        super(context);
        initialise(context);
    }

    public AutoFitTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialise(context);
    }

    private void initialise(Context context) {
        mTestPaint = new Paint();
        mTestPaint.set(this.getPaint());
        mMaxSize = context.getResources().getDimension(R.dimen.auto_fit_max_size);
    }

    private void refitText(String text, int textWidth) {
        if (textWidth <= 0)
            return;
        int targetWidth = textWidth - this.getPaddingLeft() - this.getPaddingRight();
        float hi = 100;
        float lo = 2;
        final float threshold = 0.5f;

        mTestPaint.set(this.getPaint());

        while ((hi - lo) > threshold) {
            float size = (hi + lo) / 2;
            mTestPaint.setTextSize(size);
            if (mTestPaint.measureText(text) >= targetWidth)
                hi = size;
            else
                lo = size;
        }
        this.setTextSize(TypedValue.COMPLEX_UNIT_PX, Math.min(lo, mMaxSize));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int height = getMeasuredHeight();
        refitText(this.getText().toString(), parentWidth);
        this.setMeasuredDimension(parentWidth, height);
    }

    @Override
    protected void onTextChanged(final CharSequence text, final int start, final int before,
            final int after) {
        refitText(text.toString(), this.getWidth());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w != oldw) {
            refitText(this.getText().toString(), w);
        }
    }
}
