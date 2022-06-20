package com.qiniu.droid.rtc.api.examples.model;

import com.qiniu.droid.rtc.QNAudioEffect;

public class AudioEffect {
    QNAudioEffect mAudioEffect;
    boolean mIsStarted = false;
    boolean mIsPaused = false;

    public AudioEffect(QNAudioEffect audioEffect) {
        mAudioEffect = audioEffect;
    }

    public QNAudioEffect getAudioEffect() {
        return mAudioEffect;
    }

    public String getFilePath() {
        return mAudioEffect.getFilePath();
    }

    public boolean isStarted() {
        return mIsStarted;
    }

    public void setStarted(boolean started) {
        mIsStarted = started;
    }

    public boolean isPaused() {
        return mIsPaused;
    }

    public void setPaused(boolean paused) {
        mIsPaused = paused;
    }
}
