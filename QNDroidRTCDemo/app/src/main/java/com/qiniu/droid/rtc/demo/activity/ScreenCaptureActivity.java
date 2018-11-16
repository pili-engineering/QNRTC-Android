package com.qiniu.droid.rtc.demo.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.qiniu.droid.rtc.QNLocalSurfaceView;
import com.qiniu.droid.rtc.QNRTCManager;
import com.qiniu.droid.rtc.QNRTCSetting;
import com.qiniu.droid.rtc.QNRemoteAudioCallback;
import com.qiniu.droid.rtc.QNRemoteSurfaceView;
import com.qiniu.droid.rtc.QNRoomEventListener;
import com.qiniu.droid.rtc.QNRoomState;
import com.qiniu.droid.rtc.QNStatisticsReport;
import com.qiniu.droid.rtc.QNVideoFormat;
import com.qiniu.droid.rtc.demo.R;
import com.qiniu.droid.rtc.demo.ui.RTCVideoView;
import com.qiniu.droid.rtc.demo.ui.RemoteVideoView;
import com.qiniu.droid.rtc.demo.utils.Config;
import com.qiniu.droid.rtc.demo.utils.ToastUtils;
import com.qiniu.droid.rtc.model.QNAudioDevice;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.qiniu.droid.rtc.QNErrorCode.ERROR_KICKED_OUT_OF_ROOM;

public class ScreenCaptureActivity extends Activity implements QNRoomEventListener {
    private static final String TAG = "ScreenCaptureActivity";
    public static final String EXTRA_ROOM_TOKEN = "ROOM_TOKEN";

    private static final String[] MANDATORY_PERMISSIONS = {
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET",
            "android.permission.CAMERA"
    };

    private List<RTCVideoView> mUsedWindowList;
    private List<RTCVideoView> mUnusedWindowList;
    private ConcurrentHashMap<String, RTCVideoView> mUserWindowMap;

    private ImageButton mBtnVideo;
    private RemoteVideoView mRemoteWindowA;
    private RemoteVideoView mRemoteWindowB;
    private RemoteVideoView mRemoteWindowC;
    private RemoteVideoView mRemoteWindowD;
    private RemoteVideoView mRemoteWindowE;
    private RemoteVideoView mRemoteWindowF;
    private RemoteVideoView mRemoteWindowG;
    private RemoteVideoView mRemoteWindowH;

