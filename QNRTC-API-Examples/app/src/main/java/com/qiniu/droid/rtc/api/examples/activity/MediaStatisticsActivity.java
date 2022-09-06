package com.qiniu.droid.rtc.api.examples.activity;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.qiniu.droid.rtc.QNAudioQualityPreset;
import com.qiniu.droid.rtc.QNBeautySetting;
import com.qiniu.droid.rtc.QNCameraFacing;
import com.qiniu.droid.rtc.QNCameraVideoTrack;
import com.qiniu.droid.rtc.QNCameraVideoTrackConfig;
import com.qiniu.droid.rtc.QNClientEventListener;
import com.qiniu.droid.rtc.QNConnectionDisconnectedInfo;
import com.qiniu.droid.rtc.QNConnectionState;
import com.qiniu.droid.rtc.QNCustomMessage;
import com.qiniu.droid.rtc.QNLocalAudioTrackStats;
import com.qiniu.droid.rtc.QNLocalVideoTrackStats;
import com.qiniu.droid.rtc.QNMediaRelayState;
import com.qiniu.droid.rtc.QNMicrophoneAudioTrack;
import com.qiniu.droid.rtc.QNMicrophoneAudioTrackConfig;
import com.qiniu.droid.rtc.QNNetworkQuality;
import com.qiniu.droid.rtc.QNPublishResultCallback;
import com.qiniu.droid.rtc.QNRTC;
import com.qiniu.droid.rtc.QNRTCClient;
import com.qiniu.droid.rtc.QNRTCEventListener;
import com.qiniu.droid.rtc.QNRemoteAudioTrack;
import com.qiniu.droid.rtc.QNRemoteAudioTrackStats;
import com.qiniu.droid.rtc.QNRemoteTrack;
import com.qiniu.droid.rtc.QNRemoteVideoTrack;
import com.qiniu.droid.rtc.QNRemoteVideoTrackStats;
import com.qiniu.droid.rtc.QNSurfaceView;
import com.qiniu.droid.rtc.QNVideoCaptureConfigPreset;
import com.qiniu.droid.rtc.QNVideoEncoderConfig;
import com.qiniu.droid.rtc.api.examples.R;
import com.qiniu.droid.rtc.api.examples.utils.Config;
import com.qiniu.droid.rtc.api.examples.utils.ToastUtils;
import com.qiniu.droid.rtc.model.QNAudioDevice;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 1v1 音视频通话 + 通话质量信息统计场景
 *
 * 主要步骤如下：
 * 1. 初始化视图
 * 2. 初始化 RTC
 * 3. 创建 QNRTCClient 对象
 * 4. 创建本地音视频 Track
 * 5. 加入房间
 * 6. 发布本地音视频 Track
 * 7. 订阅远端音视频 Track
 * 8. 开启通话质量统计
 * 9. 离开房间
 * 10. 反初始化 RTC 释放资源
 *
 * 文档参考：
 * - 通话信息统计文档，请参考 https://developer.qiniu.com/rtc/9860/quality-statistics-android
 * - 音视频通话中的基本概念，请参考 https://developer.qiniu.com/rtc/9909/the-rtc-basic-concept
 * - 接口文档，请参考 https://developer.qiniu.com/rtc/8773/API%20%E6%A6%82%E8%A7%88
 */
public class MediaStatisticsActivity extends AppCompatActivity {
    private static final String TAG = "MediaStatisticsActivity";
    private QNRTCClient mClient;
    private QNSurfaceView mLocalRenderView;
    private QNSurfaceView mRemoteRenderView;
    private QNCameraVideoTrack mCameraVideoTrack;
    private QNMicrophoneAudioTrack mMicrophoneAudioTrack;

    private String mFirstRemoteUserID = null;
    private boolean mMicrophoneError;

    private Timer mStatsTimer;
    private TextView mLocalUplinkNetworkQualityText;
    private TextView mLocalDownlinkNetworkQualityText;
    private TextView mLocalAudioUplinkBitrateText;
    private TextView mLocalAudioUplinkRttText;
    private TextView mLocalAudioUplinkLostRateText;
    private TextView mLocalVideoProfileText;
    private TextView mLocalVideoUplinkFrameRateText;
    private TextView mLocalVideoUplinkBitrateText;
    private TextView mLocalVideoUplinkRttText;
    private TextView mLocalVideoUplinkLostRateText;

