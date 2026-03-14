package com.hhoa.kline.web.utils;

import com.hhoa.kline.web.common.pojo.UserInfo;
import com.hhoa.kline.web.core.UserInfoContextHolder;

public final class LoginUserUtil {

    public static Long getLoginIdDefaultNull() {
        UserInfo userInfo = UserInfoContextHolder.getUserInfo();
        return userInfo == null ? null : userInfo.getUserId();
    }

    public static String getLoginUsernameDefaultNull() {
        return null;
    }
}
