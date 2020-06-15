package com.qiniu.droid.rtc.demo.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pili.pldroid.player.PLOnErrorListener;
import com.pili.pldroid.player.PLOnInfoListener;
import com.pili.pldroid.player.widget.PLVideoView;
import com.qiniu.droid.rtc.demo.R;

public class LiveRoomActivity extends Activity {
    private static final String TAG = "LiveRoomActivity";
    public static final String EXTRA_ROOM_ID = "ROOM_ID";
    private static final String BASE_URL = "rtmp://pili-rtmp.qnsdk.com/sdk-live/";

    private static final String[] MANDATORY_PERMISSIONS = {
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET",
            "android.permission.CAMERA"
    };

    private PLVideoView mVideoView;
    private LinearLayout mLogView;
    private TextView mAudioBitrateText;
    private TextView mAudioFpsText;
    private TextView mVideoBitrateText;
    private TextView mVideoFpsText;
    private TextView mPlayUrlText;
    private Toast mLogToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());
        setContentView(R.layout.activity_live_room);

        Intent intent = getIntent();
        String mRtmpUrl = BASE_URL + intent.getStringExtra(EXTRA_ROOM_ID);

        mLogView = (LinearLayout) findViewById(R.id.log_text);
        ImageButton mLogButton = (ImageButton) findViewById(R.id.log_shown_button);
        mLogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLogView.setVisibility(mLogView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });
        mAudioBitrateText = (TextView) findViewById(R.id.audio_bitrate_log_text);
        mAudioFpsText = (TextView) findViewById(R.id.audio_fps_log_text);
        mVideoBitrateText = (TextView) findViewById(R.id.video_bitrate_log_text);
        mVideoFpsText = (TextView) findViewById(R.id.video_fps_log_text);
        mVideoView = (PLVideoView) findViewById(R.id.PLVideoView);
        mPlayUrlText = (TextView) findViewById(R.id.play_url_text);
        mPlayUrlText.setText(mRtmpUrl);

        //权限校验
        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                logAndToast("Permission " + permission + " is not granted");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }

        //播放器相关
        mVideoView.setVideoPath(mRtmpUrl);
        mVideoView.setOnErrorListener(new PLOnErrorListener() {
            @Override
            public boolean onError(int errorCode) {
                switch (errorCode) {
                    case ERROR_CODE_OPEN_FAILED:
                        logAndToast("播放器打开失败，请确认是否在推流！");
                        break;
                    case ERROR_CODE_IO_ERROR:
                        logAndToast("网络异常");
                        break;
                    default:
                        logAndToast("PlayerError Code: " + errorCode);
                        break;
                }
                return false;
            }
        });
        mVideoView.setOnInfoListener(new PLOnInfoListener() {
            @Override
            public void onInfo(int what, int extra) {
                switch (what) {
                    case MEDIA_INFO_VIDEO_RENDERING_START:
                        break;
                    case MEDIA_INFO_VIDEO_BITRATE:
                        mVideoBitrateText.setText("VideoBitrate: " + extra / 1000 + " kb/s");
                        break;
                    case MEDIA_INFO_VIDEO_FPS:
                        mVideoFpsText.setText("VideoFps: " + extra);
                        break;
                    case MEDIA_INFO_AUDIO_BITRATE:
                        mAudioBitrateText.setText("AudioBitrate: " + extra / 1000 + " kb/s");
                        break;
                    case MEDIA_INFO_AUDIO_FPS:
                        mAudioFpsText.setText("AudioFps: " + extra);
                        break;
                }
            }
        });
        mVideoView.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoView.stopPlayback();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void logAndToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, msg);
                if (mLogToast != null) {
                    mLogToast.cancel();
                }
                mLogToast = Toast.makeText(LiveRoomActivity.this, msg, Toast.LENGTH_SHORT);
                mLogToast.show();
            }
        });
    }

    @TargetApi(19)
    private static int getSystemUiVisibility() {
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        return flags;
    }
}
