package com.qiniu.droid.rtc.demo.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.qiniu.droid.rtc.QNBeautySetting;
import com.qiniu.droid.rtc.QNCameraSwitchResultCallback;
import com.qiniu.droid.rtc.QNRTCManager;
import com.qiniu.droid.rtc.QNRTCSetting;
import com.qiniu.droid.rtc.QNRemoteAudioCallback;
import com.qiniu.droid.rtc.QNRemoteSurfaceView;
import com.qiniu.droid.rtc.QNRoomEventListener;
import com.qiniu.droid.rtc.QNRoomState;
import com.qiniu.droid.rtc.QNStatisticsReport;
import com.qiniu.droid.rtc.QNVideoFormat;
import com.qiniu.droid.rtc.demo.R;
import com.qiniu.droid.rtc.demo.fragment.ControlFragment;
import com.qiniu.droid.rtc.demo.ui.LocalVideoView;
import com.qiniu.droid.rtc.demo.ui.RTCVideoView;
import com.qiniu.droid.rtc.demo.utils.Config;
import com.qiniu.droid.rtc.demo.utils.QNAppServer;
import com.qiniu.droid.rtc.demo.utils.ToastUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.qiniu.droid.rtc.QNErrorCode.ERROR_KICKED_OUT_OF_ROOM;

public class RoomActivity extends Activity implements QNRoomEventListener, ControlFragment.OnCallEvents {

    private static final String TAG = "RoomActivity";

    public static final String EXTRA_ROOM_ID = "ROOM_ID";
    public static final String EXTRA_ROOM_TOKEN = "ROOM_TOKEN";
    public static final String EXTRA_USER_ID = "USER_ID";
    public static final String EXTRA_VIDEO_WIDTH = "VIDEO_WIDTH";
    public static final String EXTRA_VIDEO_HEIGHT = "VIDEO_HEIGHT";
    public static final String EXTRA_HW_CODEC = "HW_CODEC";

    private static final String[] MANDATORY_PERMISSIONS = {
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET"
    };

    private List<String> mHWBlackList = new ArrayList<>();
    private List<RTCVideoView> mUsedWindowList;
    private List<RTCVideoView> mUnusedWindowList;
    private ConcurrentHashMap<String, RTCVideoView> mUserWindowMap;
    private String[] mMergeStreamPosition;

    private RTCVideoView mRemoteWindowA;
    private RTCVideoView mRemoteWindowB;
    private RTCVideoView mRemoteWindowC;
    private RTCVideoView mRemoteWindowD;
    private RTCVideoView mRemoteWindowE;
    private RTCVideoView mRemoteWindowF;
    private RTCVideoView mRemoteWindowG;
    private RTCVideoView mRemoteWindowH;
    private RTCVideoView mLocalWindow;

    private Toast mLogToast;
    private QNRTCManager mRTCManager;

    private boolean mIsError;
    private boolean mCallControlFragmentVisible = true;
    private long mCallStartedTimeMs = 0;
    private boolean mMicEnabled = true;
    private boolean mBeautyEnabled = false;
    private boolean mVideoEnabled = true;
    private boolean mSpeakerEnabled = true;
    private boolean mIsJoinedRoom = false;
    private String mRoomId;
    private String mRoomToken;
    private String mUserId;
    private String mLocalLogText;
    private ControlFragment mControlFragment;

    private int mScreenWidth = 0;
    private int mScreenHeight = 0;
    private int mVideoWidth = 0;
    private int mVideoHeight = 0;
    private float mDensity = 0;
    private boolean mIsAdmin = false;

    private AlertDialog mKickoutDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(LayoutParams.FLAG_FULLSCREEN | LayoutParams.FLAG_KEEP_SCREEN_ON
                | LayoutParams.FLAG_DISMISS_KEYGUARD | LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());

        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(outMetrics);

        mScreenWidth = outMetrics.widthPixels;
        mScreenHeight = outMetrics.heightPixels;
        mDensity = outMetrics.density;

        setContentView(R.layout.activity_room);

