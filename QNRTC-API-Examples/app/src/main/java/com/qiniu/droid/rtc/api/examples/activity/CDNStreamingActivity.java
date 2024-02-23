package com.qiniu.droid.rtc.api.examples.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.qiniu.droid.rtc.QNAudioQualityPreset;
import com.qiniu.droid.rtc.QNBeautySetting;
import com.qiniu.droid.rtc.QNCDNStreamingClient;
import com.qiniu.droid.rtc.QNCDNStreamingConfig;
import com.qiniu.droid.rtc.QNCDNStreamingListener;
import com.qiniu.droid.rtc.QNCDNStreamingStats;
import com.qiniu.droid.rtc.QNCameraEventListener;
import com.qiniu.droid.rtc.QNCameraFacing;
import com.qiniu.droid.rtc.QNCameraVideoTrack;
import com.qiniu.droid.rtc.QNCameraVideoTrackConfig;
import com.qiniu.droid.rtc.QNConnectionState;
import com.qiniu.droid.rtc.QNDegradationPreference;
import com.qiniu.droid.rtc.QNMicrophoneAudioTrack;
import com.qiniu.droid.rtc.QNMicrophoneAudioTrackConfig;
import com.qiniu.droid.rtc.QNRTC;
import com.qiniu.droid.rtc.QNRTCEventListener;
import com.qiniu.droid.rtc.QNRTCSetting;
import com.qiniu.droid.rtc.QNSurfaceView;
import com.qiniu.droid.rtc.QNVideoCaptureConfigPreset;
import com.qiniu.droid.rtc.QNVideoEncoderConfig;
import com.qiniu.droid.rtc.QNVideoFormatPreset;
import com.qiniu.droid.rtc.api.examples.APIApplication;
import com.qiniu.droid.rtc.api.examples.R;
import com.qiniu.droid.rtc.api.examples.utils.Config;
import com.qiniu.droid.rtc.api.examples.utils.ToastUtils;
import com.qiniu.droid.rtc.model.QNAudioDevice;
import com.uuzuche.lib_zxing.activity.CaptureActivity;
import com.uuzuche.lib_zxing.activity.CodeUtils;

import org.qnwebrtc.Size;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * CDN 直推场景
 *
 * 主要步骤如下：
 * 1. 初始化视图
 * 2. 初始化 RTC
 * 3. 创建 QNCDNStreamingClient 对象
 * 4. 设置 CDN 直推事件监听器
 * 5. 创建本地音视频 Track
 * 7. 开始直播
 * 8. 停止直播
 * 9. 反初始化 RTC 释放资源
 *
 * 文档参考：
 * - 接口文档，请参考 https://developer.qiniu.com/rtc/8773/API%20%E6%A6%82%E8%A7%88
 */
public class CDNStreamingActivity extends AppCompatActivity {
    private static final String TAG = "CDNStreamingActivity";
    private static final int REQUEST_CODE_SCAN_PUBLISH_URL = 1001;

    private QNSurfaceView mLocalRenderView;
    private QNCDNStreamingClient mStreamingClient;
    private QNCameraVideoTrack mCameraVideoTrack;
    private QNMicrophoneAudioTrack mMicrophoneAudioTrack;
    private QNConnectionState mStreamingState = QNConnectionState.DISCONNECTED;

