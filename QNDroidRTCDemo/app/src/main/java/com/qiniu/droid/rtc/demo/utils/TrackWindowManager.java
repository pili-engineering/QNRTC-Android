package com.qiniu.droid.rtc.demo.utils;

import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.qiniu.droid.rtc.QNRTCClient;
import com.qiniu.droid.rtc.QNTrack;
import com.qiniu.droid.rtc.demo.ui.UserTrackView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TrackWindowManager {

    private static final String TAG = "TrackWindowManager";

    // Current userId.
    private final String mCurrentUserId;
    // Screen resolution.
    private final int mScreenWidth;
    @SuppressWarnings("all")
    private int mScreenHeight;
    // Screen density.
    private final float mDensity;
    // QNRTCEngine instance.
    private final QNRTCClient mClient;
    // TrackView full screen.
    private final UserTrackView mTrackFullScreenWin;
    // TrackView item in grid.
    private final List<UserTrackView> mTrackCandidateWins;
    // Map userId to TrackView.
    private final MyHashMap<String, UserTrackView> mUserWindowMap = new MyHashMap<>();
    // Flag, Windows mode p2p, otherwise multi user.
    private Boolean mTrackWindowP2PMode = null;

    public TrackWindowManager(String currentUserId, int screenWidth, int screenHeight, float density
            , QNRTCClient client, UserTrackView trackFullScreenWin, List<UserTrackView> trackCandidateWins) {
        mCurrentUserId = currentUserId;
        mScreenWidth = screenWidth;
        mScreenHeight = screenHeight;
        mDensity = density;
        mClient = client;
        mTrackFullScreenWin = trackFullScreenWin;
        mTrackCandidateWins = new ArrayList<>(trackCandidateWins);

        mTrackFullScreenWin.setZOrderMediaOverlay(false, true);
        mTrackFullScreenWin.changeViewBackgroundByPos(0);
        mTrackFullScreenWin.setMicrophoneStateVisibility(View.GONE);
        mTrackFullScreenWin.setOnClickListener(v -> {
            if (mUserWindowMap.size() <= 1) {
                Log.d(TAG, "skip for single user.");
            } else if (mUserWindowMap.size() == 2) {
                // swap
                switchToFullScreenWindow(mUserWindowMap.getOrderedValues(mTrackFullScreenWin).get(0));
            } else {
                // exit from full screen and display all
                if (mTrackCandidateWins.isEmpty()) {
                    mTrackFullScreenWin.unSetUserTrack();
                    mTrackFullScreenWin.setVisibility(View.GONE);
                } else {
                    UserTrackView userTrackView = mTrackCandidateWins.remove(0);
                    switchToFullScreenWindow(userTrackView);
                    userTrackView.changeViewBackgroundByPos(mUserWindowMap.size());

                    setTrackUserWindowsVisibility(View.VISIBLE);
                    updateTrackWindowsLayout();
                }
            }
        });

        for (final UserTrackView view : mTrackCandidateWins) {
            view.setZOrderMediaOverlay(true, true);
            view.setOnClickListener(v -> {
                if (mUserWindowMap.size() <= 1) {
                    Log.d(TAG, "skip for single user.");
                } else if (mUserWindowMap.size() == 2) {
                    // swap
                    switchToFullScreenWindow(view);
                } else {
                    // full screen and hide others
                    switchToFullScreenWindow(view);
                    setTrackUserWindowsVisibility(View.GONE);
                }
            });
        }
    }

    public void addTrack(String userId, List<QNTrack> trackList) {
        if (mTrackCandidateWins.size() == 0) {
            Log.e(TAG, "There were more than 9 published users in the room, with no unUsedWindow to draw.");
            return;
        }
        UserTrackView userTrackView = mUserWindowMap.get(userId);
        if (userTrackView != null) {
            // user has already displayed in screen
            userTrackView.onAddTrack(trackList);
        } else {
            // allocate new track windows
            userTrackView = mTrackCandidateWins.remove(0);
            mUserWindowMap.put(userId, userTrackView, userId.equals(mCurrentUserId));
            userTrackView.setUserTrack(mClient, userId, trackList);
            userTrackView.changeViewBackgroundByPos(mUserWindowMap.size());

            userTrackView.setVisibility(View.VISIBLE);

            // update whole layout
            updateTrackWindowsLayout();
        }
    }

    public void onTrackMuted(String remoteUserId) {
        UserTrackView window = mUserWindowMap.get(remoteUserId);
        if (window != null) {
            window.onTracksMuteChanged();
        }
    }

    public void removeTrack(String userId, List<QNTrack> trackList) {
        UserTrackView remoteVideoView = mUserWindowMap.get(userId);
        if (remoteVideoView == null) {
            return;
        }
        boolean trackRemain = remoteVideoView.onRemoveTrack(trackList);
        if (userId.equals(mCurrentUserId)) {
            // always show myself in screen
            return;
        }
        if (!trackRemain) {
            // check, if no more tracks for this user. remove it
            removeTrackWindow(userId);
        }
    }

    private void removeTrackWindow(String remoteUserId) {
        UserTrackView remoteVideoView = mUserWindowMap.remove(remoteUserId);
        if (remoteVideoView == null) {
            return;
        }
        remoteVideoView.reset();
        if (mTrackFullScreenWin == remoteVideoView) {
            if (mUserWindowMap.size() == 1) {
                switchToFullScreenWindow(mUserWindowMap.getOrderedValues().get(0));
            } else {
                mTrackFullScreenWin.setVisibility(View.GONE);
                updateTrackWindowsLayout();
            }
        } else {
            remoteVideoView.setVisibility(View.GONE);
            mTrackCandidateWins.add(remoteVideoView);
            updateTrackWindowsLayout();
        }
    }

    private void updateTrackWindowsLayout() {
        if (mUserWindowMap.isEmpty()) {
            return;
        }
        updateTrackWindowMode(mUserWindowMap.size() <= 2);

        List<UserTrackView> userWindows = mUserWindowMap.getOrderedValues(mTrackFullScreenWin);
        int userCountInGridWindow = userWindows.size();
        for (int i = 0; i < userCountInGridWindow; i++) {
            UserTrackView trackView = userWindows.get(i);
            setTargetWindowParams(userCountInGridWindow, i, trackView);
        }
    }

    private void switchToFullScreenWindow(UserTrackView userTrackView) {
        if (userTrackView == mTrackFullScreenWin) {
            return;
        }

        UserTrackView.swap(mClient, mTrackFullScreenWin, userTrackView);
        if (mTrackFullScreenWin.isTaken()) {
            Log.d(TAG, "put " + mTrackFullScreenWin.getUserId() + " to " + mTrackFullScreenWin.getResourceName());
            mTrackFullScreenWin.setVisibility(View.VISIBLE);
            mUserWindowMap.put(mTrackFullScreenWin.getUserId(), mTrackFullScreenWin, mTrackFullScreenWin.getUserId().equals(mCurrentUserId));
        } else {
            Log.d(TAG, "recycle " + mTrackFullScreenWin.getResourceName());
            mTrackFullScreenWin.reset();
            mTrackFullScreenWin.setVisibility(View.GONE);
        }

        if (userTrackView.isTaken()) {
            Log.d(TAG, "put " + userTrackView.getUserId() + " to " + userTrackView.getResourceName());
            userTrackView.setVisibility(View.VISIBLE);
            mUserWindowMap.put(userTrackView.getUserId(), userTrackView, userTrackView.getUserId().equals(mCurrentUserId));
        } else {
            Log.d(TAG, "recycle " + userTrackView.getResourceName());
            userTrackView.reset();
            userTrackView.setVisibility(View.GONE);
            // recycle
            mTrackCandidateWins.add(userTrackView);
        }
    }

    private void setTrackUserWindowsVisibility(int visibility) {
        List<UserTrackView> userWindows = mUserWindowMap.getOrderedValues(mTrackFullScreenWin);
        for (int i = 0; i < userWindows.size(); i++) {
            UserTrackView trackView = userWindows.get(i);
            trackView.setVisibility(visibility);
        }
    }

    private void updateTrackWindowMode(boolean trackWindowP2PMode) {
        if (mUserWindowMap.isEmpty()) {
            return;
        }
        if (mTrackWindowP2PMode != null && mTrackWindowP2PMode == trackWindowP2PMode) {
            return;
        }
        if (trackWindowP2PMode) {
            Log.d(TAG, "switch to p2p mode");
            // relayout. switch to p2p mode. put first user to full screen.
            // ( 0 user -> 1 user || 3 users -> 2 users)
            switchToFullScreenWindow(mUserWindowMap.getOrderedValues().get(0));
        } else {
            Log.d(TAG, "switch to multi user mode");
            // relayout. switch to multi user mode.
            // (2 users -> 3 users)
            if (mTrackFullScreenWin.isTaken()) {
                if (!mTrackCandidateWins.isEmpty()) {
                    UserTrackView userTrackView = mTrackCandidateWins.remove(0);
                    switchToFullScreenWindow(userTrackView);
                    userTrackView.changeViewBackgroundByPos(mUserWindowMap.size());
                }
            }
        }
        mTrackWindowP2PMode = trackWindowP2PMode;
    }

    private void setTargetWindowParams(final int userCount, final int targetPos, final UserTrackView targetWindow) {
        switch (userCount) {
            case 1:
                if (targetPos == 0) {
                    updateLayoutParams(targetWindow, (int) (120 * mDensity + 0.5f), (int) (160 * mDensity + 0.5f), 0, 0, Gravity.TOP | Gravity.END);
                }
                break;
            case 2:
                // never in this case.
                break;
            case 3:
                if (targetPos == 0) {
                    updateLayoutParams(targetWindow, mScreenWidth / 2, mScreenWidth / 2, 0, 0, -1);
                } else if (targetPos == 1) {
                    updateLayoutParams(targetWindow, mScreenWidth / 2, mScreenWidth / 2, mScreenWidth / 2, 0, -1);
                } else {
                    updateLayoutParams(targetWindow, mScreenWidth / 2, mScreenWidth / 2, 0, mScreenWidth / 2, Gravity.CENTER_HORIZONTAL);
                }
                break;
            case 4:
                if (targetPos == 0) {
                    updateLayoutParams(targetWindow, mScreenWidth / 2, mScreenWidth / 2, 0, 0, -1);
                } else if (targetPos == 1) {
                    updateLayoutParams(targetWindow, mScreenWidth / 2, mScreenWidth / 2, mScreenWidth / 2, 0, -1);
                } else if (targetPos == 2) {
                    updateLayoutParams(targetWindow, mScreenWidth / 2, mScreenWidth / 2, 0, mScreenWidth / 2, Gravity.START);
                } else {
                    updateLayoutParams(targetWindow, mScreenWidth / 2, mScreenWidth / 2, mScreenWidth / 2, mScreenWidth / 2, -1);
                }
                break;
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
                if (targetPos == 0) {
                    updateLayoutParams(targetWindow, mScreenWidth / 3, mScreenWidth / 3, 0, 0, -1);
                } else if (targetPos == 1) {
                    updateLayoutParams(targetWindow, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth / 3, 0, -1);
                } else if (targetPos == 2) {
                    updateLayoutParams(targetWindow, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth * 2 / 3, 0, Gravity.END);
                } else if (targetPos == 3) {
                    updateLayoutParams(targetWindow, mScreenWidth / 3, mScreenWidth / 3, 0, mScreenWidth / 3, -1);
                } else if (targetPos == 4) {
                    updateLayoutParams(targetWindow, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth / 3, -1);
                } else if (targetPos == 5) {
                    updateLayoutParams(targetWindow, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth * 2 / 3, mScreenWidth / 3, -1);
                } else if (targetPos == 6) {
                    updateLayoutParams(targetWindow, mScreenWidth / 3, mScreenWidth / 3, 0, mScreenWidth * 2 / 3, -1);
                } else if (targetPos == 7) {
                    updateLayoutParams(targetWindow, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth * 2 / 3, -1);
                } else if (targetPos == 8) {
                    updateLayoutParams(targetWindow, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth * 2 / 3, mScreenWidth * 2 / 3, -1);
                }
                break;
            default:
                break;
        }
    }

    private void updateLayoutParams(UserTrackView targetView, int width, int height, int marginStart, int marginTop, int gravity) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) targetView.getLayoutParams();
        lp.width = width;
        lp.height = height;
        lp.topMargin = marginTop;
        lp.gravity = gravity;
        lp.setMarginStart(marginStart);
        targetView.setLayoutParams(lp);
    }

    public void reset() {
        Collection<String> users = new ArrayList<>(mUserWindowMap.keySet());
        for (String userId : users) {
            removeTrackWindow(userId);
        }
        mTrackWindowP2PMode = null;
    }
}
