package com.hhoa.kline.core.core.shared;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ClineApiReqInfo {
    private String request;
    private Integer tokensIn;
    private Integer tokensOut;
    private Integer cacheWrites;
    private Integer cacheReads;
    private Double cost;
    private ClineApiReqCancelReason cancelReason;
    private String streamingFailedMessage;
    private RetryStatus retryStatus;

    @Data
    public static class RetryStatus {
        private Integer attempt;
        private Integer maxAttempts;
        private Integer delaySec;
        private String errorSnippet;
    }

    @JsonProperty(access = READ_ONLY)
    public Integer getTotalTokens() {
        return (tokensIn != null ? tokensIn : 0)
                + (tokensOut != null ? tokensOut : 0)
                + (cacheWrites != null ? cacheWrites : 0)
                + (cacheReads != null ? cacheReads : 0);
    }
}
