# QNDroidRTC Release Notes for 4.0.3

## 简介

QNDroidRTC 是七牛推出的一款适用于 Android 平台的音视频通话 SDK，提供了包括美颜、滤镜、音视频通话等多种功能，提供灵活的接口，支持高度定制以及二次开发。

## 版本

- 发布 qndroid-rtc-4.0.3.jar

## 缺陷

- 修复加房间前设置连麦角色失效的问题

## 优化

- 优化蓝牙场景的使用体验

## 注意事项

- 从 v4.0.2 版本开始，请务必依赖 happy-dns 1.0.1 版本 dns 解析库，否则将会导致不必要的崩溃：

```java
dependencies {
    implementation 'com.qiniu:happy-dns:1.0.1'
}
```

## 问题反馈

当你遇到任何问题时，可以通过在 GitHub 的 repo 提交 `issues` 来反馈问题，请尽可能的描述清楚遇到的问题，如果有错误信息也一同附带，并且在 ```Labels``` 中指明类型为 bug 或者其他。 [通过这里查看已有的 issues 和提交 bug](https://github.com/pili-engineering/QNRTC-Android/issues)
