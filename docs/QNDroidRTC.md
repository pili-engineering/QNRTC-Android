# 1 概述

QNDroidRTC 是七牛推出的一款适用于 Android 平台的音视频通话 SDK，提供了包括美颜、滤镜、水印、音视频通话等多种功能，提供灵活的接口，支持高度定制以及二次开发。

## 1.1 下载地址

- [Android Demo 以及 SDK 下载地址](https://github.com/pili-engineering/QNDroidRTC)

# 2 功能列表

| 功能                                 | 版本        |
| ---------------------------------- | --------- |
| 连麦对讲功能                             | v0.1.0(+) |
| 视频合流和音频混音功能                        | v0.1.0(+) |
| 支持内置音视频采集，带美颜、水印、闪光灯、摄像头切换、聚焦等常见功能 | v0.1.0(+) |
| 支持美颜滤镜                             | v0.1.0(+) |
| 支持踢人功能                             | v0.1.0(+) |
| 支持静音功能                             | v0.1.0(+) |
| 支持连麦帧率的配置                          | v0.1.0(+) |
| 支持连麦的视频码率的配置                       | v0.1.0(+) |
| 支持连麦的视频尺寸配置                        | v0.1.0(+) |
| 支持纯音频连麦                            | v0.1.0(+) |
| 支持后台连麦                             | v0.1.0(+) |
| 支持连麦的软硬编配置                         | v0.1.0(+) |
| 支持获取连麦房间统计信息（帧率、码率等）               | v0.1.0(+) |
| 提供丰富的连麦消息回调                        | v0.1.0(+) |

# 3 总体设计

## 3.1 基本规则

为了方便理解和使用，对于 SDK 的接口设计，我们遵循了如下的规则：

- 每一个`连麦`接口类，均以`QN`开头

## 3.2 核心接口类

核心接口类说明如下：

| 接口类名          | 功能            | 备注                           |
| ------------- | ------------- | ---------------------------- |
| QNRTCEnv      | 初始化连麦相关资源     | 初始化相关资源                      |
| QNRTCManager  | 提供连麦相关的各种接口   | 包括但不限于加入（离开）房间、发布（取消发布）视频等接口 |
| QNRTCSetting  | 提供配置相关的各种接口   | 包括但不限于音视频码率、软硬编、编码尺寸等配置      |
| QNSurfaceView | 负责连麦视频画面的渲染   | 渲染连麦画面                       |
| QNVideoFormat | 负责预览以及编码尺寸的配置 | 配置连麦尺寸以及帧率                   |

## 3.3 回调相关的接口类

回调相关接口类说明如下：

| 接口类名                       | 功能                   | 备注                                       |
| -------------------           | -------------         | ---------------------------------------- |
| QNRoomEventListener           | 提供连麦相关的所有回调    | 包括但不限于远端连麦者加入（离开）房间、发布（取消发布）音视频以及连麦状态等回调 |
| QNRoomState                   | 定义了房间的状态信息      | 包括但不限于重连以及断开连接等状态                        |
| QNErrorCode                   | 定义了连麦过程中的错误信息 | 包括但不限于 token 错误、房间不存在等错误信息               |
| QNStatisticsReport            | 提供了连麦过程中的统计信息 | 包括但不限于连麦过程中实时的音视频码率、帧率等回调信息              |
| QNCameraSwitchResultCallback  | 提供了切换摄像头的结果回调 | 包括了切换摄像头的结果回调      |

# 4 阅读对象

本文档为技术文档，需要阅读者：

- 具有基本的 Android 开发能力
- 准备接入七牛云

# 4 开发准备

## 4.1 设备以及系统要求

- 系统要求：Android 4.3 (API 18) 及以上

## 4.2 开发环境

- Android Studio 开发工具，官方[下载地址](http://developer.android.com/intl/zh-cn/sdk/index.html)
- Android 官方开发 SDK，官方[下载地址](https://developer.android.com/intl/zh-cn/sdk/index.html#Other)。

# 5 快速开始

## 5.1 下载和导入连麦 SDK

SDK 主要包含 demo 代码、SDK jar 包，以及 SDK 依赖的动态库文件。
其中，release 目录下是需要拷贝到您的 Android 工程的所有文件，以 armeabi-v7a 架构为例，具体如下：

| 文件名称               | 功能    | 大小    |       备注           |
| --------------------- | -----  | -----  | -------------------  |
| qndroid-rtc-x.y.z.jar | SDK 库 | 493KB  | 必须依赖               |
| libqndroid_rtc.so     | 连麦   | 5.7MB   | 必须依赖              |
| libqndroid_beauty.so  | 美颜   | 481 KB  | 不用自带美颜，可以不依赖 |

- 将 qndroid-rtc-x.y.z.jar 包拷贝到您的工程的 libs 目录下
- 将动态库拷贝到您的工程对应的目录下，例如：armeabi-v7a 目录下的 so 则拷贝到工程的 jniLibs/armeabi-v7a 目录下

具体可以参考 SDK 包含的 demo 工程，集成后的工程示例如下：

![](http://oyojsr1f8.bkt.clouddn.com/RTCDemo.jpg)

## 5.2 修改 build.gradle

双击打开您的工程目录下的 build.gradle，确保已经添加了如下依赖，如下所示：

```java
dependencies {
    compile files('libs/qndroid-rtc-0.1.0.jar')
}
```

## 5.3 添加相关权限

在 app/src/main 目录中的 AndroidManifest.xml 中增加如下 `uses-permission` 声明

```java
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.FLASHLIGHT" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.READ_LOGS" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

## 5.4 开发步骤

### 5.4.1 初始化

首先，在 Application 里，完成 SDK 的初始化操作：

```java
QNRTCEnv.init(getApplicationContext());
```

### 5.4.2 添加连麦需要的渲染控件

需要在 XML 中期望的位置添加 `QNSurfaceView` 用来做本地预览以及远端视频画面的渲染。多人连麦可以添加多个窗口。

示例代码如下：

```java
<com.qiniu.droid.rtc.QNSurfaceView
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:layout_centerInParent="true"
    android:visibility="gone" />
```

对于**本地预览的窗口**，可以通过 `QNRTCManager.initialize(Context context, QNSurfaceView localWindow)` 或者 `QNRTCManager.initialize(Context context, QNRTCSetting setting, QNSurfaceView localWindow)` 方法配置

示例代码如下：

```java
mLocalWindow = (QNSurfaceView) findViewById(R.id.local_surface_view);
mRTCManager = new QNRTCManager();
mRTCManager.initialize(this, setting, mLocalWindow);
```

对于**远端的渲染窗口**，可以通过 `QNRTCManager.addRemoteWindow(QNSurfaceView remoteWindow)` 方法添加

示例代码如下：

```java
mRemoteWindow = (QNSurfaceView) findViewById(R.id.remote_window);
mRTCManager.addRemoteWindow(mRemoteWindow);
```

### 5.4.3 配置连麦采集编码相关的参数

本操作推荐在 `Activity.onCreate()` 函数中完成

```java
QNRTCSetting setting = new QNRTCSetting();
setting.setAudioBitrate(100 * 1000)
        .setVideoBitrate(videoBitrate)
        .setBitrateRange(videoBitrate, QNRTCSetting.DEFAULT_MAX_BITRATE)
        .setCameraID(QNRTCSetting.CAMERA_FACING_ID.FRONT)
        .setHWCodecEnabled(isHwCodec)
        .setVideoPreviewFormat(new QNVideoFormat(videoWidth, videoHeight, QNRTCSetting.DEFAULT_FPS))
        .setVideoEncodeFormat(new QNVideoFormat(videoWidth, videoHeight, QNRTCSetting.DEFAULT_FPS));
```

### 5.4.4 调用 `initialize` 方法完成配置

本操作推荐在 `Activity.onCreate()` 函数中完成

```java
mLocalWindow = (QNSurfaceView) findViewById(R.id.local_surface_view);
mRTCManager = new QNRTCManager();
mRTCManager.initialize(Context context, QNRTCSetting setting, mLocalWindow);
```

在调用 initialize 的过程中会默认开启音视频的采集。

### 5.4.5 设置回调

`QNRoomEventListener` 包含了连麦过程中的所有重要回调接口，因此在初始化过程中需要注册该监听器:

```java
mRTCManager.setRoomEventListener(QNRoomEventListener listener);
```

### 5.4.6 开始连麦

上面一系列的配置结束后，可以调用如下接口加入房间并开始连麦，在成功加入房间之后，就可以进行媒体流的发布、订阅等操作了。加入房间的过程需要 App 服务端的配合，详情请见服务端文档。（当前没有服务端文档，后续补充）

加入房间的函数原型如下：

```java
/**
 * 加入房间
 *
 * 加入房间成功后会触发 onJoinedRoom() 的回调，可以在回调里面去发布媒体流到连麦服务器
 *
 * @param roomToken 连麦的Token，由 App 服务器动态生成
 */
public void joinRoom(String roomToken)
```

示例代码如下：

```java
mRTCManager.joinRoom(mRoomToken);
```

在连麦之后，用户可以根据业务场景的需求在适当的时间调用离开房间的接口退出连麦，详情请见[房间管理](#room_manage)

### 5.4.7 发布媒体流

在成功加入房间，即收到 `onJoinedRoom` 的回调之后，即可调用如下函数进行媒体流的发布操作：

```java
/**
 * 发布音视频媒体流
 *
 * 调用该方法发布媒体流，其中，可以通过调用 QNRTCSetting.setVideoEnabled(boolean enable) 和 QNRTCSetting.setAudioEnabled(boolean enable) 方法来控制是否发布音频或视频
 */
public void publish()
```

示例代码如下：

```java
@Override
public void onJoinedRoom() {
    mRTCManager.publish();
}
```

媒体流成功发布之后，远端用户便会收到 `onRemotePublished` 的回调并可以选择调用订阅接口去订阅该媒体流。

在媒体流发布之后，用户可以根据业务场景的需求在适当的时间调用取消发布的接口取消发布相应的媒体流，详情请见[媒体流的发布管理](#publish_manage)

### 5.4.8 订阅远端媒体流

在成功加入房间之后，如果收到了远端用户发布媒体流的回调 `onRemotePublished`，即可调用如下函数进行媒体流的订阅操作：

```java
/**
 * 订阅远端窗口
 *
 * @param userId 远端连麦者的 userId，userId 可通过 onRemotePublished 回调获取
 */
public void subscribe(String userId)
```

示例代码如下：

```java
@Override
public void onRemotePublished(String userId, boolean hasAudio, boolean hasVideo) {
    mRTCManager.subscribe(userId);
}
```

订阅成功之后会触发 `onSubscribed` 回调，用户可以在该回调中做一些操作比如开启统计信息等。

在成功订阅之后，用户可以根据业务场景的需求在适当的时间调用取消订阅的接口取消订阅相应的媒体流，详情请见[远端窗口管理](#subscribe_manage)

### 5.4.9 退出

在连麦结束，整个 `Activity` 销毁的时候(建议在 onDestroy 中)，需要调用如下函数释放资源：

```java
/**
 * Release resources
 */
public void destroy()
```

示例代码如下：

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    if (mRTCManager != null) {
        mRTCManager.destroy();
        mRTCManager = null;
    }
}
```

# 6 SDK 接口的设计

## 6.1 QNRTCManager

`QNRTCManager` 是负责管理整个连麦生命周期的核心类，提供了整个连麦过程中所需要的所有控制接口。其核心的接口描述如下：

### 6.1.1 初始化

```java
/**
 * 执行初始化操作，会默认开启音视频的采集
 *
 * 会使用默认的连麦配置参数
 *
 * @param context Android 的上下文句柄
 * @param localWindow 本地 Camera 预览窗口
 */
public void initialize(Context context, QNSurfaceView localWindow)

/**
 * 执行初始化操作，会默认开启音视频的采集
 *
 * @param context Android 的上下文句柄
 * @param setting 连麦的配置参数
 * @param localWindow 本地 Camera 预览窗口
 */
public void initialize(Context context, QNRTCSetting setting, QNSurfaceView localWindow)
```

### 6.1.2 设置连麦回调事件的监听器

```java
/**
 * 设置连麦回调事件的监听器
 *
 * @param QNRoomEventListener 设置的监听器，该监听器包含了连麦过程中的所有回调函数
 */
public void setRoomEventListener(QNRoomEventListener eventListener)
```

### 6.1.3 添加连麦窗口

```java
/**
 * 添加远端窗口
 *
 * @param remoteWindow 远端窗口控件
 */
public void addRemoteWindow(QNSurfaceView remoteWindow)
```

<span id = "room_manage" />

### 6.1.4 房间管理

```java
/**
 * 加入房间
 *
 * 加入房间成功后会触发 onJoinedRoom() 的回调，可以在回调里面去发布媒体流到连麦服务器
 *
 * @param roomToken 连麦的Token，由App服务器动态生成
 */
public void joinRoom(String roomToken)

/**
 * 离开房间
 */
 public void leaveRoom()
```

<span id = "publish_manage" />

### 6.1.5 媒体流的发布管理

```java
/**
 * 发布媒体流
 *
 * 调用该方法发布媒体流，其中，可以通过调用 QNRTCSetting.setVideoEnabled(boolean enable) 和 QNRTCSetting.setAudioEnabled(boolean enable) 方法来控制是否发布音频或视频
 */
public void publish()

/**
 * 取消发布媒体流
 */
public void unpublish()
```

<span id = "subscribe_manage" />

### 6.1.6 远端窗口管理

```java
/**
 * 订阅远端窗口
 *
 * @param userId 远端连麦者的 userId
 */
public void subscribe(String userId)

/**
 * 取消订阅远端窗口
 *
 * @param userId 远端连麦者的 userId
 */
public void unsubscribe(String userId)
```

### 6.1.7 Mute 功能

Mute 功能提供了更灵活的音视频处理机制，具体接口如下：

```java
/**
 * 开启/关闭本地视频，关闭后其他连麦者看不到本地的画面
 *
 * @param isMute mute or not
 */
public void muteLocalVideo(boolean isMute)

/**
 * 开启/关闭本地音频
 *
 * @param isMute mute or not
 */
public void muteLocalAudio(boolean isMute)

/**
 * 开启/关闭远端音频
 *
 * @param isMute mute or not
 */
public void muteRemoteAudio(boolean isMute)
```

### 6.1.8 开启/关闭预览

```java
/**
 * 开启/关闭本地的预览
 *
 * @param isEnabled enable or not
 */
public void setPreviewEnabled(boolean isEnabled)
```

### 6.1.9 镜像功能

```java
/**
 * 设置是否开启镜像
 *
 * @param isMirrorEnabled enable mirror or not
 */
public void setMirror(boolean isMirrorEnabled)
```

### 6.1.10 切换摄像头

```java
/**
 * 切换摄像头
 *
 * @param callback 切换摄像头的结果回调，如不需要回调可以传入 null
 */
public void switchCamera(QNCameraSwitchResultCallback callback)
```

### 6.1.11 闪光灯功能

```java
/**
 * 开启闪光灯
 */
public boolean turnLightOn()

/**
 * 关闭闪光灯
 */
public boolean turnLightOff()
```

### 6.1.12 对焦功能

```java
/**
 * 手动对焦
 *
 * @param x 焦点的 x 坐标
 * @param y 焦点的 y 坐标
 * @param viewWidth 焦点的宽度
 * @param viewHeight 焦点的高度
 */
public void manuFocus(float x, float y, int viewWidth, int viewHeight)
```

### 6.1.13 踢人功能

```java
/**
 * 踢人功能
 *
 * 踢人方需要有相应的踢人权限才可执行踢人的操作
 *
 * @param userId 被踢者的 userId
 */
public void kickOutUser(String userId)
```

### 6.1.14 获取参与连麦的用户的 ID 列表

```java
/**
 * 获取参与连麦的用户列表
 */
public ArrayList<String> getUserList()

/**
 * 获取参与连麦的用户中发布音视频的用户的列表
 */
public ArrayList<String> getPublishingUserList()
```

### 6.1.15 开启连麦的统计信息

```java
/**
 * 开启/关闭连麦的统计信息功能
 *
 * @param userId 待统计用户的 userId
 * @param enable 开启或者关闭
 * @param periodMs 统计的时间间隔（单位：ms）
 */
public void setStatisticsInfoEnabled(String userId, boolean enable, int periodMs)
```

### 6.1.16 获取房间信息

```java
/**
 * 获取房间信息
 *
 * QNRoomState 包括 IDLE、CONNECTING、CONNECTED 以及 RECONNECTING 等状态
 */
public QNRoomState getRoomState()
```

### 6.1.17 释放资源

```java
/**
 * 销毁整个对象，释放相关资源
 * 建议在 Activity.onDestroy() 中调用
 */
 public void destroy();
```

## 6.2 连麦参数配置

`QNRTCSetting` 是负责配置整个连麦过程中的帧率、码率以及分辨率等参数的核心类，其核心的接口描述如下：

### 6.2.1 设置音频码率

```java
/**
 * 设置连麦的音频码率
 *
 * @param audioBitrate 目标音频码率，默认值是 100 * 1000，单位：bps
 * @return 本对象的指针
 */
public QNRTCSetting setAudioBitrate(int audioBitrate)

/**
 * 获取当前设置的音频码率
 *
 * @return 当前设置的音频码率
 */
public int getAudioBitrate()
```

### 6.2.2 设置视频码率

```java
/**
 * 设置连麦的视频码率
 *
 * @param videoBitrate 目标视频码率，默认值是 800 * 1000，单位：bps
 * @return 本对象的指针
 */
public QNRTCSetting setVideoBitrate(int videoBitrate)

/**
 * 获取当前设置的视频码率
 *
 * @return 当前设置的视频码率
 */
public int getVideoBitrate()
```

### 6.2.3 设置预览尺寸

```java
/**
 * 设置连麦的预览分辨率、帧率
 *
 * @param videoFormat 目标视频配置，QNVideoFormat 需指定采集画面的宽、高以及帧率
 * @return 本对象的指针
 */
public QNRTCSetting setVideoPreviewFormat(QNVideoFormat videoFormat)

/**
 * 获取当前设置的预览配置
 *
 * @return 当前设置的预览配置
 */
public QNVideoFormat getVideoPreviewFormat()
```

### 6.2.3 设置编码尺寸

```java
/**
 * 设置连麦的编码分辨率、帧率
 *
 * @param videoFormat 目标视频配置，QNVideoFormat 需指定编码画面的宽、高以及帧率
 * @return 本对象的指针
 */
public QNRTCSetting setVideoEncodeFormat(QNVideoFormat videoFormat)

/**
 * 获取当前设置的编码配置
 *
 * @return 当前设置的编码配置
 */
public QNVideoFormat getVideoEncodeFormat()
```

### 6.2.4 指定初始的摄像头方向

```java
/**
 * 设置初始的摄像头方向
 *
 * @param cameraID 目标 camera id，CAMERA_FACING_ID 包括 FRONT、BACK 以及 ANY
 * @return 本对象的指针
 */
public QNRTCSetting setCameraID(CAMERA_FACING_ID cameraID)

/**
 * 获取当前设置的 Camera id
 *
 * @return 当前设置的 Camera id
 */
public CAMERA_FACING_ID getCameraID()
```

### 6.2.5 设置连麦编码器的软编硬编

```java
/**
 * 设置是否开启硬编
 *
 * @param enabled enable or not
 * @return 本对象的指针
 */
public QNRTCSetting setHWCodecEnabled(boolean enabled)

/**
 * 获取当前是否开启硬编
 *
 * @return 是否开启了硬编
 */
public boolean isHWCodecEnabled()
```

### 6.2.6 设置是否发布视频

```java
/**
 * 设置是否需要发布视频
 *
 * @param enabled enable or not
 * @return 本对象的指针
 */
public QNRTCSetting setVideoEnabled(final boolean enable)

/**
 * 获取当前是否发布了视频
 *
 * @return enable or not
 */
public boolean isVideoEnabled()
```

### 6.2.7 设置是否发布音频

```java
/**
 * 设置是否需要发布音频
 *
 * @param enabled enable or not
 * @return 本对象的指针
 */
public QNRTCSetting setAudioEnabled(final boolean enable)

/**
 * 获取当前是否发布了音频
 *
 * @return enable or not
 */
public boolean isAudioEnabled()
```

### 6.2.8 设置连麦编码器输出的码率

```java
/**
 * 设置连麦编码器输出的码率
 * 连麦的码率会根据当前的网络状况动态的调整
 *
 * @param minBitrate 码率浮动的下限，默认值：100 * 1000，单位：bps
 * @param maxBitrate 码率浮动的上限，默认值：10000 * 1000，单位：bps
 * @return 本对象的指针
 */
public QNRTCSetting setBitrateRange(int minBitrate, int maxBitrate)

/**
 * 获取码率浮动的下限
 *
 * @return 码率浮动的下限
 */
public int getMinBitrate()

/**
 * 获取码率浮动的上限
 *
 * @return 码率浮动的上限
 */
public int getMaxBitrate()
```

## 6.3 配置相关接口类

### 6.3.1 QNVideoFormat

`QNVideoFormat` 用来配置预览和编码的尺寸以及帧率，通过 `QNRTCSetting.setVideoPreviewFormat(QNVideoFormat videoFormat)` 和 `QNRTCSetting.setVideoEncodeFormat(QNVideoFormat videoFormat)` 方法配置。

其构造方法如下：

```java
/**
 * 构造函数
 *
 * @param width 目标的宽度
 * @param height 目标的高度
 * @param frameRate 目标的帧率
 */
public QNVideoFormat(int width, int height, int frameRate)
```

## 6.4 回调相关接口类

### 6.4.1 QNRoomEventListener

`QNRoomEventListener` 包含了连麦过程的所有回调接口，包括但不限于远端用户加入（离开）房间，发布（取消发布）音视频等相关回调。

其核心的接口描述如下：

```java
/**
 * 加入房间成功的回调
 */
void onJoinedRoom()

/**
 * 本地音视频成功发布的回调
 */
void onLocalPublished()

/**
 * 成功订阅远端媒体流的回调
 *
 * @param userId 远端用户的 userId
 */
void onSubscribed(String userId)

/**
 * 远端用户发布媒体流时触发的回调
 *
 * @param userId 远端用户的 userId
 * @param isAudioEnabled 远端用户是否发布了音频
 * @param isVideoEnabled 远端用户是否发布了视频
 */
void onRemotePublished(String userId, boolean isAudioEnabled, boolean isVideoEnabled)

/**
 * 远端用户取消发布媒体流时触发的回调
 *
 * @param userId 远端用户的 userId
 */
void onRemoteUnpublished(String userId)

/**
 * 首次收到远端媒体流时触发的回调
 *
 * @param userId 远端用户的 userId
 * @param isAudioEnabled 远端用户是否发布了音频
 * @param isVideoEnabled 远端用户是否发布了视频
 * @param isAudioMuted 远端用户是否关闭了音频
 * @param isVideoMuted 远端用户是否关闭了视频
 *
 * @return 待渲染远端画面的窗口控件
 */
QNSurfaceView onRemoteStreamAdded(String userId, boolean isAudioEnabled, boolean isVideoEnabled, boolean isAudioMuted, boolean isVideoMuted)

/**
 * 远端媒体流移除时触发的回调
 *
 * @param userId 远端用户的 userId
 */
void onRemoteStreamRemoved(String userId)

/**
 * 远端用户加入房间时触发的回调
 *
 * @param userId 远端用户的 userId
 */
void onRemoteUserJoined(String userId)

/**
 * 远端用户离开房间时触发的回调
 *
 * @param userId 远端用户的 userId
 */
void onRemoteUserLeaved(String userId)

/**
 * 远端用户开启/关闭音视频时会触发的回调
 *
 * @param userId 远端用户的 userId
 * @param isAudioMuted 远端用户是否关闭了音频
 * @param isVideoMuted 远端用户是否关闭了视频
 */
void onRemoteMute(String userId, boolean isAudioMuted, boolean isVideoMuted)

/**
 * 连麦房间状态回调
 *
 * @param state 当前的房间状态。（QNRoomState 包括 IDLE、CONNECTING、CONNECTED 以及 RECONNECTING 等状态）
 */
void onStateChanged(QNRoomState state)

/**
 * 连麦错误信息回调
 *
 * @param errorCode 错误码
 * @param description 错误信息
 */
void onError(int errorCode, String description)

/**
 * 连麦统计信息回调
 *
 * @param report 统计信息，包括但不限于音视频的码率、帧率以及丢包率等信息
 */
void onStatisticsUpdated(QNStatisticsReport report)

/**
 * 踢人成功的回调
 *
 * @param userId 被踢者的 userId
 */
void onUserKickedOut(String userId)
```

### 6.4.2 QNCameraSwitchResultCallback

`QNCameraSwitchResultCallback` 是切换摄像头时需要作为参数传入的接口类。

其核心的接口描述如下：

```java
/**
 * 切换摄像头成功的回调
 *
 * @param isFrontCamera 切换成功后，当前是否是前置摄像头，是前置即为 true，反之则为 false
 */
void onCameraSwitchDone(boolean isFrontCamera);

/**
 * 切换摄像头失败的回调
 *
 * @param errorMessage 切换失败的错误信息
 */
void onCameraSwitchError(String errorMessage);
```

### 6.4.3 QNRoomState

`QNRoomState` 是当前连麦房间状态的枚举类，主要包括：

```java
/**
 * 初始化状态
 */
IDLE,

/**
 * 正在连接
 */
CONNECTING,

/**
 * 已连接
 */
CONNECTED,

/**
 * 正在重连
 */
RECONNECTING
```

### 6.4.4 QNStatisticsReport

`QNStatisticsReport` 包含了如下统计信息：

```java
/**
 * 统计信息对应的 userId
 */
public String userId;

/**
 * 视频码率, 单位：bps
 */
public int videoBitrate;

/**
 * 每个统计时间间隔内的视频丢包率, 以百分比的形式回调
 */
public int videoPacketLostRate;

/**
 * 视频的宽
 */
public int width;

/**
 * 视频的高
 */
public int height;

/**
 * 帧率
 */
public int frameRate;

/**
 * 音频码率, 单位：bps
 */
public int audioBitrate;

/**
 * 每个统计时间间隔内的音频丢包率, 以百分比的形式回调
 */
public int audioPacketLostRate;
```

# 7 常见错误码

| 错误码   | 描述                                    |
| ----- | ------------------------------------- |
| 0     | SUCCESS                               |
| 20001 | ERROR_IO_EXCEPTION                    |
| 20051 | ERROR_WRONG_STATUS                    |
| 20100 | ERROR_SIGNAL_IO_EXCEPTION             |
| 20101 | ERROR_SIGNAL_UNKNOWN_MESSAGE          |
| 20102 | ERROR_SIGNAL_TIMEOUT                  |
| 20103 | ERROR_TOKEN_INVALID                   |
| 20104 | ERROR_JSON_INVALID                    |
| 20105 | ERROR_HTTP_SOCKET_TIMEOUT             |
| 20106 | ERROR_HTTP_IO_EXCEPTION               |
| 20107 | ERROR_HTTP_RESPONSE_EXCEPTION         |
| 20108 | ERROR_ROOMTOKEN_NULL                  |
| 20109 | ERROR_ACCESSTOKEN_NULL                |
| 20200 | ERROR_ICE_FAILED                      |
| 20300 | ERROR_PEERCONNECTION                  |
| 10001 | ERROR_TOKEN_ERROR                     |
| 10002 | ERROR_TOKEN_EXPIRED                   |
| 10003 | ERROR_ROOM_INSTANCE_CLOSED            |
| 10004 | ERROR_RECONNECT_TOKEN_ERROR           |
| 10005 | ERROR_ROOM_CLOSED                     |
| 10006 | ERROR_KICKED_OUT_OF_ROOM              |
| 10011 | ERROR_ROOM_FULL                       |
| 10012 | ERROR_ROOM_NOT_EXIST                  |
| 10021 | ERROR_PLAYER_NOT_EXIST                |
| 10022 | ERROR_PLAYER_ALREADY_EXIST            |
| 10031 | ERROR_PUBLISH_STREAM_NOT_EXIST        |
| 10032 | ERROR_PUBLISH_STREAM_INFO_NOT_MATCH   |
| 10041 | ERROR_SUBSCRIBE_STREAM_NOT_EXIST      |
| 10042 | ERROR_SUBSCRIBE_STREAM_INFO_NOT_MATCH |
| 10043 | ERROR_SUBSCRIBE_STREAM_ALREADY_EXIST  |
| 10051 | ERROR_NO_PERMISSION                   |
| 10052 | ERROR_SERVER_UNAVAILABLE              |

# 7 历史记录

- **0.1.0**
  - 发布 qndroid-rtc-0.1.0.jar
  - 发布 libqndroid_rtc.so
  - 发布 libqndroid_beauty.so
  - 连麦对讲功能
  - 视频合流和音频混音功能
  - 支持内置音视频采集，带美颜、水印、闪光灯、摄像头切换、聚焦等常见功能
  - 支持美颜滤镜
  - 支持踢人功能
  - 支持静音功能
  - 支持连麦帧率的配置
  - 支持连麦的视频码率的配置
  - 支持连麦的视频尺寸配置
  - 支持纯音频连麦
  - 支持后台连麦
  - 支持连麦的软硬编配置
  - 支持获取连麦房间统计信息（帧率、码率等）
  - 提供丰富的连麦消息回调

# 8 FAQ

## 8.1 我可以体验下 demo 吗 ？

[demo 体验](https://fir.im/b5ce)