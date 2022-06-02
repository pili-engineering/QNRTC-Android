package com.qiniu.droid.rtc.api.examples.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.qiniu.droid.rtc.QNAudioQualityPreset;
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
import com.qiniu.droid.rtc.QNRTCSetting;
import com.qiniu.droid.rtc.QNRemoteAudioTrack;
import com.qiniu.droid.rtc.QNRemoteTrack;
import com.qiniu.droid.rtc.QNRemoteVideoTrack;
import com.qiniu.droid.rtc.QNScreenVideoTrack;
import com.qiniu.droid.rtc.QNScreenVideoTrackConfig;
import com.qiniu.droid.rtc.QNSourceType;
import com.qiniu.droid.rtc.QNSurfaceView;
import com.qiniu.droid.rtc.QNVideoEncoderConfig;
import com.qiniu.droid.rtc.api.examples.R;
import com.qiniu.droid.rtc.api.examples.service.ForegroundService;
import com.qiniu.droid.rtc.api.examples.utils.Config;
import com.qiniu.droid.rtc.api.examples.utils.ToastUtils;
import com.qiniu.droid.rtc.model.QNAudioDevice;

import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 1v1 屏幕录制音视频通话场景
 *
 * 主要步骤如下：
 * 1. 初始化视图
 * 2. 确认设备是否支持屏幕录制
 * 3. 初始化 RTC
 * 4. 创建 QNRTCClient 对象
 * 5. 获取屏幕录制权限
 * 6. 创建本地音视频 Track
 * 7. 加入房间
 * 8. 发布本地音视频 Track
 * 9. 订阅远端音视频 Track
 * 10. 离开房间
 * 11. 反初始化 RTC 释放资源
 *
 * 注意：Android Q 之后，屏幕录制必须要在前台服务中进行，详细的实现方式可参考本示例
 *
 * 文档参考：
 * - 屏幕录制实现步骤，请参考 https://developer.qiniu.com/rtc/8767/audio-and-video-collection-android#2
 * - 音视频通话中的基本概念，请参考 https://developer.qiniu.com/rtc/9909/the-rtc-basic-concept
 * - 接口文档，请参考 https://developer.qiniu.com/rtc/8773/API%20%E6%A6%82%E8%A7%88
 */
public class ScreenCaptureActivity extends AppCompatActivity {
    private static final String TAG = "ScreenCaptureActivity";

    private QNRTCClient mClient;
    private QNScreenVideoTrack mScreenVideoTrack;
    private QNMicrophoneAudioTrack mMicrophoneAudioTrack;

