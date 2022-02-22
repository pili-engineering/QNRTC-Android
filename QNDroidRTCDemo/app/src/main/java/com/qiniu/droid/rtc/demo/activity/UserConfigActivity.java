package com.qiniu.droid.rtc.demo.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.qiniu.droid.rtc.demo.R;
import com.qiniu.droid.rtc.demo.utils.Config;
import com.qiniu.droid.rtc.demo.utils.PermissionChecker;
import com.qiniu.droid.rtc.demo.utils.ToastUtils;

public class UserConfigActivity extends AppCompatActivity {

    private EditText mUsernameEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_config);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        mUsernameEditText = findViewById(R.id.user_name_edit_text);
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

    private void saveUserName() {
        final String userName = mUsernameEditText.getText().toString();
        if (userName.isEmpty()) {
            ToastUtils.showShortToast(this, getString(R.string.null_user_name_toast));
            return;
        }
        if (!MainActivity.isUserNameOk(userName)) {
            ToastUtils.showShortToast(this, getString(R.string.wrong_user_name_toast));
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(Config.USER_NAME, userName);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
