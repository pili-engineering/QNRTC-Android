package com.qiniu.droid.rtc.demo.model;

import com.qiniu.droid.rtc.QNTrackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RTCRoomUsersMergeOption {

    private Map<String, RTCUserMergeOptions> mRTCUserMap;
    private List<RTCUserMergeOptions> mRTCUsers;
    private List<RTCTrackMergeOption> mRTCVideoMergeOptions;

    public RTCRoomUsersMergeOption() {
        mRTCUserMap = new HashMap<>();
        mRTCUsers = new ArrayList<>();
        mRTCVideoMergeOptions = new ArrayList<>();
    }

    public RTCUserMergeOptions getRoomUserByPosition(int pos) {
        return mRTCUsers.get(pos);
    }

    public RTCUserMergeOptions getRoomUserByUserId(String userId) {
        return mRTCUserMap.get(userId);
    }

    public List<RTCTrackMergeOption> getRTCVideoMergeOptions() {
        return mRTCVideoMergeOptions;
    }

    public List<RTCTrackMergeOption> getRTCAudioTracks() {
        List<RTCTrackMergeOption> rtcAudioTracks = new ArrayList<>();
        for (RTCUserMergeOptions item : mRTCUsers) {
            if (item.getAudioTrack() != null) {
                rtcAudioTracks.add(item.getAudioTrack());
            }
        }
        return rtcAudioTracks;
    }

    public void onUserJoined(String userId, String userData) {
        if (mRTCUserMap.get(userId) == null) {
            RTCUserMergeOptions userMergeOptions = new RTCUserMergeOptions(userId, userData);
            mRTCUserMap.put(userId, userMergeOptions);
            mRTCUsers.add(userMergeOptions);
        }
    }

    public void onUserLeft(String userId) {
        RTCUserMergeOptions userMergeOptions = mRTCUserMap.remove(userId);
        if (userMergeOptions != null) {
            mRTCUsers.remove(userMergeOptions);
        }
    }

    public void onTracksPublished(String userId, List<QNTrackInfo> trackInfoList) {
        RTCUserMergeOptions userMergeOptions = getRoomUserByUserId(userId);
        if (userMergeOptions == null) {
            return;
        }
        List<RTCTrackMergeOption> userVideoTracks = userMergeOptions.addTracks(trackInfoList);
        mRTCVideoMergeOptions.addAll(userVideoTracks);
    }

    public void onTracksUnPublished(String userId, List<QNTrackInfo> trackInfoList) {
        RTCUserMergeOptions userMergeOptions = getRoomUserByUserId(userId);
        if (userMergeOptions == null) {
            return;
        }
        List<RTCTrackMergeOption> userVideoTracks = userMergeOptions.removeTracks(trackInfoList);
        mRTCVideoMergeOptions.removeAll(userVideoTracks);
    }

    public int size() {
        return mRTCUsers.size();
    }
}