        Intent intent = getIntent();
        mRoomId = intent.getStringExtra(EXTRA_ROOM_ID);
        mRoomToken = intent.getStringExtra(EXTRA_ROOM_TOKEN);
        mUserId = intent.getStringExtra(EXTRA_USER_ID);

        mLocalWindow = (LocalVideoView) findViewById(R.id.local_video_view);
        mLocalWindow.setUserId(mUserId);

        mRemoteWindowA = (RTCVideoView) findViewById(R.id.remote_video_view_a);
        mRemoteWindowB = (RTCVideoView) findViewById(R.id.remote_video_view_b);
        mRemoteWindowC = (RTCVideoView) findViewById(R.id.remote_video_view_c);
        mRemoteWindowD = (RTCVideoView) findViewById(R.id.remote_video_view_d);
        mRemoteWindowE = (RTCVideoView) findViewById(R.id.remote_video_view_e);
        mRemoteWindowF = (RTCVideoView) findViewById(R.id.remote_video_view_f);
        mRemoteWindowG = (RTCVideoView) findViewById(R.id.remote_video_view_g);
        mRemoteWindowH = (RTCVideoView) findViewById(R.id.remote_video_view_h);

        mUsedWindowList = Collections.synchronizedList(new LinkedList<RTCVideoView>());
        mUsedWindowList.add(mLocalWindow);
        mUnusedWindowList = Collections.synchronizedList(new LinkedList<RTCVideoView>());
        mUnusedWindowList.add(mRemoteWindowA);
        mUnusedWindowList.add(mRemoteWindowB);
        mUnusedWindowList.add(mRemoteWindowC);
        mUnusedWindowList.add(mRemoteWindowD);
        mUnusedWindowList.add(mRemoteWindowE);
        mUnusedWindowList.add(mRemoteWindowF);
        mUnusedWindowList.add(mRemoteWindowG);
        mUnusedWindowList.add(mRemoteWindowH);

        for (RTCVideoView rtcVideoView : mUnusedWindowList) {
            rtcVideoView.setOnLongClickListener(mOnLongClickListener);
        }

        mUserWindowMap = new ConcurrentHashMap<>();

