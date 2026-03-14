package com.hhoa.kline.core.core.shared.services.config;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PostHogClientValidConfig extends PostHogClientConfig {
    private PostHogClientValidConfig() {
        super();
    }

    public static class Builder {
        private final PostHogClientValidConfig config = new PostHogClientValidConfig();

        public Builder apiKey(String apiKey) {
            config.setApiKey(apiKey);
            return this;
        }

        public Builder errorTrackingApiKey(String errorTrackingApiKey) {
            config.setErrorTrackingApiKey(errorTrackingApiKey);
            return this;
        }

        public Builder host(String host) {
            config.setHost(host);
            return this;
        }

        public Builder uiHost(String uiHost) {
            config.setUiHost(uiHost);
            return this;
        }

        public PostHogClientValidConfig build() {
            return config;
        }
    }

    public static Builder validBuilder() {
        return new Builder();
    }
}
