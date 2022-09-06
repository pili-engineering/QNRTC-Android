package com.qiniu.droid.rtc.api.examples.activity;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.qiniu.droid.rtc.QNAudioMixerListener;
import com.qiniu.droid.rtc.QNAudioMixerState;
import com.qiniu.droid.rtc.QNAudioMusicMixer;
import com.qiniu.droid.rtc.QNAudioMusicMixerListener;
import com.qiniu.droid.rtc.QNAudioMusicMixerState;
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
import com.qiniu.droid.rtc.api.examples.R;
import com.qiniu.droid.rtc.api.examples.utils.Config;
import com.qiniu.droid.rtc.api.examples.utils.ToastUtils;
import com.qiniu.droid.rtc.model.QNAudioDevice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 1v1 音频通话 + 混音场景
 * 本示例仅演示音频 Track 的发布订阅 + 混音的场景
 * <p>
 * 主要步骤如下：
 * 1. 初始化视图
 * 2. 初始化 RTC
 * 3. 创建 QNRTCClient 对象
 * 4. 创建本地麦克风音频采集 Track
 * 5. 加入房间
 * 6. 发布本地麦克风音频 Track
 * 7. 订阅远端音频 Track（可选操作）
 * 8. 混音操作
 * 9. 离开房间
 * 10. 反初始化 RTC 释放资源
 * <p>
 * 文档参考：
 * - 背景音乐混音的使用指南，请参考 https://developer.qiniu.com/rtc/8771/background-music-mix-android
 * - 背景音乐混音的注意事项，请参考 https://developer.qiniu.com/rtc/8771/background-music-mix-android#3
 * - 混音场景错误码，请参考 https://developer.qiniu.com/rtc/9904/rtc-error-code-android#4
 */
public class AudioMixerActivity extends AppCompatActivity {
    private static final String TAG = "AudioMixerActivity";
    private QNRTCClient mClient;
    private QNMicrophoneAudioTrack mMicrophoneAudioTrack;
    private QNAudioMusicMixer mAudioMusicMixer;
    private QNAudioMusicMixerState mAudioMixerState = QNAudioMusicMixerState.IDLE;

    private TextView mRemoteTrackTipsView;
    private EditText mMusicUrlEditText;
    private EditText mLoopTimeEditText;
    private Button mStartAudioMixButton;
    private Button mPauseAudioMixButton;
    private Switch mEarMonitorOnSwitch;
    private SeekBar mProgressSeekBar;
    private TextView mProgressTextView;
    private SeekBar mMicrophoneAudioVolumeSeekBar;
    private SeekBar mMusicMixVolumeSeekBar;
    private SeekBar mMusicPlayVolumeSeekBar;
    private boolean mIsAudioMixerControllable = true;
    private String mFirstRemoteUserID = null;
    private String mMusicPath;
    private long mMusicDurationMs;

    private float mMicrophoneAudioVolume = 1.0f;
    private float mMusicMixVolume = 1.0f;

