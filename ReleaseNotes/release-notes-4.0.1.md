# QNDroidRTC Release Notes for 4.0.1

## 简介

QNDroidRTC 是七牛推出的一款适用于 Android 平台的音视频通话 SDK，提供了包括美颜、滤镜、音视频通话等多种功能，提供灵活的接口，支持高度定制以及二次开发。

## 版本

- 发布 qndroid-rtc-4.0.1.jar

## 功能

- 新增场景和角色设置，详见 QNRTC.createClient, QNRTCClient.setClientRole
- 跨房媒体转发功能，详见 QNRTCClient.(start/update/stop)MediaRelay
- 麦克风被占用时提供检测方法或错误回调

## 缺陷

- 修复断网时统计信息依然回调质量正常的问题

## 问题反馈

当你遇到任何问题时，可以通过在 GitHub 的 repo 提交 `issues` 来反馈问题，请尽可能的描述清楚遇到的问题，如果有错误信息也一同附带，并且在 ```Labels``` 中指明类型为 bug 或者其他。 [通过这里查看已有的 issues 和提交 bug](https://github.com/pili-engineering/QNRTC-Android/issues)
