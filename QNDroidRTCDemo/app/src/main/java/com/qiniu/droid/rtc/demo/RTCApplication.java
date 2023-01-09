package com.qiniu.droid.rtc.demo;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.qiniu.droid.rtc.QNFileLogHelper;
import com.qiniu.droid.rtc.demo.utils.ToastUtils;
import com.qiniu.droid.rtc.demo.utils.Utils;

import java.io.File;

import xcrash.TombstoneManager;
import xcrash.XCrash;

public class RTCApplication extends Application {
    private static final String TAG = "RTCApplication";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        Log.i(TAG, "external file path = " + getExternalFilesDir(null));
        xcrash.XCrash.init(this, new XCrash.InitParameters()
                .setLogDir(getExternalFilesDir(null).getAbsolutePath())
                .setJavaDumpNetworkInfo(false)
                .setNativeDumpNetwork(false)
                .setNativeDumpAllThreads(false)
                .setAppVersion(Utils.getVersion(this)));
        checkToUploadCrashFiles();
    }

    private void checkToUploadCrashFiles() {
        File crashFolder = new File(getExternalFilesDir(null).getAbsolutePath());
        File[] crashFiles = crashFolder.listFiles();
        if (crashFiles == null) {
            return;
        }
        for (File crashFile : crashFiles) {
            if (crashFile.isFile() && crashFile.getName().contains("xcrash")) {
                QNFileLogHelper.getInstance().reportLogFileByPath(crashFile.getPath(), new QNFileLogHelper.LogReportCallback() {
                    @Override
                    public void onReportSuccess(String name) {
                        ToastUtils.showShortToast(RTCApplication.this, "崩溃日志已上传！");
                        TombstoneManager.deleteTombstone(crashFile.getPath());
                    }

                    @Override
                    public void onReportError(String name, String errorMsg) {
                        ToastUtils.showShortToast(RTCApplication.this, "崩溃日志上传失败 : " + errorMsg);
                    }
                });
            }
        }
    }
}
