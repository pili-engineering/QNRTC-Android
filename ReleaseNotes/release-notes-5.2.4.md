# QNDroidRTC Release Notes for 5.2.4

## 简介

QNDroidRTC 是七牛推出的一款适用于 Android 平台的音视频通话 SDK，提供了包括美颜、滤镜、音视频通话等多种功能，提供灵活的接口，支持高度定制以及二次开发。

## 版本

- 发布 qndroid-rtc-5.2.4.jar
- 更新 libqndroid_rtc.so

## 功能

- 支持设置房间重连的超时时间
- 对于转推任务异常断开的情况，在 QNLiveStreamingListener.onError() 中新增 ERROR_LIVE_STREAMING_CLOSED 错误码
- 提供视频编码参数的预设值接口
- 对 QNLocalVideoTrackStats 新增发布的宽高等统计信息

## 缺陷

- 修复在模拟器上运行崩溃的问题
- 修复 Track 在销毁后再发布崩溃的问题
- 修复使用 QNAudioMusicMixer.start() 接口无法开始混音的问题
- 修复同时订阅同一个 Track 概率发生错误的问题

## 优化

- 优化在弱网传输时的重传和接收 buffer 策略，降低卡顿率
- 优化在佩戴耳机场景下的音频处理流程，提升耳机场景的体验

## 注意事项
- 从 5.2.4 开始，org.webrtc 包被重命名为 org.qnwebrtc，请注意适配修改
- 从 5.2.4 开始，接口 QNRTCSetting.setAEC3Enabled 被删除，请使用 QNRTCSetting.setAudioScene　接口
- 从 5.1.1 开始，视频默认使用软件编码。若有需要，可以通过 QNRTCSetting.setHWCodecEnabled() 修改
- 从 5.1.1 开始，取消对接口 QNTranscodingLiveStreamingConfig.setHoldLastFrame() 的支持
- 如果您使用的版本是 5.0.1+，将不再需要依赖 happy-dns 库

## 问题反馈

当你遇到任何问题时，可以通过在 GitHub 的 repo 提交 `issues` 来反馈问题，请尽可能的描述清楚遇到的问题，如果有错误信息也一同附带，并且在 ```Labels``` 中指明类型为 bug 或者其他。 [通过这里查看已有的 issues 和提交 bug](https://github.com/pili-engineering/QNRTC-Android/issues)