    private QNRTCManager mRTCManager;
    private String mRoomToken;
    private Camera mCamera;
    private FrameLayout mPreviewLayout;
    private boolean mIsPreviewEnable = true;
    private boolean mIsPreviewFirstFrame = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());

        setContentView(R.layout.activity_screen_capture);

        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                ToastUtils.s(getApplicationContext(), "Permission " + permission + " is not granted");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }

        Intent intent = getIntent();
        mRoomToken = intent.getStringExtra(EXTRA_ROOM_TOKEN);

        SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        int videoWidth = preferences.getInt(Config.WIDTH, QNRTCSetting.DEFAULT_WIDTH);
        int videoHeight = preferences.getInt(Config.HEIGHT, QNRTCSetting.DEFAULT_HEIGHT);
        boolean isHwCodec = preferences.getInt(Config.CODEC_MODE, Config.SW) == Config.HW;

        mPreviewLayout = (FrameLayout) findViewById(R.id.camera_preview);
        mBtnVideo = (ImageButton) findViewById(R.id.video_button);
        mRemoteWindowA = (RemoteVideoView) findViewById(R.id.remote_video_view_a);
        mRemoteWindowB = (RemoteVideoView) findViewById(R.id.remote_video_view_b);
        mRemoteWindowC = (RemoteVideoView) findViewById(R.id.remote_video_view_c);
        mRemoteWindowD = (RemoteVideoView) findViewById(R.id.remote_video_view_d);
        mRemoteWindowE = (RemoteVideoView) findViewById(R.id.remote_video_view_e);
        mRemoteWindowF = (RemoteVideoView) findViewById(R.id.remote_video_view_f);
        mRemoteWindowG = (RemoteVideoView) findViewById(R.id.remote_video_view_g);
        mRemoteWindowH = (RemoteVideoView) findViewById(R.id.remote_video_view_h);

        mUserWindowMap = new ConcurrentHashMap<>();
        mUsedWindowList = Collections.synchronizedList(new LinkedList<RTCVideoView>());
        mUnusedWindowList = Collections.synchronizedList(new LinkedList<RTCVideoView>());
        mUnusedWindowList.add(mRemoteWindowA);
        mUnusedWindowList.add(mRemoteWindowB);
        mUnusedWindowList.add(mRemoteWindowC);
        mUnusedWindowList.add(mRemoteWindowD);
        mUnusedWindowList.add(mRemoteWindowE);
        mUnusedWindowList.add(mRemoteWindowF);
        mUnusedWindowList.add(mRemoteWindowG);
        mUnusedWindowList.add(mRemoteWindowH);

        TextureView previewTextureView = (TextureView) findViewById(R.id.camera_preview_texture_view);
        previewTextureView.setSurfaceTextureListener(mPreviewSurfaceTextureListener);

        QNLocalSurfaceView localSurfaceView = (QNLocalSurfaceView) findViewById(R.id.local_surface_view);
        QNRTCSetting setting = new QNRTCSetting();
        setting.setHWCodecEnabled(isHwCodec)
                .setScreenCaptureEnabled(true)
                .setVideoPreviewFormat(new QNVideoFormat(videoWidth, videoHeight, QNRTCSetting.DEFAULT_FPS))
                .setVideoEncodeFormat(new QNVideoFormat(videoWidth, videoHeight, QNRTCSetting.DEFAULT_FPS));
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
        mRTCManager.initialize(getApplicationContext(), setting, localSurfaceView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraPreview();
        mIsPreviewFirstFrame = true;
        mRTCManager.joinRoom(mRoomToken);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRTCManager != null) {
            mRTCManager.destroy();
        }

        mRemoteWindowA = null;
        mRemoteWindowB = null;
        mRemoteWindowC = null;
        mRemoteWindowD = null;
        mRemoteWindowE = null;
        mRemoteWindowF = null;
        mRemoteWindowG = null;
        mRemoteWindowH = null;
    }

    @TargetApi(19)
    private static int getSystemUiVisibility() {
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        return flags;
    }

    private static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT); // 打开前置摄像头
        } catch (Exception e) {
            // 相机不可用
        }
        return c;
    }

    private void startCameraPreview() {
        mCamera = getCameraInstance();
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            float ratio = 9f / 16f;
            float temp = 0f;
            float minDiff = 100f;
            int previewWidth = 0;
            int previewHeight = 0;
            List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
            for (Camera.Size s : supportedPreviewSizes) {
                temp = Math.abs(((float) s.height / (float) s.width) - ratio);
                if (temp < minDiff) {
                    minDiff = temp;
                    previewWidth = s.width;
                    previewHeight = s.height;
                }
            }
            parameters.setPreviewSize(previewWidth, previewHeight);
            mCamera.setParameters(parameters);
            Log.i(TAG, "previewWidth = " + previewWidth + " previewHeight = " + previewHeight);
            mCamera.startPreview();
        }
    }

    private void subscribeAllRemoteStreams() {
        ArrayList<String> publishingUsers = mRTCManager.getPublishingUserList();
        if (publishingUsers != null && !publishingUsers.isEmpty()) {
            for (String userId : publishingUsers) {
                mRTCManager.subscribe(userId);
                mRTCManager.addRemoteAudioCallback(userId, new QNRemoteAudioCallback() {
                    @Override
                    public void onRemoteAudioAvailable(String userId, ByteBuffer audioData, int size, int bitsPerSample, int sampleRate, int numberOfChannels) {
                    }
                });
            }
        }
    }

    public void onClickHangUp(View view) {
        finish();
    }

    public void onClickPreviewEnable(View view) {
        mPreviewLayout.setVisibility(mIsPreviewEnable ? View.GONE : View.VISIBLE);
        mIsPreviewEnable = !mIsPreviewEnable;
        mBtnVideo.setImageResource(mIsPreviewEnable ? R.mipmap.video_open : R.mipmap.video_close);
    }

    public void onClickToHome(View view) {
        ToastUtils.l(this, "正在全局录制，可以返回应用");
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    @Override
    public void onJoinedRoom() {
        mRTCManager.publish();
    }

    @Override
    public void onLocalPublished() {
        subscribeAllRemoteStreams();
    }

    @Override
    public void onSubscribed(String userId) {
        Log.i(TAG, "onSubscribed = " + userId);
    }

    @Override
    public void onRemotePublished(String userId, boolean hasAudio, boolean hasVideo) {
        mRTCManager.subscribe(userId);
    }

    @Override
    public QNRemoteSurfaceView onRemoteStreamAdded(final String userId, final boolean isAudioEnabled, final boolean isVideoEnabled,
                                                   final boolean isAudioMuted, final boolean isVideoMuted) {
        Log.i(TAG, "onRemoteStreamAdded: user = " + userId + ", hasAudio = " + isAudioEnabled + ", hasVideo = " + isVideoEnabled
                + ", isAudioMuted = " + isAudioMuted + ", isVideoMuted = " + isVideoMuted);
        final RTCVideoView remoteWindow = mUnusedWindowList.remove(0);
        remoteWindow.setUserId(userId);
        mUserWindowMap.put(userId, remoteWindow);
        mUsedWindowList.add(remoteWindow);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                remoteWindow.setVisible(true);
                remoteWindow.setMicrophoneStateVisibility(View.GONE);
                remoteWindow.updateMicrophoneStateView(isAudioMuted);
                if (isVideoMuted || !isVideoEnabled) {
                    remoteWindow.setAudioViewVisible(mUsedWindowList.indexOf(remoteWindow));
                    remoteWindow.setAudioOnly(!isVideoEnabled);
                }
            }
        });
        return remoteWindow.getRemoteSurfaceView();
    }

    @Override
    public void onRemoteStreamRemoved(final String userId) {
        Log.i(TAG, "onRemoteStreamRemoved: " + userId);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mUserWindowMap.containsKey(userId)) {
                    RTCVideoView remoteVideoView = mUserWindowMap.remove(userId);
                    remoteVideoView.setVisibility(View.GONE);
                    mUsedWindowList.remove(remoteVideoView);
                    mUnusedWindowList.add(remoteVideoView);
                }
            }
        });
    }

    @Override
    public void onRemoteUserJoined(String userId) {
        Log.i(TAG, "onUserIn: " + userId);
    }

    @Override
    public void onRemoteUserLeaved(String userId) {
        Log.i(TAG, "onUserOut: " + userId);
    }

    @Override
    public void onRemoteUnpublished(String userId) {
        Log.i(TAG, "onRemoteUnpublish: " + userId);
    }

    @Override
    public void onRemoteMute(final String userId, final boolean isAudioMuted, final boolean isVideoMuted) {
        Log.i(TAG, "onRemoteMute: user = " + userId + ", audio = " + isAudioMuted + ", video = " + isVideoMuted);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RTCVideoView remoteWindow = mUserWindowMap.containsKey(userId) ? mUserWindowMap.get(userId) : null;
                if (remoteWindow != null) {
                    if (isVideoMuted && remoteWindow.getAudioViewVisibility() != View.VISIBLE) {
                        remoteWindow.setAudioViewVisible(mUsedWindowList.indexOf(remoteWindow));
                    } else if (!isVideoMuted && remoteWindow.getAudioViewVisibility() != View.INVISIBLE && !remoteWindow.isAudioOnly()) {
                        remoteWindow.setAudioViewInvisible();
                    }
                }
            }
        });
    }

    @Override
    public void onStateChanged(QNRoomState state) {
        Log.i(TAG, "onStateChanged: " + state);
    }

    @Override
    public void onError(int errorCode, String description) {
        Log.i(TAG, "onError: " + errorCode + " " + description);
        if (errorCode == ERROR_KICKED_OUT_OF_ROOM) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ToastUtils.s(ScreenCaptureActivity.this, getString(R.string.kicked_by_admin));
                }
            });
            finish();
        }
    }

    @Override
    public void onStatisticsUpdated(QNStatisticsReport report) {
        Log.d(TAG, "onStatisticsUpdated: " + report.toString());
    }

    @Override
    public void onUserKickedOut(String userId) {
        Log.i(TAG, "kicked out user: " + userId);
    }

    @Override
    public void onAudioRouteChanged(QNAudioDevice routing) {
        Log.i(TAG, "onAudioRouteChanged: " + routing.value());
    }

    @Override
    public void onCreateMergeJobSuccess(String mergeJobId) {
        Log.i(TAG, "onCreateMergeJobSuccess: " + mergeJobId);
    }

    private TextureView.SurfaceTextureListener mPreviewSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            try {
                mCamera.setPreviewTexture(surface);
                mCamera.setDisplayOrientation(90);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.i(TAG, "onSurfaceTextureSizeChanged");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.i(TAG, "onSurfaceTextureDestroyed");
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            if (mCamera != null && mIsPreviewFirstFrame) {
                try {
                    mCamera.setPreviewTexture(surface);
                    mCamera.setDisplayOrientation(90);
                    mIsPreviewFirstFrame = false;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };
}
