package com.hhoa.kline.core.core.locks;

import lombok.Builder;
import lombok.Data;

/** 文件夹锁重试配置。 */
@Data
@Builder
public class FolderLockRetryConfig {
    private int initialDelayMs;
    private int incrementPerAttemptMs;
    private int maxTotalTimeoutMs;

    /**
     * 默认配置：
     *
     * <ul>
     *   <li>500ms 初始等待
     *   <li>每次重试增加 1000ms
     *   <li>最大 30s 超时
     * </ul>
     */
    public static FolderLockRetryConfig defaultConfig() {
        return FolderLockRetryConfig.builder()
                .initialDelayMs(500)
                .incrementPerAttemptMs(1000)
                .maxTotalTimeoutMs(30000)
                .build();
    }
}
