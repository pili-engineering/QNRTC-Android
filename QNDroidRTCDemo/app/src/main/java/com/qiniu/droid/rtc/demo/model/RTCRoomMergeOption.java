package com.qiniu.droid.rtc.demo.model;

import com.qiniu.droid.rtc.QNTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RTCRoomMergeOption {

    private final Map<String, RTCUserMergeOptions> mUserMap;
    private final List<RTCUserMergeOptions> mUserMergeOptions;
    private final List<RTCTrackMergeOption> mVideoMergeOptions;

    public RTCRoomMergeOption() {
        mUserMap = new HashMap<>();
        mUserMergeOptions = new ArrayList<>();
        mVideoMergeOptions = new ArrayList<>();
    }

    public RTCUserMergeOptions getUserMergeOptionByPosition(int pos) {
        return mUserMergeOptions.get(pos);
    }

    public RTCUserMergeOptions getUserMergeOptionByUserId(String userId) {
        return mUserMap.get(userId);
    }

    public List<RTCTrackMergeOption> getVideoMergeOptions() {
        return mVideoMergeOptions;
    }

    public List<RTCTrackMergeOption> getAudioTrackOptions() {
        List<RTCTrackMergeOption> rtcAudioTracks = new ArrayList<>();
        for (RTCUserMergeOptions item : mUserMergeOptions) {
            if (item.getAudioMergeOption() != null) {
                rtcAudioTracks.add(item.getAudioMergeOption());
            }
        }
        return rtcAudioTracks;
    }

    public void onUserJoined(String userId, String userData) {
        if (mUserMap.get(userId) == null) {
            RTCUserMergeOptions userMergeOptions = new RTCUserMergeOptions(userId, userData);
            mUserMap.put(userId, userMergeOptions);
            mUserMergeOptions.add(userMergeOptions);
        }
    }

    public void onUserLeft(String userId) {
        RTCUserMergeOptions userMergeOptions = mUserMap.remove(userId);
        if (userMergeOptions != null) {
            mUserMergeOptions.remove(userMergeOptions);
        }
    }

    public void onUserLeft() {
        mVideoMergeOptions.clear();
        mUserMergeOptions.clear();
        mUserMap.clear();
    }

    public void onTracksPublished(String userId, List<QNTrack> trackList) {
        RTCUserMergeOptions userMergeOptions = getUserMergeOptionByUserId(userId);
        if (userMergeOptions == null) {
            return;
        }
        List<RTCTrackMergeOption> userVideoTracks = userMergeOptions.addTracks(trackList);
        mVideoMergeOptions.addAll(userVideoTracks);
    }

    public void onTracksUnPublished(String userId, List<QNTrack> trackList) {
        RTCUserMergeOptions userMergeOptions = getUserMergeOptionByUserId(userId);
        if (userMergeOptions == null) {
            return;
        }
        List<RTCTrackMergeOption> userVideoTracks = userMergeOptions.removeTracks(trackList);
        mVideoMergeOptions.removeAll(userVideoTracks);
    }

    public int size() {
        return mUserMergeOptions.size();
    }
}
