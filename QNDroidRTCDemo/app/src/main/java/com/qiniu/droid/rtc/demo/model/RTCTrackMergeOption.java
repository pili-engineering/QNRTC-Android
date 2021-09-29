package com.qiniu.droid.rtc.demo.model;

import com.qiniu.droid.rtc.QNRenderMode;
import com.qiniu.droid.rtc.QNTrack;
import com.qiniu.droid.rtc.QNTrackKind;
import com.qiniu.droid.rtc.QNTranscodingLiveStreamingTrack;
import com.qiniu.droid.rtc.demo.utils.QNAppServer;

public class RTCTrackMergeOption {

    private final String mTrackId;
    private final QNTrack mTrack;

    private boolean mTrackInclude = true;
    private final QNTranscodingLiveStreamingTrack mMergeTrack;

    public RTCTrackMergeOption(QNTrack track) {
        mTrack = track;
        mTrackId = mTrack.getTrackID();

        mMergeTrack = new QNTranscodingLiveStreamingTrack();
        if (track.isVideo()) {
            mMergeTrack.setWidth(QNAppServer.STREAMING_WIDTH);
            mMergeTrack.setHeight(QNAppServer.STREAMING_HEIGHT);
            mMergeTrack.setRenderMode(QNRenderMode.ASPECT_FILL);
        }
        mMergeTrack.setTrackID(mTrackId);
    }

    public String getTrackId() {
        return mTrackId;
    }

    public QNTrack getTrack() {
        return mTrack;
    }

    public QNTranscodingLiveStreamingTrack getMergeTrack() {
        return mMergeTrack;
    }

    public boolean isTrackInclude() {
        return mTrackInclude;
    }

    public void setTrackInclude(boolean trackInclude) {
        mTrackInclude = trackInclude;
    }

    public void updateMergeTrack(QNTranscodingLiveStreamingTrack track) {
        if (track == null) {
            return;
        }
        mMergeTrack.setX(track.getX());
        mMergeTrack.setY(track.getY());
        mMergeTrack.setZOrder(track.getZOrder());
        mMergeTrack.setWidth(track.getWidth());
        mMergeTrack.setHeight(track.getHeight());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RTCTrackMergeOption) {
            return mTrack.equals(((RTCTrackMergeOption) obj).mTrack);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return mTrack.hashCode();
    }
}
