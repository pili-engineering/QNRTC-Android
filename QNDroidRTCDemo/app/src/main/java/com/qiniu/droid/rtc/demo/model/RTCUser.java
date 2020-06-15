package com.qiniu.droid.rtc.demo.model;

import com.qiniu.droid.rtc.QNRTCUser;
import com.qiniu.droid.rtc.QNTrackInfo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class RTCUser extends QNRTCUser {

    private UserTrack mAudioTrack;
    private List<UserTrack> mVideoTracks = new LinkedList<>();

    public RTCUser(String userId, String userData) {
        super(userId, userData);
    }

    public UserTrack getAudioTrack() {
        return mAudioTrack;
    }

    public List<UserTrack> getVideoTracks() {
        return mVideoTracks;
    }

    public List<UserTrack> addTracks(List<QNTrackInfo> trackInfoList) {
        List<UserTrack> videoTracks = new ArrayList<>();
        for (QNTrackInfo item : trackInfoList) {
            UserTrack newVideoTrack = addTrack(item);
            if (newVideoTrack != null) {
                videoTracks.add(newVideoTrack);
            }
        }
        return videoTracks;
    }

    private UserTrack addTrack(QNTrackInfo trackInfo) {
        if (trackInfo.isAudio()) {
            mAudioTrack = new UserTrack(trackInfo);
            return null;
        } else {
            UserTrack newVideoTrack = new UserTrack(trackInfo);
            // replace
            mVideoTracks.remove(newVideoTrack);
            mVideoTracks.add(newVideoTrack);
            return newVideoTrack;
        }
    }

    public List<UserTrack> removeTracks(List<QNTrackInfo> trackInfoList) {
        List<UserTrack> videoTracks = new ArrayList<>();
        for (QNTrackInfo item : trackInfoList) {
            UserTrack removedVideoTrack = removeTracks(item);
            if (removedVideoTrack != null) {
                videoTracks.add(removedVideoTrack);
            }
        }
        return videoTracks;
    }

    private UserTrack removeTracks(QNTrackInfo trackInfo) {
        if (trackInfo.isAudio()) {
            mAudioTrack = null;
            return null;
        } else {
            UserTrack newUserTrack = new UserTrack(trackInfo);
            mVideoTracks.remove(newUserTrack);
            return newUserTrack;
        }
    }
}
