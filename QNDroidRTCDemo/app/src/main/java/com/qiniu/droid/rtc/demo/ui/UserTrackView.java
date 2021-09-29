package com.qiniu.droid.rtc.demo.ui;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.qiniu.droid.rtc.QNCameraVideoTrack;
import com.qiniu.droid.rtc.QNLocalVideoTrack;
import com.qiniu.droid.rtc.QNRTCClient;
import com.qiniu.droid.rtc.QNRemoteVideoTrack;
import com.qiniu.droid.rtc.QNSurfaceView;
import com.qiniu.droid.rtc.QNTrack;
import com.qiniu.droid.rtc.demo.R;

import org.webrtc.RendererCommon;

import java.util.ArrayList;
import java.util.List;

import static com.qiniu.droid.rtc.demo.activity.RoomActivity.TRACK_TAG_CAMERA;
import static com.qiniu.droid.rtc.demo.activity.RoomActivity.TRACK_TAG_SCREEN;

public class UserTrackView extends FrameLayout {

    private static final String TAG = "UserTrackView";
    private static final boolean PRINT_DEBUG_LOG = false;

    private boolean inited = false;
    private ViewGroup mVideoViewLargeParent;
    private QNSurfaceView mSurfaceViewLarge;

    private ViewGroup mVideoViewSmallParent;
    private QNSurfaceView mSurfaceViewSmall;

    private ImageView mMicrophoneStateView;
    private TextView mAudioView;

    private QNRTCClient mClient;
    private String mUserId;
    private QNTrack mQNAudioTrack;
    private final List<QNTrack> mQNVideoTracks = new ArrayList<>();

    private QNTrack mDisplayInLargeViewTrack = null;
    private QNTrack mDisplayInSmallViewTrack = null;
    private int mMicrophoneViewVisibility = -1;
    private int mPos = -1;

    public UserTrackView(@NonNull Context context) {
        super(context);
        init();
    }

