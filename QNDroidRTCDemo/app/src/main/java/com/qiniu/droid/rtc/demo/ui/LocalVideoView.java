package com.qiniu.droid.rtc.demo.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import com.qiniu.droid.rtc.QNLocalSurfaceView;
import com.qiniu.droid.rtc.QNLocalVideoCallback;
import com.qiniu.droid.rtc.demo.R;

import org.webrtc.VideoFrame;

public class LocalVideoView extends RTCVideoView implements QNLocalVideoCallback {

    public LocalVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(mContext).inflate(R.layout.local_video_view, this, true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLocalSurfaceView = (QNLocalSurfaceView) findViewById(R.id.local_surface_view);
        mLocalSurfaceView.setLocalVideoCallback(this);
    }

    @Override
    public int onRenderingFrame(int textureId, int width, int height, VideoFrame.TextureBuffer.Type type, long timestampNs) {
        return textureId;
    }

    @Override
    public void onSurfaceCreated() {

    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onSurfaceDestroyed() {

    }
}
