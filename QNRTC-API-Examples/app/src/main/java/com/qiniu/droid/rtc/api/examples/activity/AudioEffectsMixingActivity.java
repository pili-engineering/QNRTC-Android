package com.qiniu.droid.rtc.api.examples.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.qiniu.droid.rtc.QNAudioEffect;
import com.qiniu.droid.rtc.QNAudioEffectMixer;
import com.qiniu.droid.rtc.QNAudioEffectMixerListener;
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
import com.qiniu.droid.rtc.QNRemoteAudioTrack;
import com.qiniu.droid.rtc.QNRemoteTrack;
import com.qiniu.droid.rtc.QNRemoteVideoTrack;
import com.qiniu.droid.rtc.api.examples.APIApplication;
import com.qiniu.droid.rtc.api.examples.R;
import com.qiniu.droid.rtc.api.examples.adapter.AudioEffectAdapter;
import com.qiniu.droid.rtc.api.examples.model.AudioEffect;
import com.qiniu.droid.rtc.api.examples.utils.Config;
import com.qiniu.droid.rtc.api.examples.utils.ToastUtils;
import com.qiniu.droid.rtc.model.QNAudioDevice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 1v1 音频通话 + 音效混音场景
 * 本示例仅演示音频 Track 的发布订阅 + 音效混音的场景
 * <p>
 * 主要步骤如下：
 * 1. 初始化视图
 * 2. 初始化 RTC
 * 3. 创建 QNRTCClient 对象
 * 4. 创建本地麦克风音频采集 Track
 * 5. 加入房间
 * 6. 发布本地麦克风音频 Track
 * 7. 订阅远端音频 Track（可选操作）
 * 8. 音效混音操作
 * 9. 离开房间
 * 10. 反初始化 RTC 释放资源
 * <p>
 * 文档参考：
 * - 音效混音的使用指南，请参考 https://developer.qiniu.com/rtc/11965/android_audio_effect_mixing
 * - 混音场景错误码，请参考 https://developer.qiniu.com/rtc/9904/rtc-error-code-android#4
 */
@SuppressLint("LongLogTag")
public class AudioEffectsMixingActivity extends AppCompatActivity {
    private static final String TAG = "AudioEffectsMixingActivity";
    private static final String AUDIO_EFFECTS_DIR = "effects";
    private QNRTCClient mClient;
    private QNMicrophoneAudioTrack mMicrophoneAudioTrack;
    private QNAudioEffectMixer mAudioEffectMixer;

    private TextView mRemoteTrackTipsView;
    private Button mPauseAllMixButton;
    private Switch mEarMonitorOnSwitch;
    private SeekBar mMicrophoneAudioVolumeSeekBar;
    private SeekBar mMusicPlayVolumeSeekBar;
    private boolean mIsAudioMixerControllable = true;
    private boolean mIsEffectsMixPaused;
    private String mFirstRemoteUserID = null;

    private float mMicrophoneAudioVolume = 1.0f;

