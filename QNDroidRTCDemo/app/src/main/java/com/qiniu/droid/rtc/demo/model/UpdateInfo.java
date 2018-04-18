package com.qiniu.droid.rtc.demo.model;

public class UpdateInfo {
    private String mAppID;
    private int mVersion;
    private String mDescription;
    private String mDownloadURL;
    private String mCreateTime;

    public void setAppID(String appID) {
        this.mAppID = appID;
    }

    public void setVersion(int version) {
        this.mVersion = version;
    }

    public void setDescription(String description) {
        this.mDescription = description;
    }

    public void setDownloadURL(String downloadURL) {
        this.mDownloadURL = downloadURL;
    }

    public void setCreateTime(String createTime) {
        this.mCreateTime = createTime;
    }

    public String getAppID() {
        return mAppID;
    }

    public int getVersion() {
        return mVersion;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getDownloadURL() {
        return mDownloadURL;
    }

    public String getCreateTime() {
        return mCreateTime;
    }
}