    public UserTrackView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        if (inited) {
            return;
        }
        inited = true;
        LayoutInflater.from(getContext()).inflate(getLayout(), this, true);
    }

    protected int getLayout() {
        return R.layout.user_tracks_view;
    }

    public String getUserId() {
        return mUserId;
    }

    public boolean isTaken() {
        return !TextUtils.isEmpty(getUserId());
    }

    public List<QNTrack> getTrackList() {
        List<QNTrack> trackList = new ArrayList<>();
        if (mQNAudioTrack != null) {
            trackList.add(mQNAudioTrack);
        }
        trackList.addAll(mQNVideoTracks);
        return trackList;
    }

    public void setUserTrack(QNRTCClient client, String userId, List<QNTrack> trackList) {
        setUserTrack(client, userId, trackList, View.VISIBLE);
    }

    public void setUserTrack(QNRTCClient client, String userId, List<QNTrack> trackList, int microphoneViewVisibility) {
        LogD(TAG, "setUserTrack() userId: " + userId);
        mClient = client;
        mUserId = userId;
        mQNAudioTrack = null;
        mQNVideoTracks.clear();

        if (TextUtils.isEmpty(mUserId)) {
            return;
        }
        setMicrophoneStateVisibility(microphoneViewVisibility);
        mAudioView.setText(mUserId);
        onAddTrack(trackList);
    }

    public void unSetUserTrack() {
        if (mClient != null) {
            if (mDisplayInLargeViewTrack != null) {
                if (mDisplayInLargeViewTrack instanceof QNRemoteVideoTrack) {
                    ((QNRemoteVideoTrack) mDisplayInLargeViewTrack).play(null);
                } else if (mDisplayInLargeViewTrack instanceof QNLocalVideoTrack) {
                    ((QNLocalVideoTrack) mDisplayInLargeViewTrack).play(null);
                }
            }
            if (mDisplayInSmallViewTrack != null) {
                if (mDisplayInSmallViewTrack instanceof QNRemoteVideoTrack) {
                    ((QNRemoteVideoTrack) mDisplayInSmallViewTrack).play(null);
                } else if (mDisplayInSmallViewTrack instanceof QNLocalVideoTrack) {
                    ((QNLocalVideoTrack) mDisplayInSmallViewTrack).play(null);
                }
            }
        }
        reset();
    }

    public void reset() {
        LogD(TAG, "reset()");
        mClient = null;
        mUserId = null;
        mQNAudioTrack = null;
        mQNVideoTracks.clear();
        mPos = -1;

        mSurfaceViewLarge.setVisibility(View.GONE);
        mVideoViewLargeParent.setVisibility(View.GONE);
        mSurfaceViewSmall.setVisibility(View.GONE);
        mVideoViewSmallParent.setVisibility(View.GONE);
        mAudioView.setText("");
        mAudioView.setVisibility(View.GONE);
        mDisplayInLargeViewTrack = null;
        mDisplayInSmallViewTrack = null;
    }

    public void dispose() {
        LogD(TAG, "dispose()");
        mSurfaceViewLarge.release();
        mSurfaceViewSmall.release();
    }

    public void onAddTrack(List<QNTrack> trackList) {
        LogD(TAG, "onAddTrack()");
        for (QNTrack track : trackList) {
            onAddTrack(track, false);
        }
        onTrackChanged();
    }

    private void onAddTrack(QNTrack track, boolean notify) {
        if (track.isAudio()) {
            mQNAudioTrack = track;
        } else {
            mQNVideoTracks.add(track);
        }
        if (notify) {
            onTrackChanged();
        }
    }

    public boolean onRemoveTrack(List<QNTrack> trackList) {
        LogD(TAG, "onRemoveTrack()");
        for (QNTrack track : trackList) {
            onRemoveTrack(track, false);
        }
        onTrackChanged();
        return mQNAudioTrack != null || !mQNVideoTracks.isEmpty();
    }

    public void onRemoveTrack(QNTrack track, boolean notify) {
        if (track.isAudio()) {
            mQNAudioTrack = null;
        } else {
            mQNVideoTracks.remove(track);
        }
        if (notify) {
            onTrackChanged();
        }
    }

    public void onTracksMuteChanged() {
        LogD(TAG, "onTracksMuteChanged()");
        // audio track
        if (mQNAudioTrack != null) {
            setMicrophoneStateVisibilityInner(View.VISIBLE);
            updateMicrophoneStateView(mQNAudioTrack.isMuted());
        } else {
            setMicrophoneStateVisibilityInner(View.INVISIBLE);
        }

        // video tracks
        boolean hideAudioView = containsUnMutedVideoTracks(2);
        setAudioViewStateVisibility(hideAudioView ? View.GONE : View.VISIBLE);
        // note : mSurfaceViewSmall is on top, so set visibility
        if (mDisplayInSmallViewTrack != null) {
            mSurfaceViewSmall.setVisibility(hideAudioView ? View.VISIBLE : View.GONE);
        }
    }

    private void onTrackChanged() {
        onTracksMuteChanged();

        QNTrack cameraTrack = findVideoTrack(TRACK_TAG_CAMERA);
        QNTrack screenTrack = findVideoTrack(TRACK_TAG_SCREEN);

        // in case, camera has no tag.
        if (cameraTrack == null && !mQNVideoTracks.isEmpty()) {
            List<QNTrack> tracksExcludeScreenTrack = new ArrayList<>(mQNVideoTracks);
            tracksExcludeScreenTrack.remove(screenTrack);
            if (!tracksExcludeScreenTrack.isEmpty()) {
                cameraTrack = tracksExcludeScreenTrack.get(0);
            }
        }

        QNTrack displayInLargeViewTrack = null;
        QNTrack displayInSmallViewTrack = null;
        if (cameraTrack != null && screenTrack != null) {
            LogD(TAG, "contains camera and screen track info");
            displayInLargeViewTrack = screenTrack;
            displayInSmallViewTrack = cameraTrack;
        } else {
            if (cameraTrack != null) {
                LogD(TAG, "just contains camera track info");
                displayInLargeViewTrack = cameraTrack;
            }
            if (screenTrack != null) {
                LogD(TAG, "just contains screen track info");
                displayInLargeViewTrack = screenTrack;
            }
        }
        updateLargeView(displayInLargeViewTrack);
        updateSmallView(displayInSmallViewTrack);
    }

    private QNTrack findVideoTrack(String tag) {
        QNTrack found = null;
        for (QNTrack track : mQNVideoTracks) {
            if (tag.equals(track.getTag())) {
                found = track;
                break;
            }
        }
        return found;
    }

    private void updateLargeView(QNTrack displayInLargeViewTrack) {
        if (mDisplayInLargeViewTrack != null && mDisplayInLargeViewTrack == displayInLargeViewTrack) {
            LogD(TAG, "skip updateLargeView, same track");
            return;
        }
        mDisplayInLargeViewTrack = displayInLargeViewTrack;
        if (mDisplayInLargeViewTrack != null) {
            mSurfaceViewLarge.setVisibility(View.VISIBLE);
            if (mDisplayInLargeViewTrack instanceof QNCameraVideoTrack) {
                ((QNCameraVideoTrack) mDisplayInLargeViewTrack).play(mSurfaceViewLarge);
            } else if (mDisplayInLargeViewTrack instanceof QNRemoteVideoTrack) {
                ((QNRemoteVideoTrack) mDisplayInLargeViewTrack).play(mSurfaceViewLarge);
            }
            mSurfaceViewLarge.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            mVideoViewLargeParent.setVisibility(View.VISIBLE);
        } else {
            mSurfaceViewLarge.setVisibility(View.GONE);
            mVideoViewLargeParent.setVisibility(View.GONE);
        }
    }

    private void updateSmallView(QNTrack displayInSmallViewTrack) {
        if (mDisplayInSmallViewTrack != null && mDisplayInSmallViewTrack == displayInSmallViewTrack) {
            LogD(TAG, "skip updateSmallView, same track");
            return;
        }
        mDisplayInSmallViewTrack = displayInSmallViewTrack;
        if (mDisplayInSmallViewTrack != null) {
            mSurfaceViewSmall.setVisibility(View.VISIBLE);
            if (mDisplayInSmallViewTrack instanceof QNCameraVideoTrack) {
                ((QNCameraVideoTrack) mDisplayInSmallViewTrack).play(mSurfaceViewSmall);
            } else if (mDisplayInLargeViewTrack instanceof QNRemoteVideoTrack) {
                ((QNRemoteVideoTrack) mDisplayInSmallViewTrack).play(mSurfaceViewSmall);
            }
            mSurfaceViewSmall.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            mVideoViewSmallParent.setVisibility(View.VISIBLE);
        } else {
            mSurfaceViewSmall.setVisibility(View.GONE);
            mVideoViewSmallParent.setVisibility(View.GONE);
        }
    }

    private boolean containsUnMutedVideoTracks(int count) {
        boolean unMuted = false;
        for (int i = 0; i < mQNVideoTracks.size() && i < count; i++) {
            QNTrack track = mQNVideoTracks.get(i);
            if (!track.isMuted()) {
                unMuted = true;
                break;
            }
        }
        return unMuted;
    }

    public void changeViewBackgroundByPos(int pos) {
        LogD(TAG, "changeViewBackgroundByPos() " + pos);
        mPos = pos;
        if (mPos != -1) {
            mAudioView.setBackgroundColor(getTargetColor(pos));
        }
    }

    private void setAudioViewStateVisibility(int visibility) {
        mAudioView.setVisibility(visibility);
    }

    public void setMicrophoneStateVisibility(int visibility) {
        mMicrophoneViewVisibility = visibility;
        mMicrophoneStateView.setVisibility(visibility);
    }

    private void setMicrophoneStateVisibilityInner(int visibility) {
        if (mMicrophoneViewVisibility == -1 || mMicrophoneViewVisibility == View.VISIBLE) {
            mMicrophoneStateView.setVisibility(visibility);
        }
    }

    private void updateMicrophoneStateView(boolean isMute) {
        mMicrophoneStateView.setImageResource(isMute ? R.drawable.microphone_disable : R.drawable.microphone_state_enable);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mVideoViewLargeParent = findViewById(R.id.qn_surface_view_large_parent);
        mSurfaceViewLarge = findViewById(R.id.qn_surface_view_large);

        mVideoViewSmallParent = findViewById(R.id.qn_surface_view_small_parent);
        mSurfaceViewSmall = findViewById(R.id.qn_surface_view_small);

        mMicrophoneStateView = findViewById(R.id.microphone_state_view);
        mAudioView = findViewById(R.id.qn_audio_view);
    }

    @Override
    public void setVisibility(int visibility) {
        if (visibility == View.GONE) {
            mSurfaceViewLarge.setVisibility(visibility);
            mSurfaceViewSmall.setVisibility(visibility);
        } else {
            if (mDisplayInLargeViewTrack != null) {
                mSurfaceViewLarge.setVisibility(visibility);
            }
            if (mDisplayInSmallViewTrack != null) {
                mSurfaceViewSmall.setVisibility(visibility);
            }
        }
        super.setVisibility(visibility);
    }

    private int getTargetColor(int pos) {
        int[] customizedColors = getContext().getResources().getIntArray(R.array.audioBackgroundColors);
        return customizedColors[pos % 6];
    }

    public void setZOrderMediaOverlay(boolean isMediaOverlay, boolean onTop) {
        mSurfaceViewLarge.setZOrderMediaOverlay(isMediaOverlay);
        mSurfaceViewSmall.setZOrderMediaOverlay(isMediaOverlay);
        mSurfaceViewSmall.setZOrderOnTop(onTop);
    }

    public static void swap(QNRTCClient client, UserTrackView firstTrackView, UserTrackView secondTrackView) {
        String userIdFirst = firstTrackView.getUserId();
        List<QNTrack> firstTrack = firstTrackView.getTrackList();
        int postFirst = firstTrackView.mPos;

        String userIdSecond = secondTrackView.getUserId();
        List<QNTrack> secondTrack = secondTrackView.getTrackList();
        int postSecond = secondTrackView.mPos;

        firstTrackView.setUserTrack(client, userIdSecond, secondTrack, firstTrackView.mMicrophoneViewVisibility);
        firstTrackView.changeViewBackgroundByPos(postSecond);

        secondTrackView.setUserTrack(client, userIdFirst, firstTrack, secondTrackView.mMicrophoneViewVisibility);
        secondTrackView.changeViewBackgroundByPos(postFirst);
    }

    public String getResourceName() {
        try {
            return this.getResources().getResourceEntryName(this.getId());
        } catch (Resources.NotFoundException var2) {
            return "";
        }
    }

    private void LogD(String tag, String message) {
        if (PRINT_DEBUG_LOG) {
            Log.d(tag + " " + getResourceName(), message);
        }
    }
}
