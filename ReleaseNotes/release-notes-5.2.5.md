# QNDroidRTC Release Notes for 5.2.5

## 简介

QNDroidRTC 是七牛推出的一款适用于 Android 平台的音视频通话 SDK，提供了包括美颜、滤镜、音视频通话等多种功能，提供灵活的接口，支持高度定制以及二次开发。

## 版本

- 发布 qndroid-rtc-5.2.5.jar
- 更新 libqndroid_rtc.so
- 新增 libqnquic.so
- 新增 libqcrash.so

## 功能

- 新增对 quic 协议信令交互的支持
- 新增崩溃搜集模块
- 新增图片推流场景下的错误回调
- 新增支持 QNRTCSetting.setCustomSharedLibraryDir 接口设置未创建的路径

## 缺陷

- 修复日志本地存储场景下偶现的崩溃问题
- 修复个别场景下已知的低概率崩溃问题

## 注意事项
- libqnquic.so 用于优化弱网下的信令传输，为非必需依赖项
- libqcrash.so 用于搜集 SDK 的崩溃信息，为非必需依赖项
- 从 5.2.4 开始，org.webrtc 包被重命名为 org.qnwebrtc，请注意适配修改
- 从 5.2.4 开始，接口 QNRTCSetting.setAEC3Enabled 被删除，请使用 QNRTCSetting.setAudioScene 接口

## 问题反馈

当你遇到任何问题时，可以通过在 GitHub 的 repo 提交 `issues` 来反馈问题，请尽可能的描述清楚遇到的问题，如果有错误信息也一同附带，并且在 ```Labels``` 中指明类型为 bug 或者其他。 [通过这里查看已有的 issues 和提交 bug](https://github.com/pili-engineering/QNRTC-Android/issues)
