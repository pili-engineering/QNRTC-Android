package com.qiniu.droid.rtc.demo.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.qiniu.droid.rtc.QNLocalSurfaceView;
import com.qiniu.droid.rtc.QNRemoteSurfaceView;
import com.qiniu.droid.rtc.demo.R;

public class RTCVideoView extends FrameLayout implements View.OnLongClickListener {

    protected Context mContext;
    protected QNLocalSurfaceView mLocalSurfaceView;
    protected QNRemoteSurfaceView mRemoteSurfaceView;
    private ImageView mMicrophoneStateView;
    private TextView mAudioView;
    private OnLongClickListener mOnLongClickListener;
    private String mUserId;

    public interface OnLongClickListener {
        void onLongClick(String userId);
    }

    public RTCVideoView(Context context) {
        super(context);
        mContext = context;
    }

    public RTCVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public void setUserId(String userId) {
        mUserId = userId;
    }

    public void updateMicrophoneStateView(boolean isMute) {
        mMicrophoneStateView.setImageResource(isMute ? R.mipmap.microphone_disable : R.drawable.microphone_state_enable);
    }

    public QNLocalSurfaceView getLocalSurfaceView() {
        return mLocalSurfaceView;
    }

    public QNRemoteSurfaceView getRemoteSurfaceView() {
        return mRemoteSurfaceView;
    }

    public ImageView getMicrophoneStateView() {
        return mMicrophoneStateView;
    }

    public void setVisible(boolean isVisible) {
        if (!isVisible) {
            mUserId = null;
            mAudioView.setVisibility(INVISIBLE);
        }
        setVisibility(isVisible ? VISIBLE : INVISIBLE);
        mMicrophoneStateView.setVisibility(isVisible ? VISIBLE : INVISIBLE);
        setVideoViewVisible(isVisible);
    }

    public void setMicrophoneStateVisibility(int visibility) {
        mMicrophoneStateView.setVisibility(visibility);
    }

    public void setAudioViewVisible(int pos) {
        if (getVisibility() != VISIBLE) {
            setVisibility(VISIBLE);
        }
        mAudioView.setText(mUserId);
        mAudioView.setBackgroundColor(getTargetColor(pos));
        mAudioView.setVisibility(VISIBLE);
        setVideoViewVisible(false);
    }

    public void setAudioViewInvisible() {
        mAudioView.setVisibility(INVISIBLE);
        setVideoViewVisible(true);
    }

    public void updateAudioView(int pos) {
        mAudioView.setBackgroundColor(getTargetColor(pos));
    }

    public int getAudioViewVisibility() {
        return mAudioView.getVisibility();
    }

    public void setOnLongClickListener(OnLongClickListener listener) {
        mOnLongClickListener = listener;
    }

    private void setVideoViewVisible(boolean isVisible) {
        if (mLocalSurfaceView != null) {
            mLocalSurfaceView.setVisibility(isVisible ? VISIBLE : INVISIBLE);
        }
        if (mRemoteSurfaceView != null) {
            mRemoteSurfaceView.setVisibility(isVisible ? VISIBLE : INVISIBLE);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setOnLongClickListener(this);
        mMicrophoneStateView = (ImageView) findViewById(R.id.microphone_state_view);
        mAudioView = (TextView) findViewById(R.id.qn_audio_view);
    }

    private int getTargetColor(int pos) {
        int[] customizedColors = mContext.getResources().getIntArray(R.array.audioBackgroundColors);
        return customizedColors[pos % 6];
    }

    @Override
    public boolean onLongClick(View v) {
        if (mOnLongClickListener != null) {
            mOnLongClickListener.onLongClick(mUserId);
        }
        return false;
    }
}
