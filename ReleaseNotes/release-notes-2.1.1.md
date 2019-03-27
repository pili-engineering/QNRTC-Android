# QNDroidRTC Release Notes for 2.1.1

## 简介

QNDroidRTC 是七牛推出的一款适用于 Android 平台的音视频通话 SDK，提供了包括美颜、滤镜、水印、音视频通话等多种功能，提供灵活的接口，支持高度定制以及二次开发。

## 版本

- 发布 qndroid-rtc-2.1.1.jar
- 更新 libqndroid_rtc.so

## 功能

- 支持在硬编时自动调节分辨率
- 支持采集分辨率与编码分辨率分别采用不同朝向
- 新增销毁指定 Track 的接口
- 本地视频预览增加开始采集/停止采集回调

## 缺陷

- 修复部分机型及部分情况下的花屏问题
- 修复统计信息接口无法统计所有用户信息的问题

## 优化

- 合并部分状态码以便开发者处理
- SDK 内部增加对订阅失败及发布失败的重试逻辑

## 问题反馈

当你遇到任何问题时，可以通过在 GitHub 的 repo 提交 `issues` 来反馈问题，请尽可能的描述清楚遇到的问题，如果有错误信息也一同附带，并且在 ```Labels``` 中指明类型为 bug 或者其他。 [通过这里查看已有的 issues 和提交 bug](https://github.com/pili-engineering/QNRTC-Android/issues)
