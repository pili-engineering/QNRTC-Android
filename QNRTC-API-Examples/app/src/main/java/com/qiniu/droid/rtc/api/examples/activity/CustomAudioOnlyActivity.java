package com.qiniu.droid.rtc.api.examples.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.qiniu.droid.rtc.QNAudioFrame;
import com.qiniu.droid.rtc.QNAudioQuality;
import com.qiniu.droid.rtc.QNAudioVolumeInfo;
import com.qiniu.droid.rtc.QNClientEventListener;
import com.qiniu.droid.rtc.QNConnectionDisconnectedInfo;
import com.qiniu.droid.rtc.QNConnectionState;
import com.qiniu.droid.rtc.QNCustomAudioTrack;
import com.qiniu.droid.rtc.QNCustomAudioTrackConfig;
import com.qiniu.droid.rtc.QNCustomMessage;
import com.qiniu.droid.rtc.QNMediaRelayState;
import com.qiniu.droid.rtc.QNPublishResultCallback;
import com.qiniu.droid.rtc.QNRTC;
import com.qiniu.droid.rtc.QNRTCClient;
import com.qiniu.droid.rtc.QNRTCEventListener;
import com.qiniu.droid.rtc.QNRemoteAudioTrack;
import com.qiniu.droid.rtc.QNRemoteTrack;
import com.qiniu.droid.rtc.QNRemoteVideoTrack;
import com.qiniu.droid.rtc.api.examples.APIApplication;
import com.qiniu.droid.rtc.api.examples.R;
import com.qiniu.droid.rtc.api.examples.capture.ExtAudioCapture;
import com.qiniu.droid.rtc.api.examples.utils.Config;
import com.qiniu.droid.rtc.api.examples.utils.ToastUtils;
import com.qiniu.droid.rtc.model.QNAudioDevice;

import java.nio.ByteBuffer;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 1v1 外部数据导入纯音频通话场景
 * 本示例仅演示本地外部数据导入音频 Track 的发布和远端音频的订阅场景
 *
 * 主要步骤如下：
 * 1. 初始化视图
 * 2. 初始化 RTC
 * 3. 创建 QNRTCClient 对象
 * 4. 创建本地外部数据导入音频 Track
 * 5. 加入房间
 * 6. 发布本地外部数据导入音频 Track
 * 7. 订阅远端音频 Track
 * 8. 离开房间
 * 9. 反初始化 RTC 释放资源
 *
 * 文档参考：
 * - 音视频通话中的基本概念，请参考 https://developer.qiniu.com/rtc/9909/the-rtc-basic-concept
 * - 接口文档，请参考 https://developer.qiniu.com/rtc/8773/API%20%E6%A6%82%E8%A7%88
 */
public class CustomAudioOnlyActivity extends AppCompatActivity {
    private static final String TAG = "CustomAudioOnlyActivity";
    private QNRTCClient mClient;
    private QNCustomAudioTrack mCustomAudioTrack;

    private ExtAudioCapture mExtAudioCapture;

