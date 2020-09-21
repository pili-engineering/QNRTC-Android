package com.qiniu.droid.rtc.demo.model;

import com.qiniu.droid.rtc.QNRTCUser;
import com.qiniu.droid.rtc.QNTrackInfo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class RTCUserMergeOptions extends QNRTCUser {

    private RTCTrackMergeOption mAudioTrack;
    private List<RTCTrackMergeOption> mVideoTracks = new LinkedList<>();

    public RTCUserMergeOptions(String userId, String userData) {
        super(userId, userData);
    }

    public RTCTrackMergeOption getAudioTrack() {
        return mAudioTrack;
    }

    public List<RTCTrackMergeOption> getVideoTracks() {
        return mVideoTracks;
    }

    public List<RTCTrackMergeOption> addTracks(List<QNTrackInfo> trackInfoList) {
        List<RTCTrackMergeOption> videoTracks = new ArrayList<>();
        for (QNTrackInfo item : trackInfoList) {
            RTCTrackMergeOption newVideoTrack = addTrack(item);
            if (newVideoTrack != null) {
                videoTracks.add(newVideoTrack);
            }
        }
        return videoTracks;
    }

    private RTCTrackMergeOption addTrack(QNTrackInfo trackInfo) {
        if (trackInfo.isAudio()) {
            mAudioTrack = new RTCTrackMergeOption(trackInfo);
            return null;
        } else {
            RTCTrackMergeOption newVideoTrack = new RTCTrackMergeOption(trackInfo);
            // replace
            mVideoTracks.remove(newVideoTrack);
            mVideoTracks.add(newVideoTrack);
            return newVideoTrack;
        }
    }

    public List<RTCTrackMergeOption> removeTracks(List<QNTrackInfo> trackInfoList) {
        List<RTCTrackMergeOption> videoTracks = new ArrayList<>();
        for (QNTrackInfo item : trackInfoList) {
            RTCTrackMergeOption removedVideoTrack = removeTracks(item);
            if (removedVideoTrack != null) {
                videoTracks.add(removedVideoTrack);
            }
        }
        return videoTracks;
    }

    private RTCTrackMergeOption removeTracks(QNTrackInfo trackInfo) {
        if (trackInfo.isAudio()) {
            mAudioTrack = null;
            return null;
        } else {
            RTCTrackMergeOption newUserTrack = new RTCTrackMergeOption(trackInfo);
            mVideoTracks.remove(newUserTrack);
            return newUserTrack;
        }
    }
}
