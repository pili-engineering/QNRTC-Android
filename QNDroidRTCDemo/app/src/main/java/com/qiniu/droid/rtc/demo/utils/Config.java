package com.qiniu.droid.rtc.demo.utils;

import com.qiniu.droid.rtc.QNDegradationPreference;

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
    public static final String VIDEO_CONFIG_POS = "videoConfigPos";
    public static final String VIDEO_DEGRADATION_POS = "videoDegradationPos";
    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";
    public static final String FPS = "fps";
    public static final String CODEC_MODE = "encodeMode";
    public static final String SAMPLE_RATE = "sampleRate";
    public static final String AUDIO_SCENE = "audioScene";
    public static final String CAPTURE_MODE = "captureMode";
    public static final String BITRATE = "bitrate";

    public static final int HW = 0;
    public static final int SW = 1;
    public static final int LOW_SAMPLE_RATE = 0;
    public static final int HIGH_SAMPLE_RATE = 1;
    public static final int DEFAULT_AUDIO_SCENE = 0;
    public static final int VOICE_CHAT_AUDIO_SCENE = 1;
    public static final int SOUND_EQUALIZE_AUDIO_SCENE = 2;
    public static final int CAMERA_CAPTURE = 0;
    public static final int SCREEN_CAPTURE = 1;
    public static final int ONLY_AUDIO_CAPTURE = 2;
    public static final int MUTI_TRACK_CAPTURE = 3;
    public static final float DEFAULT_SCREEN_VIDEO_TRACK_SIZE_SCALE = 0.5f;
    public static final int DEFAULT_VIDEO_DEGRADATION_POS = 0;

    public static final QNDegradationPreference[] VIDEO_DEGRADATION_PRESET = {
            QNDegradationPreference.MAINTAIN_FRAMERATE,
            QNDegradationPreference.MAINTAIN_RESOLUTION,
            QNDegradationPreference.BALANCED,
            QNDegradationPreference.ADAPT_BITRATE_ONLY,
    };

    public static final String[] VIDEO_DEGRADATION_TIPS = {
            "保持帧率, 降低分辨率和适当的码率",
            "保持分辨率, 降低帧率和适当的码率",
            "平衡模式, 降低帧率，分辨率和适当的码率",
            "控制码率, 保持帧率和分辨率"
    };

    /**
     * 视频的分辨率，码率和帧率设置会影响到连麦质量；更高的分辨率和帧率也就意味着需要更大的码率和更好的网络环境。
     *
     * 首先，建议您根据实际产品情况选择分辨率，在不超过视频源分辨率的情况下更高的分辨率对应着更好的质量，
     * 在具体数值上，建议您根据下表或者常见的视频分辨率来做设置；
     * 然后，可以根据您的实际情况来选择帧率，帧率越高更能表现运动画面效果；通常设置为 25 或者 30 即可；
     * 最后，选择合适的码率设置，如果实际场景中有运动情况较多，可以参考下表中选择上限值。
     *
     * 如果您需要的分辨率或者帧率不在下表中，可参考此篇文章给出的推荐值：
     * http://www.lighterra.com/papers/videoencodingh264/
     */
    public static final int[][] DEFAULT_RESOLUTION = {
            {352, 288}, // 240p
            {640, 480}, // 480p
            {960, 544}, // 540p
            {1280, 720} // 720p
    };

    public static int[] DEFAULT_FPS = {
            15,
            20,
            25,
            30
    };

    public static int[] DEFAULT_BITRATE = {
            600,  // (500 - 600kbps)
            1000,  // (900 - 1200kbps)
            1500, // (1400 - 1500kbps)
            2000  // (1800 - 2000kbps)
    };
}
