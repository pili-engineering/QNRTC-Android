package com.qiniu.droid.rtc.demo.model;

import com.qiniu.droid.rtc.QNTrackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomUserList {

    private Map<String, RTCUser> mRTCUserMap;
    private List<RTCUser> mRTCUsers;
    private List<UserTrack> mRTCVideoTracks;

    public RoomUserList() {
        mRTCUserMap = new HashMap<>();
        mRTCUsers = new ArrayList<>();
        mRTCVideoTracks = new ArrayList<>();
    }

    public RTCUser getRoomUserByPosition(int pos) {
        return mRTCUsers.get(pos);
    }

    public RTCUser getRTCUserByUserId(String userId) {
        return mRTCUserMap.get(userId);
    }

    public List<UserTrack> getRTCVideoTracks() {
        return mRTCVideoTracks;
    }

    public List<UserTrack> getRTCAudioTracks() {
        List<UserTrack> rtcAudioTracks = new ArrayList<>();
        for (RTCUser item : mRTCUsers) {
            if (item.getAudioTrack() != null) {
                rtcAudioTracks.add(item.getAudioTrack());
            }
        }
        return rtcAudioTracks;
    }

    public void onUserJoined(String userId, String userData) {
        RTCUser rtcUser = new RTCUser(userId, userData);
        mRTCUserMap.put(userId, rtcUser);
        mRTCUsers.add(rtcUser);
    }

    public void onUserLeft(String userId) {
        RTCUser rtcUser = mRTCUserMap.remove(userId);
        if (rtcUser != null) {
            mRTCUsers.remove(rtcUser);
        }
    }

    public void onTracksPublished(String userId, List<QNTrackInfo> trackInfoList) {
        RTCUser rtcUser = getRTCUserByUserId(userId);
        if (rtcUser == null) {
            return;
        }
        List<UserTrack> userVideoTracks = rtcUser.addTracks(trackInfoList);
        mRTCVideoTracks.addAll(userVideoTracks);
    }

    public void onTracksUnPublished(String userId, List<QNTrackInfo> trackInfoList) {
        RTCUser rtcUser = getRTCUserByUserId(userId);
        if (rtcUser == null) {
            return;
        }
        List<UserTrack> userVideoTracks = rtcUser.removeTracks(trackInfoList);
        mRTCVideoTracks.removeAll(userVideoTracks);
    }

    public int size() {
        return mRTCUsers.size();
    }
}
