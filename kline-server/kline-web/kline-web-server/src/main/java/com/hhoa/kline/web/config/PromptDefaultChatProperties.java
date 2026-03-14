package com.hhoa.kline.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kline.prompt.default-chat")
@Data
public class PromptDefaultChatProperties {

    private String apiKey;
    private String baseUrl;
    private String modelId = "3";
    private String providerId = "deepseek";
}
