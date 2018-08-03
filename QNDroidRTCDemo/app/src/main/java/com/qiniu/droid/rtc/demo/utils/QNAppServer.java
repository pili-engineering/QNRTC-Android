package com.qiniu.droid.rtc.demo.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.qiniu.droid.rtc.demo.R;
import com.qiniu.droid.rtc.demo.model.UpdateInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyStore;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class QNAppServer {
    /**
     * 设置推流画面尺寸，仅用于 Demo 测试，用户可以在创建七牛 APP 时设置该参数
     */
    public static final int STREAMING_WIDTH = 480;
    public static final int STREAMING_HEIGHT = 848;
    public static final int MERGE_STREAM_WIDTH = 160;
    public static final int MERGE_STREAM_HEIGHT = 240;
    public static final String ADMIN_USER = "admin";

    public static final int[][] MERGE_STREAM_POS = new int[][] {
            // X     Y
            {320,   608 },
            {320,   304 },
            {320,   0   },
            {160,   608 },
            {160,   304 },
            {160,   0   },
            {0,     608 },
            {0,     304 },
            {0,     0   },
    };

    private static final String TAG = "QNAppServer";
    private static final String APP_SERVER_ADDR = "https://api-demo.qnsdk.com";
    public static final String APP_ID = "d8lk7l4ed";
    public static final String TEST_MODE_APP_ID = "d8dre8w1p";

    private static class QNAppServerHolder {
        private static final QNAppServer instance = new QNAppServer();
    }

    private QNAppServer(){}

    public static QNAppServer getInstance() {
        return QNAppServerHolder.instance;
    }

    public String requestRoomToken(Context context, String userId, String roomName) {
        /**
         * 此处服务器 URL 仅用于 Demo 测试，随时可能修改/失效，请勿用于 App 线上环境！！
         * 此处服务器 URL 仅用于 Demo 测试，随时可能修改/失效，请勿用于 App 线上环境！！
         * 此处服务器 URL 仅用于 Demo 测试，随时可能修改/失效，请勿用于 App 线上环境！！
         */
        String url = APP_SERVER_ADDR + "/v1/rtc/token/admin/app/" + getAppId(context) + "/room/" + roomName + "/user/" + userId + "?bundleId=" + packageName(context);
        try {
            OkHttpClient okHttpClient = new OkHttpClient.Builder().sslSocketFactory(new SSLSocketFactoryCompat(), getTrustManager()).build();
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Response response = okHttpClient.newCall(request).execute();
            return response.body().string();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public UpdateInfo getUpdateInfo() {
        String url = APP_SERVER_ADDR + "/v1/upgrade/app?appId=com.qiniu.droid.rtc.demo";

        OkHttpClient okHttpClient = new OkHttpClient.Builder().sslSocketFactory(new SSLSocketFactoryCompat(), getTrustManager()).build();

        Request request = new Request.Builder()
                .url(url)
                .build();
        try {
            Response response = okHttpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                JSONObject jsonObject = new JSONObject(response.body().string());
                UpdateInfo updateInfo = new UpdateInfo();
                updateInfo.setAppID(jsonObject.getString(Config.APP_ID));
                updateInfo.setVersion(jsonObject.getInt(Config.VERSION));
                updateInfo.setDescription(jsonObject.getString(Config.DESCRIPTION));
                updateInfo.setDownloadURL(jsonObject.getString(Config.DOWNLOAD_URL));
                updateInfo.setCreateTime(jsonObject.getString(Config.CREATE_TIME));
                return updateInfo;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static X509TrustManager getTrustManager() {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore)null);
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    return (X509TrustManager) tm;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // This shall not happen
        return null;
    }

    public static String packageName(Context context) {
        PackageInfo info;
        try {
            info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.packageName;
        } catch (PackageManager.NameNotFoundException e) {
            // e.printStackTrace();
        }
        return "";
    }

    private String getAppId(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name),Context.MODE_PRIVATE);
        return sharedPreferences.getString(Config.APP_ID, APP_ID);
    }
}