    private TextView mPublishUrlEditText;
    private TextView mConnectionStateText;
    private TextView mStreamingStatsText;
    private boolean mNeedScannerStart;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cdn_streaming);

        // 1. 初始化视图
        initView();
        // 2. 初始化 RTC
        QNRTC.init(this, new QNRTCSetting(), mRTCEventListener);
        APIApplication.mRTCInit = true;
        // 3. 创建 QNCDNStreamingClient 对象
        mStreamingClient = QNRTC.createCDNStreamingClient();
        // 4. 设置 CDN 直推事件监听器
        mStreamingClient.setCDNStreamingListener(mStreamingListener);
        // 5. 创建本地音视频 Track
        initLocalTracks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraVideoTrack.startCapture();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 从 Android 9 开始，设备将无法在后台访问相机，本示例不做后台采集的演示
        // 详情可参考 https://developer.qiniu.com/rtc/kb/10074/FAQ-Android?category=kb#3
        if (mStreamingClient != null) {
            mStreamingClient.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (APIApplication.mRTCInit) {
            // 9. 反初始化 RTC 释放资源
            QNRTC.deinit();
            APIApplication.mRTCInit = false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN_PUBLISH_URL) {
            //处理扫描结果（在界面上显示）
            if (null != data) {
                Bundle bundle = data.getExtras();
                if (bundle == null) {
                    return;
                }
                if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                    Config.PUBLISH_URL = bundle.getString(CodeUtils.RESULT_STRING);
                    mPublishUrlEditText.setText(Config.PUBLISH_URL);
                } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                    Toast.makeText(CDNStreamingActivity.this,
                            "解析二维码失败", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public void onClickScanQRCode(View view) {
        if (mCameraVideoTrack != null) {
            mNeedScannerStart = true;
            mCameraVideoTrack.stopCapture();
        }
    }

    public void onClickStartCDNStreaming(View view) {
        if ("".equals(mPublishUrlEditText.getText().toString()) || !mPublishUrlEditText.getText().toString().startsWith("rtmp")) {
            ToastUtils.showShortToast(this, getString(R.string.invalid_publish_url_toast));
            return;
        }
        if (mStreamingState != QNConnectionState.DISCONNECTED) {
            ToastUtils.showShortToast(this, getString(R.string.already_exist_cdn_streaming_toast));
            return;
        }
        // 创建单路转推配置类实例
        QNCDNStreamingConfig config = new QNCDNStreamingConfig()
                .setAudioTrack(mMicrophoneAudioTrack) // 设置音频 track
                .setVideoTrack(mCameraVideoTrack)     // 设置音频 track
                .setPublishUrl(mPublishUrlEditText.getText().toString()); // 设置待转推的音频 Track
        // 7. 开始直播
        if (mStreamingClient != null) {
            mStreamingClient.start(config);
        }
    }

    public void onClickStopCDNStreaming(View view) {
        // 8. 停止直播
        if (mStreamingClient != null) {
            mStreamingClient.stop();
        }
    }

    private void initView() {
        // 初始化本地预览视图
        mLocalRenderView = findViewById(R.id.local_render_view);

        mPublishUrlEditText = findViewById(R.id.publish_url_edit_text);
        mStreamingStatsText = findViewById(R.id.streaming_stats_tips);
        mConnectionStateText = findViewById(R.id.connection_state_tips);
        mConnectionStateText.setText("未连接");
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
                // 开启固定分辨率，以避免单路转推场景下，动态分辨率造成的非预期问题
                .setVideoEncoderConfig(new QNVideoEncoderConfig(
                        QNVideoFormatPreset.VIDEO_1280x720_15,
                        QNDegradationPreference.MAINTAIN_RESOLUTION)) // 设置编码参数
                .setCameraFacing(QNCameraFacing.FRONT) // 设置摄像头方向
                .setMultiProfileEnabled(false); // 设置是否开启大小流
        mCameraVideoTrack = QNRTC.createCameraVideoTrack(cameraVideoTrackConfig);
        // 设置本地预览视图
        mCameraVideoTrack.play(mLocalRenderView);
        // 初始化并配置美颜
        mCameraVideoTrack.setBeauty(new QNBeautySetting(0.5f, 0.5f, 0.5f));
        mCameraVideoTrack.setCameraEventListener(new QNCameraEventListener() {
            @SuppressLint("LongLogTag")
            @Override
            public int[] onCameraOpened(List<Size> list, List<Integer> list1) {
                return new int[]{-1, -1};
            }

            @SuppressLint("LongLogTag")
            @Override
            public void onCaptureStarted() {
                Log.i(TAG, "onCaptureStarted");
            }

            @SuppressLint("LongLogTag")
            @Override
            public void onCaptureStopped() {
                Log.i(TAG, "onCaptureStopped");
                if (mNeedScannerStart) {
                    mNeedScannerStart = false;
                    Intent intent = new Intent(CDNStreamingActivity.this, CaptureActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SCAN_PUBLISH_URL);
                }
            }

            @SuppressLint("LongLogTag")
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
        mMicrophoneAudioTrack.setMicrophoneEventListener((errorCode, errorMessage) -> {
            // 麦克风采集出错
            ToastUtils.showShortToast(
                    CDNStreamingActivity.this, "麦克风错误：[" + errorCode + ", " + errorMessage + "]");
        });
        mMicrophoneAudioTrack.startRecording();
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

    /**
     * CDN 直推事件监听
     */
    private final QNCDNStreamingListener mStreamingListener = new QNCDNStreamingListener() {
        /**
         * 推流状态改变时触发
         *
         * @param state 推流状态
         * @param errorCode 错误码
         * @param message 错误信息
         */
        @Override
        public void onCDNStreamingConnectionStateChanged(QNConnectionState state, int errorCode, String message) {
            mStreamingState = state;
            String connectionState;
            switch (state) {
                case CONNECTED:
                    connectionState = "已连接";
                    break;
                case CONNECTING:
                    connectionState = "连接中";
                    break;
                case RECONNECTING:
                    connectionState = "重连中";
                    break;
                case RECONNECTED:
                    connectionState = "重连成功";
                    break;
                case DISCONNECTED:
                default:
                    connectionState = "未连接";
                    break;
            }
            mConnectionStateText.setText(connectionState);
        }

        /**
         * 实时流信息更新时触发
         *
         * @param stats 流状态
         */
        @Override
        public void onCDNStreamingStats(QNCDNStreamingStats stats) {
            mStreamingStatsText.setText(String.format(getString(R.string.streaming_stats),
                    stats.audioBitrate, stats.videoBitrate, stats.sendVideoFps, stats.droppedVideoFrames));
        }
    };
}
