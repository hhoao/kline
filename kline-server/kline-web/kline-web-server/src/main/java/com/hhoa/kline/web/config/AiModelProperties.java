package com.hhoa.kline.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 配置类
 *
 * @author hhoa
 * @since 1.0
 */
@ConfigurationProperties(prefix = "kline.ai")
@Data
public class AiModelProperties {
    /** Midjourney 绘图 */
    private MidjourneyProperties midjourney;

    @Data
    public static class MidjourneyProperties {

        private String enable;
        private String baseUrl;

        private String apiKey;
        private String notifyUrl;
    }
}
