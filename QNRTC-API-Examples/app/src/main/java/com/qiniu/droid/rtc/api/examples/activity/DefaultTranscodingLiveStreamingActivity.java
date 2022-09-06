package com.qiniu.droid.rtc.api.examples.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.qiniu.droid.rtc.QNAudioQualityPreset;
import com.qiniu.droid.rtc.QNBeautySetting;
import com.qiniu.droid.rtc.QNCameraFacing;
import com.qiniu.droid.rtc.QNCameraVideoTrack;
import com.qiniu.droid.rtc.QNCameraVideoTrackConfig;
import com.qiniu.droid.rtc.QNClientEventListener;
import com.qiniu.droid.rtc.QNConnectionDisconnectedInfo;
import com.qiniu.droid.rtc.QNConnectionState;
import com.qiniu.droid.rtc.QNCustomMessage;
import com.qiniu.droid.rtc.QNLiveStreamingErrorInfo;
import com.qiniu.droid.rtc.QNLiveStreamingListener;
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
import com.qiniu.droid.rtc.QNTranscodingLiveStreamingTrack;
import com.qiniu.droid.rtc.QNVideoCaptureConfigPreset;
import com.qiniu.droid.rtc.QNVideoEncoderConfig;
import com.qiniu.droid.rtc.api.examples.R;
import com.qiniu.droid.rtc.api.examples.utils.Config;
import com.qiniu.droid.rtc.api.examples.utils.ToastUtils;
import com.qiniu.droid.rtc.model.QNAudioDevice;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 1v1 音视频通话 + 默认合流转推任务配置场景
 * <p>
 * 主要步骤如下：
 * 1. 初始化视图
 * 2. 初始化 RTC
 * 3. 创建 QNRTCClient 对象
 * 4. 设置 CDN 转推事件监听器
 * 5. 创建本地音视频 Track
 * 6. 加入房间
 * 7. 发布本地音视频 Track
 * 8. 订阅远端音视频 Track
 * 9. 配置（新增/移除）合流布局
 * 10. 离开房间
 * 11. 反初始化 RTC 释放资源
 * <p>
 * 文档参考：
 * - CDN 转推任务文档，请参考 https://developer.qiniu.com/rtc/8770/turn-the-cdn-push-android
 * - 接口文档，请参考 https://developer.qiniu.com/rtc/8773/API%20%E6%A6%82%E8%A7%88
 */
public class DefaultTranscodingLiveStreamingActivity extends AppCompatActivity {
    private static final String TAG = "DefaultTranscodingLiveStreamingActivity";
    private QNRTCClient mClient;
    private QNSurfaceView mLocalRenderView;
    private QNSurfaceView mRemoteRenderView;
    private QNCameraVideoTrack mCameraVideoTrack;
    private QNMicrophoneAudioTrack mMicrophoneAudioTrack;
    private QNRemoteVideoTrack mRemoteVideoTrack;
    private QNRemoteAudioTrack mRemoteAudioTrack;

    private final List<QNTranscodingLiveStreamingTrack> mTranscodingLiveStreamingTracks = new ArrayList<>();

    private EditText mXEditText;
    private EditText mYEditText;
    private EditText mWidthEditText;
    private EditText mHeightEditText;
    private EditText mZOrderEditText;

