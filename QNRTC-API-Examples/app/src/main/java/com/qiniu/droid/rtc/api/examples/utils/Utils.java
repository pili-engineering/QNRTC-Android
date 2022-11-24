package com.qiniu.droid.rtc.api.examples.utils;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Base64;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

public final class Utils {

    public static String packageName(Context context) {
        PackageInfo info;
        try {
            info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.packageName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static int appVersion(Context context) {
        PackageInfo info;
        try {
            info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void showAlertDialog(Context context, String message) {
        new AlertDialog.Builder(context)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("确定", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    public static String base64Decode(String msg) {
        try {
            return new String(Base64.decode(msg.getBytes(), Base64.DEFAULT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static JSONObject parseRoomToken(String roomToken) {
        if (roomToken == null) {
            return null;
        }
        String[] tokens = roomToken.split(":");
        if (tokens.length != 3) {
            return null;
        }
        String roomInfo = Utils.base64Decode(tokens[2]);
        if (roomInfo == null) {
            return null;
        }
        try {
            return new JSONObject(roomInfo);
        } catch (JSONException e) {
            return null;
        }
    }

    @TargetApi(19)
    public static int getSystemUiVisibility() {
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        return flags;
    }
}
