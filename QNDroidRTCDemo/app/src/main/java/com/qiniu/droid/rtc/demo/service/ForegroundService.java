package com.qiniu.droid.rtc.demo.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.util.Log;

public class ForegroundService extends Service {

    private static final String TAG = "ForegroundService";
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        ForegroundService getService() {
            return ForegroundService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            String channelID = "screen share";
            NotificationChannel channel = new NotificationChannel(channelID,
                    "screen share",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            Notification notification = new NotificationCompat.Builder(this, channelID)
                    .setContentTitle("")
                    .setContentText("").build();
            startForeground(1, notification);
            Log.i(TAG, "start foreground");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Build.VERSION.SDK_INT >= 26) {
            stopForeground(true);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
}