    private QNSurfaceView mRemoteRenderView;
    private String mFirstRemoteUserID = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_screen_capture);

        // 1. 初始化视图
        initView();
        // 2. 确认设备是否支持屏幕录制
        if (!QNScreenVideoTrack.isScreenCaptureSupported()) {
            ToastUtils.showShortToast(this, "当前设备不支持屏幕录制");
            finish();
        }
        // 3. 初始化 RTC
        QNRTCSetting setting = new QNRTCSetting()
                .setMaintainResolution(true)  // 设置开启固定分辨率
                .setHWCodecEnabled(false);  // 为保证编码质量，开启软编
        QNRTC.init(this, setting, mRTCEventListener);
        // 4. 创建 QNRTCClient 对象
        mClient = QNRTC.createClient(mClientEventListener);
        // 本示例仅针对 1v1 连麦场景，因此，关闭自动订阅选项。关于自动订阅的配置，可参考 https://developer.qiniu.com/rtc/8769/publish-and-subscribe-android#3
        mClient.setAutoSubscribe(false);
        // 5. 获取屏幕录制权限
        QNScreenVideoTrack.requestPermission(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mClient != null) {
            // 10. 离开房间
            mClient.leave();
            mClient = null;
        }
        // 11. 反初始化 RTC 释放资源
        QNRTC.deinit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == QNScreenVideoTrack.SCREEN_CAPTURE_PERMISSION_REQUEST_CODE) {
            if (QNScreenVideoTrack.checkActivityResult(requestCode, resultCode, data)) {
                // 6. 创建本地音视频 Track
                initLocalTracks();
                // 7. 加入房间，Android Q 之后屏幕录制需要 foreground service，因此可等待 service 回调后再加入房间
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    mClient.join(Config.ROOM_TOKEN);
                }
            } else {
                ToastUtils.showShortToast(this, "用户未授予录屏权限");
                finish();
            }
        }
    }

    private void initView() {
        // 初始化远端视频渲染视图
        mRemoteRenderView = findViewById(R.id.remote_render_view);
        mRemoteRenderView.setZOrderOnTop(true);
    }

    private void initLocalTracks() {
        // 创建麦克风采集 Track
        QNMicrophoneAudioTrackConfig microphoneAudioTrackConfig = new QNMicrophoneAudioTrackConfig(Config.TAG_MICROPHONE_TRACK)
                .setAudioQuality(QNAudioQualityPreset.STANDARD) // 设置音频参数，建议实时音视频通话场景使用默认值即可
                .setCommunicationModeOn(true); // 设置是否开启通话模式，开启后会启用硬件回声消除等处理
        mMicrophoneAudioTrack = QNRTC.createMicrophoneAudioTrack(microphoneAudioTrackConfig);

        // 创建屏幕录制采集 Track
        createScreenTrack();
    }

    // 处理 Build.VERSION_CODES.Q 及以上的兼容问题
    private void createScreenTrack() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent intent = new Intent(this, ForegroundService.class);
            startForegroundService(intent);
            bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
            Log.i(TAG, "start service for Q");
        } else {
            // 创建屏幕录制采集 Track
            // 编码分辨率约贴近屏幕分辨率，码率越大，画面越清晰，但是考虑到编码性能，需根据您的场景选择特定的编码分辨率
            QNScreenVideoTrackConfig screenVideoTrackConfig = new QNScreenVideoTrackConfig(Config.TAG_SCREEN_TRACK)
                    .setVideoEncoderConfig(new QNVideoEncoderConfig(
                            Config.DEFAULT_SCREEN_VIDEO_TRACK_WIDTH, Config.DEFAULT_SCREEN_VIDEO_TRACK_HEIGHT,
                            Config.DEFAULT_FPS, Config.DEFAULT_SCREEN_VIDEO_TRACK_BITRATE));
            mScreenVideoTrack = QNRTC.createScreenVideoTrack(screenVideoTrackConfig);
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
            Log.i(TAG, "onConnectionStateChanged : " + state.name());
            ToastUtils.showShortToast(ScreenCaptureActivity.this,
                    String.format(getString(R.string.connection_state_changed), state.name()));
            if (state == QNConnectionState.CONNECTED) {
                // 8. 发布本地音视频 Track
                // 发布订阅场景注意事项可参考 https://developer.qiniu.com/rtc/8769/publish-and-subscribe-android
                mClient.publish(new QNPublishResultCallback() {
                    @Override
                    public void onPublished() { // 发布成功
                        runOnUiThread(() -> ToastUtils.showShortToast(ScreenCaptureActivity.this,
                                getString(R.string.publish_success)));
                    }

                    @Override
                    public void onError(int errorCode, String errorMessage) { // 发布失败
                        runOnUiThread(() -> ToastUtils.showLongToast(ScreenCaptureActivity.this,
                                String.format(getString(R.string.publish_failed), errorCode, errorMessage)));
                    }
                }, mScreenVideoTrack, mMicrophoneAudioTrack);
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
            ToastUtils.showShortToast(ScreenCaptureActivity.this, getString(R.string.remote_user_left_toast));
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
                // 9. 手动订阅远端音视频 Track
                mClient.subscribe(trackList);
            } else {
                ToastUtils.showShortToast(ScreenCaptureActivity.this, getString(R.string.toast_other_user_published));
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

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // 创建屏幕录制采集 Track
            // 编码分辨率约贴近屏幕分辨率，码率越大，画面越清晰，但是考虑到编码性能，需根据您的场景选择特定的编码分辨率
            QNScreenVideoTrackConfig screenVideoTrackConfig = new QNScreenVideoTrackConfig(Config.TAG_SCREEN_TRACK)
                    .setVideoEncoderConfig(new QNVideoEncoderConfig(
                            Config.DEFAULT_SCREEN_VIDEO_TRACK_WIDTH, Config.DEFAULT_SCREEN_VIDEO_TRACK_HEIGHT,
                            Config.DEFAULT_FPS, Config.DEFAULT_SCREEN_VIDEO_TRACK_BITRATE));
            mScreenVideoTrack = QNRTC.createScreenVideoTrack(screenVideoTrackConfig);

            // 7. 加入房间，Android Q 之后屏幕录制需要 foreground service，因此可等待 service 回调后再加入房间
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mClient.join(Config.ROOM_TOKEN);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
}
