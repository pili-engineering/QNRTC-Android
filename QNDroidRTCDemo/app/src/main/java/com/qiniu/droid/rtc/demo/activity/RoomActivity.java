package com.qiniu.droid.rtc.demo.activity;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.qiniu.droid.rtc.QNAudioQualityPreset;
import com.qiniu.droid.rtc.QNAudioScene;
import com.qiniu.droid.rtc.QNAudioVolumeInfo;
import com.qiniu.droid.rtc.QNBeautySetting;
import com.qiniu.droid.rtc.QNCameraEventListener;
import com.qiniu.droid.rtc.QNCameraFacing;
import com.qiniu.droid.rtc.QNCameraSwitchResultCallback;
import com.qiniu.droid.rtc.QNCameraVideoTrack;
import com.qiniu.droid.rtc.QNCameraVideoTrackConfig;
import com.qiniu.droid.rtc.QNClientEventListener;
import com.qiniu.droid.rtc.QNConnectionDisconnectedInfo;
import com.qiniu.droid.rtc.QNConnectionState;
import com.qiniu.droid.rtc.QNCustomMessage;
import com.qiniu.droid.rtc.QNDegradationPreference;
import com.qiniu.droid.rtc.QNDirectLiveStreamingConfig;
import com.qiniu.droid.rtc.QNErrorCode;
import com.qiniu.droid.rtc.QNLiveStreamingErrorInfo;
import com.qiniu.droid.rtc.QNLiveStreamingListener;
import com.qiniu.droid.rtc.QNLocalTrack;
import com.qiniu.droid.rtc.QNMediaRelayState;
import com.qiniu.droid.rtc.QNMicrophoneAudioTrack;
import com.qiniu.droid.rtc.QNMicrophoneAudioTrackConfig;
import com.qiniu.droid.rtc.QNMicrophoneEventListener;
import com.qiniu.droid.rtc.QNNetworkQuality;
import com.qiniu.droid.rtc.QNNetworkQualityListener;
import com.qiniu.droid.rtc.QNPublishResultCallback;
import com.qiniu.droid.rtc.QNRTC;
import com.qiniu.droid.rtc.QNRTCClient;
import com.qiniu.droid.rtc.QNRTCEventListener;
import com.qiniu.droid.rtc.QNRTCSetting;
import com.qiniu.droid.rtc.QNRemoteAudioTrack;
import com.qiniu.droid.rtc.QNRemoteTrack;
import com.qiniu.droid.rtc.QNRemoteVideoTrack;
import com.qiniu.droid.rtc.QNScreenVideoTrack;
import com.qiniu.droid.rtc.QNScreenVideoTrackConfig;
import com.qiniu.droid.rtc.QNTrack;
import com.qiniu.droid.rtc.QNTrackInfoChangedListener;
import com.qiniu.droid.rtc.QNTranscodingLiveStreamingConfig;
import com.qiniu.droid.rtc.QNTranscodingLiveStreamingTrack;
import com.qiniu.droid.rtc.QNVideoCaptureConfig;
import com.qiniu.droid.rtc.QNVideoEncoderConfig;
import com.qiniu.droid.rtc.demo.R;
import com.qiniu.droid.rtc.demo.fragment.ControlFragment;
import com.qiniu.droid.rtc.demo.model.RTCRoomMergeOption;
import com.qiniu.droid.rtc.demo.model.RTCTrackMergeOption;
import com.qiniu.droid.rtc.demo.model.RTCUserMergeOptions;
import com.qiniu.droid.rtc.demo.service.ForegroundService;
import com.qiniu.droid.rtc.demo.ui.CircleTextView;
import com.qiniu.droid.rtc.demo.ui.MergeLayoutConfigView;
import com.qiniu.droid.rtc.demo.ui.UserTrackView;
import com.qiniu.droid.rtc.demo.utils.Config;
import com.qiniu.droid.rtc.demo.utils.QNAppServer;
import com.qiniu.droid.rtc.demo.utils.SplitUtils;
import com.qiniu.droid.rtc.demo.utils.ToastUtils;
import com.qiniu.droid.rtc.demo.utils.TrackWindowManager;
import com.qiniu.droid.rtc.demo.utils.Utils;
import com.qiniu.droid.rtc.model.QNAudioDevice;

import org.qnwebrtc.Size;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.qiniu.droid.rtc.demo.utils.Config.DEFAULT_FPS;
import static com.qiniu.droid.rtc.demo.utils.Config.DEFAULT_RESOLUTION;
import static com.qiniu.droid.rtc.demo.utils.Utils.getSystemUiVisibility;

public class RoomActivity extends FragmentActivity implements ControlFragment.OnCallEvents {
    private static final String TAG = "RoomActivity";

    public static final String TRACK_TAG_MIC = "MICROPHONE";
    public static final String TRACK_TAG_CAMERA = "CAMERA";
    public static final String TRACK_TAG_SCREEN = "SCREEN";

    public static final String EXTRA_USER_ID = "USER_ID";
    public static final String EXTRA_ROOM_TOKEN = "ROOM_TOKEN";
    public static final String EXTRA_ROOM_ID = "ROOM_ID";

    private static final String CUSTOM_MESSAGE_KICKOUT = "KICKOUT";

    private static final String[] MANDATORY_PERMISSIONS = {
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET"
    };

    private Toast mLogToast;

    private UserTrackView mTrackWindowFullScreen;
    private List<UserTrackView> mTrackWindowsList;
    private AlertDialog mKickOutDialog;

    private QNRTCClient mClient;
    private String mRoomToken;
    private String mUserId;
    private String mRoomId;
    private boolean mMicEnabled = true;
    private boolean mBeautyEnabled = false;
    private boolean mVideoEnabled = true;
    private boolean mSpeakerEnabled = true;
    private boolean mIsAdmin = false;
    private boolean mIsJoinedRoom = false;
    private ControlFragment mControlFragment;
    private List<QNLocalTrack> mLocalTrackList;

    private QNCameraVideoTrack mCameraTrack;
    private QNMicrophoneAudioTrack mMicrophoneTrack;
    private QNScreenVideoTrack mLocalScreenTrack;

    private int mCaptureMode = Config.CAMERA_CAPTURE;

    private TrackWindowManager mTrackWindowManager;

    private int mVideoWidth;
    private int mVideoHeight;
    private int mVideoFps;
    private int mVideoBitrate;

