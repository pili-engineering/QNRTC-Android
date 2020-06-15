# QNDroidRTC Release Notes for 2.4.0

## 简介

QNDroidRTC 是七牛推出的一款适用于 Android 平台的音视频通话 SDK，提供了包括美颜、滤镜、音视频通话等多种功能，提供灵活的接口，支持高度定制以及二次开发。

## 版本

- 发布 qndroid-rtc-2.4.0.jar

## 功能

- 添加对端网络信息回调 onRemoteStatisticsUpdated()
- 添加本地统计回调字段，rtt，networkGrade
- 增加本地离开房间的回调
- 新增在线接口文档

## 缺陷

- 修复自定义消息 QNCustomMessage#mId 为空
- 修复双路视频流发布，设置各码率配置与实际码率不符
- 修复统计信息回调 onStatisticsUpdated() 间隔异常
- 修复某些华为机器编码花屏问题
- 修复双声道混音异常问题
- 修复其他 SDK 异常问题

## 问题反馈

当你遇到任何问题时，可以通过在 GitHub 的 repo 提交 `issues` 来反馈问题，请尽可能的描述清楚遇到的问题，如果有错误信息也一同附带，并且在 ```Labels``` 中指明类型为 bug 或者其他。 [通过这里查看已有的 issues 和提交 bug](https://github.com/pili-engineering/QNRTC-Android/issues)
