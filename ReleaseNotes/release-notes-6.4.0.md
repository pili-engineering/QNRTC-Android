# QNDroidRTC Release Notes for 6.4.0

## 简介

QNDroidRTC 是七牛推出的一款适用于 Android 平台的音视频通话 SDK，提供了包括美颜、滤镜、音视频通话等多种功能，提供灵活的接口，支持高度定制以及二次开发。

## 版本

- 发布 qndroid-rtc-6.4.0.jar
- 更新 libqndroid_rtc.so

## 功能

- 支持内置媒体播放器

## 缺陷

- 修复远端音频回调数据异常的问题

## 注意事项
- 混音的使用姿势发生了改变，详情可参考[背景音乐混音](https://developer.qiniu.com/rtc/8771/background-music-mix-android)、[多音效混音](https://developer.qiniu.com/rtc/11965/android_audio_effect_mixing)、[音频裸数据混音](https://developer.qiniu.io/rtc/12581/android_audio_source_mixing)使用指南
- libqnquic.so 用于优化弱网下的信令传输，为非必需依赖项
- libqcrash.so 用于搜集 SDK 的崩溃信息，为非必需依赖项

## 问题反馈

当你遇到任何问题时，可以通过在 GitHub 的 repo 提交 `issues` 来反馈问题，请尽可能的描述清楚遇到的问题，如果有错误信息也一同附带，并且在 ```Labels``` 中指明类型为 bug 或者其他。 [通过这里查看已有的 issues 和提交 bug](https://github.com/pili-engineering/QNRTC-Android/issues)