    /**
     * 合流相关
     * 注意：
     * 一个房间仅需要一个用户可以配置合流布局即可，该用户可以基于 SDK 提供的远端用户相关回调对远端用户的动态进行监听，
     * 进而进行合流布局的实时更改。
     * demo 中默认 userID 为 "admin" 的用户可以控制合流布局的配置
     */
    private MergeLayoutConfigView mMergeLayoutConfigView;
    private PopupWindow mPopWindow;
    private UserListAdapter mUserListAdapter;
    private RTCRoomMergeOption mRoomMergeOption;
    private RTCUserMergeOptions mMergeOption;
    private volatile boolean mIsMergeStreaming;
    /**
     * 如果 QNTranscodingLiveStreamingConfig 中的 StreamID 为 null，则表示使用默认合流配置
     * 默认合流配置的宽高、码率等参数可以通过登录后台 https://portal.qiniu.com/rtn 并对连麦应用进行编辑来配置
     * 自定义合流转推可以通过自定义 {@link QNTranscodingLiveStreamingConfig} 并调用 {@link QNRTCClient#startLiveStreaming(QNTranscodingLiveStreamingConfig)} 接口来开始
     * 注意：自定义合流转推需要在加入房间之后才可执行
     */
    private QNTranscodingLiveStreamingConfig mCurrentMergeConfig;

    /**
     * 单路转推相关
     * 注意：
     * 1. 单路转推仅支持配置一路音频和一路视频
     * 2. 单路转推场景需要在初始化的时候保证配置了 "固定分辨率"{@link QNDegradationPreference#MAINTAIN_RESOLUTION} 选项的开启，否则会出问题！！！
     * demo 中默认 userId 为 "admin" 的用户可以开启单路转推功能
     */
    private QNDirectLiveStreamingConfig mCurrentDirectConfig;
    private volatile boolean mIsDirectStreaming;

    /**
     * 如果您的场景包括合流转推和单路转推的切换，那么务必维护一个 serialNum 的参数，代表流的优先级，
     * 使其不断自增来实现 rtmp 流的无缝切换，否则可能会出现抢流的现象
     * {@link QNDirectLiveStreamingConfig} 以及 {@link QNTranscodingLiveStreamingConfig} 中 Url 的格式为：rtmp://domain/app/stream?serialnum=xxx
     * 切换流程推荐为：
     * 1. 单路转推 -> 开始合流转推（以成功的回调为准） -> 停止单路转推
     * 2. 合流转推 -> 开始单路转推（以成功的回调为准） -> 停止合流转推
     * 注意：
     * 1. 两种合流转推，推流地址应该保持一致，只有 serialnum 存在差异
     * 2. 在两种推流切换的场景下，合流转推务必使用自定义合流配置，并指定推流地址的 serialnum
     */
    private int mSerialNum = 0;

    /**
     * {@link QNRTC#init} 和 {@link QNRTC#deinit()} 为静态方法，需要对称调用；
     * 为了避免在页面退出进入时存在顺序问题，建议保存是否初始化状态
     */
    private static boolean mInitRTC;

    private final Semaphore mCaptureStoppedSem = new Semaphore(1);