    private TextView mRemoteUplinkNetworkQualityText;
    private TextView mRemoteDownlinkNetworkQualityText;
    private TextView mRemoteAudioDownlinkBitrateText;
    private TextView mRemoteAudioDownlinkLostRateText;
    private TextView mRemoteAudioUplinkRttText;
    private TextView mRemoteAudioUplinkLostRateText;
    private TextView mRemoteVideoProfileText;
    private TextView mRemoteVideoDownlinkFrameRateText;
    private TextView mRemoteVideoDownlinkBitrateText;
    private TextView mRemoteVideoDownlinkLostRateText;
    private TextView mRemoteVideoUplinkRttText;
    private TextView mRemoteVideoUplinkLostRateText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_media_statistics);

        // 1. 初始化视图
        initView();
        // 2. 初始化 RTC
        QNRTC.init(this, mRTCEventListener);
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
            // 9. 离开房间
            mClient.leave();
            mClient = null;
        }
        destroyLocalTracks();
        // 10. 反初始化 RTC 释放资源
        QNRTC.deinit();
    }

    /**
     * 初始化视图控件
     */
    private void initView() {
        // 初始化本地预览视图
        mLocalRenderView = findViewById(R.id.local_render_view);
        mLocalRenderView.setZOrderOnTop(true);
        // 初始化远端预览视图
        mRemoteRenderView = findViewById(R.id.remote_render_view);
        mRemoteRenderView.setZOrderOnTop(true);
        
        mLocalDownlinkNetworkQualityText = findViewById(R.id.downlink_network_quality);
        mLocalUplinkNetworkQualityText = findViewById(R.id.upload_network_quality);
        mLocalAudioUplinkBitrateText = findViewById(R.id.local_audio_bitrate);
        mLocalAudioUplinkRttText = findViewById(R.id.local_audio_rtt);
        mLocalAudioUplinkLostRateText = findViewById(R.id.local_audio_lost_rate);
        mLocalVideoProfileText = findViewById(R.id.local_video_profile);
        mLocalVideoUplinkFrameRateText = findViewById(R.id.local_video_frame_rate);
        mLocalVideoUplinkBitrateText = findViewById(R.id.local_video_bitrate);
        mLocalVideoUplinkRttText = findViewById(R.id.local_video_rtt);
        mLocalVideoUplinkLostRateText = findViewById(R.id.local_video_lost_rate);

        mRemoteDownlinkNetworkQualityText = findViewById(R.id.remote_downlink_network_quality);
        mRemoteUplinkNetworkQualityText = findViewById(R.id.remote_upload_network_quality);
        mRemoteAudioDownlinkBitrateText = findViewById(R.id.remote_audio_downlink_bitrate);
        mRemoteAudioDownlinkLostRateText = findViewById(R.id.remote_audio_downlink_lost_rate);
        mRemoteAudioUplinkRttText = findViewById(R.id.remote_audio_uplink_rtt);
        mRemoteAudioUplinkLostRateText = findViewById(R.id.remote_audio_uplink_lost_rate);
        mRemoteVideoProfileText = findViewById(R.id.remote_video_profile);
        mRemoteVideoDownlinkFrameRateText = findViewById(R.id.remote_video_downlink_frame_rate);
        mRemoteVideoDownlinkBitrateText = findViewById(R.id.remote_video_downlink_bitrate);
        mRemoteVideoDownlinkLostRateText = findViewById(R.id.remote_video_downlink_lost_rate);
        mRemoteVideoUplinkRttText = findViewById(R.id.remote_video_uplink_rtt);
        mRemoteVideoUplinkLostRateText = findViewById(R.id.remote_video_uplink_lost_rate);
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
                .setVideoCaptureConfig(QNVideoCaptureConfigPreset.CAPTURE_1280x720) // 设置采集参数
                .setVideoEncoderConfig(new QNVideoEncoderConfig(
                        Config.DEFAULT_WIDTH, Config.DEFAULT_HEIGHT, Config.DEFAULT_FPS, Config.DEFAULT_VIDEO_BITRATE)) // 设置编码参数
                .setCameraFacing(QNCameraFacing.FRONT) // 设置摄像头方向
                .setMultiProfileEnabled(true); // 设置是否开启大小流
        mCameraVideoTrack = QNRTC.createCameraVideoTrack(cameraVideoTrackConfig);
        // 设置本地预览视图
        mCameraVideoTrack.play(mLocalRenderView);
        // 初始化并配置美颜
        mCameraVideoTrack.setBeauty(new QNBeautySetting(0.5f, 0.5f, 0.5f));

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

    /**
     * 开始通话质量统计
     */
    private synchronized void startStatisticsScheduler() {
        mStatsTimer = new Timer();
        mStatsTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (mClient != null) {
                        // 本地视频 Track 质量统计
                        Map<String, List<QNLocalVideoTrackStats>> localVideoTrackStats = mClient.getLocalVideoTrackStats();
                        for (Map.Entry<String, List<QNLocalVideoTrackStats>> entry : localVideoTrackStats.entrySet()) {
                            for (QNLocalVideoTrackStats stats : entry.getValue()) {
                                mLocalVideoProfileText.setText(stats.profile.name());
                                mLocalVideoUplinkBitrateText.setText(String.format(getString(R.string.bitrate), stats.uplinkBitrate / 1000));
                                mLocalVideoUplinkLostRateText.setText(String.format(getString(R.string.lost_rate), stats.uplinkLostRate));
                                mLocalVideoUplinkFrameRateText.setText(String.format(getString(R.string.fps), stats.uplinkFrameRate));
                                mLocalVideoUplinkRttText.setText(String.format(getString(R.string.rtt), stats.uplinkRTT));
                            }
                        }
                        // 本地音频 Track 质量统计
                        Map<String, QNLocalAudioTrackStats> localAudioTrackStats = mClient.getLocalAudioTrackStats();
                        for (Map.Entry<String, QNLocalAudioTrackStats> entry : localAudioTrackStats.entrySet()) {
                            QNLocalAudioTrackStats stats = entry.getValue();
                            mLocalAudioUplinkBitrateText.setText(String.format(getString(R.string.bitrate), stats.uplinkBitrate / 1000));
                            mLocalAudioUplinkLostRateText.setText(String.format(getString(R.string.lost_rate), stats.uplinkLostRate));
                            mLocalAudioUplinkRttText.setText(String.format(getString(R.string.rtt), stats.uplinkRTT));
                        }
                        // 远端视频 Track 质量统计
                        Map<String, QNRemoteVideoTrackStats> remoteVideoTrackStats = mClient.getRemoteVideoTrackStats();
                        for (Map.Entry<String, QNRemoteVideoTrackStats> entry : remoteVideoTrackStats.entrySet()) {
                            QNRemoteVideoTrackStats stats = entry.getValue();
                            if (stats.profile != null) {
                                mRemoteVideoProfileText.setText(stats.profile.name());
                            }
                            mRemoteVideoUplinkLostRateText.setText(String.format(getString(R.string.lost_rate), stats.uplinkLostRate));
                            mRemoteVideoUplinkRttText.setText(String.format(getString(R.string.rtt), stats.uplinkRTT));
                            mRemoteVideoDownlinkBitrateText.setText(String.format(getString(R.string.bitrate), stats.downlinkBitrate / 1000));
                            mRemoteVideoDownlinkFrameRateText.setText(String.format(getString(R.string.fps), stats.downlinkFrameRate));
                            mRemoteVideoDownlinkLostRateText.setText(String.format(getString(R.string.lost_rate), stats.downlinkLostRate));
                        }
                        // 远端音频 Track 质量统计
                        Map<String, QNRemoteAudioTrackStats> remoteAudioTrackStats = mClient.getRemoteAudioTrackStats();
                        for (Map.Entry<String, QNRemoteAudioTrackStats> entry : remoteAudioTrackStats.entrySet()) {
                            QNRemoteAudioTrackStats stats = entry.getValue();
                            mRemoteAudioUplinkLostRateText.setText(String.format(getString(R.string.lost_rate), stats.uplinkLostRate));
                            mRemoteAudioUplinkRttText.setText(String.format(getString(R.string.rtt), stats.uplinkRTT));
                            mRemoteAudioDownlinkBitrateText.setText(String.format(getString(R.string.bitrate), stats.downlinkBitrate / 1000));
                            mRemoteAudioDownlinkLostRateText.setText(String.format(getString(R.string.lost_rate), stats.downlinkLostRate));
                        }
                        // 远端用户网络质量统计
                        Map<String, QNNetworkQuality> userNetworkQuality = mClient.getUserNetworkQuality();
                        for (Map.Entry<String, QNNetworkQuality> entry : userNetworkQuality.entrySet()) {
                            mRemoteDownlinkNetworkQualityText.setText(entry.getValue().downlinkNetworkGrade.name());
                            mRemoteUplinkNetworkQualityText.setText(entry.getValue().uplinkNetworkGrade.name());
                        }
                    }
                });
            }
        }, 0, 5000);
        if (mClient != null) {
            // 设置本地网络质量统计
            mClient.setNetworkQualityListener(networkQuality -> runOnUiThread(() -> {
                mLocalDownlinkNetworkQualityText.setText(networkQuality.downlinkNetworkGrade.name());
                mLocalUplinkNetworkQualityText.setText(networkQuality.uplinkNetworkGrade.name());
            }));
        }
    }

    /**
     * 停止通话质量统计
     */
    public synchronized void stopStatisticsScheduler() {
        if (mClient != null) {
            mClient.setNetworkQualityListener(null);
        }
        if (mStatsTimer != null) {
            mStatsTimer.cancel();
            mStatsTimer = null;
        }
    }

    /**
     * 远端用户离开后，重置 UI
     */
    private void resetRemoteStatisticsView() {
        mRemoteDownlinkNetworkQualityText.setText(getString(R.string.none));
        mRemoteUplinkNetworkQualityText.setText(getString(R.string.none));
        mRemoteAudioUplinkLostRateText.setText(String.format(getString(R.string.lost_rate), 0));
        mRemoteAudioUplinkRttText.setText(String.format(getString(R.string.rtt), 0));
        mRemoteAudioDownlinkBitrateText.setText(String.format(getString(R.string.bitrate), 0));
        mRemoteAudioDownlinkLostRateText.setText(String.format(getString(R.string.lost_rate), 0));
        mRemoteVideoProfileText.setText(getString(R.string.none));
        mRemoteVideoUplinkLostRateText.setText(String.format(getString(R.string.lost_rate), 0));
        mRemoteVideoUplinkRttText.setText(String.format(getString(R.string.rtt), 0));
        mRemoteVideoDownlinkBitrateText.setText(String.format(getString(R.string.bitrate), 0));
        mRemoteVideoDownlinkFrameRateText.setText(String.format(getString(R.string.fps), 0));
        mRemoteVideoDownlinkLostRateText.setText(String.format(getString(R.string.lost_rate), 0));
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
            ToastUtils.showShortToast(MediaStatisticsActivity.this,
                    String.format(getString(R.string.connection_state_changed), state.name()));
            if (state == QNConnectionState.CONNECTED) {
                // 6. 发布本地音视频 Track
                // 发布订阅场景注意事项可参考 https://developer.qiniu.com/rtc/8769/publish-and-subscribe-android
                mClient.publish(new QNPublishResultCallback() {
                    @Override
                    public void onPublished() { // 发布成功
                        runOnUiThread(() -> ToastUtils.showShortToast(MediaStatisticsActivity.this,
                                getString(R.string.publish_success)));
                    }

                    @Override
                    public void onError(int errorCode, String errorMessage) { // 发布失败
                        runOnUiThread(() -> ToastUtils.showLongToast(MediaStatisticsActivity.this,
                                String.format(getString(R.string.publish_failed), errorCode, errorMessage)));
                    }
                }, mCameraVideoTrack, mMicrophoneAudioTrack);
                // 8. 开启通话质量统计
                startStatisticsScheduler();
            } else if (state == QNConnectionState.DISCONNECTED) {
                // 停止通话质量统计
                stopStatisticsScheduler();
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
            ToastUtils.showShortToast(MediaStatisticsActivity.this, getString(R.string.remote_user_left_toast));
            resetRemoteStatisticsView();
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
                ToastUtils.showShortToast(MediaStatisticsActivity.this, getString(R.string.toast_other_user_published));
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
