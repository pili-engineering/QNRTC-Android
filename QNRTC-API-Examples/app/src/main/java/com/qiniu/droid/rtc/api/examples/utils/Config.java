package com.qiniu.droid.rtc.api.examples.utils;

public class Config {
    // 输入您的 RoomToken，生成方式可参考 https://developer.qiniu.com/rtc/9858/applist#4
    public static String ROOM_TOKEN = "";
    // CDN 转推场景下需要配置推流的 rtmp 地址，获取方式可参考 https://developer.qiniu.com/pili/1221/the-console-quick-start
    public static String PUBLISH_URL = "自定义转推 rtmp 地址";

    public static final String KEY_APP_ID = "appId";
    public static final String KEY_ROOM_NAME = "roomName";
    public static final String KEY_USER_ID = "userId";

    public static final String TAG_CAMERA_TRACK = "camera";
    public static final String TAG_MICROPHONE_TRACK = "microphone";
    public static final String TAG_SCREEN_TRACK = "screen";
    public static final String TAG_CUSTOM_VIDEO_TRACK = "custom_video";
    public static final String TAG_CUSTOM_AUDIO_TRACK = "custom_audio";

    public static final int DEFAULT_WIDTH = 640;
    public static final int DEFAULT_HEIGHT = 480;
    public static final int DEFAULT_FPS = 15;
    public static final int DEFAULT_VIDEO_BITRATE = 1000;
    public static final int DEFAULT_AUDIO_SAMPLE_RATE = 44100;
    public static final int DEFAULT_AUDIO_CHANNEL_COUNT = 1;
    public static final int DEFAULT_AUDIO_BITRATE = 64;
    public static final float DEFAULT_SCREEN_VIDEO_TRACK_SIZE_SCALE = 0.5f;
}
