package com.qiniu.droid.rtc.api.examples.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.qiniu.droid.rtc.api.examples.BuildConfig;
import com.qiniu.droid.rtc.api.examples.R;
import com.qiniu.droid.rtc.api.examples.utils.Config;
import com.qiniu.droid.rtc.api.examples.utils.PermissionChecker;
import com.qiniu.droid.rtc.api.examples.utils.ToastUtils;
import com.qiniu.droid.rtc.api.examples.utils.Utils;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private PermissionChecker mChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mChecker = new PermissionChecker(this);
        TextView appInfo = findViewById(R.id.app_info);
        JSONObject roomInfo = Utils.parseRoomToken(Config.ROOM_TOKEN);
        String userID = roomInfo.optString(Config.KEY_USER_ID);
        String roomName = roomInfo.optString(Config.KEY_ROOM_NAME);
        appInfo.setText(String.format(getString(R.string.app_info), userID, roomName, getSdkVersion(), getBuildTimeDescription()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] state) {
        super.onRequestPermissionsResult(requestCode, permissions, state);
        mChecker.onRequestPermissionsResult(requestCode, permissions, state);
    }

    public void onClickScenes(View v) {
        if (v.getId() != R.id.screen_microphone && !isPermissionOK()) {
            return;
        }
        if (Utils.parseRoomToken(Config.ROOM_TOKEN) == null) {
            ToastUtils.showShortToast(this, getString(R.string.invalid_token_toast));
            return;
        }
        Intent intent = null;
        if (v.getId() == R.id.camera_microphone) {
            intent = new Intent(this, CameraMicrophoneActivity.class);
        } else if (v.getId() == R.id.screen_microphone) {
            intent = new Intent(this, ScreenCaptureActivity.class);
        } else if (v.getId() == R.id.custom_video_audio) {
            intent = new Intent(this, CustomAVCaptureActivity.class);
        } else if (v.getId() == R.id.microphone_only) {
            intent = new Intent(this, MicrophoneOnlyActivity.class);
        } else if (v.getId() == R.id.custom_audio_only) {
            intent = new Intent(this, CustomAudioOnlyActivity.class);
        } else if (v.getId() == R.id.direct_streaming) {
            intent = new Intent(this, DirectLiveStreamingActivity.class);
        } else if (v.getId() == R.id.custom_message) {
            intent = new Intent(this, CustomMessageActivity.class);
        } else if (v.getId() == R.id.multi_profile) {
            intent = new Intent(this, MultiProfileActivity.class);
        } else if (v.getId() == R.id.media_statistics) {
            intent = new Intent(this, MediaStatisticsActivity.class);
        } else if (v.getId() == R.id.audio_mixing) {
            intent = new Intent(this, AudioMixerActivity.class);
        } else if (v.getId() == R.id.audio_effect_mixing) {
            intent = new Intent(this, AudioEffectsMixingActivity.class);
        } else if (v.getId() == R.id.audio_source_mixing) {
            intent = new Intent(this, AudioSourcesMixingActivity.class);
        } else if (v.getId() == R.id.default_transcoding_streaming) {
            intent = new Intent(this, DefaultTranscodingLiveStreamingActivity.class);
        } else if (v.getId() == R.id.custom_transcoding_streaming) {
            intent = new Intent(this, CustomTranscodingLiveStreamingActivity.class);
        } else if (v.getId() == R.id.media_relay) {
            intent = new Intent(this, MediaRelayActivity.class);
        }
        if (intent != null) {
            startActivity(intent);
        }
    }

    private boolean isPermissionOK() {
        boolean isPermissionOK = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || mChecker.checkPermission();
        return isPermissionOK;
    }

    private String getBuildTimeDescription() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(BuildConfig.BUILD_TIMESTAMP);
    }

    private String getSdkVersion() {
        return com.qiniu.droid.rtc.BuildConfig.VERSION_NAME + "-" + com.qiniu.droid.rtc.BuildConfig.GIT_HASH;
    }
}
