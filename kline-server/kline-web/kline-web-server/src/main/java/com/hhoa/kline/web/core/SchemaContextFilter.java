package com.hhoa.kline.web.core;

import com.hhoa.kline.plugins.jdbc.core.SchemaContextHolder;
import com.hhoa.kline.web.common.web.config.WebProperties;
import com.hhoa.kline.web.common.web.core.filter.ApiRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SchemaContextFilter extends ApiRequestFilter {

    public SchemaContextFilter(WebProperties webProperties) {
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
            SchemaContextHolder.setSchema("public");
            try {
                filterChain.doFilter(request, response);
            } catch (IOException | ServletException e) {
                throw new RuntimeException(e);
            }
        } finally {
            SchemaContextHolder.clear();
        }
    }
}
