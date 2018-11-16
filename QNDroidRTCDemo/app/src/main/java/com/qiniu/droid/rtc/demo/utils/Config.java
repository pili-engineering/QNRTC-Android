package com.qiniu.droid.rtc.demo.utils;

public class Config {
    public static final String ROOM_NAME_RULE = "^[a-zA-Z0-9_-]{3,64}$";
    public static final String USER_NAME_RULE = "^[a-zA-Z0-9_-]{3,50}$";
    public static final String DOWNLOAD_URL = "DownloadURL";
    public static final String APP_ID = "AppId";
    public static final String VERSION = "Version";
    public static final String DESCRIPTION = "Description";
    public static final String CREATE_TIME = "CreateAt";

    public static final String PILI_ROOM = "test";
    public static final String ROOM_NAME = "roomName";
    public static final String USER_NAME = "userName";
    public static final String CONFIG_POS = "configPos";
    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";
    public static final String FPS = "fps";
    public static final String CODEC_MODE = "encodeMode";
    public static final String CAPTURE_MODE = "captureMode";
    public static final String BITRATE = "bitrate";

    public static final int HW = 0;
    public static final int SW = 1;
    public static final int CAMERA_CAPTURE = 0;
    public static final int SCREEN_CAPTURE = 1;
    public static final int ONLY_AUDIO_CAPTURE = 2;

    public static final int [][] DEFAULT_RESOLUTION = {
            {352, 288},
            {640, 480},
            {960, 540},
            {1280, 720}
    };

    public static int [] DEFAULT_FPS = {
            15,
            15,
            15,
            20
    };

    public static int [] DEFAULT_BITRATE = {
            400,
            800,
            800,
            1800
    };
}
