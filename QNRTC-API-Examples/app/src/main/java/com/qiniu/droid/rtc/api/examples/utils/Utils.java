package com.qiniu.droid.rtc.api.examples.utils;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Base64;
import android.view.View;

import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.NetworkInfo;
import com.qiniu.android.dns.Record;
import com.qiniu.android.dns.dns.DnsUdpResolver;
import com.qiniu.android.dns.dns.DohResolver;

import org.json.JSONException;
import org.json.JSONObject;

import static com.qiniu.android.dns.IResolver.DNS_DEFAULT_TIMEOUT;

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

    /**
     * 配置自定义 DNS 服务器，非必须
     *
     * - 可通过创建 {@link DnsUdpResolver} 对象配置自定义的 DNS 服务器地址
     * - 可通过创建 {@link DohResolver} 对象配置支持 Doh(Dns over http) 协议的 url
     * 其中，UDP 的方式解析速度快，但是安全性无法得到保证，HTTPDNS 的方式解析速度慢，但是安全性有保证，您可根据您的
     * 使用场景自行选择合适的解析方式
     */
    public static DnsManager getDefaultDnsManager() {
        IResolver[] resolvers = new IResolver[2];
        // 配置自定义 DNS 服务器地址
        String[] udpDnsServers = new String[]{"223.5.5.5", "114.114.114.114", "1.1.1.1", "208.67.222.222"};
        resolvers[0] = new DnsUdpResolver(udpDnsServers, Record.TYPE_A, DNS_DEFAULT_TIMEOUT);
        // 配置 HTTPDNS 地址
        String[] httpDnsServers = new String[]{"https://223.6.6.6/dns-query", "https://8.8.8.8/dns-query"};
        resolvers[1] = new DohResolver(httpDnsServers, Record.TYPE_A, DNS_DEFAULT_TIMEOUT);
        return new DnsManager(NetworkInfo.normal, resolvers);
    }

    public static String getVersion(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }
}