    private Handler mSubThreadHandler;
    private boolean mMicrophoneError;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_audio_mixer);

        HandlerThread ht = new HandlerThread(TAG);
        ht.start();
        mSubThreadHandler = new Handler(ht.getLooper());

        // 检查本地是否存在指定音乐文件
        checkMusicFile();
        // 1. 初始化视图
        initView();
        // 2. 初始化 RTC
        QNRTC.init(this, mRTCEventListener);
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
        // 10. 反初始化 RTC 释放资源
        QNRTC.deinit();
    }

    /**
     * 初始化本地视图
     */
    private void initView() {
        // 初始化远端音频提示视图
        mRemoteTrackTipsView = findViewById(R.id.remote_window_tips_view);

        mMusicUrlEditText = findViewById(R.id.music_url_edit_text);
        mMusicUrlEditText.setText(mMusicPath);
        mLoopTimeEditText = findViewById(R.id.loop_times_edit_text);
        mProgressTextView = findViewById(R.id.progress_text);
        mProgressTextView.setText(String.format(getString(R.string.audio_mix_progress), "00:00", "00:00"));

        // 初始化开始、停止混音控件
        mStartAudioMixButton = findViewById(R.id.start_mix_button);
        mStartAudioMixButton.setOnClickListener(v -> {
            if (TextUtils.isEmpty(mMusicUrlEditText.getText())) {
                ToastUtils.showShortToast(AudioMixerActivity.this, getString(R.string.invalid_music_url_toast));
                return;
            }
            if (TextUtils.isEmpty(mLoopTimeEditText.getText())) {
                ToastUtils.showShortToast(AudioMixerActivity.this, getString(R.string.invalid_loop_times_toast));
                return;
            }
            if (mAudioMixerState == QNAudioMusicMixerState.IDLE
                    || mAudioMixerState == QNAudioMusicMixerState.STOPPED
                    || mAudioMixerState == QNAudioMusicMixerState.COMPLETED) {
                // 开始音乐混音
                startAudioMix(mMusicUrlEditText.getText().toString(),
                        Integer.parseInt(mLoopTimeEditText.getText().toString()), mProgressSeekBar);
            } else if (mAudioMixerState == QNAudioMusicMixerState.MIXING || mAudioMixerState == QNAudioMusicMixerState.PAUSED) {
                mAudioMusicMixer.stop();
            }
        });

        // 初始化暂停、恢复混音控件
        mPauseAudioMixButton = findViewById(R.id.pause_mix_button);
        mPauseAudioMixButton.setOnClickListener(v -> {
            if (mAudioMixerState == QNAudioMusicMixerState.STOPPED
                    || mAudioMixerState == QNAudioMusicMixerState.COMPLETED) {
                ToastUtils.showShortToast(AudioMixerActivity.this, getString(R.string.audio_mix_first_toast));
                return;
            }
            if (mAudioMixerState == QNAudioMusicMixerState.MIXING) {
                // 暂停音乐混音
                mAudioMusicMixer.pause();
            } else if (mAudioMixerState == QNAudioMusicMixerState.PAUSED) {
                // 从暂停处开始恢复音乐混音
                mAudioMusicMixer.resume();
            }
        });

        mEarMonitorOnSwitch = findViewById(R.id.ear_monitor_on);
        mEarMonitorOnSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 开启返听，建议在佩戴耳机的场景下使用该接口
            if (mMicrophoneAudioTrack != null) {
                mMicrophoneAudioTrack.setEarMonitorEnabled(isChecked);
            }
        });

        mProgressSeekBar = findViewById(R.id.audio_mix_progress);
        mProgressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mAudioMusicMixer != null) {
                    // 跳到指定位置进行混音，单位为 us
                    mAudioMusicMixer.seekTo(seekBar.getProgress());
                }
            }
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
                if (mMicrophoneAudioTrack != null) {
                    mMicrophoneAudioVolume = seekBar.getProgress() / 100.0f;
                    mMicrophoneAudioTrack.setVolume(mMicrophoneAudioVolume);
                }
            }
        });

        // 初始化音乐混音音量设置控件
        mMusicMixVolumeSeekBar = findViewById(R.id.seek_bar_music_volume);
        mMusicMixVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mAudioMusicMixer != null) {
                    // 设置音乐混音音量，【 0.0f - 1.0f 】
                    mMusicMixVolume = seekBar.getProgress() / 100.0f;
                    mAudioMusicMixer.setMixingVolume(mMusicMixVolume);
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
     * 开启音乐混音
     *
     * @param filePath 混音文件路径
     * @param loopTimes 混音次数，-1 为无限循环
     * @param durationProgress 混音进度控件
     */
    private void startAudioMix(String filePath, int loopTimes, final SeekBar durationProgress) {
        if (mMicrophoneAudioTrack == null) {
            ToastUtils.showShortToast(getApplicationContext(), "请先创建音频轨道");
            return;
        }
        mSubThreadHandler.post(() -> {
            // 创建混音管理器 QNAudioMusicMixer 实例
            // 当前仅支持同一时间混一路背景音乐，若需要切换混音的背景音乐，可通过重新调用 MicrophoneAudioTrack.createAudioMusicMixer
            // 创建 QNAudioMusicMixer 的方式实现。
            mAudioMusicMixer = mMicrophoneAudioTrack.createAudioMusicMixer(filePath, new QNAudioMusicMixerListener() {
                /**
                 * 混音状态改变时触发
                 *
                 * @param state 当前状态
                 */
                @Override
                public void onStateChanged(QNAudioMusicMixerState state) {
                    Log.i(TAG, "混音状态改变 : " + state.name());
                    mAudioMixerState = state;
                    if (state == QNAudioMusicMixerState.MIXING) {
                        setAudioMixerControllable(true);
                        mStartAudioMixButton.setText(getString(R.string.stop_audio_mix));
                        mPauseAudioMixButton.setText(getString(R.string.pause_audio_mix));
                    }
                    if (state == QNAudioMusicMixerState.PAUSED) {
                        mPauseAudioMixButton.setText(getString(R.string.resume_audio_mix));
                    }
                    if (state == QNAudioMusicMixerState.STOPPED || state == QNAudioMusicMixerState.COMPLETED) {
                        durationProgress.setProgress(0);
                        setAudioMixerControllable(false);
                        mStartAudioMixButton.setText(getString(R.string.start_audio_mix));
                        mPauseAudioMixButton.setText(getString(R.string.pause_audio_mix));
                    }
                }

                /**
                 * 混音过程中触发
                 *
                 * @param position 当前的混音时间，单位：ms
                 */
                @Override
                public void onMixing(long position) {
                    durationProgress.setProgress((int) position);
                    SimpleDateFormat formatter = new SimpleDateFormat("mm:ss", Locale.CHINA);
                    mProgressTextView.setText(String.format(getString(R.string.audio_mix_progress),
                            formatter.format(position), formatter.format(mMusicDurationMs)));
                }

                /**
                 * 混音发生错误时触发
                 * 对应错误码可参考 https://developer.qiniu.com/rtc/9904/rtc-error-code-android#4
                 *
                 * @param errorCode 错误码
                 */
                @Override
                public void onError(int errorCode, String errorMessage) {
                    ToastUtils.showShortToast(AudioMixerActivity.this,
                            String.format(getString(R.string.audio_mix_error), errorCode, errorMessage));
                }
            });
            // QNAudioMusicMixer.getDuration 接口为同步方法，在获取在线音乐时长时可能存在耗时，因此，可根据实际需求决定是否要放到子线程执行
            mMusicDurationMs = QNAudioMusicMixer.getDuration(filePath);
            durationProgress.setMax((int) mMusicDurationMs);
            // 开始混音
            mAudioMusicMixer.start(loopTimes);
        });
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
        mMusicMixVolumeSeekBar.setEnabled(mIsAudioMixerControllable);
        mMusicPlayVolumeSeekBar.setEnabled(mIsAudioMixerControllable);
        mEarMonitorOnSwitch.setEnabled(mIsAudioMixerControllable);
    }

    /**
     * 检查音乐文件是否存在，不存在则拷贝到存储中
     */
    private void checkMusicFile() {
        try {
            mMusicPath = getExternalFilesDir(Environment.DIRECTORY_MUSIC) + File.separator + "music.mp3";
            File musicFile = new File(mMusicPath);
            if (musicFile.exists()) {
                return;
            }
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.prepare_music_tips))
                    .setCancelable(false)
                    .show();
            InputStream is = getAssets().open("music.mp3");
            FileOutputStream fos = new FileOutputStream(mMusicPath);
            byte[] buffer = new byte[1024];
            int byteCount;
            while ((byteCount = is.read(buffer)) != -1) {
                fos.write(buffer, 0, byteCount);
            }
            fos.flush();
            is.close();
            fos.close();
            alertDialog.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
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
            ToastUtils.showShortToast(AudioMixerActivity.this,
                    String.format(getString(R.string.connection_state_changed), state.name()));
            if (state == QNConnectionState.CONNECTED) {
                // 6. 发布本地麦克风音频 Track
                // 发布订阅场景注意事项可参考 https://developer.qiniu.com/rtc/8769/publish-and-subscribe-android
                mClient.publish(new QNPublishResultCallback() {
                    @Override
                    public void onPublished() { // 发布成功
                        runOnUiThread(() -> ToastUtils.showShortToast(AudioMixerActivity.this,
                                getString(R.string.publish_success)));
                    }

                    @Override
                    public void onError(int errorCode, String errorMessage) { // 发布失败
                        runOnUiThread(() -> ToastUtils.showLongToast(AudioMixerActivity.this,
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
            ToastUtils.showShortToast(AudioMixerActivity.this, getString(R.string.remote_user_left_toast));
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
                ToastUtils.showShortToast(AudioMixerActivity.this, getString(R.string.toast_other_user_published));
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

