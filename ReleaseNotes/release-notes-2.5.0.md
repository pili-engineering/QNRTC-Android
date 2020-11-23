# QNDroidRTC Release Notes for 2.5.0

## 简介

QNDroidRTC 是七牛推出的一款适用于 Android 平台的音视频通话 SDK，提供了包括美颜、滤镜、音视频通话等多种功能，提供灵活的接口，支持高度定制以及二次开发。

## 版本

- 发布 qndroid-rtc-2.5.0.jar

## 功能

- 新增支持单路转推
- 新增支持单路视频时发送及订阅大小流
- 支持设置自定义 DNS 服务
- 新增通话模式配置接口
- 音频采集数据回调，新增采样率/声道参数

## 缺陷

- 修复摄像头切换后，再次打开还是回到前置摄像头
- 修复应用在后台，摄像头被其他应用抢占后无法打开的问题
- 修复特定机型软编帧率较低问题
- 修复特定机型硬编码率无法升高问题

## 注意事项
- 如果您使用的版本是 2.5.0+，那么为了给您提供更好的使用体验，请务必依赖如下 dns 解析库：

```java
dependencies {
    implementation 'com.qiniu:happy-dns:0.2.17'
}
```

## 问题反馈

当你遇到任何问题时，可以通过在 GitHub 的 repo 提交 `issues` 来反馈问题，请尽可能的描述清楚遇到的问题，如果有错误信息也一同附带，并且在 ```Labels``` 中指明类型为 bug 或者其他。 [通过这里查看已有的 issues 和提交 bug](https://github.com/pili-engineering/QNRTC-Android/issues)
