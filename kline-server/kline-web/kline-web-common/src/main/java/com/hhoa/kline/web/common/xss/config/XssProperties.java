package com.hhoa.kline.web.common.xss.config;

import java.util.Collections;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Xss 配置属性
 *
 */
@ConfigurationProperties(prefix = "kline.xss")
@Validated
@Data
public class XssProperties {

    /** 是否开启，默认为 true */
    private boolean enable = true;

    /** 需要排除的 URL，默认为空 */
    private List<String> excludeUrls = Collections.emptyList();
}
