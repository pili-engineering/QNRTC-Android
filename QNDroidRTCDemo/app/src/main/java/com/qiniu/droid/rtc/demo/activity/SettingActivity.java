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

import com.qiniu.droid.rtc.QNFileLogHelper;
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
    private TextView mConfigTextView;
    private TextView mUploadTextView;
    private EditText mAppIdEditText;

    private int mSelectPos = 0;
    private String mUserName;
    private int mEncodeMode = 0;
    private int mSampleRatePos = 0;
    private int mAudioScenePos = 0;
    private boolean mMaintainResolution = false;
    private boolean mIsAec3Enabled = false;
    private final List<String> mDefaultConfiguration = new ArrayList<>();
    private ArrayAdapter<String> mConfigAdapter;
    private SpinnerPopupWindow mConfigPopupWindow;
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
        mConfigTextView = findViewById(R.id.config_text_view);
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

        RadioGroup maintainResRadioGroup = findViewById(R.id.maintain_resolution_button);
        maintainResRadioGroup.setOnCheckedChangeListener(mOnMaintainResCheckedChangeListener);
        RadioButton maintainResolutionYes = findViewById(R.id.maintain_res_button_yes);
        RadioButton maintainResolutionNo = findViewById(R.id.maintain_res_button_no);

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
        mDefaultConfiguration.addAll(Arrays.asList(configurations));

        mSelectPos = preferences.getInt(Config.CONFIG_POS, 1);
        mConfigTextView.setText(mDefaultConfiguration.get(mSelectPos));

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

        mMaintainResolution = preferences.getBoolean(Config.MAINTAIN_RES, false);
        if (mMaintainResolution) {
            maintainResolutionYes.setChecked(true);
        } else {
            maintainResolutionNo.setChecked(true);
        }

        mIsAec3Enabled = preferences.getBoolean(Config.AEC3_ENABLE, true);
        aec3Switch.setChecked(mIsAec3Enabled);

        mConfigPopupWindow = new SpinnerPopupWindow(this);
        mConfigPopupWindow.setOnSpinnerItemClickListener(mOnSpinnerItemClickListener);

        mConfigAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, mDefaultConfiguration);
    }

    public void onClickBack(View v) {
        finish();
    }

    public void onClickConfigParams(View v) {
        showConfigPopupWindow();
    }

    public void onClickUploadLog(View v) {
        showLogFilePopupWindow();
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

        editor.putInt(Config.CONFIG_POS, mSelectPos);
        editor.putInt(Config.CODEC_MODE, mEncodeMode);
        editor.putInt(Config.SAMPLE_RATE, mSampleRatePos);
        editor.putInt(Config.AUDIO_SCENE, mAudioScenePos);
        editor.putBoolean(Config.MAINTAIN_RES, mMaintainResolution);
        editor.putBoolean(Config.AEC3_ENABLE, mIsAec3Enabled);

        editor.putInt(Config.WIDTH, Config.DEFAULT_RESOLUTION[mSelectPos][0]);
        editor.putInt(Config.HEIGHT, Config.DEFAULT_RESOLUTION[mSelectPos][1]);
        editor.putInt(Config.FPS, Config.DEFAULT_FPS[mSelectPos]);
        editor.putInt(Config.BITRATE, Config.DEFAULT_BITRATE[mSelectPos]);

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

    private void showConfigPopupWindow() {
        mConfigPopupWindow.setAdapter(mConfigAdapter);
        mConfigPopupWindow.setWidth(mConfigTextView.getWidth());
        mConfigPopupWindow.showAsDropDown(mConfigTextView);
    }

    private void showLogFilePopupWindow() {
        if (mLogFilePopupWindow == null) {
            mLogFilePopupWindow = new SpinnerPopupWindow(this);
            mLogFilePopupWindow.setOnSpinnerItemClickListener(new SpinnerPopupWindow.OnSpinnerItemClickListener() {
                @Override
                public void onItemClick(int pos) {
                    QNFileLogHelper.getInstance().reportLogFile(mLogFileNames.get(pos), new QNFileLogHelper.LogReportCallback() {
                        @Override
                        public void onReportSuccess(String name) {
                            ToastUtils.showShortToast(SettingActivity.this, "上传成功：" + name);
                        }

                        @Override
                        public void onReportError(String name, String errorMsg) {
                            ToastUtils.showShortToast(SettingActivity.this, "上传失败：" + name + "；" + errorMsg);
                        }
                    });
                    mLogFilePopupWindow.dismiss();
                }
            });
        }
        QNFileLogHelper.getInstance().init(this);
        mLogFileNames = QNFileLogHelper.getInstance().getLogFiles();
        if (mLogFileNames == null || mLogFileNames.size() == 0) {
            ToastUtils.showShortToast(SettingActivity.this, "当前无可上报日志");
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, mLogFileNames);
        mLogFilePopupWindow.setAdapter(adapter);
        mLogFilePopupWindow.setWidth(mAppIdEditText.getWidth());
        mLogFilePopupWindow.showAsDropDown(mUploadTextView);
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

    private final SpinnerPopupWindow.OnSpinnerItemClickListener mOnSpinnerItemClickListener = new SpinnerPopupWindow.OnSpinnerItemClickListener() {
        @Override
        public void onItemClick(int pos) {
            mSelectPos = pos;
            mConfigTextView.setText(mDefaultConfiguration.get(mSelectPos));
            mConfigPopupWindow.dismiss();
        }
    };

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

    private final RadioGroup.OnCheckedChangeListener mOnMaintainResCheckedChangeListener = (group, checkedId) -> {
        switch (group.getCheckedRadioButtonId()) {
            case R.id.maintain_res_button_yes:
                mMaintainResolution = true;
                break;
            case R.id.maintain_res_button_no:
                mMaintainResolution = false;
                break;
            default:
                break;
        }
    };
}
