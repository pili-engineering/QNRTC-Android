package com.qiniu.droid.rtc.api.examples.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Switch;

import com.qiniu.droid.rtc.QNAudioQualityPreset;
import com.qiniu.droid.rtc.QNBeautySetting;
import com.qiniu.droid.rtc.QNCameraEventListener;
import com.qiniu.droid.rtc.QNCameraFacing;
import com.qiniu.droid.rtc.QNCameraVideoTrack;
import com.qiniu.droid.rtc.QNCameraVideoTrackConfig;
import com.qiniu.droid.rtc.QNClientEventListener;
import com.qiniu.droid.rtc.QNConnectionDisconnectedInfo;
import com.qiniu.droid.rtc.QNConnectionState;
import com.qiniu.droid.rtc.QNCustomMessage;
import com.qiniu.droid.rtc.QNMediaRelayState;
import com.qiniu.droid.rtc.QNMicrophoneAudioTrack;
import com.qiniu.droid.rtc.QNMicrophoneAudioTrackConfig;
import com.qiniu.droid.rtc.QNPublishResultCallback;
import com.qiniu.droid.rtc.QNRTC;
import com.qiniu.droid.rtc.QNRTCClient;
import com.qiniu.droid.rtc.QNRTCEventListener;
import com.qiniu.droid.rtc.QNRemoteAudioTrack;
import com.qiniu.droid.rtc.QNRemoteTrack;
import com.qiniu.droid.rtc.QNRemoteVideoTrack;
import com.qiniu.droid.rtc.QNSurfaceView;
import com.qiniu.droid.rtc.QNVideoCaptureConfigPreset;
import com.qiniu.droid.rtc.QNVideoEncoderConfig;
import com.qiniu.droid.rtc.api.examples.APIApplication;
import com.qiniu.droid.rtc.api.examples.R;
import com.qiniu.droid.rtc.api.examples.utils.Config;
import com.qiniu.droid.rtc.api.examples.utils.ToastUtils;
import com.qiniu.droid.rtc.model.QNAudioDevice;

import org.qnwebrtc.Size;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

/**
 * 1v1 摄像头 + 麦克风音视频通话场景
 *
 * 主要步骤如下：
 * 1. 初始化视图
 * 2. 初始化 RTC
 * 3. 创建 QNRTCClient 对象
 * 4. 创建本地音视频 Track
 * 5. 加入房间
 * 6. 发布本地音视频 Track
 * 7. 订阅远端音视频 Track
 * 8. 离开房间
 * 9. 反初始化 RTC 释放资源
 *
 * 文档参考：
 * - 音视频通话中的基本概念，请参考 https://developer.qiniu.com/rtc/9909/the-rtc-basic-concept
 * - 接口文档，请参考 https://developer.qiniu.com/rtc/8773/API%20%E6%A6%82%E8%A7%88
 */
public class CameraMicrophoneActivity extends AppCompatActivity {
    private static final String TAG = "CameraMicrophoneActivity";
    private QNRTCClient mClient;
    private QNSurfaceView mLocalRenderView;
    private QNSurfaceView mRemoteRenderView;
    private QNCameraVideoTrack mCameraVideoTrack;
    private QNMicrophoneAudioTrack mMicrophoneAudioTrack;
    private QNBeautySetting mBeautySetting;

