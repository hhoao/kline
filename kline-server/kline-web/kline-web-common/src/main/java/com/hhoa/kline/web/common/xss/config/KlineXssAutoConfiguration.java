package com.hhoa.kline.web.common.xss.config;

import static com.hhoa.kline.web.common.web.config.KlineWebAutoConfiguration.createFilterBean;

import com.hhoa.kline.web.common.enums.WebFilterOrderEnum;
import com.hhoa.kline.web.common.xss.core.clean.JsoupXssCleaner;
import com.hhoa.kline.web.common.xss.core.clean.XssCleaner;
import com.hhoa.kline.web.common.xss.core.filter.XssFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@EnableConfigurationProperties(XssProperties.class)
@ConditionalOnProperty(
        prefix = "kline.xss",
        name = "enable",
        havingValue = "true",
        matchIfMissing = true) // 设置为 false 时，禁用
public class KlineXssAutoConfiguration implements WebMvcConfigurer {

    /**
     * Xss 清理者
     *
     * @return XssCleaner
     */
    @Bean
    @ConditionalOnMissingBean(XssCleaner.class)
    public XssCleaner xssCleaner() {
        return new JsoupXssCleaner();
    }

    /** 创建 XssFilter Bean，解决 Xss 安全问题 */
    @Bean
    @ConditionalOnBean(XssCleaner.class)
    public FilterRegistrationBean<XssFilter> xssFilter(
            XssProperties properties, PathMatcher pathMatcher, XssCleaner xssCleaner) {
        return createFilterBean(
                new XssFilter(properties, pathMatcher, xssCleaner), WebFilterOrderEnum.XSS_FILTER);
    }
}
