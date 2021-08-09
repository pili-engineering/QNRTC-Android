# QNDroidRTC Release Notes for 3.1.0

## 简介

QNDroidRTC 是七牛推出的一款适用于 Android 平台的音视频通话 SDK，提供了包括美颜、滤镜、音视频通话等多种功能，提供灵活的接口，支持高度定制以及二次开发。

## 版本

- 发布 qndroid-rtc-3.1.0.jar

## 功能

- 支持日志上报
- 支持视频 SEI 设置 UUID
- 支持回调订阅音频播放前数据
- 外部导入功能支持纹理方式

## 缺陷

- 修复 API 29 及以上录屏失败
- 优化某些机型上编码，解码帧率过低问题
- 优化弱网下码率控制策略

## 注意事项
- 如果您使用的版本是 2.5.0+，那么为了给您提供更好的使用体验，请务必依赖如下 dns 解析库：

```java
dependencies {
    implementation 'com.qiniu:happy-dns:0.2.17'
}
```
- 从 3.0.2 版本开始，SDK 不再提供 v1 接口
- 从 3.1.0 版本开始，SDK 不再提供 armeabi 架构库

## 问题反馈

当你遇到任何问题时，可以通过在 GitHub 的 repo 提交 `issues` 来反馈问题，请尽可能的描述清楚遇到的问题，如果有错误信息也一同附带，并且在 ```Labels``` 中指明类型为 bug 或者其他。 [通过这里查看已有的 issues 和提交 bug](https://github.com/pili-engineering/QNRTC-Android/issues)
