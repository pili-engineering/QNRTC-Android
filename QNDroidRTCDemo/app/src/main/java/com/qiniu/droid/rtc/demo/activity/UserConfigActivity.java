package com.qiniu.droid.rtc.demo.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.qiniu.droid.rtc.demo.R;
import com.qiniu.droid.rtc.demo.utils.Config;
import com.qiniu.droid.rtc.demo.utils.PermissionChecker;
import com.qiniu.droid.rtc.demo.utils.ToastUtils;


public class UserConfigActivity extends AppCompatActivity {

    private static final String TAG = "UserConfigActivity";

    private EditText mUsernameEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_config);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        mUsernameEditText = (EditText) findViewById(R.id.user_name_edit_text);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void onClickNext(View view) {
        saveUserName();
    }

    @Override
    public void onBackPressed() {
        saveUserName();
    }

    private boolean isPermissionOK() {
        PermissionChecker checker = new PermissionChecker(this);
        boolean isPermissionOK = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checker.checkPermission();
        if (!isPermissionOK) {
            ToastUtils.l(this, "Some permissions is not approved !!!");
        }
        return isPermissionOK;
    }

    private void saveUserName() {
        final String userName = mUsernameEditText.getText().toString();
        if (userName == null || userName.isEmpty()) {
            ToastUtils.s(this, getString(R.string.null_user_name_toast));
            return;
        }
        if (!MainActivity.isUserNameOk(userName)) {
            ToastUtils.s(this, getString(R.string.wrong_user_name_toast));
            return;
        }
        if (!isPermissionOK()) {
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(Config.USER_NAME, userName);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
