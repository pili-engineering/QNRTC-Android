package com.qiniu.droid.rtc.demo.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;

import com.qiniu.droid.rtc.demo.R;


public class CircleTextView extends android.support.v7.widget.AppCompatTextView {
    private Paint mPaint;
    private RectF mRectf;
    private int mColor;

    public CircleTextView(Context context) {
        super(context);
        init();
    }

    public CircleTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleTextView);
        mColor = typedArray.getColor(R.styleable.CircleTextView_circle_color,Color.parseColor("#588CEE"));
        typedArray.recycle();
        init();
    }

    public CircleTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mPaint.setColor(mColor);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
        int r = getMeasuredWidth() > getMeasuredHeight() ? getMeasuredWidth() : getMeasuredHeight();
        mRectf.set(getPaddingLeft(), getPaddingTop(), r - getPaddingRight(), r - getPaddingBottom());
        canvas.drawArc(mRectf, 0, 360, false, mPaint);
        super.onDraw(canvas);
    }

    private void init() {
        mPaint = new Paint();
        mRectf = new RectF();
    }

    public void setCircleColor(int color){
        mColor = color;
    }
}
