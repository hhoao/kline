package com.hhoa.kline.web.core;

import com.hhoa.kline.web.common.pojo.UserInfo;
import com.hhoa.kline.web.common.web.config.WebProperties;
import com.hhoa.kline.web.common.web.core.filter.ApiRequestFilter;
import com.hhoa.kline.web.common.web.core.util.WebFrameworkUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LoginFilter extends ApiRequestFilter {

    public LoginFilter(WebProperties webProperties) {
        super(webProperties);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        try {
            UserInfo userInfo = new UserInfo(0L);
            UserInfoContextHolder.setUserInfo(userInfo);
            WebFrameworkUtils.setLoginUserId(request, userInfo.getUserId());
            try {
                filterChain.doFilter(request, response);
            } catch (IOException | ServletException e) {
                throw new RuntimeException(e);
            }
        } finally {
            UserInfoContextHolder.clear();
        }
    }
}
