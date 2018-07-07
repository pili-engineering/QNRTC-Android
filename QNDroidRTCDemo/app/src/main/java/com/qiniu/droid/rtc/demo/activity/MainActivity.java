package com.qiniu.droid.rtc.demo.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.bugsnag.android.Bugsnag;
import com.qiniu.droid.rtc.QNScreenCaptureUtil;
import com.qiniu.droid.rtc.demo.R;
import com.qiniu.droid.rtc.demo.model.ProgressEvent;
import com.qiniu.droid.rtc.demo.model.UpdateInfo;
import com.qiniu.droid.rtc.demo.service.DownloadService;
import com.qiniu.droid.rtc.demo.utils.Config;
import com.qiniu.droid.rtc.demo.utils.QNAppServer;
import com.qiniu.droid.rtc.demo.utils.ToastUtils;
import com.qiniu.droid.rtc.demo.utils.Utils;

import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;

public class MainActivity extends AppCompatActivity {
    private static final int USERNAME_REQUEST_CODE = 0;

    private EditText mRoomEditText;
    private ProgressDialog mProgressDialog;

    private String mUserName;
    private String mRoomName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        Bugsnag.init(this);
        EventBus.getDefault().registerSticky(this);

        SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        mUserName = preferences.getString(Config.USER_NAME, "");
        if (mUserName.equals("")) {
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
        } else if (requestCode == QNScreenCaptureUtil.SCREEN_CAPTURE_PERMISSION_REQUEST_CODE &&
                QNScreenCaptureUtil.onActivityResult(requestCode, resultCode, data)) {
            startConference(mRoomName);
        }
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
        final String roomName = mRoomEditText.getText().toString().trim();
        if (roomName.equals("")) {
            ToastUtils.s(this, getString(R.string.null_room_name_toast));
            return;
        }
        if (!MainActivity.isRoomNameOk(roomName)) {
            ToastUtils.s(this, getString(R.string.wrong_room_name_toast));
            return;
        }

        SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        mUserName = preferences.getString(Config.USER_NAME, "");
        boolean isScreenCaptureEnabled = preferences.getInt(Config.CAPTURE_MODE, Config.CAMERA_CAPTURE) == Config.SCREEN_CAPTURE;

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Config.ROOM_NAME, roomName);
        editor.apply();
        mRoomName = roomName;

        if (isScreenCaptureEnabled) {
            QNScreenCaptureUtil.requestScreenCapture(this);
        } else {
            startConference(roomName);
        }
    }

    public void onClickToSetting(View v) {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
    }

    private void startConference(final String roomName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String token = QNAppServer.getInstance().requestRoomToken(MainActivity.this, mUserName, roomName);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (token == null) {
                            ToastUtils.s(MainActivity.this, getString(R.string.null_room_token_toast));
                            return;
                        }
                        Intent intent = new Intent(MainActivity.this, RoomActivity.class);
                        intent.putExtra(RoomActivity.EXTRA_ROOM_ID, roomName.trim());
                        intent.putExtra(RoomActivity.EXTRA_ROOM_TOKEN, token);
                        intent.putExtra(RoomActivity.EXTRA_USER_ID, mUserName);
                        startActivity(intent);
                    }
                });
            }
        }).start();
    }

    private void initView() {
        setContentView(R.layout.activity_main);
        mRoomEditText = (EditText) findViewById(R.id.room_edit_text);

        SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        String roomName = preferences.getString(Config.ROOM_NAME, Config.PILI_ROOM);
        mRoomEditText.setText(roomName);
    }

    private void checkUpdate() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final UpdateInfo updateInfo = QNAppServer.getInstance().getUpdateInfo();
                if (updateInfo != null && updateInfo.getVersion() > Utils.appVersion(getApplicationContext())) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showUpdateDialog(updateInfo.getDescription(), updateInfo.getDownloadURL());
                        }
                    });
                }
            }
        }).start();
    }

    private void showUpdateDialog(String content, final String downloadUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.android_auto_update_dialog_title);
        builder.setMessage(Html.fromHtml(content))
                .setPositiveButton(R.string.android_auto_update_dialog_btn_download, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        createProgressDialog();
                        goToDownload(downloadUrl);
                    }
                })
                .setNegativeButton(R.string.android_auto_update_dialog_btn_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
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
}
