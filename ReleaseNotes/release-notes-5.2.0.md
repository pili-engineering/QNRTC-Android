# QNDroidRTC Release Notes for 5.2.0

## 简介

QNDroidRTC 是七牛推出的一款适用于 Android 平台的音视频通话 SDK，提供了包括美颜、滤镜、音视频通话等多种功能，提供灵活的接口，支持高度定制以及二次开发。

## 版本

- 发布 qndroid-rtc-5.2.0.jar
- 更新 libqndroid_rtc.so

## 优化

- 优化弱网下视频质量调整策略，平衡清晰度和流畅性

## 功能

- 支持音乐，音效通过 RTC 进行本地播放
- 支持外部音源 (PCM) 混音
- 支持设置拉伸模式进行视频渲染

## 缺陷

- 修复稳定性崩溃问题
- 修复展讯平台硬编码无法使用的问题

## 注意事项
- 从 5.1.1 开始，视频默认使用软件编码。若有需要，可以通过 QNRTCSetting.setHWCodecEnabled() 修改
- 从 5.1.1 开始，取消对接口 QNTranscodingLiveStreamingConfig.setHoldLastFrame() 的支持
- 如果您使用的版本是 5.0.1+，将不再需要依赖 happy-dns 库

## 问题反馈

当你遇到任何问题时，可以通过在 GitHub 的 repo 提交 `issues` 来反馈问题，请尽可能的描述清楚遇到的问题，如果有错误信息也一同附带，并且在 ```Labels``` 中指明类型为 bug 或者其他。 [通过这里查看已有的 issues 和提交 bug](https://github.com/pili-engineering/QNRTC-Android/issues)