        mControlFragment = new ControlFragment();

        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                logAndToast("Permission " + permission + " is not granted");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }

        SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        mVideoWidth = preferences.getInt(Config.WIDTH, QNRTCSetting.DEFAULT_WIDTH);
        mVideoHeight = preferences.getInt(Config.HEIGHT, QNRTCSetting.DEFAULT_HEIGHT);
        int videoBitrate = preferences.getInt(Config.BITRATE, 800 * 1000);
        boolean isHwCodec = preferences.getInt(Config.CODEC_MODE, Config.SW) == Config.HW;

        // get the items in hw black list, and set isHwCodec false forcibly
        String[] hwBlackList = getResources().getStringArray(R.array.hw_black_list);
        mHWBlackList.addAll(Arrays.asList(hwBlackList));
        if (mHWBlackList.contains(Build.MODEL)) {
            isHwCodec = false;
        }

        QNRTCSetting setting = new QNRTCSetting();
        setting.setAudioBitrate(100 * 1000)
                .setVideoBitrate(videoBitrate)
                .setBitrateRange(videoBitrate, QNRTCSetting.DEFAULT_MAX_BITRATE)
                .setCameraID(QNRTCSetting.CAMERA_FACING_ID.FRONT)
                .setHWCodecEnabled(isHwCodec)
                .setVideoPreviewFormat(new QNVideoFormat(mVideoWidth, mVideoHeight, QNRTCSetting.DEFAULT_FPS))
                .setVideoEncodeFormat(new QNVideoFormat(mVideoWidth, mVideoHeight, QNRTCSetting.DEFAULT_FPS));

        mControlFragment.setArguments(intent.getExtras());
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.control_fragment_container, mControlFragment);
        ft.commitAllowingStateLoss();

        mRTCManager = new QNRTCManager();
        mRTCManager.setRoomEventListener(this);
        mRTCManager.addRemoteWindow(mRemoteWindowA.getRemoteSurfaceView());
        mRTCManager.addRemoteWindow(mRemoteWindowB.getRemoteSurfaceView());
        mRTCManager.addRemoteWindow(mRemoteWindowC.getRemoteSurfaceView());
        mRTCManager.addRemoteWindow(mRemoteWindowD.getRemoteSurfaceView());
        mRTCManager.addRemoteWindow(mRemoteWindowE.getRemoteSurfaceView());
        mRTCManager.addRemoteWindow(mRemoteWindowF.getRemoteSurfaceView());
        mRTCManager.addRemoteWindow(mRemoteWindowG.getRemoteSurfaceView());
        mRTCManager.addRemoteWindow(mRemoteWindowH.getRemoteSurfaceView());
        mRTCManager.initialize(this, setting, mLocalWindow.getLocalSurfaceView());
    }

    public void onClickScreen(View v) {
        if (mUsedWindowList.size() < 3) {
            toggleControlFragmentVisibility();
        }
    }

    private void toggleControlFragmentVisibility() {
        if (!mControlFragment.isAdded()) {
            return;
        }

        mCallControlFragmentVisible = !mCallControlFragmentVisible;
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (mCallControlFragmentVisible) {
            ft.show(mControlFragment);
        } else {
            ft.hide(mControlFragment);
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commitAllowingStateLoss();
    }

    private void startCall() {
        if (mRTCManager == null || mIsJoinedRoom) {
            return;
        }
        mCallStartedTimeMs = System.currentTimeMillis();

        logAndToast(getString(R.string.connecting_to, mRoomId));
        mRTCManager.joinRoom(mRoomToken);
        mIsJoinedRoom = true;
    }

    private void onConnectedInternal() {
        final long delay = System.currentTimeMillis() - mCallStartedTimeMs;
        Log.i(TAG, "Call connected: delay=" + delay + "ms");
        logAndToast(getString(R.string.connected_to_room));
    }

    private void subscribeAllRemoteStreams() {
        ArrayList<String> publishingUsers = mRTCManager.getPublishingUserList();
        if (publishingUsers != null && !publishingUsers.isEmpty()) {
            for (String userId : publishingUsers) {
                mRTCManager.subscribe(userId);
                mRTCManager.addRemoteAudioCallback(userId, new QNRemoteAudioCallback() {
                    @Override
                    public void onData(String s, ByteBuffer byteBuffer, int i, int i1, int i2, int i3) {

                    }
                });
            }
        }
    }

    private void clearAllRemoteStreams() {
        if (mUsedWindowList.size() > 2) {
            setTargetWindowParams(1, 0, mLocalWindow);
        }
        mUsedWindowList.clear();
        mUsedWindowList.add(mLocalWindow);

        for (RTCVideoView rtcVideoView : mUserWindowMap.values()) {
            rtcVideoView.setVisible(false);
        }
        mUserWindowMap.clear();

        mUnusedWindowList.clear();
        mUnusedWindowList.add(mRemoteWindowA);
        mUnusedWindowList.add(mRemoteWindowB);
        mUnusedWindowList.add(mRemoteWindowC);
        mUnusedWindowList.add(mRemoteWindowD);
        mUnusedWindowList.add(mRemoteWindowE);
        mUnusedWindowList.add(mRemoteWindowF);
        mUnusedWindowList.add(mRemoteWindowG);
        mUnusedWindowList.add(mRemoteWindowH);
    }

    private void disconnect() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mControlFragment.stopTimer();
            }
        });
        if (mLogToast != null) {
            mLogToast.cancel();
        }
        if (mRTCManager != null) {
            if (mIsAdmin) {
                mRTCManager.stopMergeStream();
            }
            mRTCManager.destroy();
            mRTCManager = null;
        }
        mLocalWindow = null;
        mRemoteWindowA = null;
        mRemoteWindowB = null;
        mRemoteWindowC = null;
        mRemoteWindowD = null;
        mRemoteWindowE = null;
        mRemoteWindowF = null;
        mRemoteWindowG = null;
        mRemoteWindowH = null;

        mIsJoinedRoom = false;
    }

    private void disconnectWithErrorMessage(final String errorMessage) {
        new AlertDialog.Builder(this)
                .setTitle(getText(R.string.channel_error_title))
                .setMessage(errorMessage)
                .setCancelable(false)
                .setNeutralButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                .create()
                .show();
    }

    private void logAndToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, msg);
                if (mLogToast != null) {
                    mLogToast.cancel();
                }
                mLogToast = Toast.makeText(RoomActivity.this, msg, Toast.LENGTH_SHORT);
                mLogToast.show();
            }
        });
    }

    private void reportError(final String description) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!mIsError) {
                    mIsError = true;
                    disconnectWithErrorMessage(description);
                }
            }
        });
    }

    private RTCVideoView getWindowByUserId(String userId) {
        return mUserWindowMap.containsKey(userId) ? mUserWindowMap.get(userId) : null;
    }

    private void toggleToMultiUsersUI(final int userCount, List<RTCVideoView> windowList) {
        for (int i = 0; i < userCount; i++) {
            setTargetWindowParams(userCount, i, windowList.get(i));
        }
    }

    public synchronized void setTargetWindowParams(final int userCount, final int targetPos, final RTCVideoView targetWindow) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (userCount) {
                    case 1:
                        updateLayoutParams(targetWindow, 0, mScreenWidth, mScreenHeight, 0, 0, -1);
                    case 2:
                        if (targetPos == 0) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth, mScreenHeight, 0, 0, -1);
                        } else if (targetPos == 1) {
                            updateLayoutParams(targetWindow, targetPos, (int) (120 * mDensity + 0.5f), (int) (160 * mDensity + 0.5f), 0, 0, Gravity.TOP | Gravity.END);
                        }
                        break;
                    case 3:
                        if (targetPos == 0) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 2, mScreenWidth / 2, 0, 0, -1);
                        } else if (targetPos == 1) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 2, mScreenWidth / 2, mScreenWidth / 2, 0, -1);
                        } else {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 2, mScreenWidth / 2, 0, mScreenWidth / 2, Gravity.CENTER_HORIZONTAL);
                        }
                        break;
                    case 4:
                        if (targetPos == 0) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 2, mScreenWidth / 2, 0, 0, -1);
                        } else if (targetPos == 1) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 2, mScreenWidth / 2, mScreenWidth / 2, 0, -1);
                        } else if (targetPos == 2) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 2, mScreenWidth / 2, 0, mScreenWidth / 2, Gravity.START);
                        } else {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 2, mScreenWidth / 2, mScreenWidth / 2, mScreenWidth / 2, -1);
                        }
                        break;
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                        if (targetPos == 0) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 3, mScreenWidth / 3, 0, 0, -1);
                        } else if (targetPos == 1) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth / 3, 0, -1);
                        } else if (targetPos == 2) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth * 2 / 3, 0, Gravity.END);
                        } else if (targetPos == 3) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 3, mScreenWidth / 3, 0, mScreenWidth / 3, -1);
                        } else if (targetPos == 4) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth / 3, -1);
                        } else if (targetPos == 5) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth * 2 / 3, mScreenWidth / 3, -1);
                        } else if (targetPos == 6) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 3, mScreenWidth / 3, 0, mScreenWidth * 2 / 3, -1);
                        } else if (targetPos == 7) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth * 2 / 3, -1);
                        } else if (targetPos == 8) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth * 2 / 3, mScreenWidth * 2 / 3, -1);
                        }
                        break;
                }
            }
        });
    }

    private void updateLayoutParams(RTCVideoView targetView, int targetPos, int width, int height, int marginStart, int marginTop, int gravity) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) targetView.getLayoutParams();
        lp.width = width;
        lp.height = height;
        lp.topMargin = marginTop;
        lp.gravity = gravity;
        lp.setMarginStart(marginStart);
        targetView.setLayoutParams(lp);
        targetView.setMicrophoneStateVisibility(
                (width == mScreenWidth && height == mScreenHeight) ? View.INVISIBLE : View.VISIBLE);
        if (targetView.getAudioViewVisibility() == View.VISIBLE) {
            targetView.updateAudioView(targetPos);
        }
    }

    private void updateRemoteLogText(final String logText) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mControlFragment.updateRemoteLogText(logText);
            }
        });
    }

    private boolean isAdmin() {
        return mUserId.toLowerCase().indexOf(QNAppServer.ADMIN_USER) != -1;
    }

    private synchronized void clearMergeStreamPos(String userId) {
        int pos = -1;
        if (mMergeStreamPosition != null && !TextUtils.isEmpty(userId)) {
            for (int i = 0; i < mMergeStreamPosition.length; i++) {
                if (userId.equals(mMergeStreamPosition[i])) {
                    pos = i;
                    break;
                }
            }
        }
        if (pos >= 0 && pos < mMergeStreamPosition.length) {
            mMergeStreamPosition[pos] = null;
        }
    }

    private int getMergeStreamIdlePos() {
        int pos = -1;
        for (int i = 0; i< mMergeStreamPosition.length; i++) {
            if (TextUtils.isEmpty(mMergeStreamPosition[i])) {
                pos = i;
                break;
            }
        }
        return pos;
    }

    private synchronized void setMergeRemoteStreamLayout(String userId) {
        if (mIsAdmin) {
            int pos = getMergeStreamIdlePos();
            if (pos == -1) {
                Log.e(TAG, "No idle position for merge streaming, so discard.");
                return;
            }
            int x = QNAppServer.MERGE_STREAM_POS[pos][0];
            int y = QNAppServer.MERGE_STREAM_POS[pos][1];
            mRTCManager.setMergeStreamLayout(userId, x, y, 1, QNAppServer.MERGE_STREAM_WIDTH, QNAppServer.MERGE_STREAM_HEIGHT);
            mMergeStreamPosition[pos] = userId;
        }
    }

    private void showKickoutDialog(final String userId) {
        if (mKickoutDialog == null) {
            mKickoutDialog = new AlertDialog.Builder(this)
                    .setNegativeButton(R.string.negative_dialog_tips, null)
                    .create();
        }
        mKickoutDialog.setMessage(getString(R.string.kickout_tips, userId));
        mKickoutDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.positive_dialog_tips),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mRTCManager.kickOutUser(userId);
                    }
                });
        mKickoutDialog.show();
    }

    @TargetApi(19)
    private static int getSystemUiVisibility() {
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        return flags;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCall();
    }

    @Override
    public void onBackPressed() {
        disconnect();
        super.onBackPressed();
    }

    @Override
    public void onCallHangUp() {
        disconnect();
        finish();
    }

    @Override
    public void onCameraSwitch() {
        if (mRTCManager != null) {
            mRTCManager.switchCamera(new QNCameraSwitchResultCallback() {
                @Override
                public void onCameraSwitchDone(boolean isFrontCamera) {
                }

                @Override
                public void onCameraSwitchError(String errorMessage) {
                }
            });
        }
    }

    @Override
    public boolean onToggleMic() {
        if (mRTCManager != null) {
            mMicEnabled = !mMicEnabled;
            mRTCManager.muteLocalAudio(!mMicEnabled);
            mLocalWindow.updateMicrophoneStateView(!mMicEnabled);
        }
        return mMicEnabled;
    }

    @Override
    public boolean onToggleVideo() {
        if (mRTCManager != null) {
            mVideoEnabled = !mVideoEnabled;
            mRTCManager.muteLocalVideo(!mVideoEnabled);
            if (!mVideoEnabled) {
                mUsedWindowList.get(0).setAudioViewVisible(0);
            } else {
                mUsedWindowList.get(0).setAudioViewInvisible();
            }
            mRTCManager.setPreviewEnabled(mVideoEnabled);
        }
        return mVideoEnabled;
    }

    @Override
    public boolean onToggleSpeaker() {
        if (mRTCManager != null) {
            mSpeakerEnabled = !mSpeakerEnabled;
            mRTCManager.muteRemoteAudio(!mSpeakerEnabled);
        }
        return mSpeakerEnabled;
    }

    @Override
    public boolean onToggleBeauty() {
        if (mRTCManager != null) {
            mBeautyEnabled = !mBeautyEnabled;
            QNBeautySetting beautySetting = new QNBeautySetting(0.5f, 0.5f, 0.5f);
            beautySetting.setEnable(mBeautyEnabled);
            mRTCManager.setBeauty(beautySetting);
        }
        return mBeautyEnabled;
    }

    @Override
    public void onJoinedRoom() {
        mIsAdmin = isAdmin();
        if (mIsAdmin) {
            mMergeStreamPosition = new String[9];
        }
        onConnectedInternal();
        mRTCManager.publish();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mControlFragment.startTimer();
            }
        });
    }

    @Override
    public void onLocalPublished() {
        if (mIsAdmin) {
            mRTCManager.setMergeStreamLayout(mUserId, 0, 0, 0, QNAppServer.STREAMING_WIDTH, QNAppServer.STREAMING_HEIGHT);
        }
        subscribeAllRemoteStreams();
        mRTCManager.setStatisticsInfoEnabled(mUserId, true, 3000);
    }

    @Override
    public void onSubscribed(String userId) {
        Log.i(TAG, "onSubscribed: userId: " + userId);
        updateRemoteLogText("onSubscribed : " + userId);
        setMergeRemoteStreamLayout(userId);
        mRTCManager.setStatisticsInfoEnabled(userId, true, 3000);
    }

    @Override
    public void onRemotePublished(String userId, boolean hasAudio, boolean hasVideo) {
        Log.i(TAG, "onRemotePublished: userId: " + userId);
        updateRemoteLogText("onRemotePublished : " + userId + " hasAudio : " + hasAudio + " hasVideo : " + hasVideo);
        mRTCManager.subscribe(userId);
        mRTCManager.addRemoteAudioCallback(userId, new QNRemoteAudioCallback() {
            @Override
            public void onData(String userId, ByteBuffer audioData, int size, int bitsPerSample, int sampleRate, int numberOfChannels) {
            }
        });
    }

    @Override
    public QNRemoteSurfaceView onRemoteStreamAdded(final String userId, final boolean isAudioEnabled, final boolean isVideoEnabled,
                                                   final boolean isAudioMuted, final boolean isVideoMuted) {
        Log.i(TAG, "onRemoteStreamAdded: user = " + userId + ", hasAudio = " + isAudioEnabled + ", hasVideo = " + isVideoEnabled
                + ", isAudioMuted = " + isAudioMuted + ", isVideoMuted = " + isVideoMuted);
        updateRemoteLogText("onRemoteStreamAdded : " + userId);

        final RTCVideoView remoteWindow = mUnusedWindowList.remove(0);
        remoteWindow.setUserId(userId);
        mUserWindowMap.put(userId, remoteWindow);
        mUsedWindowList.add(remoteWindow);
        final int userCount = mUsedWindowList.size();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                remoteWindow.setVisible(true);
                remoteWindow.updateMicrophoneStateView(isAudioMuted);
                if (isVideoMuted || !isVideoEnabled) {
                    remoteWindow.setAudioViewVisible(mUsedWindowList.indexOf(remoteWindow));
                }

                if (userCount <= 5) {
                    toggleToMultiUsersUI(userCount, mUsedWindowList);
                } else {
                    setTargetWindowParams(userCount, userCount - 1, remoteWindow);
                }
                if (userCount >= 3) {
                    mCallControlFragmentVisible = true;
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.show(mControlFragment);
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                    ft.commitAllowingStateLoss();
                }
            }
        });
        return remoteWindow.getRemoteSurfaceView();
    }

    @Override
    public void onRemoteStreamRemoved(final String userId) {
        Log.i(TAG, "onRemoteStreamRemoved: " + userId);
        updateRemoteLogText("onRemoteStreamRemoved : " + userId);
        clearMergeStreamPos(userId);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mUserWindowMap.containsKey(userId)) {
                    RTCVideoView remoteVideoView = mUserWindowMap.remove(userId);
                    remoteVideoView.setVisible(false);
                    mUsedWindowList.remove(remoteVideoView);
                    mUnusedWindowList.add(remoteVideoView);
                }
                toggleToMultiUsersUI(mUsedWindowList.size(), mUsedWindowList);
            }
        });
    }

    @Override
    public void onRemoteUserLeaved(String userId) {
        Log.i(TAG, "onUserOut: " + userId);
        updateRemoteLogText("onRemoteUserLeaved : " + userId);
    }

    @Override
    public void onRemoteUserJoined(String userId) {
        Log.i(TAG, "onUserIn: " + userId);
        updateRemoteLogText("onRemoteUserJoined : " + userId);
    }

    @Override
    public void onRemoteUnpublished(String userId) {
        Log.i(TAG, "onRemoteUnpublish: " + userId);
        updateRemoteLogText("onRemoteUnpublished : " + userId);
    }

    @Override
    public void onRemoteMute(final String userId, final boolean isAudioMuted, final boolean isVideoMuted) {
        Log.i(TAG, "onRemoteMute: user = " + userId + ", audio = " + isAudioMuted + ", video = " + isVideoMuted);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RTCVideoView remoteWindow = getWindowByUserId(userId);
                if (remoteWindow != null) {
                    if (isVideoMuted && remoteWindow.getAudioViewVisibility() != View.VISIBLE) {
                        remoteWindow.setAudioViewVisible(mUsedWindowList.indexOf(remoteWindow));
                    } else if (!isVideoMuted && remoteWindow.getAudioViewVisibility() != View.INVISIBLE) {
                        remoteWindow.setAudioViewInvisible();
                    }
                    remoteWindow.updateMicrophoneStateView(isAudioMuted);
                }
            }
        });
    }

    @Override
    public void onStateChanged(QNRoomState state) {
        Log.i(TAG, "onStateChanged: " + state);
        updateRemoteLogText("onStateChanged : " + state.name());
        switch (state) {
            case RECONNECTING:
                mCallStartedTimeMs = System.currentTimeMillis();
                logAndToast(getString(R.string.reconnecting_to_room));
                break;
            case CONNECTED:
                break;
        }
    }

    @Override
    public void onError(final int errorCode, String description) {
        Log.i(TAG, "onError: " + errorCode + " " + description);
        updateRemoteLogText("onError : " + errorCode + " " + description);
        switch (errorCode) {
            case ERROR_KICKED_OUT_OF_ROOM:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtils.s(RoomActivity.this, getString(R.string.kicked_by_admin));
                    }
                });
                onCallHangUp();
                break;
            default:
                reportError("errorCode: " + errorCode + "\ndescription: \n" + description);
                break;
        }
    }

    @Override
    public void onStatisticsUpdated(QNStatisticsReport report) {
        Log.d(TAG, "onStatisticsUpdated: " + report.toString());
        if (!mUserId.equals(report.userId)) {
            return;
        }
        mLocalLogText = String.format(getString(R.string.log_text), report.userId, report.frameRate, report.videoBitrate / 1000, report.audioBitrate / 1000, report.videoPacketLostRate, report.audioPacketLostRate, report.width, report.height);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mControlFragment.updateLocalLogText(mLocalLogText);
            }
        });
    }

    @Override
    public void onUserKickedOut(String userId) {
        Log.i(TAG, "kicked out user: " + userId);
        updateRemoteLogText("onUserKickedOut : " + userId);
    }

    private RTCVideoView.OnLongClickListener mOnLongClickListener = new RTCVideoView.OnLongClickListener() {
        @Override
        public void onLongClick(String userId) {
            if (!mIsAdmin) {
                Log.i(TAG, "Only admin user can kick a player!");
                return;
            }
            showKickoutDialog(userId);
        }
    };
}