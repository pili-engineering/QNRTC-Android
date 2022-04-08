package com.qiniu.droid.rtc.api.examples.utils;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.qiniu.droid.rtc.api.examples.R;

public class ToastUtils {
    public static void showShortToast(Context context, String content) {
        showToast(context, content, Toast.LENGTH_SHORT);
    }

    public static void showLongToast(Context context, String content) {
        showToast(context, content, Toast.LENGTH_LONG);
    }

    private static void showToast(Context context, String content, int duration) {
        Toast toast = new Toast(context.getApplicationContext());
        View view = LayoutInflater.from(context.getApplicationContext()).inflate(R.layout.toast_message, null);
        TextView contentView = view.findViewById(R.id.toast_content);
        contentView.setText(content);
        toast.setView(view);
        toast.setDuration(duration);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
}
