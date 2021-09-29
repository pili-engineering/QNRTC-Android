package com.qiniu.droid.rtc.demo.utils;

import android.content.Context;
import android.widget.Toast;

public class ToastUtils {
    public static void showShortToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static void showLongToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }
}
