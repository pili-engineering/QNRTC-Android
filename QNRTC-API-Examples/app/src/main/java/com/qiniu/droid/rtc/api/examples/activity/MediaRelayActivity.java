package com.qiniu.droid.rtc.api.examples.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.qiniu.droid.rtc.QNAudioQualityPreset;
import com.qiniu.droid.rtc.QNBeautySetting;
import com.qiniu.droid.rtc.QNCameraEventListener;
import com.qiniu.droid.rtc.QNCameraFacing;
import com.qiniu.droid.rtc.QNCameraVideoTrack;
import com.qiniu.droid.rtc.QNCameraVideoTrackConfig;
import com.qiniu.droid.rtc.QNClientEventListener;
import com.qiniu.droid.rtc.QNClientMode;
import com.qiniu.droid.rtc.QNClientRole;
import com.qiniu.droid.rtc.QNConnectionDisconnectedInfo;
import com.qiniu.droid.rtc.QNConnectionState;
import com.qiniu.droid.rtc.QNCustomMessage;
import com.qiniu.droid.rtc.QNMediaRelayConfiguration;
import com.qiniu.droid.rtc.QNMediaRelayInfo;
import com.qiniu.droid.rtc.QNMediaRelayResultCallback;
import com.qiniu.droid.rtc.QNMediaRelayState;
import com.qiniu.droid.rtc.QNMicrophoneAudioTrack;
import com.qiniu.droid.rtc.QNMicrophoneAudioTrackConfig;
import com.qiniu.droid.rtc.QNPublishResultCallback;
import com.qiniu.droid.rtc.QNRTC;
import com.qiniu.droid.rtc.QNRTCClient;
import com.qiniu.droid.rtc.QNRTCClientConfig;
import com.qiniu.droid.rtc.QNRTCEventListener;
import com.qiniu.droid.rtc.QNRemoteAudioTrack;
import com.qiniu.droid.rtc.QNRemoteTrack;
import com.qiniu.droid.rtc.QNRemoteVideoTrack;
import com.qiniu.droid.rtc.QNSurfaceView;
import com.qiniu.droid.rtc.QNVideoCaptureConfigPreset;
import com.qiniu.droid.rtc.QNVideoEncoderConfig;
import com.qiniu.droid.rtc.api.examples.R;
import com.qiniu.droid.rtc.api.examples.utils.Config;
import com.qiniu.droid.rtc.api.examples.utils.ToastUtils;
import com.qiniu.droid.rtc.api.examples.utils.Utils;
import com.qiniu.droid.rtc.model.QNAudioDevice;
import com.uuzuche.lib_zxing.activity.CaptureActivity;
import com.uuzuche.lib_zxing.activity.CodeUtils;

import org.json.JSONObject;
import org.webrtc.Size;

import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 跨房媒体转发场景
 *
 * 主要步骤如下：
 * 1. 初始化视图
 * 2. 初始化 RTC
 * 3. 创建主播直播场景的 QNRTCClient 对象
 * 4. 创建本地音视频 Track
 * 5. 加入房间
 * 6. 发布本地音视频 Track
 * 7. 开始跨房到目标房间
 * 8. 更新跨房到目标房间
 * 9. 停止跨房媒体转发
 * 10. 离开房间
 * 11. 反初始化 RTC 释放资源
 *
 * 文档参考：
 * - 跨房媒体转发，请参考 https://developer.qiniu.com/rtc/10631/media-relay-android
 * - 接口文档，请参考 https://developer.qiniu.com/rtc/8773/API%20%E6%A6%82%E8%A7%88
 */
public class MediaRelayActivity extends AppCompatActivity {
    private static final String TAG = "MediaRelayActivity";
    private static final int REQUEST_CODE_SCAN_TOKEN = 1000;
    private QNRTCClient mClient;
    private QNSurfaceView mLocalRenderView;
    private QNSurfaceView mRemoteRenderView;
    private QNCameraVideoTrack mCameraVideoTrack;
    private QNMicrophoneAudioTrack mMicrophoneAudioTrack;
    private QNBeautySetting mBeautySetting;

    private String mRoomName;
    private String mFirstRemoteUserID = null;
    private boolean mMicrophoneError;
    private boolean mMediaRelayStarted;

