package com.qiniu.droid.rtc.demo.model;

import com.qiniu.droid.rtc.QNTrackInfo;
import com.qiniu.droid.rtc.model.QNMergeTrackOption;

public class RemoteTrack {

    private final String mTrackId;
    private final QNTrackInfo mQNTrackInfo;

    private boolean mTrackInclude = true;
    private final QNMergeTrackOption mQNMergeTrackOption;

    public RemoteTrack(QNTrackInfo QNTrackInfo) {
        mQNTrackInfo = QNTrackInfo;
        mTrackId = mQNTrackInfo.getTrackId();

        mQNMergeTrackOption = new QNMergeTrackOption();
        mQNMergeTrackOption.setTrackId(mTrackId);
    }

    public String getTrackId() {
        return mTrackId;
    }

    public QNTrackInfo getQNTrackInfo() {
        return mQNTrackInfo;
    }

    public QNMergeTrackOption getQNMergeTrackOption() {
        return mQNMergeTrackOption;
    }

    public boolean isTrackInclude() {
        return mTrackInclude;
    }

    public void setTrackInclude(boolean trackInclude) {
        mTrackInclude = trackInclude;
    }

    public void updateQNMergeTrackOption(QNMergeTrackOption option) {
        if (option == null) {
            return;
        }
        mQNMergeTrackOption.setX(option.getX());
        mQNMergeTrackOption.setY(option.getY());
        mQNMergeTrackOption.setZ(option.getZ());
        mQNMergeTrackOption.setWidth(option.getWidth());
        mQNMergeTrackOption.setHeight(option.getHeight());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RemoteTrack) {
            return mQNTrackInfo.equals(((RemoteTrack) obj).mQNTrackInfo);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return mQNTrackInfo.hashCode();
    }
}
