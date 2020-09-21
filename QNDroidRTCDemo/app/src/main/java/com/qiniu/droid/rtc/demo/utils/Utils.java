package com.qiniu.droid.rtc.demo.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.NetworkInfo;
import com.qiniu.android.dns.http.DnspodFree;
import com.qiniu.android.dns.local.AndroidDnsServer;
import com.qiniu.android.dns.local.Resolver;

import java.io.IOException;
import java.net.InetAddress;

public final class Utils {

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

    public static DnsManager getDefaultDnsManager(Context context) {
        IResolver r0 = null;
        try {
            // 默认使用阿里云公共 DNS 服务，避免系统 DNS 解析可能出现的跨运营商、重定向等问题，详情可参考 https://www.alidns.com/
            // 超时时间参数可选，不指定默认为 10s 的超时
            // 超时时间单位：s
            r0 = new Resolver(InetAddress.getByName("223.5.5.5"), 3);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 默认 Dnspod 服务，使用腾讯公共 DNS 服务，详情可参考 https://www.dnspod.cn/Products/Public.DNS
        // 超时时间参数可选，不指定默认为 10s 的超时
        // 超时时间单位：s
        IResolver r1 = new DnspodFree("119.29.29.29", 3);
        // 系统默认 DNS 解析，可能会出现解析跨运营商等问题
        IResolver r2 = AndroidDnsServer.defaultResolver(context);
        return new DnsManager(NetworkInfo.normal, new IResolver[]{r0, r1, r2});
    }
}