    private boolean mNeedScannerStart;
    private EditText mRoomTokenEt;
    private TextView mTargetRoomInfoTv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_media_relay);

        JSONObject roomInfo = Utils.parseRoomToken(Config.ROOM_TOKEN);
        mRoomName = roomInfo.optString(Config.KEY_ROOM_NAME);

        // 1. 初始化视图
        initView();
        // 2. 初始化 RTC
        QNRTC.init(this, mRTCEventListener);
        // 3. 创建 QNRTCClient 对象
        // 注意，跨房媒体转发功能仅支持直播场景，且角色为主播的情况
        QNRTCClientConfig clientConfig = new QNRTCClientConfig(QNClientMode.LIVE, QNClientRole.BROADCASTER);
        mClient = QNRTC.createClient(clientConfig, mClientEventListener);
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
            // 10. 离开房间
            mClient.leave();
            mClient = null;
        }
        destroyLocalTracks();
        // 11. 反初始化 RTC 释放资源
        QNRTC.deinit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN_TOKEN) {
            //处理扫描结果（在界面上显示）
            if (null != data) {
                Bundle bundle = data.getExtras();
                if (bundle == null) {
                    return;
                }
                if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                    String token = bundle.getString(CodeUtils.RESULT_STRING);
                    JSONObject roomInfo = Utils.parseRoomToken(token);
                    if (roomInfo != null) {
                        mRoomTokenEt.setText(token);
                        mTargetRoomInfoTv.setText(roomInfo.toString());
                    } else {
                        Toast.makeText(MediaRelayActivity.this, "token 格式错误", Toast.LENGTH_LONG).show();
                    }
                } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                    Toast.makeText(MediaRelayActivity.this, "解析二维码失败", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void initView() {
        // 初始化本地预览视图
        mLocalRenderView = findViewById(R.id.local_render_view);
        mLocalRenderView.setZOrderOnTop(true);
        // 初始化远端预览视图
        mRemoteRenderView = findViewById(R.id.remote_render_view);
        mRemoteRenderView.setZOrderOnTop(true);

        mRoomTokenEt = findViewById(R.id.room_token_edit_text);
        mTargetRoomInfoTv = findViewById(R.id.target_room_info_text);

        Button startMediaRelayBtn = findViewById(R.id.start_media_relay_button);
        startMediaRelayBtn.setOnClickListener(v -> {
            String roomToken = mRoomTokenEt.getText().toString();
            if (TextUtils.isEmpty(roomToken)) {
                ToastUtils.showShortToast(this, "token 为空！");
                return;
            }
            String roomName = Utils.parseRoomToken(roomToken).optString(Config.KEY_ROOM_NAME);
            if (TextUtils.isEmpty(roomName)) {
                ToastUtils.showShortToast(this, "非法 token！");
                return;
            }
            QNMediaRelayInfo srcRoomRelayInfo = new QNMediaRelayInfo(mRoomName, Config.ROOM_TOKEN); // 配置跨房源房间信息
            QNMediaRelayInfo dstRoomRelayInfo = new QNMediaRelayInfo(roomName, roomToken); // 配置跨房目标房间信息
            QNMediaRelayConfiguration mediaRelayConfiguration = new QNMediaRelayConfiguration(srcRoomRelayInfo); // 创建跨房媒体转发配置类
            mediaRelayConfiguration.addDestRoomInfo(dstRoomRelayInfo); // 设置跨房目标房间信息
            if (!mMediaRelayStarted) {
                mMediaRelayStarted = true;
                // 7. 开始跨房媒体转发到目标房间
                mClient.startMediaRelay(mediaRelayConfiguration, new QNMediaRelayResultCallback() {
                    /**
                     * 跨房媒体转发操作成功时触发该回调
                     * @param stateMap 媒体转发状态，key 为房间名，value 为该房间的转发状态
                     */
                    @Override
                    public void onResult(Map<String, QNMediaRelayState> stateMap) {
                        if (!stateMap.containsKey(roomName)) {
                            return;
                        }
                        runOnUiThread(() -> {
                            if (stateMap.get(roomName) == QNMediaRelayState.SUCCESS) {
                                // 成功开始跨房
                                startMediaRelayBtn.setText(getString(R.string.update_media_relay));
                            } else {
                                ToastUtils.showShortToast(MediaRelayActivity.this,
                                        String.format(getString(R.string.media_relay_error), stateMap.get(roomName).name()));
                            }
                        });
                    }

                    /**
                     * 跨房媒体转发操作失败时触发该回调
                     */
                    @Override
                    public void onError(int errorCode, String description) {
                        mMediaRelayStarted = false;
                        runOnUiThread(() -> ToastUtils.showShortToast(MediaRelayActivity.this,
                                String.format(getString(R.string.media_relay_error_code), errorCode, description)));
                    }
                });
            } else {
                // 8. 更新跨房媒体转发到目标房间
                // 注意，更新跨房媒体转发为全量接口，mediaRelayConfiguration 会全量替换 startMediaRelay 时指定的房间信息
                mClient.updateMediaRelay(mediaRelayConfiguration, new QNMediaRelayResultCallback() {
                    /**
                     * 跨房媒体转发操作成功时触发该回调
                     * @param stateMap 媒体转发状态，key 为房间名，value 为该房间的转发状态
                     */
                    @Override
                    public void onResult(Map<String, QNMediaRelayState> stateMap) {
                        if (!stateMap.containsKey(roomName)) {
                            return;
                        }
                        runOnUiThread(() -> {
                            if (stateMap.get(roomName) == QNMediaRelayState.SUCCESS) {
                                // 成功更新跨房
                                ToastUtils.showShortToast(MediaRelayActivity.this, getString(R.string.update_media_relay_success));
                            } else {
                                ToastUtils.showShortToast(MediaRelayActivity.this,
                                        String.format(getString(R.string.media_relay_error), stateMap.get(roomName).name()));
                            }
                        });
                    }

                    /**
                     * 跨房媒体转发操作失败时触发该回调
                     */
                    @Override
                    public void onError(int errorCode, String description) {
                        runOnUiThread(() -> ToastUtils.showShortToast(MediaRelayActivity.this,
                                String.format(getString(R.string.media_relay_error_code), errorCode, description)));
                    }
                });
            }
        });

        Button stopMediaRelayBtn = findViewById(R.id.stop_media_relay_button);
        stopMediaRelayBtn.setOnClickListener(v -> {
            if (mMediaRelayStarted) {
                // 9. 停止跨房媒体转发
                mClient.stopMediaRelay(new QNMediaRelayResultCallback() {
                    /**
                     * 跨房媒体转发操作成功时触发该回调
                     * @param stateMap 媒体转发状态，key 为房间名，value 为该房间的转发状态
                     */
                    @Override
                    public void onResult(Map<String, QNMediaRelayState> stateMap) {
                        String roomToken = mRoomTokenEt.getText().toString();
                        String roomName = Utils.parseRoomToken(roomToken).optString(Config.KEY_ROOM_NAME);
                        if (!stateMap.containsKey(roomName)) {
                            return;
                        }
                        if (stateMap.get(roomName) == QNMediaRelayState.STOPPED) {
                            // 成功停止跨房
                            runOnUiThread(() -> startMediaRelayBtn.setText(getString(R.string.start_media_relay)));
                            mMediaRelayStarted = false;
                        }
                    }

                    /**
                     * 跨房媒体转发操作失败时触发该回调
                     */
                    @Override
                    public void onError(int errorCode, String description) {
                        runOnUiThread(() -> ToastUtils.showShortToast(MediaRelayActivity.this,
                                String.format(getString(R.string.media_relay_error_code), errorCode, description)));
                    }
                });
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
                if (mNeedScannerStart) {
                    mNeedScannerStart = false;
                    Intent intent = new Intent(MediaRelayActivity.this, CaptureActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SCAN_TOKEN);
                }
            }

            @Override
            public void onError(int i, String s) {
                Log.i(TAG, "onError [" + i + ", " + s + "]");
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

    public void onClickScanQRCode(View view) {
        if (mCameraVideoTrack != null) {
            mNeedScannerStart = true;
            mCameraVideoTrack.stopCapture();
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
            ToastUtils.showShortToast(MediaRelayActivity.this,
                    String.format(getString(R.string.connection_state_changed), state.name()));
            if (state == QNConnectionState.CONNECTED) {
                // 6. 发布本地音视频 Track
                // 发布订阅场景注意事项可参考 https://developer.qiniu.com/rtc/8769/publish-and-subscribe-android
                mClient.publish(new QNPublishResultCallback() {
                    @Override
                    public void onPublished() { // 发布成功
                        runOnUiThread(() -> ToastUtils.showShortToast(MediaRelayActivity.this,
                                getString(R.string.publish_success)));
                    }

                    @Override
                    public void onError(int errorCode, String errorMessage) { // 发布失败
                        runOnUiThread(() -> ToastUtils.showLongToast(MediaRelayActivity.this,
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
            ToastUtils.showShortToast(MediaRelayActivity.this, getString(R.string.remote_user_left_toast));
            if (remoteUserID.equals(mFirstRemoteUserID)) {
                mFirstRemoteUserID = null;
                mRemoteRenderView.setVisibility(View.INVISIBLE);
            }
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
            ToastUtils.showShortToast(MediaRelayActivity.this,
                    String.format(getString(R.string.media_relay_state_changed), relayRoom, state.name()));
        }
    };
}
