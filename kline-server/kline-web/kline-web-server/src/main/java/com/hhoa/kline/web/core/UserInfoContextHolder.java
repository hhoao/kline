package com.hhoa.kline.web.core;

import com.hhoa.kline.web.common.pojo.UserInfo;

/**
 * UserInfoContextHolder
 *
 * @author xianxing
 * @since 2026/2/14
 */
public class UserInfoContextHolder {
    private static final ThreadLocal<UserInfo> CONTEXT_HOLDER = new InheritableThreadLocal<>();

    /**
     * 获取当前线程的用户信息
     *
     * @return 用户信息
     */
    public static UserInfo getUserInfo() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 设置当前线程的用户信息
     *
     * @param userInfo 用户信息
     */
    public static void setUserInfo(UserInfo userInfo) {
        CONTEXT_HOLDER.set(userInfo);
    }

    /** 清除当前线程的用户信息 */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

    public Long getLoginUserId() {
        return getUserInfo().getUserId();
    }
}