    // 麦克风错误标志
    private boolean mMicrophoneError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());

        final WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(outMetrics);
        int screenWidth = outMetrics.widthPixels;
        int screenHeight = outMetrics.heightPixels;

        SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        mVideoWidth = preferences.getInt(Config.WIDTH, DEFAULT_RESOLUTION[1][0]);
        mVideoHeight = preferences.getInt(Config.HEIGHT, DEFAULT_RESOLUTION[1][1]);
        mVideoFps = preferences.getInt(Config.FPS, DEFAULT_FPS[1]);
        mVideoBitrate = preferences.getInt(Config.BITRATE, Config.DEFAULT_BITRATE[1]);

        setContentView(R.layout.activity_muti_track_room);

        Intent intent = getIntent();
        mRoomToken = intent.getStringExtra(EXTRA_ROOM_TOKEN);
        mUserId = intent.getStringExtra(EXTRA_USER_ID);
        mRoomId = intent.getStringExtra(EXTRA_ROOM_ID);
        mIsAdmin = mUserId.equals(QNAppServer.ADMIN_USER);

        mTrackWindowFullScreen = findViewById(R.id.track_window_full_screen);
        mTrackWindowsList = new LinkedList<>(Arrays.asList(
                findViewById(R.id.track_window_a),
                findViewById(R.id.track_window_b),
                findViewById(R.id.track_window_c),
                findViewById(R.id.track_window_d),
                findViewById(R.id.track_window_e),
                findViewById(R.id.track_window_f),
                findViewById(R.id.track_window_g),
                findViewById(R.id.track_window_h),
                findViewById(R.id.track_window_i)
        ));

        for (final UserTrackView view : mTrackWindowsList) {
            view.setOnLongClickListener(v -> {
                if (mIsAdmin) {
                    showKickoutDialog(view.getUserId());
                }
                return false;
            });
        }

        // 初始化控制面板
        mControlFragment = new ControlFragment();
        mControlFragment.setArguments(intent.getExtras());
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.control_fragment_container, mControlFragment);
        ft.commitAllowingStateLoss();

        // 权限申请
        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                logAndToast("Permission " + permission + " is not granted");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }

        if (mInitRTC) {
            ToastUtils.showShortToast(RoomActivity.this, "RTC 未释放完成，当前页面不可用，请退出后重试！");
        } else {
            // 初始化 Client 和本地 Track 列表
            initClient();
            // 初始化本地音视频 track
            initLocalTracks();
            // 初始化合流相关配置
            initMergeLayoutConfig();

            // 多人显示窗口管理类
            mTrackWindowManager = new TrackWindowManager(mUserId, screenWidth, screenHeight, outMetrics.density, mClient, mTrackWindowFullScreen, mTrackWindowsList);

            List<QNTrack> localTrackListExcludeScreenTrack = new ArrayList<>(mLocalTrackList);
            localTrackListExcludeScreenTrack.remove(mLocalScreenTrack);
            mTrackWindowManager.addTrack(mUserId, localTrackListExcludeScreenTrack);
            new Timer().schedule(mUpdateNetWorkQualityInfoTask, 5000, 10000);
            mInitRTC = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 开始视频采集
        startCaptureAfterAcquire();
        if (!mIsJoinedRoom && mClient != null) {
            // 加入房间
            mClient.join(mRoomToken);
        }
        if (mMicrophoneError && mClient != null && mMicrophoneTrack != null) {
            mClient.unpublish(mMicrophoneTrack);
            mClient.publish(new QNPublishResultCallback() {
                @Override
                public void onPublished() {
                }
                @Override
                public void onError(int errorCode, String errorMessage) {

                }
            }, mMicrophoneTrack);
            mMicrophoneError = false;
        }
    }

    private void startCaptureAfterAcquire() {
        boolean acquired = false;
        try {
            acquired = mCaptureStoppedSem.tryAcquire(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (acquired && mCameraTrack != null) {
            mCameraTrack.startCapture();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 停止视频采集
        if (mCameraTrack != null) {
            mCameraTrack.stopCapture();
        }
        if (mPopWindow != null && mPopWindow.isShowing()) {
            mPopWindow.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseClient();
        destroyLocalTracks();
        if (mInitRTC) {
            // 反初始化
            QNRTC.deinit();
            mInitRTC = false;
        }
        if (mTrackWindowFullScreen != null) {
            mTrackWindowFullScreen.dispose();
        }
        for (UserTrackView item : mTrackWindowsList) {
            item.dispose();
        }
        mTrackWindowsList.clear();
        mPopWindow = null;
    }

    private void destroyLocalTracks() {
        if (mLocalTrackList != null) {
            for (QNLocalTrack localTrack : mLocalTrackList) {
                localTrack.destroy();
            }
            mLocalTrackList.clear();
        }
        mCameraTrack = null;
        mLocalScreenTrack = null;
        mMicrophoneTrack = null;
    }

    private void releaseClient() {
        mUpdateNetWorkQualityInfoTask.cancel();
        if (mClient != null) {
            if (mIsAdmin && mIsMergeStreaming) {
                // 如果当前正在合流，则停止
                mClient.stopLiveStreaming(mCurrentMergeConfig);
                mIsMergeStreaming = false;
            }
            if (mIsAdmin && mIsDirectStreaming) {
                // 如果当前正在单路转推，则停止
                mClient.stopLiveStreaming(mCurrentDirectConfig);
                mIsDirectStreaming = false;
            }
            // 离开房间
            mClient.leave();
            mClient = null;
        }
    }

    /**
     * 初始化 QNRTCClient
     */
    private void initClient() {
        SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        boolean isHwCodec = preferences.getInt(Config.CODEC_MODE, Config.SW) == Config.HW;

        /**
         * 设置音频场景，不同的音频场景，系统音量模式是不一样的
         */
        int audioScenePos = preferences.getInt(Config.AUDIO_SCENE, Config.DEFAULT_AUDIO_SCENE);
        QNAudioScene audioScene;
        if (audioScenePos == Config.DEFAULT_AUDIO_SCENE) {
            audioScene = QNAudioScene.DEFAULT;
        } else if (audioScenePos == Config.VOICE_CHAT_AUDIO_SCENE) {
            audioScene = QNAudioScene.VOICE_CHAT;
        } else {
            audioScene = QNAudioScene.SOUND_EQUALIZE;
        }
        mCaptureMode = preferences.getInt(Config.CAPTURE_MODE, Config.CAMERA_CAPTURE);

        QNRTCSetting setting = new QNRTCSetting();
        setting.setHWCodecEnabled(isHwCodec)
                .setAudioScene(audioScene);
        QNRTC.init(this, setting, mRTCEventListener);
        mClient = QNRTC.createClient(mClientEventListener);
        mClient.setLiveStreamingListener(mLiveStreamingListener);
        mClient.setNetworkQualityListener(mNetworkQualityListener);
    }

    /**
     * 初始化本地音视频 track
     * 关于 Track 的概念介绍 https://doc.qnsdk.com/rtn/android/docs/preparation#5
     */
    private void initLocalTracks() {
        mLocalTrackList = new ArrayList<>();
        QNMicrophoneAudioTrackConfig microphoneAudioTrackConfig = new QNMicrophoneAudioTrackConfig(TRACK_TAG_MIC);
        microphoneAudioTrackConfig.setAudioQuality(QNAudioQualityPreset.STANDARD);
        mMicrophoneTrack = QNRTC.createMicrophoneAudioTrack(microphoneAudioTrackConfig);
        mMicrophoneTrack.setMicrophoneEventListener((errorCode, errorMessage) -> mMicrophoneError = true);

        mLocalTrackList.add(mMicrophoneTrack);

        SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        int videoDegradation = preferences.getInt(Config.VIDEO_DEGRADATION_POS, Config.DEFAULT_VIDEO_DEGRADATION_POS);
        switch (mCaptureMode) {
            case Config.CAMERA_CAPTURE:
                // 创建 Camera 采集的视频 Track
                QNCameraVideoTrackConfig cameraVideoTrackConfig = new QNCameraVideoTrackConfig(TRACK_TAG_CAMERA)
                        .setCameraFacing(QNCameraFacing.FRONT)
                        .setVideoCaptureConfig(new QNVideoCaptureConfig(mVideoWidth, mVideoHeight, mVideoFps))
                        .setVideoEncoderConfig(new QNVideoEncoderConfig(mVideoWidth, mVideoHeight, mVideoFps, mVideoBitrate,
                                Config.VIDEO_DEGRADATION_PRESET[videoDegradation]));
                mCameraTrack = QNRTC.createCameraVideoTrack(cameraVideoTrackConfig);
                mCameraTrack.setCameraEventListener(mCameraEventListener);
                mLocalTrackList.add(mCameraTrack);
                break;
            case Config.ONLY_AUDIO_CAPTURE:
                mControlFragment.setAudioOnly(true);
                break;
            case Config.SCREEN_CAPTURE:
                // 创建屏幕录制的视频 Track
                createScreenTrack();
                mControlFragment.setAudioOnly(true);
                break;
            case Config.MUTI_TRACK_CAPTURE:
                // 视频通话 + 屏幕共享两路 track
                createScreenTrack();
                QNCameraVideoTrackConfig videoTrackConfig = new QNCameraVideoTrackConfig(TRACK_TAG_CAMERA)
                        .setCameraFacing(QNCameraFacing.FRONT)
                        .setVideoCaptureConfig(new QNVideoCaptureConfig(mVideoWidth, mVideoHeight, mVideoFps))
                        .setVideoEncoderConfig(new QNVideoEncoderConfig(mVideoWidth, mVideoHeight, mVideoFps, mVideoBitrate,
                                Config.VIDEO_DEGRADATION_PRESET[videoDegradation]));
                mCameraTrack = QNRTC.createCameraVideoTrack(videoTrackConfig);
                mCameraTrack.setCameraEventListener(mCameraEventListener);
                mLocalTrackList.add(mCameraTrack);
                break;
            default:
                break;
        }
    }

    // 录屏时建议分辨率和屏幕分辨率比例保存一致，避免录屏画面有黑边或者不清晰
    private QNVideoEncoderConfig createScreenEncoderConfig() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        int videoDegradation = preferences.getInt(Config.VIDEO_DEGRADATION_POS, Config.DEFAULT_VIDEO_DEGRADATION_POS);
        int width = (int) (size.x * Config.DEFAULT_SCREEN_VIDEO_TRACK_SIZE_SCALE);
        int height = (int) (size.y * Config.DEFAULT_SCREEN_VIDEO_TRACK_SIZE_SCALE);
        int bitrate = (int) (width * height * 1.0f / Config.DEFAULT_RESOLUTION[1][0] /
                Config.DEFAULT_RESOLUTION[1][1] * Config.DEFAULT_BITRATE[1]);
        return new QNVideoEncoderConfig(width, height,  Config.DEFAULT_FPS[0], bitrate, Config.VIDEO_DEGRADATION_PRESET[videoDegradation]);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            QNScreenVideoTrackConfig screenVideoTrackConfig = new QNScreenVideoTrackConfig(TRACK_TAG_SCREEN)
                    .setVideoEncoderConfig(createScreenEncoderConfig());
            mLocalScreenTrack = QNRTC.createScreenVideoTrack(screenVideoTrackConfig);
            mLocalTrackList.add(mLocalScreenTrack);
            if (mClient != null && (mClient.getConnectionState() == QNConnectionState.CONNECTED
                                   || mClient.getConnectionState() == QNConnectionState.RECONNECTED)) {
                mClient.publish(mPublishResultCallback, Collections.singletonList(mLocalScreenTrack));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    // 处理 Build.VERSION_CODES.Q 及以上的兼容问题
    private void createScreenTrack() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent intent = new Intent(this, ForegroundService.class);
            startForegroundService(intent);
            bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
            Log.i(TAG, "start service for Q");
        } else {
            QNScreenVideoTrackConfig screenVideoTrackConfig = new QNScreenVideoTrackConfig(TRACK_TAG_SCREEN)
                    .setVideoEncoderConfig(createScreenEncoderConfig());
            mLocalScreenTrack = QNRTC.createScreenVideoTrack(screenVideoTrackConfig);
            mLocalTrackList.add(mLocalScreenTrack);
        }
    }

    /**
     * 合流转推、单路转推相关处理
     */
    private void initMergeLayoutConfig() {
        mMergeLayoutConfigView = new MergeLayoutConfigView(this);
        mMergeLayoutConfigView.setRoomId(mRoomId);
        mUserListAdapter = new UserListAdapter();
        mRoomMergeOption = new RTCRoomMergeOption();
        mMergeLayoutConfigView.getUserListView().setAdapter(mUserListAdapter);
        mMergeLayoutConfigView.setOnClickedListener(() -> {
            // 保存当前用户选择的配置信息
            mMergeLayoutConfigView.updateMergeOptions();

            if (mClient == null) {
                return;
            }
            if (!mMergeLayoutConfigView.isStreamingEnabled()) {
                // 处理停止合流逻辑
                if (mIsMergeStreaming) {
                    // 如果正在推流，则停止之前的合流转推
                    mClient.stopLiveStreaming(mCurrentMergeConfig);
                    mIsMergeStreaming = false;
                    ToastUtils.showShortToast(RoomActivity.this, "停止合流！！！");
                } else {
                    ToastUtils.showShortToast(RoomActivity.this, "未开启合流，配置未生效！！！");
                }
                if (mPopWindow != null) {
                    mPopWindow.dismiss();
                }
                return;
            }
            // 如果当前正在进行单路转推，那么切换到合流转推的时候，务必使用自定义合流配置，否则可能会出现抢流的现象
            if (mIsDirectStreaming && !mMergeLayoutConfigView.isCustomMerge()) {
                Utils.showAlertDialog(RoomActivity.this, getString(R.string.create_merge_warning));
                mMergeLayoutConfigView.updateStreamingStatus(false);
                if (mPopWindow != null) {
                    mPopWindow.dismiss();
                }
                return;
            }
            if (mMergeLayoutConfigView.isCustomMerge()) {
                // 处理自定义合流转推的逻辑
                QNTranscodingLiveStreamingConfig mergeConfig = mMergeLayoutConfigView.getCustomMergeConfig();
                if (mergeConfig != null) {
                    // 如果正在推流，则停止之前的合流转推
                    if (mIsMergeStreaming) {
                        mClient.stopLiveStreaming(mCurrentMergeConfig);
                    }
                    mCurrentMergeConfig = mergeConfig;
                    // 开始自定义合流转推
                    mClient.startLiveStreaming(mCurrentMergeConfig);
                } else {
                    // 更新合流布局到自定义合流配置
                    setMergeStreamLayouts();
                }
            } else {
                // 更新合流布局到默认合流配置
                mCurrentMergeConfig = new QNTranscodingLiveStreamingConfig();
                setMergeStreamLayouts();
            }
            if (mPopWindow != null) {
                mPopWindow.dismiss();
            }
        });
    }

    private void logAndToast(final String msg) {
        Log.d(TAG, msg);
        if (mLogToast != null) {
            mLogToast.cancel();
        }
        mLogToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        mLogToast.show();
    }

    private void disconnectWithErrorMessage(final String errorMessage) {
        new AlertDialog.Builder(this)
                .setTitle(getText(R.string.channel_error_title))
                .setMessage(errorMessage)
                .setCancelable(false)
                .setNeutralButton(R.string.ok, (dialog, id) -> {
                    dialog.cancel();
                    finish();
                })
                .create()
                .show();
    }

    private void showKickoutDialog(final String userId) {
        if (mKickOutDialog == null) {
            mKickOutDialog = new AlertDialog.Builder(this)
                    .setNegativeButton(R.string.negative_dialog_tips, null)
                    .create();
        }
        mKickOutDialog.setMessage(getString(R.string.kickout_tips, userId));
        mKickOutDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.positive_dialog_tips), (dialog, which) -> {
            if (mClient != null) {
                mClient.sendMessage(Collections.singletonList(userId),CUSTOM_MESSAGE_KICKOUT,CUSTOM_MESSAGE_KICKOUT);
            }
        });
        mKickOutDialog.show();
    }

    private void updateRemoteLogText(final String logText) {
        Log.i(TAG, logText);
        mControlFragment.updateRemoteLogText(logText);
    }

    /**
     * 当新的本地、远端 Track 变化时，重新排列合流画面配置
     */
    private void resetMergeStream() {
        Log.d(TAG, "resetMergeStream()");

        // video tracks merge layout options.
        List<RTCTrackMergeOption> roomVideoTrackList = mRoomMergeOption.getVideoMergeOptions();
        boolean isDefaultStreaming = (mCurrentMergeConfig == null || TextUtils.isEmpty(mCurrentMergeConfig.getStreamID()));
        if (!roomVideoTrackList.isEmpty()) {
            List<QNTranscodingLiveStreamingTrack> mergeTrackOptions = SplitUtils.split(
                    roomVideoTrackList.size(),
                    isDefaultStreaming ?
                            QNAppServer.STREAMING_WIDTH
                            : mCurrentMergeConfig.getWidth(),
                    isDefaultStreaming ?
                            QNAppServer.STREAMING_HEIGHT
                            : mCurrentMergeConfig.getHeight()
            );
            if (mergeTrackOptions.size() != roomVideoTrackList.size()) {
                Log.e(TAG, "split option error.");
                return;
            }

            for (int i = 0; i < mergeTrackOptions.size(); i++) {
                RTCTrackMergeOption trackMergeOption = roomVideoTrackList.get(i);

                if (!trackMergeOption.isTrackInclude()) {
                    continue;
                }
                QNTranscodingLiveStreamingTrack item = mergeTrackOptions.get(i);
                trackMergeOption.updateMergeTrack(item);
            }
        }

        if (mIsMergeStreaming) {
            updateMergeTrack();
        }
    }

    private void userJoinedForStreaming(String userId, String userData) {
        mRoomMergeOption.onUserJoined(userId, userData);
        if (mUserListAdapter != null) {
            mUserListAdapter.notifyDataSetChanged();
        }
    }

    private void userLeftForStreaming(String userId, boolean localLeft) {
        if (localLeft) {
            mRoomMergeOption.onUserLeft();
        } else {
            mRoomMergeOption.onUserLeft(userId);
        }
        if (mUserListAdapter != null) {
            mUserListAdapter.notifyDataSetChanged();
        }
    }

    private int updateSerialNum() {
        mMergeLayoutConfigView.updateSerialNum(++mSerialNum);
        return mSerialNum;
    }

    /**
     * 配置各个用户当前选中的 Track 信息到合流布局
     */
    private void setMergeStreamLayouts() {
        updateMergeTrack();
        mIsMergeStreaming = true;
        ToastUtils.showShortToast(RoomActivity.this, "已发送合流配置，请等待合流画面生效");
    }

    private void updateMergeTrack() {
        List<RTCTrackMergeOption> userOptions = new ArrayList<>();
        for (int user = 0; user < mRoomMergeOption.size(); user++) {
            RTCUserMergeOptions userMergeOptions = mRoomMergeOption.getUserMergeOptionByPosition(user);
            if (userMergeOptions.getAudioMergeOption() != null) {
                userOptions.add(userMergeOptions.getAudioMergeOption());
            }
            if (userMergeOptions.getVideoMergeOptions().size() > 0) {
                userOptions.addAll(userMergeOptions.getVideoMergeOptions());
            }
        }

        List<QNTranscodingLiveStreamingTrack> mergeTracks = new ArrayList<>();
        List<QNTranscodingLiveStreamingTrack> removedTracks = new ArrayList<>();
        for (RTCTrackMergeOption item : userOptions) {
            if (item.isTrackInclude()) {
                mergeTracks.add(item.getMergeTrack());
            } else {
                removedTracks.add(item.getMergeTrack());
            }
        }
        if (!mergeTracks.isEmpty()) {
            // 配置对应 Track 的合流配置信息
            if (mMergeLayoutConfigView.isCustomMerge()) {
                mClient.setTranscodingLiveStreamingTracks(mCurrentMergeConfig.getStreamID(), mergeTracks);
            } else {
                mClient.setTranscodingLiveStreamingTracks(null, mergeTracks);
            }
        }
        if (!removedTracks.isEmpty()) {
            // 移除对应 Track 的合流配置，移除后相应 Track 的数据将不会参与合流
            if (mMergeLayoutConfigView.isCustomMerge()) {
                mClient.removeTranscodingLiveStreamingTracks(mCurrentMergeConfig.getStreamID(), removedTracks);
            } else {
                mClient.removeTranscodingLiveStreamingTracks(null, removedTracks);
            }
        }
    }

    @Override
    public void onCallHangUp() {
        releaseClient();
        finish();
    }

    @Override
    public void onCameraSwitch() {
        if (mCameraTrack != null) {
            mCameraTrack.switchCamera(new QNCameraSwitchResultCallback() {
                @Override
                public void onSwitched(boolean isFrontCamera) {
                }

                @Override
                public void onError(String errorMessage) {
                }
            });
        }
    }

    @Override
    public boolean onToggleMic() {
        if (mMicrophoneTrack != null) {
            mMicEnabled = !mMicEnabled;
            mMicrophoneTrack.setMuted(!mMicEnabled);
            if (mTrackWindowManager != null) {
                mTrackWindowManager.onTrackMuted(mUserId);
            }
        }
        return mMicEnabled;
    }

    @Override
    public boolean onToggleVideo() {
        if (mCameraTrack != null) {
            mVideoEnabled = !mVideoEnabled;
            mCameraTrack.setMuted(!mVideoEnabled);
            if (mLocalScreenTrack != null) {
                mLocalScreenTrack.setMuted(!mVideoEnabled);
            }
            mCameraTrack.setMuted(!mVideoEnabled);
            if (mTrackWindowManager != null) {
                mTrackWindowManager.onTrackMuted(mUserId);
            }
        }
        return mVideoEnabled;
    }

    @Override
    public boolean onToggleSpeaker() {
        if (mClient != null) {
            mSpeakerEnabled = !mSpeakerEnabled;
            QNRTC.setSpeakerphoneMuted(!mSpeakerEnabled);
        }
        return mSpeakerEnabled;
    }

    @Override
    public boolean onToggleBeauty() {
        if (mCameraTrack != null) {
            mBeautyEnabled = !mBeautyEnabled;
            QNBeautySetting beautySetting = new QNBeautySetting(0.5f, 0.5f, 0.5f);
            beautySetting.setEnable(mBeautyEnabled);
            mCameraTrack.setBeauty(beautySetting);
        }
        return mBeautyEnabled;
    }

    @Override
    public void onCallMerge() {
        if (!mIsAdmin) {
            ToastUtils.showShortToast(RoomActivity.this, "只有 \"admin\" 用户可以开启合流转推！！！");
            return;
        }
        // 配置页
        if (mRoomMergeOption.size() == 0) {
            return;
        }
        mMergeOption = mRoomMergeOption.getUserMergeOptionByPosition(0);
        mMergeLayoutConfigView.updateConfigInfo(mMergeOption);
        mMergeLayoutConfigView.updateMergeConfigInfo();
        mUserListAdapter.notifyDataSetChanged();

        if (mPopWindow == null) {
            mPopWindow = new PopupWindow(mMergeLayoutConfigView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
            mPopWindow.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.popupWindowBackground)));
        }
        mPopWindow.showAtLocation(getWindow().getDecorView().getRootView(), Gravity.BOTTOM, 0, 0);
    }

    @Override
    public void onToggleDirectLiving() {
        if (!mIsAdmin) {
            ToastUtils.showShortToast(RoomActivity.this, "只有 \"admin\" 用户可以开启单流转推！！！");
            return;
        }
        if (!mIsDirectStreaming) {
            // 如果当前正在进行默认配置合流转推，则不允许切换成单路转推，需要停止默认配置合流转推或者使用自定义合流转推
            if (mIsMergeStreaming && mCurrentMergeConfig == null) {
                Utils.showAlertDialog(this, getString(R.string.create_direct_warning));
                return;
            }
            if (mCurrentDirectConfig == null) {
                mCurrentDirectConfig = new QNDirectLiveStreamingConfig();
                mCurrentDirectConfig.setStreamID(mRoomId);
                mCurrentDirectConfig.setAudioTrack(mMicrophoneTrack);
                switch (mCaptureMode) {
                    case Config.CAMERA_CAPTURE:
                    case Config.MUTI_TRACK_CAPTURE:
                        mCurrentDirectConfig.setVideoTrack(mCameraTrack);
                        break;
                    case Config.SCREEN_CAPTURE:
                        mCurrentDirectConfig.setVideoTrack(mLocalScreenTrack);
                        break;
                    default:
                        break;
                }
            }
            mCurrentDirectConfig.setUrl(String.format(getResources().getString(R.string.publish_url), mRoomId, mSerialNum));
            if (mClient != null) {
              mClient.startLiveStreaming(mCurrentDirectConfig);
            }
        } else {
            if (mClient != null) {
              mClient.stopLiveStreaming(mCurrentDirectConfig);
              mIsDirectStreaming = false;
              mControlFragment.updateDirectText(getString(R.string.direct_btn_text));
              ToastUtils.showShortToast(RoomActivity.this, "已停止 id=" + mCurrentDirectConfig.getStreamID() + " 的单流转推！！！");
            }
        }
    }

    private final TimerTask mUpdateNetWorkQualityInfoTask = new TimerTask() {
        @Override
        public void run() {
            if (mClient != null) {
                Map<String, QNNetworkQuality> qualityMap = mClient.getUserNetworkQuality();
                for (Map.Entry<String, QNNetworkQuality> entry : qualityMap.entrySet()) {
                    Log.i(TAG, "remote user " + entry.getKey() + " " + entry.getValue().toString());
                }
            }
        }
    };

    /**
     * 用户合流配置相关
     */
    private class UserListAdapter extends RecyclerView.Adapter<ViewHolder> {
        int[] mColor = {
                Color.parseColor("#588CEE"),
                Color.parseColor("#F8CF5F"),
                Color.parseColor("#4D9F67"),
                Color.parseColor("#F23A48")
        };

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(getApplicationContext()).inflate(R.layout.item_user, parent, false));
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            RTCUserMergeOptions RTCUserMergeOptions = mRoomMergeOption.getUserMergeOptionByPosition(position);
            String userId = RTCUserMergeOptions.getUserID();
            holder.username.setText(userId);
            holder.username.setCircleColor(mColor[position % 4]);
            if (mMergeOption != null && mMergeOption.getUserID().equals(userId)) {
                holder.itemView.setBackground(getResources().getDrawable(R.drawable.white_background));
            } else {
                holder.itemView.setBackgroundResource(0);
            }
            holder.itemView.setOnClickListener(v -> {
                mMergeOption = mRoomMergeOption.getUserMergeOptionByPosition(holder.getAdapterPosition());
                mMergeLayoutConfigView.updateConfigInfo(mMergeOption);
                notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return mRoomMergeOption.size();
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        CircleTextView username;

        private ViewHolder(View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.user_name_text);
        }
    }

    private final QNRTCEventListener mRTCEventListener = new QNRTCEventListener() {
        /**
         * 当音频路由发生变化时会回调此方法
         *
         * @param device 音频设备, 详情请参考{@link QNAudioDevice}
         */
        @Override
        public void onAudioRouteChanged(QNAudioDevice device) {
            updateRemoteLogText("onAudioRouteChanged: " + device.name());
        }
    };

    private final QNClientEventListener mClientEventListener = new QNClientEventListener() {
        /**
         * 连接状态改变时会回调此方法
         * 连接状态回调只需要做提示用户，或者更新相关 UI； 不需要再做加入房间或者重新发布等其他操作！
         * @param state 连接状态，可参考 {@link QNConnectionState}
         */
        @Override
        public void onConnectionStateChanged(QNConnectionState state, @Nullable QNConnectionDisconnectedInfo info) {
            Log.i(TAG, "onConnectionStateChanged:" + state.name());
            switch (state) {
                case DISCONNECTED:
                    /**
                     *【TOKEN 相关】
                     * 1. {@link QNErrorCode.ERROR_TOKEN_ERROR} 表示您提供的房间 token 不符合七牛 token 签算规则,
                     *    详情请参考【服务端开发说明.RoomToken 签发服务】https://doc.qnsdk.com/rtn/docs/server_overview#1
                     * 2. {@link QNErrorCode.ERROR_TOKEN_EXPIRED} 表示您的房间 token 过期, 需要重新生成 token 再加入；
                     *
                     *【房间设置相关】以下情况可以与您的业务服务开发确认具体设置
                     * 1. {@link QNErrorCode.ERROR_ROOM_FULL} 当房间已加入人数超过每个房间的人数限制触发；请确认后台服务的设置；
                     * 2. {@link QNErrorCode.ERROR_PLAYER_ALREADY_EXIST} 后台如果配置为开启【禁止自动踢人】,则同一用户重复加入/未正常退出再加入会触发此错误，您的业务可根据实际情况选择配置；
                     * 3. {@link QNErrorCode.ERROR_NO_PERMISSION} 用户对于特定操作，如合流需要配置权限，禁止出现未授权的用户操作；
                     * 4. {@link QNErrorCode.ERROR_ROOM_CLOSED} 房间已被管理员关闭；
                     *
                     *【其他错误】
                     * 1. {@link QNErrorCode.ERROR_AUTH_FAIL} 服务验证时出错，可能为服务网络异常。建议重新尝试加入房间；
                     * 2. {@link QNErrorCode.ERROR_PUBLISH_FAIL} 发布失败, 会有如下3种情况:
                     * 1 ）请确认成功加入房间后，再执行发布操作
                     * 2 ）请确定对于音频/视频 Track，分别最多只能有一路为 master
                     * 3 ）请确认您的网络状况是否正常
                     * 3. {@link QNErrorCode.ERROR_RECONNECT_TOKEN_ERROR} 内部重连后出错，一般出现在网络非常不稳定时出现，建议提示用户并尝试重新加入房间；
                     *    另外，当前用户之前进行的合流转推、单路转推的服务将会被销毁，重新加入房间后应该重新开始合流转推、单路转推 ！！！
                     * 4. {@link QNErrorCode.ERROR_INVALID_PARAMETER} 服务交互参数错误，请在开发时注意合流、踢人动作等参数的设置。
                     * 5. {@link QNErrorCode.ERROR_DEVICE_CAMERA} 系统摄像头错误, 建议提醒用户检查
                     */
                    if (info == null || info.getReason() != QNConnectionDisconnectedInfo.Reason.ERROR) {
                        return;
                    }
                    switch (info.getErrorCode()) {
                        case QNErrorCode.ERROR_AUTH_FAILED:
                            logAndToast("服务验证时出错，可能为服务网络异常");
                            mClient.join(mRoomToken);
                            break;
                        case QNErrorCode.ERROR_TOKEN_ERROR:
                            logAndToast("roomToken 错误，请检查后重新生成，再加入房间");
                            break;
                        case QNErrorCode.ERROR_TOKEN_EXPIRED:
                            logAndToast("roomToken过期");
                            mRoomToken = QNAppServer.getInstance().requestRoomToken(RoomActivity.this, mUserId, mRoomId);
                            mClient.join(mRoomToken);
                            break;
                        case QNErrorCode.ERROR_PLAYER_ALREADY_EXIST:
                            logAndToast("不允许同一用户重复加入");
                            break;
                        case QNErrorCode.ERROR_MEDIA_CAP_NOT_SUPPORT:
                            logAndToast("该设备不支持指定的音视频格式，无法进行连麦");
                            break;
                        case QNErrorCode.ERROR_FATAL:
                            logAndToast("非预期错误");
                            finish();
                            break;
                        case QNErrorCode.ERROR_RECONNECT_FAILED:
                            logAndToast("内部重连失败");
                            mClient.join(mRoomToken);
                        default:
                            logAndToast("errorCode:" + info.getErrorCode() + " description:" + info.getErrorMessage());
                            break;
                    }
                    if (mIsAdmin) {
                        userLeftForStreaming(mUserId, true);
                    } else if (info.getReason() == QNConnectionDisconnectedInfo.Reason.KICKED_OUT) {
                        ToastUtils.showShortToast(RoomActivity.this, getString(R.string.kicked_by_admin));
                        finish();
                    }
                    break;
                case RECONNECTING:
                    logAndToast(getString(R.string.reconnecting_to_room));
                    mControlFragment.stopTimer();
                    break;
                case CONNECTED:
                    if (mIsAdmin) {
                        userJoinedForStreaming(mUserId, "");
                    }
                    // 加入房间后可以进行 tracks 的发布
                    mClient.publish(mPublishResultCallback, mLocalTrackList);
                    logAndToast(getString(R.string.connected_to_room));
                    mIsJoinedRoom = true;
                    mControlFragment.startTimer();

                    // 重连失败后再次加入房间后，恢复无效的合流转推
                    if (mIsMergeStreaming && mMergeLayoutConfigView.isCustomMerge() && !mMergeLayoutConfigView.isMergeConfigValid()) {
                        QNTranscodingLiveStreamingConfig mergeConfig = mMergeLayoutConfigView.getCustomMergeConfig();
                        if (mergeConfig != null) {
                            mCurrentMergeConfig = mergeConfig;
                            // 开始自定义合流转推
                            mClient.startLiveStreaming(mCurrentMergeConfig);
                        }
                    }
                    break;
                case RECONNECTED:
                    logAndToast(getString(R.string.connected_to_room));
                    mControlFragment.startTimer();
                    break;
                case CONNECTING:
                    logAndToast(getString(R.string.connecting_to, mRoomId));
                    break;
                default:
                    break;
            }
        }

        /**
         * 远端用户加入房间时会回调此方法
         * @see QNRTCClient#join(String, String) 可指定 userData 字段
         *
         * @param remoteUserID 远端用户的 userId
         * @param userData 透传字段，用户自定义内容
         */
        @Override
        public void onUserJoined(String remoteUserID, String userData) {
            updateRemoteLogText("onRemoteUserJoined:remoteUserId = " + remoteUserID + " ,userData = " + userData);
            if (mIsAdmin) {
                userJoinedForStreaming(remoteUserID, userData);
            }
        }

        @Override
        public void onUserReconnecting(String remoteUserID) {
            logAndToast("远端用户: " + remoteUserID + " 重连中");
        }

        @Override
        public void onUserReconnected(String remoteUserID) {
            logAndToast("远端用户: " + remoteUserID + " 重连成功");
        }

        /**
         * 远端用户离开房间时会回调此方法
         *
         * @param remoteUserID 远端离开用户的 userId
         */
        @Override
        public void onUserLeft(String remoteUserID) {
            updateRemoteLogText("onRemoteUserLeft:remoteUserId = " + remoteUserID);
            if (mIsAdmin) {
                userLeftForStreaming(remoteUserID, false);
            }
        }

        /**
         * 远端用户 tracks 成功发布时会回调此方法
         *
         * @param remoteUserID 远端用户 userId
         * @param trackList 远端用户发布的 tracks 列表
         */
        @Override
        public void onUserPublished(String remoteUserID, List<QNRemoteTrack> trackList) {
            updateRemoteLogText("onRemotePublished:remoteUserId = " + remoteUserID);
            mRoomMergeOption.onTracksPublished(remoteUserID, new ArrayList<>(trackList));
            // 如果希望在远端发布音视频的时候，自动配置合流，则可以在此处重新调用 setMergeStreamLayouts 进行配置
            if (mIsAdmin) {
                resetMergeStream();
            }
        }

        /**
         * 远端用户 tracks 成功取消发布时会回调此方法
         *
         * @param remoteUserID 远端用户 userId
         * @param remoteTracks 远端用户取消发布的 tracks 列表
         */
        @Override
        public void onUserUnpublished(String remoteUserID, List<QNRemoteTrack> remoteTracks) {
            updateRemoteLogText("onRemoteUnpublished:remoteUserId = " + remoteUserID);
            List<QNTrack> trackList = new ArrayList<>(remoteTracks);
            if (mTrackWindowManager != null) {
                mTrackWindowManager.removeTrack(remoteUserID, trackList);
            }
            mRoomMergeOption.onTracksUnPublished(remoteUserID, trackList);
            if (mIsAdmin) {
                resetMergeStream();
            }
        }

        /**
         * 订阅远端用户 Track 成功时会回调此方法
         *
         * @param remoteUserID 远端用户 userId
         * @param remoteAudioTracks 订阅的远端用户音频 tracks 列表
         * @param remoteVideoTracks 订阅的远端用户视频 tracks 列表
         */
        @Override
        public void onSubscribed(String remoteUserID, List<QNRemoteAudioTrack> remoteAudioTracks, List<QNRemoteVideoTrack> remoteVideoTracks) {
            updateRemoteLogText("onSubscribed:remoteUserId = " + remoteUserID);
            if (mTrackWindowManager != null) {
                List<QNTrack> tracks = new ArrayList<>();
                tracks.addAll(remoteAudioTracks);
                tracks.addAll(remoteVideoTracks);
                mTrackWindowManager.addTrack(remoteUserID, tracks);
                for (QNTrack track : tracks) {
                    ((QNRemoteTrack)track).setTrackInfoChangedListener(new QNTrackInfoChangedListener() {
                        @Override
                        public void onMuteStateChanged(boolean isMuted) {
                            updateRemoteLogText("onRemoteUserMuted:remoteUserId = " + remoteUserID);
                            if (mTrackWindowManager != null) {
                                mTrackWindowManager.onTrackMuted(remoteUserID);
                            }
                        }
                    });
                }
            }
        }

        /**
         * 当收到自定义消息时回调此方法
         *
         * @param message 自定义信息，详情请参考 {@link QNCustomMessage}
         */
        @Override
        public void onMessageReceived(QNCustomMessage message) {
            if (message.getContent().equals(CUSTOM_MESSAGE_KICKOUT)){
                ToastUtils.showShortToast(RoomActivity.this, "您被踢出房间！");
                finish();
            }
        }

        /**
         * 跨房媒体转发状态改变时会回调此方法
         *
         * @param relayRoom 媒体转发的房间名
         * @param state 媒体转发的状态
         */
        @Override
        public void onMediaRelayStateChanged(String relayRoom, QNMediaRelayState state) {

        }

        /**
         * 用户音量提示回调，本地远端一起回调，本地 user id 为空
         *
         * @param userVolumeList 用户音量信息，按音量由高到低排序，静音用户不在此列表中体现。
         */
        @Override
        public void onUserVolumeIndication(List<QNAudioVolumeInfo> userVolumeList) {

        }
    };

    private final QNCameraEventListener mCameraEventListener = new QNCameraEventListener() {
        @Override
        public int[] onCameraOpened(List<Size> sizes, List<Integer> fpsAscending) {
            SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
            int videoWidth = preferences.getInt(Config.WIDTH, DEFAULT_RESOLUTION[1][0]);
            int videoHeight = preferences.getInt(Config.HEIGHT, DEFAULT_RESOLUTION[1][1]);
            int fps = preferences.getInt(Config.FPS, DEFAULT_FPS[1]);

            // 根据设备能力选择匹配的采集参数
            int wantSize = -1; // 选择的分辨率下标, -1 表示不做选择, 使用 QNCameraVideoTrackConfig 的设置
            int wantFps = -1;  // 选择的帧率下标, -1 表示不做选择, 使用 QNCameraVideoTrackConfig 的设置
            /**
             * 以下代码仅示例：
             * 当硬件可用分辨率和当前设置宽高一致时，直接选择该分辨率；当没有完全一致的宽高时，选择使用高度匹配的一个分辨率;
             * 您也可以根据自己业务需要，选择宽高最接近的一个分辨率或其他匹配方式。
             *
             * 如果没有需要，也可以返回 -1 由 SDK 根据设置来匹配接近的分辨率。
             */
            for (int i = 0; i < sizes.size(); i++) {
                if (sizes.get(i).height == videoHeight) {
                    wantSize = i;
                    if (sizes.get(i).width == videoWidth) {
                        break;
                    }
                }
            }
            for (int value : fpsAscending) {
                if (fps == value) {
                    wantFps = fps;
                    break;
                }
            }
            return new int[]{wantSize, wantFps};
        }

        @Override
        public void onCaptureStarted() {

        }

        @Override
        public void onCaptureStopped() {
            mCaptureStoppedSem.drainPermits();
            mCaptureStoppedSem.release();
        }

        @Override
        public void onError(int errorCode, String description) {

        }

        @Override
        public void onPushImageError(int errorCode, String errorMessage) {

        }
    };

    private final QNPublishResultCallback mPublishResultCallback = new QNPublishResultCallback() {
        /**
         * 本地 Track 成功发布时会回调
         */
        @Override
        public void onPublished() {
            updateRemoteLogText("onLocalPublished");
            if (mIsAdmin) {
                mRoomMergeOption.onTracksPublished(mUserId, new ArrayList<>(mLocalTrackList));
                resetMergeStream();
            }
        }

        /**
         * 本地 Track 发布失败时回调
         *
         * @param errorCode    错误码
         * @param errorMessage 详细错误信息
         */
        @Override
        public void onError(int errorCode, String errorMessage) {

        }
    };

    private final QNLiveStreamingListener mLiveStreamingListener = new QNLiveStreamingListener() {
        /**
         * 当自定义合流任务和直接转推任务开启成功的时候会回调此方法
         *
         * @param streamID 转推成功的 streamID
         */
        @Override
        public void onStarted(String streamID) {
            if (mCurrentMergeConfig != null && mCurrentMergeConfig.getStreamID() != null &&
                mCurrentMergeConfig.getStreamID().equals(streamID)) {
                updateSerialNum();
                ToastUtils.showShortToast(RoomActivity.this, "合流转推 " + streamID + " 创建成功！");
                setMergeStreamLayouts();

                // 取消单路转推
                if (mIsDirectStreaming) {
                    // 注意：A 房间中开始的单路转推，只能在 A 房间中进行停止，无法在其他房间中停止
                    mClient.stopLiveStreaming(mCurrentDirectConfig);
                    mIsDirectStreaming = false;
                    mControlFragment.updateDirectText(getString(R.string.direct_btn_text));
                }
            } else {
                updateSerialNum();
                mControlFragment.updateDirectText(getString(R.string.stop_direct_text));
                ToastUtils.showShortToast(RoomActivity.this, "单路转推 " + streamID + " 创建成功！");
                mIsDirectStreaming = true;

                // 取消合流转推
                if (mIsMergeStreaming && mCurrentMergeConfig != null) {
                    // 注意：A 房间中开始的合流转推，只能在 A 房间中进行停止，无法在其他房间中停止
                    mClient.stopLiveStreaming(mCurrentMergeConfig);
                    mIsMergeStreaming = false;
                    mMergeLayoutConfigView.updateStreamingStatus(false);
                    mMergeLayoutConfigView.updateMergeConfigValid(false);
                }
            }
        }

        @Override
        public void onStopped(String streamID) {
        }

        @Override
        public void onTranscodingTracksUpdated(String streamID) {

        }

        @Override
        public void onError(String streamID, QNLiveStreamingErrorInfo errorInfo) {

        }
    };

    private final QNNetworkQualityListener mNetworkQualityListener = new QNNetworkQualityListener() {
        /**
         * 当网络质量更新时会回调此方法
         *
         * {@link QNNetworkQuality#uplinkNetworkGrade} 代表上行网络质量
         * {@link QNNetworkQuality#downlinkNetworkGrade} 代表下行网络质量
         * 可以用来向用户提示自己网络状态不佳（比如连续一段时间网络质量为 {@link com.qiniu.droid.rtc.QNNetworkGrade#POOR}）。
         *
         * @param quality 网络质量，详情请参考 {@link QNNetworkQuality}
         */
        @Override
        public void onNetworkQualityNotified(QNNetworkQuality quality) {
            runOnUiThread(() -> mControlFragment.updateLocalVideoLogText(quality.toString()));
        }
    };
}
