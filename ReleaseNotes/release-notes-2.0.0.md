# QNDroidRTC Release Notes for 2.0.0

## 简介

QNDroidRTC 是七牛推出的一款适用于 Android 平台的音视频通话 SDK，提供了包括美颜、滤镜、水印、音视频通话等多种功能，提供灵活的接口，支持高度定制以及二次开发。

## 版本

- 发布 qndroid-rtc-2.0.0.jar
- 更新 libqndroid_rtc.so
- 更新 libqndroid_beauty.so

## 注意

本次升级为主版本升级(1.2.0 -> 2.0.0)，为了支持更灵活的连麦控制和更低的资源开销有比较大的重构。请查看我们的[新版文档站](https://doc.qnsdk.com/rtn/android/)。

自 2.0.0 后，我们为了提高用户阅读文档的体验，使用了新的文档站（老文档地址继续保留）。新文档站地址 https://doc.qnsdk.com/rtn

## 功能

- 新增核心类 QNRTCEngine，支持本地发布多路视频，支持音频与视频分开发布
- 新增自动订阅功能

## 优化

- 内部优化，使用 QNRTCEngine 能够达到更低的功耗和更低的内存占用

## 注意事项

- 2.0.0 之前的部分接口我们已标记为废弃，但不影响使用，我们不推荐继续使用 QNRTCManager 内的接口。建议将 QNRTCManager 升级至 QNRTCEngine，获得更好的体验。

## 问题反馈

当你遇到任何问题时，可以通过在 GitHub 的 repo 提交 `issues` 来反馈问题，请尽可能的描述清楚遇到的问题，如果有错误信息也一同附带，并且在 ```Labels``` 中指明类型为 bug 或者其他。 [通过这里查看已有的 issues 和提交 bug](https://github.com/pili-engineering/QNRTC-Android/issues)