    private String mFirstRemoteUserID = null;
    private boolean mIsLocalUserConfig = true;
    private boolean mIsLocalPublished;
    private boolean mMicrophoneError;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_default_transcoding_live_streaming);

        // 1. 初始化视图
        initView();
        // 2. 初始化 RTC
        QNRTC.init(this, mRTCEventListener);
        // 3. 创建 QNRTCClient 对象
        mClient = QNRTC.createClient(mClientEventListener);
        // 4. 设置 CDN 转推事件监听器
        mClient.setLiveStreamingListener(mLiveStreamingListener);
        // 本示例仅针对 1v1 连麦场景，因此，关闭自动订阅选项。关于自动订阅的配置，可参考 https://developer.qiniu.com/rtc/8769/publish-and-subscribe-android#3
        mClient.setAutoSubscribe(false);
        // 5. 创建本地音视频 Track
        initLocalTracks();
        // 6. 加入房间
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
            // 10. 离开房间
            mClient.leave();
            mClient = null;
        }
        destroyLocalTracks();
        // 11. 反初始化 RTC 释放资源
        QNRTC.deinit();
    }

    /**
     * 新增合流布局
     */
    public void onClickAddTranscodingTracks(View view) {
        if (TextUtils.isEmpty(mXEditText.getText()) || TextUtils.isEmpty(mYEditText.getText())
                || TextUtils.isEmpty(mWidthEditText.getText()) || TextUtils.isEmpty(mHeightEditText.getText())
                || TextUtils.isEmpty(mZOrderEditText.getText())) {
            ToastUtils.showShortToast(this, getString(R.string.invalid_parameters_toast));
            return;
        }
        if (mIsLocalUserConfig && !mIsLocalPublished) {
            ToastUtils.showShortToast(this, getString(R.string.publish_first_toast));
            return;
        }
        if (!mIsLocalUserConfig && mFirstRemoteUserID == null) {
            ToastUtils.showShortToast(this, getString(R.string.subscribe_first_toast));
            return;
        }
        List<QNTranscodingLiveStreamingTrack> addLiveStreamingTracks = new ArrayList<>();
        if (mIsLocalUserConfig || mRemoteVideoTrack != null) {
            QNTranscodingLiveStreamingTrack videoTranscodingTrack = null;
            for (QNTranscodingLiveStreamingTrack liveStreamingTrack : mTranscodingLiveStreamingTracks) {
                if ((mIsLocalUserConfig && liveStreamingTrack.getTrackID().equals(mCameraVideoTrack.getTrackID()))
                        || (!mIsLocalUserConfig && liveStreamingTrack.getTrackID().equals(mRemoteVideoTrack.getTrackID()))) {
                    videoTranscodingTrack = liveStreamingTrack;
                    break;
                }
            }
            // 设置视频 Track 的合流布局
            if (videoTranscodingTrack == null) {
                videoTranscodingTrack = new QNTranscodingLiveStreamingTrack();
                videoTranscodingTrack.setTrackID(mIsLocalUserConfig ? mCameraVideoTrack.getTrackID() : mRemoteVideoTrack.getTrackID()); // 设置 TrackID
                mTranscodingLiveStreamingTracks.add(videoTranscodingTrack);
            }
            videoTranscodingTrack.setX(Integer.parseInt(mXEditText.getText().toString())); // 设置视频 Track 在合流布局中位置的 x 坐标
            videoTranscodingTrack.setY(Integer.parseInt(mYEditText.getText().toString())); // 设置视频 Track 在合流布局中位置的 y 坐标
            videoTranscodingTrack.setWidth(Integer.parseInt(mWidthEditText.getText().toString())); // 设置视频 Track 在合流布局中位置的宽度
            videoTranscodingTrack.setHeight(Integer.parseInt(mHeightEditText.getText().toString())); // 设置视频 Track 在合流布局中位置的高度
            videoTranscodingTrack.setZOrder(Integer.parseInt(mZOrderEditText.getText().toString())); // 设置视频 Track 在合流布局中位置的层级
            addLiveStreamingTracks.add(videoTranscodingTrack);
        }

        if (mIsLocalUserConfig || mRemoteAudioTrack != null) {
            QNTranscodingLiveStreamingTrack audioTranscodingTrack = null;
            for (QNTranscodingLiveStreamingTrack liveStreamingTrack : mTranscodingLiveStreamingTracks) {
                if ((mIsLocalUserConfig && liveStreamingTrack.getTrackID().equals(mMicrophoneAudioTrack.getTrackID()))
                        || (!mIsLocalUserConfig && liveStreamingTrack.getTrackID().equals(mRemoteAudioTrack.getTrackID()))) {
                    audioTranscodingTrack = liveStreamingTrack;
                    break;
                }
            }
            if (audioTranscodingTrack == null) {
                // 将音频 Track 添加到合流任务中，仅需配置 TrackID 即可
                audioTranscodingTrack = new QNTranscodingLiveStreamingTrack();
                audioTranscodingTrack.setTrackID(mIsLocalUserConfig ? mMicrophoneAudioTrack.getTrackID() : mRemoteAudioTrack.getTrackID());; // 设置 TrackID
                mTranscodingLiveStreamingTracks.add(audioTranscodingTrack);
            }
            addLiveStreamingTracks.add(audioTranscodingTrack);
        }
        // 9. 配置（新增）合流布局，streamID 为 null 代表设置默认合流任务的合流布局
        mClient.setTranscodingLiveStreamingTracks(null, addLiveStreamingTracks);
    }

    /**
     * 移除合流布局
     */
    public void onClickRemoveTranscodingTracks(View view) {
        List<QNTranscodingLiveStreamingTrack> removeTranscodingTracks = new ArrayList<>();
        for (QNTranscodingLiveStreamingTrack liveStreamingTrack : mTranscodingLiveStreamingTracks) {
            if (mIsLocalUserConfig &&
                    (liveStreamingTrack.getTrackID().equals(mCameraVideoTrack.getTrackID())
                    || liveStreamingTrack.getTrackID().equals(mMicrophoneAudioTrack.getTrackID()))) {
                removeTranscodingTracks.add(liveStreamingTrack);
            }
            if (!mIsLocalUserConfig &&
                    (liveStreamingTrack.getTrackID().equals(mRemoteVideoTrack.getTrackID())
                            || liveStreamingTrack.getTrackID().equals(mRemoteAudioTrack.getTrackID()))) {
                removeTranscodingTracks.add(liveStreamingTrack);
            }
        }
        if (!removeTranscodingTracks.isEmpty()) {
            // 9. 配置（移除）合流布局，streamID 为 null 代表设置默认合流任务的合流布局
            mClient.removeTranscodingLiveStreamingTracks(null, removeTranscodingTracks);
        }
    }

    /**
     * 初始化视图
     */
    private void initView() {
        // 初始化本地预览视图
        mLocalRenderView = findViewById(R.id.local_render_view);
        mLocalRenderView.setZOrderOnTop(true);
        // 初始化远端预览视图
        mRemoteRenderView = findViewById(R.id.remote_render_view);
        mRemoteRenderView.setZOrderOnTop(true);

        mXEditText = findViewById(R.id.layout_x_edit_text);
        mYEditText = findViewById(R.id.layout_y_edit_text);
        mWidthEditText = findViewById(R.id.layout_width_edit_text);
        mHeightEditText = findViewById(R.id.layout_height_edit_text);
        mZOrderEditText = findViewById(R.id.layout_z_order_edit_text);

        RadioGroup roleSelectRadioGroup = findViewById(R.id.role_select_radio_group);
        roleSelectRadioGroup.setOnCheckedChangeListener((group, checkedId) -> mIsLocalUserConfig = checkedId == R.id.local_user_setting);
    }

    /**
     * 创建音视频采集 Track
     * <p>
     * 摄像头采集 Track 创建方式：
     * 1. 创建 QNCameraVideoTrackConfig 对象，指定采集编码相关的配置
     * 2. 通过 QNCameraVideoTrackConfig 对象创建摄像头采集 Track，若使用无参的方法创建则会使用 SDK 默认配置参数（640x480, 20fps, 800kbps）
     * <p>
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
                .setMultiProfileEnabled(false); // 设置是否开启大小流
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
     * CDN 转推事件监听
     * CDN 转推使用指南可参考 https://developer.qiniu.com/rtc/8770/turn-the-cdn-push-android
     */
    private final QNLiveStreamingListener mLiveStreamingListener = new QNLiveStreamingListener() {
        /**
         * 转推任务开始后会触发该回调
         *
         * @param streamID 已开始的转推任务的 streamID
         */
        @Override
        public void onStarted(String streamID) {

        }

        /**
         * 转推任务停止后会触发该回调
         *
         * @param streamID 已停止的转推任务的 streamID
         */
        @Override
        public void onStopped(String streamID) {

        }

        /**
         * 合流转推任务布局更新时会触发该回调
         *
         * @param streamID 布局更新的转推任务的 streamID
         */
        @Override
        public void onTranscodingTracksUpdated(String streamID) {
            ToastUtils.showShortToast(DefaultTranscodingLiveStreamingActivity.this, getString(R.string.transcoding_layout_updated_toast));
        }

        /**
         * 转推任务发生错误是会触发该回调
         * CDN 转推任务错误码可参考 https://developer.qiniu.com/rtc/9904/rtc-error-code-android#5
         *
         * @param streamID 出错的转推任务的 streamID
         * @param errorInfo 错误信息
         */
        @Override
        public void onError(String streamID, QNLiveStreamingErrorInfo errorInfo) {
            if (errorInfo == null) {
                return;
            }
            switch (errorInfo.type) {
                case START:
                    // 开始单路转推场景下出错
                    ToastUtils.showShortToast(DefaultTranscodingLiveStreamingActivity.this,
                            String.format(getString(R.string.live_streaming_error), "开始", errorInfo.code));
                    break;
                case STOP:
                    // 停止单路转推场景下出错
                    ToastUtils.showShortToast(DefaultTranscodingLiveStreamingActivity.this,
                            String.format(getString(R.string.live_streaming_error), "停止", errorInfo.code));
                    break;
            }
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
            ToastUtils.showShortToast(DefaultTranscodingLiveStreamingActivity.this,
                    String.format(getString(R.string.connection_state_changed), state.name()));
            if (state == QNConnectionState.CONNECTED) {
                // 7. 发布本地音视频 Track
                // 发布订阅场景注意事项可参考 https://developer.qiniu.com/rtc/8769/publish-and-subscribe-android
                mClient.publish(new QNPublishResultCallback() {
                    @Override
                    public void onPublished() { // 发布成功
                        mIsLocalPublished = true;
                        runOnUiThread(() -> ToastUtils.showShortToast(DefaultTranscodingLiveStreamingActivity.this,
                                getString(R.string.publish_success)));
                    }

                    @Override
                    public void onError(int errorCode, String errorMessage) { // 发布失败
                        runOnUiThread(() -> ToastUtils.showLongToast(DefaultTranscodingLiveStreamingActivity.this,
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
            ToastUtils.showShortToast(DefaultTranscodingLiveStreamingActivity.this, getString(R.string.remote_user_left_toast));
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
                // 8. 手动订阅远端音视频 Track
                mClient.subscribe(trackList);
            } else {
                ToastUtils.showShortToast(DefaultTranscodingLiveStreamingActivity.this, getString(R.string.toast_other_user_published));
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
                mRemoteVideoTrack = null;
                mRemoteRenderView.setVisibility(View.INVISIBLE);
                List<QNTranscodingLiveStreamingTrack> unpublishedTrack = new ArrayList<>();
                for (QNTranscodingLiveStreamingTrack liveStreamingTrack : mTranscodingLiveStreamingTracks) {
                    for (QNRemoteTrack remoteTrack : trackList) {
                        if (liveStreamingTrack.getTrackID().equals(remoteTrack.getTrackID())) {
                            unpublishedTrack.add(liveStreamingTrack);
                        }
                    }
                }
                mTranscodingLiveStreamingTracks.removeAll(unpublishedTrack);
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
                mRemoteVideoTrack = remoteVideoTracks.get(0);
                mRemoteVideoTrack.play(mRemoteRenderView);
                mRemoteRenderView.setVisibility(View.VISIBLE);
            }
            if (!remoteAudioTracks.isEmpty()) {
                mRemoteAudioTrack = remoteAudioTracks.get(0);
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
