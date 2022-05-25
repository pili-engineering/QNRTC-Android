# QNDroidRTC Release Notes for 4.0.2

## 简介

QNDroidRTC 是七牛推出的一款适用于 Android 平台的音视频通话 SDK，提供了包括美颜、滤镜、音视频通话等多种功能，提供灵活的接口，支持高度定制以及二次开发。

## 版本

- 发布 qndroid-rtc-4.0.2.jar
- 更新 libqndroid_rtc.so

## 功能

- 升级自研音频回声消除算法

## 缺陷

- 修复 HE-AAC 音频格式混音播放异常
- 修复无法创建纯音频合流转推任务的问题
- 修复个别场景下偶现的空指针问题
- 修复蓝牙耳机场景下的异常问题

## 注意事项

- 从 v4.0.2 版本开始，请务必依赖 happy-dns 1.0.0 版本 dns 解析库，否则将会导致不必要的崩溃：

```java
dependencies {
    implementation 'com.qiniu:happy-dns:1.0.0'
}
```

## 问题反馈

当你遇到任何问题时，可以通过在 GitHub 的 repo 提交 `issues` 来反馈问题，请尽可能的描述清楚遇到的问题，如果有错误信息也一同附带，并且在 ```Labels``` 中指明类型为 bug 或者其他。 [通过这里查看已有的 issues 和提交 bug](https://github.com/pili-engineering/QNRTC-Android/issues)
