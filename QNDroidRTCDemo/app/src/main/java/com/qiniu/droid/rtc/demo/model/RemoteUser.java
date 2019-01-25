package com.qiniu.droid.rtc.demo.model;

import com.qiniu.droid.rtc.QNRTCUser;
import com.qiniu.droid.rtc.QNTrackInfo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class RemoteUser extends QNRTCUser {

    private RemoteTrack mRemoteAudioTrack;
    private List<RemoteTrack> mRemoteVideoTracks = new LinkedList<>();

    public RemoteUser(String userId, String userData) {
        super(userId, userData);
    }

    public RemoteTrack getRemoteAudioTrack() {
        return mRemoteAudioTrack;
    }

    public List<RemoteTrack> getRemoteVideoTracks() {
        return mRemoteVideoTracks;
    }

    public List<RemoteTrack> addTracks(List<QNTrackInfo> trackInfoList) {
        List<RemoteTrack> videoTracks = new ArrayList<>();
        for (QNTrackInfo item : trackInfoList) {
            RemoteTrack newRemoteVideoTrack = addTrack(item);
            if (newRemoteVideoTrack != null) {
                videoTracks.add(newRemoteVideoTrack);
            }
        }
        return videoTracks;
    }

    private RemoteTrack addTrack(QNTrackInfo trackInfo) {
        if (trackInfo.isAudio()) {
            mRemoteAudioTrack = new RemoteTrack(trackInfo);
            return null;
        } else {
            RemoteTrack newRemoteVideoTrack = new RemoteTrack(trackInfo);
            // replace
            mRemoteVideoTracks.remove(newRemoteVideoTrack);
            mRemoteVideoTracks.add(newRemoteVideoTrack);
            return newRemoteVideoTrack;
        }
    }

    public List<RemoteTrack> removeTracks(List<QNTrackInfo> trackInfoList) {
        List<RemoteTrack> videoTracks = new ArrayList<>();
        for (QNTrackInfo item : trackInfoList) {
            RemoteTrack removedVideoTrack = removeTracks(item);
            if (removedVideoTrack != null) {
                videoTracks.add(removedVideoTrack);
            }
        }
        return videoTracks;
    }

    private RemoteTrack removeTracks(QNTrackInfo trackInfo) {
        if (trackInfo.isAudio()) {
            mRemoteAudioTrack = null;
            return null;
        } else {
            RemoteTrack newRemoteTrack = new RemoteTrack(trackInfo);
            mRemoteVideoTracks.remove(newRemoteTrack);
            return newRemoteTrack;
        }
    }
}
