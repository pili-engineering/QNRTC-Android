package com.qiniu.droid.rtc.demo.ui;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;

import com.qiniu.droid.rtc.demo.R;

public class UserTrackViewFullScreen extends UserTrackView {

    public UserTrackViewFullScreen(@NonNull Context context) {
        super(context);
    }

    public UserTrackViewFullScreen(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected int getLayout() {
        return R.layout.user_tracks_view_full_screen;
    }
}