    private TextView mRemoteTrackTipsView;
    private String mFirstRemoteUserID = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_custom_audio_only);

        // 1. 初始化视图
        initView();
        // 2. 初始化 RTC
        QNRTC.init(this, mRTCEventListener);
        APIApplication.mRTCInit = true;
        // 3. 创建 QNRTCClient 对象
        mClient = QNRTC.createClient(mClientEventListener);
        // 本示例仅针对 1v1 连麦场景，因此，关闭自动订阅选项。关于自动订阅的配置，可参考 https://developer.qiniu.com/rtc/8769/publish-and-subscribe-android#3
        mClient.setAutoSubscribe(false);
        // 4. 创建本地麦克风音频采集 Track
        initLocalTracks();
        // 初始化外部采集实例
        initExtCapture();
        // 5. 加入房间
        mClient.join(Config.ROOM_TOKEN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mExtAudioCapture.startCapture();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing() && mClient != null) {
            // 8. 离开房间
            mClient.leave();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExtAudioCapture.stopCapture();
        if (mCustomAudioTrack != null) {
            mCustomAudioTrack.destroy();
            mCustomAudioTrack = null;
        }
        if (APIApplication.mRTCInit) {
            // 9. 反初始化 RTC 释放资源
            QNRTC.deinit();
            APIApplication.mRTCInit = false;
        }
    }

    /**
     * 初始化本地视图
     */
    private void initView() {
        // 初始化远端音频提示视图
        mRemoteTrackTipsView = findViewById(R.id.remote_window_tips_view);
    }

    /**
     * 初始化本地麦克风采集 Track
     */
    private void initLocalTracks() {
        // 初始化外部导入音频 Track
        QNCustomAudioTrackConfig customAudioTrackConfig = new QNCustomAudioTrackConfig(Config.TAG_CUSTOM_AUDIO_TRACK)
                .setAudioQuality(new QNAudioQuality(Config.DEFAULT_AUDIO_SAMPLE_RATE, Config.DEFAULT_AUDIO_CHANNEL_COUNT,
                        16, Config.DEFAULT_AUDIO_BITRATE)); // 设置外部音频数据导入的目标编码参数
        mCustomAudioTrack = QNRTC.createCustomAudioTrack(customAudioTrackConfig);
    }

    /**
     * 初始化外部采集实例
     */
    private void initExtCapture() {
        mExtAudioCapture = new ExtAudioCapture();
        mExtAudioCapture.setOnAudioFrameCapturedListener(mOnAudioFrameCapturedListener);
    }

    private final ExtAudioCapture.OnAudioFrameCapturedListener mOnAudioFrameCapturedListener = new ExtAudioCapture.OnAudioFrameCapturedListener() {
        @Override
        public void onAudioFrameCaptured(byte[] audioData) {
            if (mCustomAudioTrack == null || TextUtils.isEmpty(mCustomAudioTrack.getTrackID())) {
                return;
            }
            // 推送自定义音频数据
            // 数据导入支持情况，请参考 https://developer.qiniu.com/rtc/8767/audio-and-video-collection-android#5
            QNAudioFrame audioFrame = new QNAudioFrame(ByteBuffer.wrap(audioData), audioData.length,
                    16, Config.DEFAULT_AUDIO_SAMPLE_RATE, Config.DEFAULT_AUDIO_CHANNEL_COUNT);
            mCustomAudioTrack.pushAudioFrame(audioFrame);
        }
    };

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
            ToastUtils.showShortToast(CustomAudioOnlyActivity.this,
                    String.format(getString(R.string.connection_state_changed), state.name()));
            if (state == QNConnectionState.CONNECTED) {
                // 6. 发布本地外部数据导入音频 Track
                // 发布订阅场景注意事项可参考 https://developer.qiniu.com/rtc/8769/publish-and-subscribe-android
                mClient.publish(new QNPublishResultCallback() {
                    @Override
                    public void onPublished() { // 发布成功
                        runOnUiThread(() -> ToastUtils.showShortToast(CustomAudioOnlyActivity.this,
                                getString(R.string.publish_success)));
                    }

                    @Override
                    public void onError(int errorCode, String errorMessage) { // 发布失败
                        runOnUiThread(() -> ToastUtils.showLongToast(CustomAudioOnlyActivity.this,
                                String.format(getString(R.string.publish_failed), errorCode, errorMessage)));
                    }
                }, mCustomAudioTrack);
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
            ToastUtils.showShortToast(CustomAudioOnlyActivity.this, getString(R.string.remote_user_left_toast));
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
                // 7. 手动订阅远端音频 Track
                for (QNRemoteTrack remoteTrack : trackList) {
                    if (remoteTrack.isAudio()) {
                        mClient.subscribe(remoteTrack);
                    }
                }
            } else {
                ToastUtils.showShortToast(CustomAudioOnlyActivity.this, getString(R.string.toast_other_user_published));
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
                mRemoteTrackTipsView.setVisibility(View.INVISIBLE);
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
            if (remoteUserID.equals(mFirstRemoteUserID) && !remoteAudioTracks.isEmpty()) {
                // 成功订阅远端音频 Track 后，SDK 会默认对音频 Track 进行渲染，无需其他操作
                mRemoteTrackTipsView.setVisibility(View.VISIBLE);
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

        /**
         * 用户音量提示回调，本地远端一起回调，本地 user id 为空
         *
         * @param list 用户音量信息，按音量由高到低排序，静音用户不在此列表中体现。
         */
        @Override
        public void onUserVolumeIndication(List<QNAudioVolumeInfo> list) {

        }
    };
}
