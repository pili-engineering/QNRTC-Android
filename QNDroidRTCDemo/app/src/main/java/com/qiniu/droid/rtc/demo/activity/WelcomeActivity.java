package com.qiniu.droid.rtc.demo.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.qiniu.droid.rtc.demo.R;

public class WelcomeActivity extends AppCompatActivity {

    private Handler mWelcomeHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        setContentView(R.layout.activity_welcome);

        mWelcomeHandler = new Handler();
        mWelcomeHandler.postDelayed(() -> {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, 1500);
    }

    @Override
    public void onBackPressed() {
        mWelcomeHandler.removeCallbacksAndMessages(null);
        finish();
    }
}