    private String mFirstRemoteUserID = null;
    private boolean mMicrophoneError;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera_microphone);

        // 1. 初始化视图
        initView();
        // 2. 初始化 RTC
        QNRTC.init(this, mRTCEventListener);
        APIApplication.mRTCInit = true;
        // 3. 创建 QNRTCClient 对象
        mClient = QNRTC.createClient(mClientEventListener);
        // 本示例仅针对 1v1 连麦场景，因此，关闭自动订阅选项。关于自动订阅的配置，可参考 https://developer.qiniu.com/rtc/8769/publish-and-subscribe-android#3
        mClient.setAutoSubscribe(false);
        // 4. 创建本地音视频 Track
        initLocalTracks();
        // 5. 加入房间
        mClient.join(Config.ROOM_TOKEN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 退后台会关闭采集，回到前台重新开启采集
        mCameraVideoTrack.startCapture();
        if (mMicrophoneError && mClient != null && mMicrophoneAudioTrack != null) {
            mClient.unpublish(mMicrophoneAudioTrack);
            mClient.publish(new QNPublishResultCallback() {
                @Override
                public void onPublished() {
                }
                @Override
                public void onError(int errorCode, String errorMessage) {

                }
            }, mMicrophoneAudioTrack);
            mMicrophoneError = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isFinishing()) {
            // 从 Android 9 开始，设备将无法在后台访问相机，本示例不做后台采集的演示
            // 详情可参考 https://developer.qiniu.com/rtc/kb/10074/FAQ-Android?category=kb#3
            if (mCameraVideoTrack != null) {
                mCameraVideoTrack.stopCapture();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mClient != null) {
            mClient.leave();
            mClient = null;
        }
        destroyLocalTracks();
        if (APIApplication.mRTCInit) {
            // 9. 反初始化 RTC 释放资源
            QNRTC.deinit();
            APIApplication.mRTCInit = false;
        }
    }

    private void initView() {
        // 初始化本地预览视图
        mLocalRenderView = findViewById(R.id.local_render_view);
        mLocalRenderView.setZOrderOnTop(true);
        // 初始化远端预览视图
        mRemoteRenderView = findViewById(R.id.remote_render_view);
        mRemoteRenderView.setZOrderOnTop(true);

        SwitchCompat beautyOnSwitch = findViewById(R.id.switch_beauty_on);
        beautyOnSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 控制美颜的开关
            if (mCameraVideoTrack != null) {
                mBeautySetting.setEnable(isChecked);
                mCameraVideoTrack.setBeauty(mBeautySetting);
            }
        });

        SeekBar beautyStrengthSeekBar = findViewById(R.id.seek_bar_beauty_strength);
        beautyStrengthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mCameraVideoTrack != null) {
                    mBeautySetting.setSmoothLevel(seekBar.getProgress() / 100.0f);
                    mCameraVideoTrack.setBeauty(mBeautySetting);
                }
            }
        });

        SeekBar beautyReddenSeekBar = findViewById(R.id.seek_bar_redden);
        beautyReddenSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mCameraVideoTrack != null) {
                    mBeautySetting.setRedden(seekBar.getProgress() / 100.0f);
                    mCameraVideoTrack.setBeauty(mBeautySetting);
                }
            }
        });

        SeekBar beautyWhitenSeekBar = findViewById(R.id.seek_bar_whiten);
        beautyWhitenSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mCameraVideoTrack != null) {
                    mBeautySetting.setWhiten(seekBar.getProgress() / 100.0f);
                    mCameraVideoTrack.setBeauty(mBeautySetting);
                }
            }
        });
    }

    /**
     * 创建音视频采集 Track
     *
     * 摄像头采集 Track 创建方式：
     * 1. 创建 QNCameraVideoTrackConfig 对象，指定采集编码相关的配置
     * 2. 通过 QNCameraVideoTrackConfig 对象创建摄像头采集 Track，若使用无参的方法创建则会使用 SDK 默认配置参数（640x480, 20fps, 800kbps）
     *
     * 麦克风采集 Track 创建方式：
     * 1. 创建 QNMicrophoneAudioTrackConfig 对象指定音频参数，亦可使用 SDK 的预设值，预设值可见 {@link QNAudioQualityPreset}
     * 2. 通过 QNMicrophoneAudioTrackConfig 对象创建麦克风采集 Track，若使用无参的方法创建则会使用 SDK 默认配置参数（16kHz, 单声道, 24kbps）
     */
    private void initLocalTracks() {
        // 创建摄像头采集 Track
        QNCameraVideoTrackConfig cameraVideoTrackConfig = new QNCameraVideoTrackConfig(Config.TAG_CAMERA_TRACK)
                .setVideoCaptureConfig(QNVideoCaptureConfigPreset.CAPTURE_640x480) // 设置采集参数
                .setVideoEncoderConfig(new QNVideoEncoderConfig(
                        Config.DEFAULT_WIDTH, Config.DEFAULT_HEIGHT, Config.DEFAULT_FPS, Config.DEFAULT_VIDEO_BITRATE)) // 设置编码参数
                .setCameraFacing(QNCameraFacing.FRONT) // 设置摄像头方向
                .setMultiProfileEnabled(false); // 设置是否开启大小流
        mCameraVideoTrack = QNRTC.createCameraVideoTrack(cameraVideoTrackConfig);
        // 设置本地预览视图
        mCameraVideoTrack.play(mLocalRenderView);
        // 初始化并配置美颜
        mBeautySetting = new QNBeautySetting(0.5f, 0.5f, 0.5f);
        mCameraVideoTrack.setBeauty(mBeautySetting);
        mCameraVideoTrack.setCameraEventListener(new QNCameraEventListener() {
            @Override
            public int[] onCameraOpened(List<Size> list, List<Integer> list1) {
                return new int[]{-1, -1};
            }

            @Override
            public void onCaptureStarted() {
                Log.i(TAG, "onCaptureStarted");
            }

            @Override
            public void onCaptureStopped() {
                Log.i(TAG, "onCaptureStopped");
            }

            @Override
            public void onError(int i, String s) {
                Log.i(TAG, "onError [" + i + ", " + s + "]");
            }

            @Override
            public void onPushImageError(int i, String s) {
                Log.i(TAG, "onPushImageError [" + i + ", " + s + "]");
            }
        });

        // 创建麦克风采集 Track
        QNMicrophoneAudioTrackConfig microphoneAudioTrackConfig = new QNMicrophoneAudioTrackConfig(Config.TAG_MICROPHONE_TRACK)
                .setAudioQuality(QNAudioQualityPreset.STANDARD); // 设置音频参数，建议实时音视频通话场景使用默认值即可
        mMicrophoneAudioTrack = QNRTC.createMicrophoneAudioTrack(microphoneAudioTrackConfig);
        mMicrophoneAudioTrack.setMicrophoneEventListener((errorCode, errorMessage) -> mMicrophoneError = true);
    }

    /**
     * 销毁本地 Tracks
     */
    private void destroyLocalTracks() {
        if (mCameraVideoTrack != null) {
            mCameraVideoTrack.destroy();
            mCameraVideoTrack = null;
        }
        if (mMicrophoneAudioTrack != null) {
            mMicrophoneAudioTrack.destroy();
            mMicrophoneAudioTrack = null;
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
            ToastUtils.showShortToast(CameraMicrophoneActivity.this,
                    String.format(getString(R.string.connection_state_changed), state.name()));
            if (state == QNConnectionState.CONNECTED) {
                // 6. 发布本地音视频 Track
                // 发布订阅场景注意事项可参考 https://developer.qiniu.com/rtc/8769/publish-and-subscribe-android
                mClient.publish(new QNPublishResultCallback() {
                    @Override
                    public void onPublished() { // 发布成功
                        runOnUiThread(() -> ToastUtils.showShortToast(CameraMicrophoneActivity.this,
                                getString(R.string.publish_success)));
                    }

                    @Override
                    public void onError(int errorCode, String errorMessage) { // 发布失败
                        runOnUiThread(() -> ToastUtils.showLongToast(CameraMicrophoneActivity.this,
                                String.format(getString(R.string.publish_failed), errorCode, errorMessage)));
                    }
                }, mCameraVideoTrack, mMicrophoneAudioTrack);
            }
        }

        /**
         * 远端用户加入房间时会回调此方法
         * @see QNRTCClient#join(String, String) 可指定 userData 字段
         *
         * @param remoteUserID 远端用户的 userID
         * @param userData 透传字段，用户自定义内容
         */
        @Override
        public void onUserJoined(String remoteUserID, String userData) {

        }

        /**
         * 远端用户重连时会回调此方法
         *
         * @param remoteUserID 远端用户的 userID
         */
        @Override
        public void onUserReconnecting(String remoteUserID) {

        }

        /**
         * 远端用户重连成功时会回调此方法
         *
         * @param remoteUserID 远端用户的 userID
         */
        @Override
        public void onUserReconnected(String remoteUserID) {

        }

        /**
         * 远端用户离开房间时会回调此方法
         *
         * @param remoteUserID 远端离开用户的 userID
         */
        @Override
        public void onUserLeft(String remoteUserID) {
            ToastUtils.showShortToast(CameraMicrophoneActivity.this, getString(R.string.remote_user_left_toast));
        }

        /**
         * 远端用户成功发布 tracks 时会回调此方法
         *
         * 手动订阅场景下，可以在该回调中选择待订阅的 Track，并通过 {@link QNRTCClient#subscribe(QNRemoteTrack...)}
         * 接口进行订阅的操作。
         *
         * @param remoteUserID 远端用户 userID
         * @param trackList 远端用户发布的 tracks 列表
         */
        @Override
        public void onUserPublished(String remoteUserID, List<QNRemoteTrack> trackList) {
            if (mFirstRemoteUserID == null || remoteUserID.equals(mFirstRemoteUserID)) {
                mFirstRemoteUserID = remoteUserID;
                // 7. 手动订阅远端音视频 Track
                mClient.subscribe(trackList);
            } else {
                ToastUtils.showShortToast(CameraMicrophoneActivity.this, getString(R.string.toast_other_user_published));
            }
        }

        /**
         * 远端用户成功取消发布 tracks 时会回调此方法
         *
         * @param remoteUserID 远端用户 userID
         * @param trackList 远端用户取消发布的 tracks 列表
         */
        @Override
        public void onUserUnpublished(String remoteUserID, List<QNRemoteTrack> trackList) {
            if (remoteUserID.equals(mFirstRemoteUserID)) {
                mFirstRemoteUserID = null;
                mRemoteRenderView.setVisibility(View.INVISIBLE);
            }
        }

        /**
         * 成功订阅远端用户 Track 时会回调此方法
         *
         * @param remoteUserID 远端用户 userID
         * @param remoteAudioTracks 订阅的远端用户音频 tracks 列表
         * @param remoteVideoTracks 订阅的远端用户视频 tracks 列表
         */
        @Override
        public void onSubscribed(String remoteUserID, List<QNRemoteAudioTrack> remoteAudioTracks, List<QNRemoteVideoTrack> remoteVideoTracks) {
            if (!remoteVideoTracks.isEmpty()) {
                // 成功订阅远端音视频 Track 后，对视频 Track 进行渲染，由于本示例仅实现一对一的连麦，因此，直接渲染即可
                remoteVideoTracks.get(0).play(mRemoteRenderView);
                mRemoteRenderView.setVisibility(View.VISIBLE);
            }
        }

        /**
         * 当收到自定义消息时回调此方法
         *
         * @param message 自定义信息，详情请参考 {@link QNCustomMessage}
         */
        @Override
        public void onMessageReceived(QNCustomMessage message) {

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
    };
}
