package com.hhoa.kline.web.common.web.core.filter;

import static com.hhoa.ai.kline.commons.exception.enums.GlobalErrorCodeConstants.DEMO_DENY;

import cn.hutool.core.util.StrUtil;
import com.hhoa.kline.web.common.pojo.CommonResult;
import com.hhoa.kline.web.common.web.ServletUtils;
import com.hhoa.kline.web.common.web.core.util.WebFrameworkUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 演示 Filter，禁止用户发起写操作，避免影响测试数据
 *
 */
public class DemoFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        return !StrUtil.equalsAnyIgnoreCase(method, "POST", "PUT", "DELETE") // 写操作时，不进行过滤率
                || WebFrameworkUtils.getLoginUserId(request) == null; // 非登录用户时，不进行过滤
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        // 直接返回 DEMO_DENY 的结果。即，请求不继续
        ServletUtils.writeJSON(response, CommonResult.error(DEMO_DENY));
    }
}
