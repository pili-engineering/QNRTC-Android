package com.qiniu.droid.rtc.api.examples.activity;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.qiniu.droid.rtc.QNAudioQualityPreset;
import com.qiniu.droid.rtc.QNAudioScene;
import com.qiniu.droid.rtc.QNAudioVolumeInfo;
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
import com.qiniu.droid.rtc.api.examples.APIApplication;
import com.qiniu.droid.rtc.api.examples.R;
import com.qiniu.droid.rtc.api.examples.utils.Config;
import com.qiniu.droid.rtc.api.examples.utils.ToastUtils;
import com.qiniu.droid.rtc.model.QNAudioDevice;

import java.text.NumberFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 1v1 麦克风纯音频通话场景
 * 本示例仅演示本地麦克风音频 Track 的发布和远端音频的订阅场景
 *
 * 主要步骤如下：
 * 1. 初始化视图
 * 2. 初始化 RTC
 * 3. 创建 QNRTCClient 对象
 * 4. 创建本地麦克风音频采集 Track
 * 5. 加入房间
 * 6. 发布本地麦克风音频 Track
 * 7. 订阅远端音频 Track
 * 8. 离开房间
 * 9. 反初始化 RTC 释放资源
 *
 * 文档参考：
 * - 音视频通话中的基本概念，请参考 https://developer.qiniu.com/rtc/9909/the-rtc-basic-concept
 * - 接口文档，请参考 https://developer.qiniu.com/rtc/8773/API%20%E6%A6%82%E8%A7%88
 */
public class MicrophoneOnlyActivity extends AppCompatActivity {
    private static final String TAG = "MicrophoneOnlyActivity";
    private static final int GET_VOLUME_LEVEL_PERIOD = 500;
    private QNRTCClient mClient;
    private QNMicrophoneAudioTrack mMicrophoneAudioTrack;
    private QNRemoteAudioTrack mRemoteAudioTrack;

    // 定时器，用来周期性获取当前本地和远端的说话音量
    private Timer mAudioVolumeTimer;
    private NumberFormat mAudioVolumeFormat;

