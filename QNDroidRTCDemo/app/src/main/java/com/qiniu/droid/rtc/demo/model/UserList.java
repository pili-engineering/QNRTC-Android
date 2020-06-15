package com.qiniu.droid.rtc.demo.model;

import java.util.List;

/**
 * 用于存放从服务端获取的房间内的用户列表
 */
public class UserList {
    private List<UsersBean> users;

    public List<UsersBean> getUsers() {
        return users;
    }

    public void setUsers(List<UsersBean> users) {
        this.users = users;
    }

    public static class UsersBean {
        private String userId;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
    }
}
