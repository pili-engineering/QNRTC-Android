package com.qiniu.droid.rtc.api.examples.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.qiniu.droid.rtc.api.examples.R;
import com.qiniu.droid.rtc.api.examples.utils.Config;
import com.qiniu.droid.rtc.api.examples.utils.PermissionChecker;
import com.qiniu.droid.rtc.api.examples.utils.ToastUtils;
import com.qiniu.droid.rtc.api.examples.utils.Utils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
        } else if (v.getId() == R.id.default_transcoding_streaming) {
            intent = new Intent(this, DefaultTranscodingLiveStreamingActivity.class);
        } else if (v.getId() == R.id.custom_transcoding_streaming) {
            intent = new Intent(this, CustomTranscodingLiveStreamingActivity.class);
        }
        if (intent != null) {
            startActivity(intent);
        }
    }

    private boolean isPermissionOK() {
        PermissionChecker checker = new PermissionChecker(this);
        boolean isPermissionOK = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checker.checkPermission();
        if (!isPermissionOK) {
            Toast.makeText(this, "Some permissions is not approved !!!", Toast.LENGTH_SHORT).show();
        }
        return isPermissionOK;
    }
}
