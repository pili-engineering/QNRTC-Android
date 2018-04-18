package com.qiniu.droid.rtc.demo;

import android.app.Application;

import com.qiniu.droid.rtc.QNLogLevel;
import com.qiniu.droid.rtc.QNRTCEnv;

public class RTCApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        QNRTCEnv.setLogLevel(QNLogLevel.INFO);
        /**
         * init must be called before any other func
         */
        QNRTCEnv.init(getApplicationContext());
    }
}
