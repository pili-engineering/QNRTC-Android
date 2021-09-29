package com.qiniu.droid.rtc.demo.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.widget.ScrollView;

public class FixedHeightScrollView extends ScrollView {
    private final Context mContext;

    public FixedHeightScrollView(Context context) {
        super(context);
        mContext = context;
    }

    public FixedHeightScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public FixedHeightScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public FixedHeightScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        try {
            Display display = ((Activity) mContext).getWindowManager().getDefaultDisplay();
            DisplayMetrics d = new DisplayMetrics();
            display.getMetrics(d);
            // 设置控件高度不能超过屏幕高度的 2/3
            int maxHeight = d.heightPixels * 2 / 3;
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
