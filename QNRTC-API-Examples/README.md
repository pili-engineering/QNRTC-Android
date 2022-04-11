# QNRTC-API-Examples

七牛实时音视频 API Examples 用于演示如何通过 QNRTC SDK 来实现不同的场景需求

## 项目运行

该项目仅用于演示不同场景下的接口调用情况，不包含 token 生成等业务服务相关的逻辑实现。因此，为保证项目正常运行，需要您在如下位置自行填入音视频通话的房间 [token](https://developer.qiniu.com/rtc/9909/the-rtc-basic-concept#4)，若您还没有生成 token 的相关服务，可参考[如何通过控制台获取 token](https://developer.qiniu.com/rtc/9858/applist#4) 来获取临时 token。

```java
public class Config {
    public static final String ROOM_TOKEN = "自定义房间 token";
}
```

若您有 CDN 转推的相关需求，同样需要在如下位置自行填入待转推的推流地址才可正常运行，推流地址的获取方式可参考[直播云快速入门文档](https://developer.qiniu.com/pili/1221/the-console-quick-start)：

```java
public class Config {
    public static final String PUBLISH_URL = "自定义推流地址";
}
```