    private Handler mSubThreadHandler;
    private boolean mMicrophoneError;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_audio_effects);

        HandlerThread ht = new HandlerThread(TAG);
        ht.start();
        mSubThreadHandler = new Handler(ht.getLooper());

        // 1. 初始化视图
        initView();
        // 检查本地是否存在指定音效文件
        mSubThreadHandler.post(() -> checkAudioEffectFiles(getApplicationContext()));
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
    protected void onPause() {
        super.onPause();
        if (isFinishing() && mClient != null) {
            // 9. 离开房间
            mClient.leave();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSubThreadHandler.getLooper().quit();
        mSubThreadHandler = null;
        if (mMicrophoneAudioTrack != null) {
            mMicrophoneAudioTrack.destroy();
            mMicrophoneAudioTrack = null;
        }
        if (APIApplication.mRTCInit) {
            // 10. 反初始化 RTC 释放资源
            QNRTC.deinit();
            APIApplication.mRTCInit = false;
        }
    }

    /**
     * 初始化除音效混音外的本地视图
     */
    private void initView() {
        // 初始化远端音频提示视图
        mRemoteTrackTipsView = findViewById(R.id.remote_window_tips_view);

        mEarMonitorOnSwitch = findViewById(R.id.ear_monitor_on);
        mEarMonitorOnSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 开启返听，建议在佩戴耳机的场景下使用该接口
            mMicrophoneAudioTrack.setEarMonitorEnabled(isChecked);
        });

        // 初始化麦克风混音音量设置控件
        mMicrophoneAudioVolumeSeekBar = findViewById(R.id.seek_bar_microphone_volume);
        mMicrophoneAudioVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 设置麦克风混音音量，【 0.0f - 1.0f 】
                mMicrophoneAudioVolume = seekBar.getProgress() / 100.0f;
                if (mMicrophoneAudioTrack != null) {
                    mMicrophoneAudioTrack.setVolume(mMicrophoneAudioVolume);
                }
            }
        });

        // 初始化音乐本地播放音量设置控件
        mMusicPlayVolumeSeekBar = findViewById(R.id.seek_bar_music_play_volume);
        mMusicPlayVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 设置音乐本地播放音量，【 0.0f - 1.0f 】
                if (mMicrophoneAudioTrack != null) {
                    mMicrophoneAudioTrack.setPlayingVolume(seekBar.getProgress() / 100.0f);
                }
            }
        });
        setAudioMixerControllable(false);
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
     * 设置混音相关控件是否可操作
     *
     * @param controllable 是否可操作
     */
    private void setAudioMixerControllable(boolean controllable) {
        if (mIsAudioMixerControllable == controllable) {
            return;
        }
        mIsAudioMixerControllable = controllable;
        mMicrophoneAudioVolumeSeekBar.setEnabled(mIsAudioMixerControllable);
        mMusicPlayVolumeSeekBar.setEnabled(mIsAudioMixerControllable);
        mEarMonitorOnSwitch.setEnabled(mIsAudioMixerControllable);
    }

    /**
     * 检查音效文件是否存在，不存在则拷贝到存储中
     */
    public void checkAudioEffectFiles(Context context) {
        try {
            String[] fileNames = context.getAssets().list(AUDIO_EFFECTS_DIR);
            int fileCountHandled = 0;
            for (String fileName : fileNames) {
                String filePath = getExternalFilesDir(Environment.DIRECTORY_MUSIC) + File.separator + fileName;
                File audioFile = new File(filePath);
                if (audioFile.isDirectory()) {
                    continue;
                }
                if (audioFile.exists()) {
                    Log.i(TAG, fileName + " exist");
                    continue;
                }

                InputStream is = getAssets().open(AUDIO_EFFECTS_DIR + File.separator + fileName);
                FileOutputStream fos = new FileOutputStream(filePath);
                byte[] buffer = new byte[1024];
                int byteCount;
                while ((byteCount = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, byteCount);
                }
                fos.flush();
                is.close();
                fos.close();
                fileCountHandled++;
            }
            if (fileCountHandled != 0) {
                ToastUtils.showShortToast(this, "音效文件已准备");
            }
            setAudioMixerControllable(true);
            // 创建并初始化混音相关视图及其操作
            runOnUiThread(this::initEffectsView);
            Log.i(TAG, "checkAudioEffectFiles done");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 8. 音效混音操作
     */
    private void initEffectsView() {
        AudioEffectAdapter audioEffectAdapter = new AudioEffectAdapter();
        if (mAudioEffectMixer == null && mMicrophoneAudioTrack != null) {
            // 创建音效混音控制器，仅需创建一次即可
            mAudioEffectMixer = mMicrophoneAudioTrack.createAudioEffectMixer(new QNAudioEffectMixerListener() {
                @Override
                public void onEffectFinished(int effectID) {
                    ToastUtils.showShortToast(getApplicationContext(), "音效混音完成 : " + effectID);
                    audioEffectAdapter.audioEffectMixFinished(effectID);
                }

                @Override
                public void onEffectError(int effectID, int errCode, String errorMessage) {
                    ToastUtils.showShortToast(getApplicationContext(), "音效 " + effectID + " 混音出错 : " + errCode + " " + errorMessage);
                }

                @Override
                public void onError(int errorCode, String errorMessage) {
                    ToastUtils.showShortToast(getApplicationContext(), "音效混音出错 : " + errorCode + " " + errorMessage);
                }
            });
        }

        List<AudioEffect> audioEffects = new ArrayList<>();
        int audioEffectID = 0;
        try {
            String[] fileNames = getAssets().list(AUDIO_EFFECTS_DIR);
            for (String fileName : fileNames) {
                // 创建 QNAudioEffect 对象，用于进行音效混音配置
                QNAudioEffect audioEffect = mAudioEffectMixer.createAudioEffect(
                        audioEffectID++, getExternalFilesDir(Environment.DIRECTORY_MUSIC) + File.separator + fileName);
                audioEffects.add(new AudioEffect(audioEffect));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        RecyclerView audioEffectRv = findViewById(R.id.audio_effects_list);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        audioEffectRv.setLayoutManager(linearLayoutManager);
        DividerItemDecoration itemDecorator = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        audioEffectRv.addItemDecoration(itemDecorator);
        audioEffectAdapter.init(audioEffects, new AudioEffectAdapter.OnAudioEffectClickListener() {
            @Override
            public void onStartClicked(int effectID, boolean start) {
                if (mAudioEffectMixer == null) {
                    return;
                }
                if (start) {
                    mAudioEffectMixer.start(effectID); // 开始指定音效混音
                } else {
                    mAudioEffectMixer.stop(effectID); // 停止指定音效混音
                }
            }

            @Override
            public void onPauseClicked(int effectID, boolean pause) {
                if (mAudioEffectMixer == null) {
                    return;
                }
                if (pause) {
                    mAudioEffectMixer.pause(effectID); // 暂停指定音效混音
                } else {
                    mAudioEffectMixer.resume(effectID); // 恢复指定音效混音
                }
            }

            @Override
            public void onPublishClicked(int effectID, boolean publish) {
                if (mAudioEffectMixer == null) {
                    return;
                }
                mAudioEffectMixer.setPublishEnabled(effectID, publish);
            }
        });
        audioEffectRv.setAdapter(audioEffectAdapter);

        // 初始化开始、停止混音控件
        Button stopAllMixButton = findViewById(R.id.stop_all_button);
        stopAllMixButton.setOnClickListener(v -> {
            if (mAudioEffectMixer != null) {
                // 停止所有正在混音的音效
                mAudioEffectMixer.stopAll();
                audioEffectAdapter.stopAll();
            }
        });

        // 初始化暂停、恢复全部音效的混音控件
        mPauseAllMixButton = findViewById(R.id.pause_all_button);
        mPauseAllMixButton.setOnClickListener(v -> {
            if (mAudioEffectMixer != null) {
                if (mIsEffectsMixPaused) {
                    // 恢复所有暂停的音效混音
                    mAudioEffectMixer.resumeAll();
                    audioEffectAdapter.resumeAll();
                    mIsEffectsMixPaused = false;
                    mPauseAllMixButton.setText(R.string.pause_all);
                } else {
                    // 暂停所有正在混音的音效
                    mAudioEffectMixer.pauseAll();
                    audioEffectAdapter.pauseAll();
                    mIsEffectsMixPaused = true;
                    mPauseAllMixButton.setText(R.string.resume_all);
                }
            }
        });
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
            ToastUtils.showShortToast(AudioEffectsMixingActivity.this,
                    String.format(getString(R.string.connection_state_changed), state.name()));
            if (state == QNConnectionState.CONNECTED) {
                // 6. 发布本地麦克风音频 Track
                // 发布订阅场景注意事项可参考 https://developer.qiniu.com/rtc/8769/publish-and-subscribe-android
                mClient.publish(new QNPublishResultCallback() {
                    @Override
                    public void onPublished() { // 发布成功
                        runOnUiThread(() -> ToastUtils.showShortToast(AudioEffectsMixingActivity.this,
                                getString(R.string.publish_success)));
                    }

                    @Override
                    public void onError(int errorCode, String errorMessage) { // 发布失败
                        runOnUiThread(() -> ToastUtils.showLongToast(AudioEffectsMixingActivity.this,
                                String.format(getString(R.string.publish_failed), errorCode, errorMessage)));
                    }
                }, mMicrophoneAudioTrack);
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
            ToastUtils.showShortToast(AudioEffectsMixingActivity.this, getString(R.string.remote_user_left_toast));
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
                ToastUtils.showShortToast(AudioEffectsMixingActivity.this, getString(R.string.toast_other_user_published));
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
    };
}

