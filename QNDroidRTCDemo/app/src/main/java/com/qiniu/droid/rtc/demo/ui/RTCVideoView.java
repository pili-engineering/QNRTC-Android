package com.qiniu.droid.rtc.demo.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.qiniu.droid.rtc.QNSurfaceView;
import com.qiniu.droid.rtc.demo.R;

public class RTCVideoView extends FrameLayout implements View.OnLongClickListener {

    private Context mContext;
    private QNSurfaceView mQNSurfaceView;
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
        LayoutInflater.from(mContext).inflate(R.layout.rtc_video_view, this, true);
    }

    public void setUserId(String userId) {
        mUserId = userId;
    }

    public void updateMicrophoneStateView(boolean isMute) {
        mMicrophoneStateView.setImageResource(isMute ? R.mipmap.microphone_disable : R.drawable.microphone_state_enable);
    }

    public QNSurfaceView getSurfaceView() {
        return mQNSurfaceView;
    }

    public ImageView getMicrophoneStateView() {
        return mMicrophoneStateView;
    }

    public void setRemoteWindowInvisible() {
        mUserId = null;
        mQNSurfaceView.setVisibility(INVISIBLE);
        mMicrophoneStateView.setVisibility(INVISIBLE);
        mAudioView.setVisibility(INVISIBLE);
        setVisibility(INVISIBLE);
    }

    public void setVideoViewVisible() {
        if (getVisibility() != VISIBLE) {
            setVisibility(VISIBLE);
        }
        mQNSurfaceView.setVisibility(VISIBLE);
        mMicrophoneStateView.setVisibility(VISIBLE);
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
        mQNSurfaceView.setVisibility(INVISIBLE);
    }

    public void setAudioViewInvisible() {
        mAudioView.setVisibility(INVISIBLE);
        mQNSurfaceView.setVisibility(VISIBLE);
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

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setOnLongClickListener(this);
        mQNSurfaceView = (QNSurfaceView) findViewById(R.id.qn_surface_view);
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
