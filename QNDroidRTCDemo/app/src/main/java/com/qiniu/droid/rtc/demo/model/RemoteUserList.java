package com.qiniu.droid.rtc.demo.model;

import com.qiniu.droid.rtc.QNTrackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteUserList {

    private Map<String, RemoteUser> mRemoteUserMap;
    private List<RemoteUser> mRemoteUsers;
    private List<RemoteTrack> mRemoteVideoTracks;

    public RemoteUserList() {
        mRemoteUserMap = new HashMap<>();
        mRemoteUsers = new ArrayList<>();
        mRemoteVideoTracks = new ArrayList<>();
    }

    public RemoteUser getRemoteUserByPosition(int pos) {
        return mRemoteUsers.get(pos);
    }

    public RemoteUser getRemoteUserByUserId(String userId) {
        return mRemoteUserMap.get(userId);
    }

    public List<RemoteTrack> getRemoteVideoTracks() {
        return mRemoteVideoTracks;
    }

    public List<RemoteTrack> getRemoteAudioTracks() {
        List<RemoteTrack> remoteAudioTracks = new ArrayList<>();
        for (RemoteUser item : mRemoteUsers) {
            if (item.getRemoteAudioTrack() != null) {
                remoteAudioTracks.add(item.getRemoteAudioTrack());
            }
        }
        return remoteAudioTracks;
    }

    public void onUserJoined(String userId, String userData) {
        RemoteUser remoteUser = new RemoteUser(userId, userData);
        mRemoteUserMap.put(userId, remoteUser);
        mRemoteUsers.add(remoteUser);
    }

    public void onUserLeft(String userId) {
        RemoteUser remoteUser = mRemoteUserMap.remove(userId);
        if (remoteUser != null) {
            mRemoteUsers.remove(remoteUser);
        }
    }

    public void onTracksPublished(String userId, List<QNTrackInfo> trackInfoList) {
        RemoteUser remoteUser = getRemoteUserByUserId(userId);
        if (remoteUser == null) {
            return;
        }
        List<RemoteTrack> remoteVideoTracks = remoteUser.addTracks(trackInfoList);
        mRemoteVideoTracks.addAll(remoteVideoTracks);
    }

    public void onTracksUnPublished(String userId, List<QNTrackInfo> trackInfoList) {
        RemoteUser remoteUser = getRemoteUserByUserId(userId);
        if (remoteUser == null) {
            return;
        }
        List<RemoteTrack> remoteVideoTracks = remoteUser.removeTracks(trackInfoList);
        mRemoteVideoTracks.removeAll(remoteVideoTracks);
    }

    public int size() {
        return mRemoteUsers.size();
    }
}
