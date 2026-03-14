package com.hhoa.kline.web.config;

import com.hhoa.kline.web.common.web.config.WebProperties;
import com.hhoa.kline.web.core.LoginFilter;
import com.hhoa.kline.web.core.SchemaContextFilter;
import com.hhoa.kline.web.core.TaskContextFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnWebApplication
@EnableConfigurationProperties(WebProperties.class)
public class RequestContextFilterConfiguration {
    @Bean
    public FilterRegistrationBean<LoginFilter> loginFilter(WebProperties webProperties) {
        FilterRegistrationBean<LoginFilter> bean =
                new FilterRegistrationBean<>(new LoginFilter(webProperties));
        bean.setOrder(-102);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<SchemaContextFilter> schemaContextFilter(
            WebProperties webProperties) {
        FilterRegistrationBean<SchemaContextFilter> bean =
                new FilterRegistrationBean<>(new SchemaContextFilter(webProperties));
        bean.setOrder(-101);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<TaskContextFilter> aiRequestContextFilter(
            WebProperties webProperties) {
        FilterRegistrationBean<TaskContextFilter> bean =
                new FilterRegistrationBean<>(new TaskContextFilter(webProperties));
        bean.setOrder(-100);
        return bean;
    }
}
