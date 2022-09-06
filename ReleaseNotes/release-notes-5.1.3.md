# QNDroidRTC Release Notes for 5.1.3

## 简介

QNDroidRTC 是七牛推出的一款适用于 Android 平台的音视频通话 SDK，提供了包括美颜、滤镜、音视频通话等多种功能，提供灵活的接口，支持高度定制以及二次开发。

## 版本

- 发布 qndroid-rtc-5.1.3.jar
- 更新 libqndroid_rtc.so

## 优化

- 升级音频回声处理算法, 优化回声处理的兼容性和稳定性

## 功能

- 新增音频场景化配置功能

## 缺陷

- 修复 init/deinit 过程中概率崩溃的问题
- 修复硬解码概率崩溃的问题

## 注意事项
- 从 5.1.1 开始，视频默认使用软件编码。若有需要，可以通过 QNRTCSetting.setHWCodecEnabled() 修改
- 从 5.1.1 开始，取消对接口 QNTranscodingLiveStreamingConfig.setHoldLastFrame() 的支持
- 如果您使用的版本是 5.0.1+，将不再需要依赖 happy-dns 库

## 问题反馈

当你遇到任何问题时，可以通过在 GitHub 的 repo 提交 `issues` 来反馈问题，请尽可能的描述清楚遇到的问题，如果有错误信息也一同附带，并且在 ```Labels``` 中指明类型为 bug 或者其他。 [通过这里查看已有的 issues 和提交 bug](https://github.com/pili-engineering/QNRTC-Android/issues)
