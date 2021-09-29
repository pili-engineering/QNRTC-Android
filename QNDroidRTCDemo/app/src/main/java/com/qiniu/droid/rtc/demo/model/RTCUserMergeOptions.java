package com.qiniu.droid.rtc.demo.model;

import com.qiniu.droid.rtc.QNTrack;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class RTCUserMergeOptions {

    private final String mUserID;
    private final String mUserData;

    private RTCTrackMergeOption mAudioMergeOption;
    private final List<RTCTrackMergeOption> mVideoMergeOptions = new LinkedList<>();

    public RTCUserMergeOptions(String userID, String userData) {
        mUserID = userID;
        mUserData = userData;
    }

    public String getUserID() {
        return mUserID;
    }

    public String getUserData() {
        return mUserData;
    }

    public RTCTrackMergeOption getAudioMergeOption() {
        return mAudioMergeOption;
    }

    public List<RTCTrackMergeOption> getVideoMergeOptions() {
        return mVideoMergeOptions;
    }

    public List<RTCTrackMergeOption> addTracks(List<QNTrack> trackList) {
        List<RTCTrackMergeOption> videoTracks = new ArrayList<>();
        for (QNTrack track : trackList) {
            RTCTrackMergeOption newVideoTrack = addTrack(track);
            if (newVideoTrack != null) {
                videoTracks.add(newVideoTrack);
            }
        }
        return videoTracks;
    }

    private RTCTrackMergeOption addTrack(QNTrack track) {
        if (track.isAudio()) {
            mAudioMergeOption = new RTCTrackMergeOption(track);
            return null;
        } else {
            RTCTrackMergeOption newVideoTrack = new RTCTrackMergeOption(track);
            // replace
            mVideoMergeOptions.remove(newVideoTrack);
            mVideoMergeOptions.add(newVideoTrack);
            return newVideoTrack;
        }
    }

    public List<RTCTrackMergeOption> removeTracks(List<QNTrack> trackList) {
        List<RTCTrackMergeOption> videoTracks = new ArrayList<>();
        for (QNTrack track : trackList) {
            RTCTrackMergeOption removedVideoTrack = removeTracks(track);
            if (removedVideoTrack != null) {
                videoTracks.add(removedVideoTrack);
            }
        }
        return videoTracks;
    }

    private RTCTrackMergeOption removeTracks(QNTrack track) {
        if (track.isAudio()) {
            mAudioMergeOption = null;
            return null;
        } else {
            RTCTrackMergeOption newUserTrack = new RTCTrackMergeOption(track);
            mVideoMergeOptions.remove(newUserTrack);
            return newUserTrack;
        }
    }
}
