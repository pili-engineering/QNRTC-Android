package com.qiniu.droid.rtc.demo.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.bugsnag.android.Bugsnag;
import com.qiniu.droid.rtc.QNScreenVideoTrack;
import com.qiniu.droid.rtc.demo.R;
import com.qiniu.droid.rtc.demo.model.ProgressEvent;
import com.qiniu.droid.rtc.demo.model.UpdateInfo;
import com.qiniu.droid.rtc.demo.service.DownloadService;
import com.qiniu.droid.rtc.demo.ui.RadioGroupFlow;
import com.qiniu.droid.rtc.demo.utils.Config;
import com.qiniu.droid.rtc.demo.utils.PermissionChecker;
import com.qiniu.droid.rtc.demo.utils.QNAppServer;
import com.qiniu.droid.rtc.demo.utils.ToastUtils;
import com.qiniu.droid.rtc.demo.utils.Utils;

import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;

public class MainActivity extends AppCompatActivity {
    private static final int USERNAME_REQUEST_CODE = 0;

    private EditText mRoomEditText;
    private ProgressDialog mProgressDialog;
    private PermissionChecker mChecker;

    private String mUserName;
    private String mRoomName;
    private int mCaptureMode = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bugsnag.init(this);
        EventBus.getDefault().registerSticky(this);
        SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        mUserName = preferences.getString(Config.USER_NAME, "");
        mChecker = new PermissionChecker(this);
        if ("".equals(mUserName)) {
            Intent intent = new Intent(this, UserConfigActivity.class);
            startActivityForResult(intent, USERNAME_REQUEST_CODE);
        } else {
            initView();
            checkUpdate();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == USERNAME_REQUEST_CODE) {
            mUserName = data.getStringExtra(Config.USER_NAME);
            SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).edit();
            editor.putString(Config.USER_NAME, mUserName);
            editor.apply();
            initView();
            checkUpdate();
        } else if (requestCode == QNScreenVideoTrack.SCREEN_CAPTURE_PERMISSION_REQUEST_CODE &&
                QNScreenVideoTrack.checkActivityResult(requestCode, resultCode, data)) {
            startConference(mRoomName);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] state) {
        super.onRequestPermissionsResult(requestCode, permissions, state);
        mChecker.onRequestPermissionsResult(requestCode, permissions, state);
    }

    public void onEvent(ProgressEvent progressEvent) {
        mProgressDialog.setProgress(progressEvent.getProgress());
        if (progressEvent.getProgress() == 100) {
            mProgressDialog.dismiss();
        }
    }

    public static boolean isUserNameOk(String userName) {
        Pattern pattern = Pattern.compile(Config.USER_NAME_RULE);
        return pattern.matcher(userName).matches();
    }

    public static boolean isRoomNameOk(String roomName) {
        Pattern pattern = Pattern.compile(Config.ROOM_NAME_RULE);
        return pattern.matcher(roomName).matches();
    }

    public void onClickConference(final View v) {
        handleRoomInfo();
        SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        mUserName = preferences.getString(Config.USER_NAME, "");
        boolean isScreenCaptureEnabled = (mCaptureMode == Config.SCREEN_CAPTURE || mCaptureMode == Config.MUTI_TRACK_CAPTURE);
        if (isScreenCaptureEnabled) {
            QNScreenVideoTrack.requestPermission(this);
        } else {
            startConference(mRoomName);
        }
    }

    public void onClickToSetting(View v) {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
    }

    private void startConference(final String roomName) {
        if (!isPermissionOK()) {
            return;
        }
        if (!handleRoomInfo()) {
            return;
        }
        new Thread(() -> {
            // 获取连麦所需的 RoomToken，需要您自行实现业务服务器的相关逻辑
            // 详情请参考【服务端开发说明.RoomToken 签发服务】https://doc.qnsdk.com/rtn/docs/server_overview#1
            final String token = QNAppServer.getInstance().requestRoomToken(MainActivity.this, mUserName, roomName);

            runOnUiThread(() -> {
                if (token == null) {
                    ToastUtils.showShortToast(MainActivity.this, getString(R.string.null_room_token_toast));
                    return;
                }
                Intent intent = new Intent(MainActivity.this, RoomActivity.class);
                intent.putExtra(RoomActivity.EXTRA_ROOM_ID, roomName.trim());
                intent.putExtra(RoomActivity.EXTRA_ROOM_TOKEN, token);
                intent.putExtra(RoomActivity.EXTRA_USER_ID, mUserName);
                startActivity(intent);
            });
        }).start();
    }

    public void onClickLiveRoom(View v) {
        if (!handleRoomInfo()) {
            return;
        }
        Intent intent = new Intent(MainActivity.this, LiveRoomActivity.class);
        intent.putExtra(LiveRoomActivity.EXTRA_ROOM_ID, mRoomName.trim());
        startActivity(intent);
    }

    private boolean handleRoomInfo() {
        String roomName = mRoomEditText.getText().toString().trim();
        if ("".equals(roomName)) {
            ToastUtils.showShortToast(this, getString(R.string.null_room_name_toast));
            return false;
        }
        if (!MainActivity.isRoomNameOk(roomName)) {
            ToastUtils.showShortToast(this, getString(R.string.wrong_room_name_toast));
            return false;
        }

        SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        mUserName = preferences.getString(Config.USER_NAME, "");
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Config.ROOM_NAME, roomName);
        editor.putInt(Config.CAPTURE_MODE, mCaptureMode);
        if (mCaptureMode == Config.SCREEN_CAPTURE) {
            editor.putInt(Config.CODEC_MODE, Config.HW);
        }
        editor.apply();
        mRoomName = roomName;
        return true;
    }

    private void initView() {
        setContentView(R.layout.activity_main);
        mRoomEditText = findViewById(R.id.room_edit_text);
        RadioGroupFlow captureModeRadioGroup = findViewById(R.id.capture_mode_button);
        captureModeRadioGroup.setOnCheckedChangeListener(mOnCheckedChangeListener);
        RadioButton screenCapture = findViewById(R.id.screen_capture_button);
        RadioButton cameraCapture = findViewById(R.id.camera_capture_button);
        RadioButton onlyAudioCapture = findViewById(R.id.audio_capture_button);
        RadioButton mutiTrackCapture = findViewById(R.id.muti_track_button);

        SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        String roomName = preferences.getString(Config.ROOM_NAME, Config.PILI_ROOM);
        int captureMode = preferences.getInt(Config.CAPTURE_MODE, Config.CAMERA_CAPTURE);
        if (QNScreenVideoTrack.isScreenCaptureSupported()) {
            if (captureMode == Config.SCREEN_CAPTURE) {
                screenCapture.setChecked(true);
            } else if (captureMode == Config.CAMERA_CAPTURE) {
                cameraCapture.setChecked(true);
            } else if (captureMode == Config.ONLY_AUDIO_CAPTURE){
                onlyAudioCapture.setChecked(true);
            } else {
                mutiTrackCapture.setChecked(true);
            }
        } else {
            screenCapture.setEnabled(false);
        }
        mRoomEditText.setText(roomName);
        mRoomEditText.setSelection(roomName.length());
    }

    private void checkUpdate() {
        new Thread(() -> {
            final UpdateInfo updateInfo = QNAppServer.getInstance().getUpdateInfo();
            if (updateInfo != null && updateInfo.getVersion() > Utils.appVersion(getApplicationContext())) {
                runOnUiThread(() -> showUpdateDialog(updateInfo.getDescription(), updateInfo.getDownloadURL()));
            }
        }).start();
    }

    private void showUpdateDialog(String content, final String downloadUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.android_auto_update_dialog_title);
        builder.setMessage(Html.fromHtml(content))
                .setPositiveButton(R.string.android_auto_update_dialog_btn_download, (dialog, id) -> {
                    dialog.dismiss();
                    createProgressDialog();
                    goToDownload(downloadUrl);
                })
                .setNegativeButton(R.string.android_auto_update_dialog_btn_cancel, (dialog, id) -> {
                });

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void createProgressDialog() {
        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setTitle(getString(R.string.updating));
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setMax(100);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();
    }

    private void goToDownload(String downloadUrl) {
        Intent intent = new Intent(MainActivity.this, DownloadService.class);
        intent.putExtra(Config.DOWNLOAD_URL, downloadUrl);
        startService(intent);
    }

    private boolean isPermissionOK() {
        boolean isPermissionOK = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || mChecker.checkPermission();
        return isPermissionOK;
    }

    private final RadioGroup.OnCheckedChangeListener mOnCheckedChangeListener = (group, checkedId) -> {
        switch (group.getCheckedRadioButtonId()) {
            case R.id.camera_capture_button:
                mCaptureMode = Config.CAMERA_CAPTURE;
                break;
            case R.id.screen_capture_button:
                mCaptureMode = Config.SCREEN_CAPTURE;
                break;
            case R.id.audio_capture_button:
                mCaptureMode = Config.ONLY_AUDIO_CAPTURE;
                break;
            case R.id.muti_track_button:
                mCaptureMode = Config.MUTI_TRACK_CAPTURE;
                break;
            default:
                break;
        }
    };
}