    private TextView mRemoteTrackTipsView;
    private TextView mLocalAudioVolumeTextView;
    private TextView mRemoteAudioVolumeTextView;
    private SeekBar mRemoteAudioVolumeSeekBar;
    private String mFirstRemoteUserID = null;
    private boolean mMicrophoneError;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_microphone_audio_only);

        mAudioVolumeFormat = NumberFormat.getNumberInstance() ;
        mAudioVolumeFormat.setMaximumFractionDigits(2);

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
        // 5. 加入房间
        mClient.join(Config.ROOM_TOKEN);
    }

    @Override
    protected void onResume() {
        super.onResume();
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
    protected void onDestroy() {
        super.onDestroy();
        stopAudioVolumeScheduler();
        if (mClient != null) {
            // 8. 离开房间
            mClient.leave();
            mClient = null;
        }
        if (mMicrophoneAudioTrack != null) {
            mMicrophoneAudioTrack.destroy();
            mMicrophoneAudioTrack = null;
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
        // 本地音频音量显示视图
        mLocalAudioVolumeTextView = findViewById(R.id.local_audio_volume);
        // 远端音频音量显示视图
        mRemoteAudioVolumeTextView = findViewById(R.id.remote_audio_volume);

        // 初始化本地音频采集音量设置控件
        SeekBar localAudioVolumeSeekBar = findViewById(R.id.local_audio_volume_seek_bar);
        localAudioVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 设置音频采集后的音量，该接口可用于适度对采集音量做放大或者缩小
                // 音量值在 0.0 - 1.0f 之间为软件缩小；1.0f 为原始音量播放；大于 1.0f 且小于 10.0f 为软件放大,
                // 在需要放大时，应从 1.x 开始设置用最小的放大值来取得合适的播放效果，过大将可能出现音频失真的现象
                if (mMicrophoneAudioTrack != null) {
                    mMicrophoneAudioTrack.setVolume((double) seekBar.getProgress() / 10.0);
                }
            }
        });

        // 初始化远端音频播放音量设置控件
        mRemoteAudioVolumeSeekBar = findViewById(R.id.remote_audio_volume_seek_bar);
        mRemoteAudioVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mRemoteAudioTrack != null) {
                    // 设置远端音频播放音量，该接口可用于适度对播放音量做放大或者缩小
                    // 音量值在 0.0 - 1.0f 之间为软件缩小；1.0f 为原始音量播放；大于 1.0f 且小于 10.0f 为软件放大,
                    // 在需要放大时，应从 1.x 开始设置用最小的放大值来取得合适的播放效果，过大将可能出现音频失真的现象
                    mRemoteAudioTrack.setVolume((double) seekBar.getProgress() / 10.0);
                }
            }
        });
        mRemoteAudioVolumeSeekBar.setEnabled(false);

        // 设置音频场景，不同场景使用不同的音频模式，对应调整的设备音量也存在差异
        RadioGroup audioSceneRadioGroup = findViewById(R.id.audio_scene_button);
        audioSceneRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.default_audio_scene) {
                // 默认场景，仅发布或仅订阅时，SDK 使用媒体模式；同时发布和订阅时，SDK 自动切换到通话模式
                QNRTC.setAudioScene(QNAudioScene.DEFAULT);
            } else if (checkedId == R.id.voice_chat_audio_scene) {
                // 清晰语聊场景，使用通话模式，调节音量为通话音量；为了人声清晰，环境音和音乐声会有一定抑制
                QNRTC.setAudioScene(QNAudioScene.VOICE_CHAT);
            } else {
                // 音质均衡场景，使用媒体模式，调节音量为媒体音量；平衡音质，对环境音和音乐声的还原性更优
                QNRTC.setAudioScene(QNAudioScene.SOUND_EQUALIZE);
            }
        });
    }

    /**
     * 初始化本地麦克风采集 Track
     */
    private void initLocalTracks() {
        // 创建麦克风采集 Track
        QNMicrophoneAudioTrackConfig microphoneAudioTrackConfig = new QNMicrophoneAudioTrackConfig(Config.TAG_MICROPHONE_TRACK)
                .setAudioQuality(QNAudioQualityPreset.STANDARD); // 设置音频参数，建议实时音视频通话场景使用默认值即可
        mMicrophoneAudioTrack = QNRTC.createMicrophoneAudioTrack(microphoneAudioTrackConfig);
        mMicrophoneAudioTrack.setMicrophoneEventListener((errorCode, errorMessage) -> mMicrophoneError = true);
    }

    /**
     * 获取本地和远端的音频音量
     *
     * 在安静的环境下，获取到 0.0x 大小的数值为环境音的音量，属于预期现象，您可根据您的需求自行决定判断的阈值
     */
    private void startAudioVolumeScheduler() {
        if (mAudioVolumeTimer != null) {
            return;
        }
        mAudioVolumeTimer = new Timer();
        mAudioVolumeTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (mMicrophoneAudioTrack != null) {
                        mLocalAudioVolumeTextView.setText(mAudioVolumeFormat.format(mMicrophoneAudioTrack.getVolumeLevel()));
                    }
                    if (mRemoteAudioTrack != null) {
                        mRemoteAudioVolumeTextView.setText(mAudioVolumeFormat.format(mRemoteAudioTrack.getVolumeLevel()));
                    }
                });
            }
        }, 0, GET_VOLUME_LEVEL_PERIOD);
    }

    /**
     * 停止获取本地和远端的音频音量
     */
    private void stopAudioVolumeScheduler() {
        if (mAudioVolumeTimer != null) {
            mAudioVolumeTimer.cancel();
            mAudioVolumeTimer = null;
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
            ToastUtils.showShortToast(MicrophoneOnlyActivity.this,
                    String.format(getString(R.string.connection_state_changed), state.name()));
            if (state == QNConnectionState.CONNECTED) {
                // 6. 发布本地麦克风音频 Track
                // 发布订阅场景注意事项可参考 https://developer.qiniu.com/rtc/8769/publish-and-subscribe-android
                mClient.publish(new QNPublishResultCallback() {
                    @Override
                    public void onPublished() { // 发布成功
                        runOnUiThread(() -> ToastUtils.showShortToast(MicrophoneOnlyActivity.this,
                                getString(R.string.publish_success)));
                        startAudioVolumeScheduler(); // 开启获取音频音量的定时器
                    }

                    @Override
                    public void onError(int errorCode, String errorMessage) { // 发布失败
                        runOnUiThread(() -> ToastUtils.showLongToast(MicrophoneOnlyActivity.this,
                                String.format(getString(R.string.publish_failed), errorCode, errorMessage)));
                    }
                }, mMicrophoneAudioTrack);
            } else if (state == QNConnectionState.DISCONNECTED) {
                stopAudioVolumeScheduler();
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
            ToastUtils.showShortToast(MicrophoneOnlyActivity.this, getString(R.string.remote_user_left_toast));
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
                ToastUtils.showShortToast(MicrophoneOnlyActivity.this, getString(R.string.toast_other_user_published));
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
                mRemoteAudioTrack = null;
                mRemoteTrackTipsView.setVisibility(View.INVISIBLE);
                mRemoteAudioVolumeSeekBar.setProgress(10);
                mRemoteAudioVolumeSeekBar.setEnabled(false);
                mRemoteAudioVolumeTextView.setVisibility(View.INVISIBLE);
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
                mRemoteAudioTrack = remoteAudioTracks.get(0);
                mRemoteAudioVolumeSeekBar.setEnabled(true);
                mRemoteTrackTipsView.setVisibility(View.VISIBLE);

                // 开启获取音频音量的定时器
                startAudioVolumeScheduler();
                mRemoteAudioVolumeTextView.setVisibility(View.VISIBLE);
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
