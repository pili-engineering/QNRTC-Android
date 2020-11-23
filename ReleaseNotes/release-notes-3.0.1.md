# QNDroidRTC Release Notes for 3.0.1

## 简介

QNDroidRTC 是七牛推出的一款适用于 Android 平台的音视频通话 SDK，提供了包括美颜、滤镜、音视频通话等多种功能，提供灵活的接口，支持高度定制以及二次开发。

## 版本

- 发布 qndroid-rtc-3.0.1.jar

## 功能

- 支持摄像头切换图片连麦
- 支持单路转推添加 SEI 功能
- 支持添加视频水印功能
- 支持合流时单独设置 Track 填充模式
- 支持回调本地 Track 音量大小
- 支持调节麦克风采集后音量
- 支持对合流和单路任务设置延时关闭

## 缺陷

- 修复特定机型加入房间失败
- 修复混音时低概率 ANR
- 修复某些机型初始化时抛出异常

## 注意事项
- 如果您使用的版本是 2.5.0+，那么为了给您提供更好的使用体验，请务必依赖如下 dns 解析库：

```java
dependencies {
    implementation 'com.qiniu:happy-dns:0.2.17'
}
```

## 问题反馈

当你遇到任何问题时，可以通过在 GitHub 的 repo 提交 `issues` 来反馈问题，请尽可能的描述清楚遇到的问题，如果有错误信息也一同附带，并且在 ```Labels``` 中指明类型为 bug 或者其他。 [通过这里查看已有的 issues 和提交 bug](https://github.com/pili-engineering/QNRTC-Android/issues)
