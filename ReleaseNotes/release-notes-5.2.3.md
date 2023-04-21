# QNDroidRTC Release Notes for 5.2.3

## 简介

QNDroidRTC 是七牛推出的一款适用于 Android 平台的音视频通话 SDK，提供了包括美颜、滤镜、音视频通话等多种功能，提供灵活的接口，支持高度定制以及二次开发。

## 版本

- 发布 qndroid-rtc-5.2.3.jar
- 更新 libqndroid_rtc.so

## 功能

- 支持同时发布多个 QNLocalAudioTrack
- 支持新的日志上报功能
- 支持设置视频在弱网下的降级模式
- 支持设置自定义的 so 加载路径

## 缺陷

- 修复在某些 vivo 手机上，使用蓝牙耳机崩溃的问题
- 修复使用 QNDroidRTC 上线谷歌商店有安全警告的问题
- 修复在某些定制的 Android 设备上，加房抛出 10054 的问题
- 修复对 QNRemoteVideoTrack 设置 QNVideoFrameListener 后，必需调用 play 接口才有回调的问题

## 优化

- 优化在低带宽，高丢包场景下的媒体传输质量
- 优化视频软编码的效率和质量
- 优化在音乐场景下的声音质量

## 注意事项
- 从 5.1.1 开始，视频默认使用软件编码。若有需要，可以通过 QNRTCSetting.setHWCodecEnabled() 修改
- 从 5.1.1 开始，取消对接口 QNTranscodingLiveStreamingConfig.setHoldLastFrame() 的支持
- 如果您使用的版本是 5.0.1+，将不再需要依赖 happy-dns 库

## 问题反馈

当你遇到任何问题时，可以通过在 GitHub 的 repo 提交 `issues` 来反馈问题，请尽可能的描述清楚遇到的问题，如果有错误信息也一同附带，并且在 ```Labels``` 中指明类型为 bug 或者其他。 [通过这里查看已有的 issues 和提交 bug](https://github.com/pili-engineering/QNRTC-Android/issues)
