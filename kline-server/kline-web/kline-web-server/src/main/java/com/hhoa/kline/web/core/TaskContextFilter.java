package com.hhoa.kline.web.core;

import com.hhoa.kline.core.core.api.TaskContextHolder;
import com.hhoa.kline.web.common.web.config.WebProperties;
import com.hhoa.kline.web.common.web.core.filter.ApiRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TaskContextFilter extends ApiRequestFilter {

    public TaskContextFilter(WebProperties webProperties) {
        super(webProperties);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            TaskContextHolder.set(new RequestContext());
            filterChain.doFilter(request, response);
        } finally {
            TaskContextHolder.clear();
        }
    }
}
