package com.qiniu.droid.rtc.demo.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.qiniu.droid.rtc.QNRTC;
import com.qiniu.droid.rtc.demo.BuildConfig;
import com.qiniu.droid.rtc.demo.R;
import com.qiniu.droid.rtc.demo.ui.SpinnerPopupWindow;
import com.qiniu.droid.rtc.demo.utils.Config;
import com.qiniu.droid.rtc.demo.utils.QNAppServer;
import com.qiniu.droid.rtc.demo.utils.ToastUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SettingActivity extends AppCompatActivity {

    private EditText mUserNameEditText;
    private TextView mVideoConfigTextView;
    private TextView mVideoDegradationTextView;
    private TextView mUploadTextView;
    private EditText mAppIdEditText;

    private int mVideoConfigSelectPos = 0;
    private int mVideoDegradationSelectPos = 0;
    private String mUserName;
    private int mEncodeMode = 0;
    private int mSampleRatePos = 0;
    private int mAudioScenePos = 0;
    private boolean mIsAec3Enabled = false;
    private final List<String> mVideoDefaultConfiguration = new ArrayList<>();
    private ArrayAdapter<String> mVideoConfigAdapter;
    private SpinnerPopupWindow mVideoConfigPopupWindow;
    private ArrayAdapter<String> mVideoDegradationAdapter;
    private SpinnerPopupWindow mVideoDegradationPopupWindow;
    private List<String> mLogFileNames;
    private SpinnerPopupWindow mLogFilePopupWindow;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        mUserNameEditText = findViewById(R.id.user_name_edit_text);
        mVideoConfigTextView = findViewById(R.id.video_config_tv);
        mVideoDegradationTextView = findViewById(R.id.video_degradation_tv);
        TextView versionCodeTextView = findViewById(R.id.version_code);
        mUploadTextView = findViewById(R.id.report_log);
        RadioGroup codecModeRadioGroup = findViewById(R.id.codec_mode_button);
        codecModeRadioGroup.setOnCheckedChangeListener(mOnCheckedChangeListener);
        RadioButton hwCodecMode = findViewById(R.id.hw_radio_button);
        RadioButton swCodecMode = findViewById(R.id.sw_radio_button);
        RadioGroup audioSampleRateRadioGroup = findViewById(R.id.sample_rate_button);
        audioSampleRateRadioGroup.setOnCheckedChangeListener(mOnCheckedChangeListener);
        RadioButton lowSampleRateBtn = findViewById(R.id.low_sample_rate_button);
        RadioButton highSampleRateBtn = findViewById(R.id.high_sample_rate_button);
        RadioGroup audioSceneRadioGroup = findViewById(R.id.audio_scene_button);
        audioSceneRadioGroup.setOnCheckedChangeListener(mOnCheckedChangeListener);
        RadioButton defaultAudioSceneBtn = findViewById(R.id.default_audio_scene);
        RadioButton voiceChatAudioSceneBtn = findViewById(R.id.voice_chat_audio_scene);
        RadioButton soundEqualizeAudioSceneBtn = findViewById(R.id.sound_equalize_audio_scene);

        mAppIdEditText = findViewById(R.id.app_id_edit_text);
        SwitchCompat aec3Switch = findViewById(R.id.webrtc_aec3_enable_btn);
        aec3Switch.setOnCheckedChangeListener((buttonView, isChecked) -> mIsAec3Enabled = isChecked);

        versionCodeTextView.setText(String.format(getString(R.string.version_code), getVersionDescription(), getBuildTimeDescription(), getSdkVersion()));

        SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        mUserName = preferences.getString(Config.USER_NAME, "");
        mUserNameEditText.setHint("用户名称：" + mUserName);
        String mAppId = preferences.getString(Config.APP_ID, QNAppServer.APP_ID);

        if (!mAppId.equals(QNAppServer.APP_ID)) {
            mAppIdEditText.setText(mAppId);
        }

        //test mode setting
        LinearLayout testModeLayout = findViewById(R.id.test_mode_layout);
        testModeLayout.setVisibility(isTestMode() ? View.VISIBLE : View.GONE);

        String[] configurations = getResources().getStringArray(R.array.conference_configuration);
        mVideoDefaultConfiguration.addAll(Arrays.asList(configurations));

        mVideoConfigSelectPos = preferences.getInt(Config.VIDEO_CONFIG_POS, 1);
        mVideoConfigTextView.setText(mVideoDefaultConfiguration.get(mVideoConfigSelectPos));

        mVideoDegradationSelectPos = preferences.getInt(Config.VIDEO_DEGRADATION_POS, Config.DEFAULT_VIDEO_DEGRADATION_POS);
        mVideoDegradationTextView.setText(Config.VIDEO_DEGRADATION_TIPS[mVideoDegradationSelectPos]);

        int codecMode = preferences.getInt(Config.CODEC_MODE, Config.SW);
        if (codecMode == Config.HW) {
            hwCodecMode.setChecked(true);
        } else {
            swCodecMode.setChecked(true);
        }

        int sampleRatePos = preferences.getInt(Config.SAMPLE_RATE, Config.LOW_SAMPLE_RATE);
        if (sampleRatePos == Config.LOW_SAMPLE_RATE) {
            lowSampleRateBtn.setChecked(true);
        } else {
            highSampleRateBtn.setChecked(true);
        }

        int audioScenePos = preferences.getInt(Config.AUDIO_SCENE, Config.DEFAULT_AUDIO_SCENE);
        if (audioScenePos == Config.DEFAULT_AUDIO_SCENE) {
            defaultAudioSceneBtn.setChecked(true);
        } else if (audioScenePos == Config.VOICE_CHAT_AUDIO_SCENE) {
            voiceChatAudioSceneBtn.setChecked(true);
        } else {
            soundEqualizeAudioSceneBtn.setChecked(true);
        }

        mIsAec3Enabled = preferences.getBoolean(Config.AEC3_ENABLE, true);
        aec3Switch.setChecked(mIsAec3Enabled);

        mVideoConfigPopupWindow = new SpinnerPopupWindow(this);
        mVideoConfigPopupWindow.setOnSpinnerItemClickListener(pos -> {
            mVideoConfigSelectPos = pos;
            mVideoConfigTextView.setText(mVideoDefaultConfiguration.get(mVideoConfigSelectPos));
            mVideoConfigPopupWindow.dismiss();
        });

        mVideoConfigAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, mVideoDefaultConfiguration);

        mVideoDegradationPopupWindow = new SpinnerPopupWindow(this);
        mVideoDegradationPopupWindow.setOnSpinnerItemClickListener(pos -> {
            mVideoDegradationSelectPos = pos;
            mVideoDegradationTextView.setText(Config.VIDEO_DEGRADATION_TIPS[mVideoDegradationSelectPos]);
            mVideoDegradationPopupWindow.dismiss();
        });

        mVideoDegradationAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, Config.VIDEO_DEGRADATION_TIPS);
    }

    public void onClickBack(View v) {
        finish();
    }

    public void onClickVideoConfig(View v) {
        mVideoConfigPopupWindow.setAdapter(mVideoConfigAdapter);
        mVideoConfigPopupWindow.setWidth(mVideoConfigTextView.getWidth());
        mVideoConfigPopupWindow.showAsDropDown(mVideoConfigTextView);
    }

    public void onClickVideoDegradation(View v) {
        mVideoDegradationPopupWindow.setAdapter(mVideoDegradationAdapter);
        mVideoDegradationPopupWindow.setWidth(mVideoDegradationTextView.getWidth());
        mVideoDegradationPopupWindow.showAsDropDown(mVideoDegradationTextView);
    }

    public void onClickUploadLog(View v) {
        QNRTC.init(this, null);
        QNRTC.uploadLog((fileName, code, remaining) -> runOnUiThread(() -> {
            String logName = fileName.substring(fileName.lastIndexOf('/') + 1);
            ToastUtils.showShortToast(SettingActivity.this,
                    String.format(getString(R.string.upload_result),
                            logName, (code == 0 ? "成功" : ("失败：" + code))));
        }));
        QNRTC.deinit();
    }

    public void onClickSaveConfiguration(View v) {
        String userName = mUserNameEditText.getText().toString().trim();
        String appId = mAppIdEditText.getText().toString().trim();

        SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).edit();
        if (!"".equals(userName)) {
            if (!MainActivity.isUserNameOk(userName)) {
                ToastUtils.showShortToast(this, getString(R.string.wrong_user_name_toast));
                return;
            }

            if (!mUserName.equals(userName)) {
                editor.putString(Config.USER_NAME, userName);
            }
        }

        editor.putString(Config.APP_ID, TextUtils.isEmpty(appId) ? QNAppServer.APP_ID : appId);

        editor.putInt(Config.VIDEO_CONFIG_POS, mVideoConfigSelectPos);
        editor.putInt(Config.VIDEO_DEGRADATION_POS, mVideoDegradationSelectPos);
        editor.putInt(Config.CODEC_MODE, mEncodeMode);
        editor.putInt(Config.SAMPLE_RATE, mSampleRatePos);
        editor.putInt(Config.AUDIO_SCENE, mAudioScenePos);
        editor.putBoolean(Config.AEC3_ENABLE, mIsAec3Enabled);

        editor.putInt(Config.WIDTH, Config.DEFAULT_RESOLUTION[mVideoConfigSelectPos][0]);
        editor.putInt(Config.HEIGHT, Config.DEFAULT_RESOLUTION[mVideoConfigSelectPos][1]);
        editor.putInt(Config.FPS, Config.DEFAULT_FPS[mVideoConfigSelectPos]);
        editor.putInt(Config.BITRATE, Config.DEFAULT_BITRATE[mVideoConfigSelectPos]);

        if (isTestMode()) {
            saveTestMode(editor);
        }
        editor.apply();
        finish();
    }

    private void saveTestMode(SharedPreferences.Editor editor) {
        int testModeWidth = 0;
        int testModeHigh = 0;
        int testModeFPS = 0;
        int testModeBitrate = 0;

        EditText testModeWidthEditText = findViewById(R.id.test_mode_width);
        EditText testModeHighEditText = findViewById(R.id.test_mode_high);
        EditText testModeFPSEditText = findViewById(R.id.test_mode_fps);
        EditText testModeBitrateEditText = findViewById(R.id.test_mode_bitrate);

        try {
            testModeWidth = Integer.parseInt(testModeWidthEditText.getText().toString());
            testModeHigh = Integer.parseInt(testModeHighEditText.getText().toString());
            testModeFPS = Integer.parseInt(testModeFPSEditText.getText().toString());
            testModeBitrate = Integer.parseInt(testModeBitrateEditText.getText().toString());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if (testModeWidth > 0 && testModeHigh > 0 && testModeFPS > 0 && testModeBitrate > 0) {
            editor.putInt(Config.WIDTH, testModeWidth);
            editor.putInt(Config.HEIGHT, testModeHigh);
            editor.putInt(Config.FPS, testModeFPS);
            editor.putInt(Config.BITRATE, testModeBitrate);
        }
    }

    private String getVersionDescription() {
        PackageManager packageManager = getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "unknown";
    }

    protected String getBuildTimeDescription() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(BuildConfig.BUILD_TIMESTAMP);
    }

    protected String getSdkVersion() {
        return com.qiniu.droid.rtc.BuildConfig.VERSION_NAME + "-" + com.qiniu.droid.rtc.BuildConfig.GIT_HASH;
    }

    private boolean isTestMode() {
        return mAppIdEditText.getText().toString().compareTo(QNAppServer.TEST_MODE_APP_ID) == 0;
    }

    private final RadioGroup.OnCheckedChangeListener mOnCheckedChangeListener = (group, checkedId) -> {
        switch (group.getCheckedRadioButtonId()) {
            case R.id.hw_radio_button:
                mEncodeMode = Config.HW;
                break;
            case R.id.sw_radio_button:
                mEncodeMode = Config.SW;
                break;
            case R.id.low_sample_rate_button:
                mSampleRatePos = Config.LOW_SAMPLE_RATE;
                break;
            case R.id.high_sample_rate_button:
                mSampleRatePos = Config.HIGH_SAMPLE_RATE;
                break;
            case R.id.default_audio_scene:
                mAudioScenePos = Config.DEFAULT_AUDIO_SCENE;
                break;
            case R.id.voice_chat_audio_scene:
                mAudioScenePos = Config.VOICE_CHAT_AUDIO_SCENE;
                break;
            case R.id.sound_equalize_audio_scene:
                mAudioScenePos = Config.SOUND_EQUALIZE_AUDIO_SCENE;
                break;
            default:
                break;
        }
    };
}
