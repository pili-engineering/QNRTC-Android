package com.qiniu.droid.rtc.demo;

import android.app.Application;

import com.qiniu.droid.rtc.QNLogLevel;
import com.qiniu.droid.rtc.QNRTCEnv;
import com.qiniu.droid.rtc.demo.utils.Utils;

public class RTCApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        QNRTCEnv.setLogLevel(QNLogLevel.INFO);
        /**
         * init must be called before any other func
         */
        QNRTCEnv.init(getApplicationContext());
        QNRTCEnv.setLogFileEnabled(true);
        // 设置自定义 DNS manager，不设置则使用 SDK 默认 DNS 服务
        QNRTCEnv.setDnsManager(Utils.getDefaultDnsManager(getApplicationContext()));
    }
}
